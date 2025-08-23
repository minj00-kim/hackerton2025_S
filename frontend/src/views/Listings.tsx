// src/views/Listings.tsx
import SearchHero from '../components/SearchHero';
import CategoryGrid from '../components/CategoryGrid';
import { useEffect, useState } from 'react';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import { getListings, type Listing } from '../services/api';

type DealType = 'all' | 'sale' | 'jeonse' | 'wolse';
type SortKey = 'price' | 'area' | 'popular';

const DEALS: { key: DealType; label: string }[] = [
  { key: 'all',    label: '전체' },
  { key: 'sale',   label: '매매' },
  { key: 'jeonse', label: '전세' },
  { key: 'wolse',  label: '월세' },
];

const SORTS: { key: SortKey; label: string }[] = [
  { key: 'price',   label: '가격순' },
  { key: 'area',    label: '면적순' },
  { key: 'popular', label: '인기순' }, // 조회수+즐겨찾기 수
];

// 인기 점수(프론트 보조용): 백엔드가 없을 때 작동
function popularityScore(l: Listing) {
  // 다양한 후보 키를 포괄
  const views =
    (l as any).views ??
    (l as any).viewCount ??
    (l as any).hits ??
    0;
  const favs =
    (l as any).favCount ??
    (l as any).favoriteCount ??
    (l as any).favorites ??
    (l as any).likes ??
    0;
  // 즐겨찾기에 가중치 부여(필요 시 조절)
  return Number(views) + Number(favs) * 20;
}

// 프론트 보조 정렬(백엔드 미연동 시 안전망)
function localSort(list: Listing[], sort: SortKey) {
  const byPriceVal = (l: Listing) => (l.rent && l.rent > 0 ? l.rent : (l.deposit ?? 0));
  switch (sort) {
    case 'price':   return [...list].sort((a,b) => byPriceVal(a) - byPriceVal(b));
    case 'area':    return [...list].sort((a,b) => (b.area ?? 0) - (a.area ?? 0));
    case 'popular': return [...list].sort((a,b) => popularityScore(b) - popularityScore(a));
    default:        return list;
  }
}

// 프론트 보조 “거래유형” 필터(데이터 모양에 따라 조정)
function localDealFilter(list: Listing[], deal: DealType) {
  if (deal === 'all') return list;
  return list.filter(l => {
    const rent = l.rent ?? 0;
    const deposit = l.deposit ?? 0;
    if (deal === 'wolse')  return rent > 0;                    // 월세
    if (deal === 'jeonse') return rent === 0 && deposit > 0;   // 전세
    if (deal === 'sale')   return rent === 0 && deposit === 0; // 매매(가정)
    return true;
  });
}

