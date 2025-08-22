# -*- coding: utf-8 -*-
import argparse, os, json, datetime, urllib.parse, re, time
from urllib.parse import urlparse, urljoin, quote_plus

# ---- deps ----
try:
    import feedparser
except ImportError:
    raise SystemExit("feedparser 미설치: venv에서 'pip install feedparser' 실행")

try:
    import requests
except ImportError:
    raise SystemExit("requests 미설치: venv에서 'pip install requests' 실행")

try:
    from bs4 import BeautifulSoup
except Exception:
    BeautifulSoup = None  # og:image 크롤 옵션에서만 사용, 없어도 동작

# ---- const ----
KST = datetime.timezone(datetime.timedelta(hours=9))
UA = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0 Safari/537.36"
    ),
    "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
}

def now_kst():
    return datetime.datetime.now(KST)

# ---------------- feed load ----------------
def load_feed(url: str):
    """requests로 받아 feedparser에 넘김(UA/timeout 설정 + 디버그)"""
    try:
        r = requests.get(url, headers=UA, timeout=float(os.environ.get("CRAWL_TIMEOUT", "10")))
        r.raise_for_status()
        f = feedparser.parse(r.content)
        entries_len = len(getattr(f, "entries", []))
        print(f"[DEBUG] GET {url} -> {r.status_code}, entries={entries_len}")
        if getattr(f, "bozo", False):
            print(f"[WARN] feed.bozo={f.bozo}; {getattr(f,'bozo_exception',None)}")
        return f
    except Exception as e:
        print(f"[ERROR] load_feed failed: {e}")
        return feedparser.parse(b"")

# ---------------- image helpers ----------------
def first_image_url(e):
    """RSS entry에서 가장 그럴듯한 이미지 URL 하나 추출"""
    # 1) <media:content url="...">
    for mc in (getattr(e, "media_content", []) or []):
        u = mc.get("url")
        if u:
            return u

    # 2) <media:thumbnail url="...">
    for mt in (getattr(e, "media_thumbnail", []) or []):
        u = mt.get("url")
        if u:
            return u

    # 3) enclosure 링크
    for lk in (getattr(e, "links", []) or []):
        if (lk.get("rel") == "enclosure") and ("image" in (lk.get("type") or "")):
            u = lk.get("href")
            if u:
                return u

    # 4) summary(HTML)에 <img src="...">
    raw = getattr(e, "summary", "") or ""
    m = re.search(r'<img[^>]+src=["\'](.*?)["\']', raw, re.I)
    if m:
        return m.group(1)

    return None

def favicon_url(u: str) -> str | None:
    """최후의 보루: 도메인 파비콘"""
    try:
        netloc = urlparse(u).netloc
        if not netloc:
            return None
        return f"https://www.google.com/s2/favicons?sz=128&domain={netloc}"
    except Exception:
        return None

def extract_from_json_ld(soup, base_url: str) -> str | None:
    """<script type='application/ld+json'> 에서 image / thumbnailUrl 추출"""
    def pick_image(obj):
        if isinstance(obj, list):
            for it in obj:
                u = pick_image(it)
                if u: return u
            return None
        if not isinstance(obj, dict):
            return None

        # image: str | dict{url} | [str|dict]
        img = obj.get("image")
        if isinstance(img, str):
            return urljoin(base_url, img)
        if isinstance(img, dict) and img.get("url"):
            return urljoin(base_url, img["url"])
        if isinstance(img, list):
            for it in img:
                if isinstance(it, str):
                    return urljoin(base_url, it)
                if isinstance(it, dict) and it.get("url"):
                    return urljoin(base_url, it["url"])

        # thumbnailUrl
        thumb = obj.get("thumbnailUrl")
        if isinstance(thumb, str):
            return urljoin(base_url, thumb)

        # @graph 안에 중첩된 경우
        if "@graph" in obj:
            return pick_image(obj["@graph"])

        return None

    for tag in soup.find_all("script", type="application/ld+json"):
        try:
            data = json.loads(tag.string or tag.get_text() or "")
        except Exception:
            continue
        u = pick_image(data)
        if u: return u
    return None

def fetch_og_image(url: str, timeout: int = 8) -> str | None:
    """
    필요할 때만 본문 페이지에서 og/twitter/json-ld 이미지 추출.
    CRAWL_FETCH_OG=1 이고 bs4 설치되어 있을 때만 실행.
    """
    flag = os.environ.get("CRAWL_FETCH_OG", "").lower() in ("1", "true", "yes")
    if not flag or BeautifulSoup is None:
        return None
    try:
        r = requests.get(url, headers=UA, timeout=timeout, allow_redirects=True)
        r.raise_for_status()
        base = r.url  # 리다이렉트 최종 URL 기준으로 상대경로 보정
        soup = BeautifulSoup(r.text, "html.parser")

        # 1) og:image / og:image:secure_url
        m = soup.find("meta", property="og:image") or soup.find("meta", property="og:image:secure_url") \
            or soup.find("meta", attrs={"name": "og:image"})
        if m and m.get("content"):
            return urljoin(base, m["content"].strip())

        # 2) twitter:image
        tw = soup.find("meta", attrs={"name": "twitter:image"}) or soup.find("meta", property="twitter:image")
        if tw and tw.get("content"):
            return urljoin(base, tw["content"].strip())

        # 3) JSON-LD
        jl = extract_from_json_ld(soup, base)
        if jl:
            return jl

    except Exception as e:
        print(f"[OG] skip {url}: {e}")
    return None