export default function Listings() {
  const location = useLocation();
  const [params] = useSearchParams();
  const q      = params.get('q')      ?? '';
  const region = params.get('region') ?? '';
  const type   = params.get('type')   ?? '';
  const theme  = params.get('theme')  ?? '';

  const [dealType, setDealType] = useState<DealType>('all');
  // ✅ 기본 정렬을 가격순으로
  const [sort, setSort]         = useState<SortKey>('price');

  const [loading, setLoading]   = useState(false);
  const [rows, setRows]         = useState<Listing[]>([]);

  async function load() {
    setLoading(true);
    try {
      // ✅ sort는 'price' | 'area' | 'popular' 그대로 백엔드로 전달
      const list = await getListings({ q, region, type, theme, dealType, sort });
      const filtered = localDealFilter(list, dealType);
      setRows(localSort(filtered, sort));
    } catch {
      setRows([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [dealType, sort, location.search]);
  useEffect(() => { load(); /* eslint-disable-line */ }, []);

  const empty = !loading && rows.length === 0;

  return (
    <>
      <div className="mx-auto px-4 w-[94vw] md:w-[80vw] xl:max-w-[1050px]">
        {/* 제목 */}
        <div className="pt-6 pb-3">
          <h1 className="text-2xl font-bold">공실 매물</h1>
        </div>

        {/* 검색창 */}
        <div className="mb-4">
          <SearchHero noBg />
        </div>

        {/* 카테고리 */}
        <div className="mb-4">
          <CategoryGrid />
        </div>

        {/* 거래유형 + 정렬 바 */}
        <div className="sticky top-[56px] z-10 bg-slate-50/80 backdrop-blur border-y py-2 mb-4">
          <div className="flex flex-col md:flex-row md:items-center gap-3 md:gap-6 mx-1">
            {/* 거래유형 라디오 */}
            <div className="flex items-center gap-3 flex-wrap">
              {DEALS.map(d => (
                <label key={d.key} className="inline-flex items-center gap-1.5 cursor-pointer">
                  <input
                    type="radio"
                    name="dealType"
                    value={d.key}
                    checked={dealType === d.key}
                    onChange={() => setDealType(d.key)}
                  />
                  <span className="text-sm">{d.label}</span>
                </label>
              ))}
            </div>

            {/* 정렬 토글: 가격순 · 면적순 · 인기순 (이 순서 고정) */}
            <div className="md:ml-auto flex items-center gap-4 flex-wrap">
              {SORTS.map(s => (
                <button
                  key={s.key}
                  type="button"
                  onClick={() => setSort(s.key)}
                  className={
                    'text-sm transition hover:opacity-80 ' +
                    (sort === s.key ? 'font-semibold text-brand-700' : 'text-gray-600')
                  }
                >
                  {sort === s.key ? '✓ ' : ''}{s.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* 리스트 */}
        {loading && <div className="text-gray-500 text-sm">불러오는 중…</div>}
        {empty &&   <div className="text-gray-500 text-sm">조건에 맞는 매물이 없습니다.</div>}

        <div className="space-y-4">
          {rows.map(item => (
            <Link
              to={`/listings/${item.id}`}
              key={item.id}
              className="block bg-white rounded-2xl overflow-hidden shadow-sm border hover:shadow-md transition"
            >
              {item.images?.length ? (
                <div className="h-[220px] w-full overflow-hidden bg-slate-100">
                  <img src={item.images[0]} alt={item.title} className="w-full h-full object-cover" />
                </div>
              ) : null}

              <div className="p-4">
                <div className="font-semibold">{item.title}</div>
                <div className="text-sm text-gray-600 mt-0.5">
                  {item.region} · {item.address}
                </div>

                <div className="text-sm mt-2 flex items-center gap-3">
                  {item.area ? <span>{item.area}㎡</span> : null}
                  <span className="text-gray-500">·</span>
                  <span>
                    {item.rent && item.rent > 0
                      ? `보증금 ${item.deposit?.toLocaleString() ?? 0}만 / 월세 ${item.rent.toLocaleString()}만`
                      : item.deposit
                      ? `전세 ${item.deposit.toLocaleString()}만`
                      : '매매/협의'}
                  </span>
                </div>

                {item.theme?.length ? (
                  <div className="mt-2 flex gap-2 flex-wrap">
                    {item.theme.slice(0, 4).map(t => (
                      <span key={t} className="px-2 py-0.5 rounded-full bg-slate-100 text-xs text-gray-700">
                        {t}
                      </span>
                    ))}
                  </div>
                ) : null}
              </div>
            </Link>
          ))}
        </div>
      </div>
    </>
  );
}

/* ---------- tailwind 헬퍼 ----------
.input { @apply w/full h-10 rounded-xl border px-3 text-sm bg-white; }
.btn-primary { @apply h-10 rounded-xl px-4 bg-brand-600 text-white text-sm font-semibold hover:bg-brand-700; }
.text-brand-700 { @apply text-blue-700; }
.bg-brand-600 { @apply bg-blue-600; }
.bg-brand-700 { @apply bg-blue-700; }
*/