# ---------------- per-query parse ----------------
def parse_feed(q: str, limit: int):
    """Google News RSS에서 q 키워드로 최대 limit개 파싱"""
    q_enc = quote_plus(q)  # 공백은 + 로
    rss = f"https://news.google.com/rss/search?q={q_enc}&hl=ko&gl=KR&ceid=KR:ko"
    feed = load_feed(rss)

    out = []
    now = now_kst()

    for e in feed.entries:
        title = getattr(e, "title", "") or ""
        link = getattr(e, "link", "") or ""
        source = getattr(getattr(e, "source", None), "title", "GoogleNews")

        if hasattr(e, "published_parsed") and e.published_parsed:
            dt = datetime.datetime(*e.published_parsed[:6], tzinfo=datetime.timezone.utc).astimezone(KST)
        else:
            dt = now

        raw_summary = getattr(e, "summary", "") or ""
        clean_summary = re.sub(r"<[^>]+>", "", raw_summary).strip()
        tags = [t.term for t in getattr(e, "tags", []) if hasattr(t, "term")]

        # 최신일수록 높은 점수(0~1)
        age_days = max(0.0, (now - dt).total_seconds() / 86400.0)
        score = round(max(0.0, 1.0 - age_days / 7.0), 3)

        # 이미지: RSS → (옵션) og/twitter/json-ld → (최후) 파비콘
        img = first_image_url(e)
        if not img:
            img = fetch_og_image(link)
        if not img and os.environ.get("CRAWL_FAVICON_FALLBACK", "1") in ("1","true","yes"):
            img = favicon_url(link)

        out.append({
            "title": title,
            "url": link,
            "source": source,
            "publishedAt": dt.isoformat(),
            "summary": clean_summary,
            "score": score,
            "tags": tags + [q],   # 쿼리도 태그에 포함
            "query": q,
            "imageUrl": img,      # ← 프론트에서 사용
        })

        if len(out) >= limit:
            break

        # 너무 빠르게 여러 사이트를 두드리지 않도록 살짝 쉬어주기
        time.sleep(float(os.environ.get("CRAWL_SLEEP_MS", "50")) / 1000.0)

    return out

# ---------------- args ----------------
parser = argparse.ArgumentParser()
parser.add_argument("--query", action="append", help="검색어 (여러 번 지정 가능)")
parser.add_argument("--max-items", type=int, default=20, help="총 수집 개수(전체 합)")
args = parser.parse_args()

queries = args.query if args.query else ["부동산 정책"]
total_limit = max(1, args.max_items)

# ---------------- 출력 경로 ----------------
DEFAULT_BASE = os.path.join(os.getcwd(), "data", "crawl", "outputs")
BASE_OUT = os.environ.get("CRAWL_BASE_OUT", DEFAULT_BASE)

ts = now_kst().strftime("%Y%m%d_%H%M%S")
outdir = os.path.join(BASE_OUT, ts)
os.makedirs(outdir, exist_ok=True)

# ---------------- 수집/머지/중복제거 ----------------
items = []
seen = set()  # (title, url)

per_query = max(1, total_limit // len(queries))

for q in queries:
    chunk = parse_feed(q, per_query)
    for it in chunk:
        key = (it["title"], it["url"])
        if key in seen:
            continue
        seen.add(key)
        items.append(it)
        if len(items) >= total_limit:
            break
    if len(items) >= total_limit:
        break

# 부족하면 남은 수만큼 보강
if len(items) < total_limit:
    for q in queries:
        need = total_limit - len(items)
        if need <= 0:
            break
        more = parse_feed(q, need)
        for it in more:
            key = (it["title"], it["url"])
            if key in seen:
                continue
            seen.add(key)
            items.append(it)
            if len(items) >= total_limit:
                break

# ---------------- 정렬(점수 desc -> 발행일 desc -> 제목 asc) ----------------
def sort_key(it):
    try:
        dt = datetime.datetime.fromisoformat(it.get("publishedAt") or "")
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=KST)
        ts_val = dt.timestamp()
    except Exception:
        ts_val = 0.0
    return (-float(it.get("score") or 0.0), -ts_val, it.get("title") or "")

items.sort(key=sort_key)
items = items[:total_limit]

# ---------------- 저장 ----------------
out_path = os.path.join(outdir, "youth_grants_extracted.json")  # 백엔드와 호환
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(items, f, ensure_ascii=False, indent=2)

print(f"Wrote {len(items)} items to {out_path}")
