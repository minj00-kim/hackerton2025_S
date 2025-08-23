// src/views/Account.tsx
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import Card from '../components/Card'
import { useStore } from '../store/useStore'
import { getListings, getMyListings } from '../services/api'
import type { Listing } from '../services/api'
import { ensureAuthKey, maskKey } from '../lib/auth'
import { listFavIds } from '../lib/fav'

function FavoriteGrid({ items }: { items: Listing[] }) {
  if (!items.length) {
    return <div className="text-sm text-gray-500">아직 즐겨찾기한 매물이 없습니다.</div>
  }
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {items.map(l => (
        <Link
          key={l.id}
          to={`/listings/${l.id}`}
          className="block overflow-hidden rounded-2xl border bg-white hover:shadow-md transition"
        >
          {l.images?.[0] && (
            <img src={l.images[0]} alt={l.title} className="w-full h-44 object-cover" />
          )}
          <div className="p-3">
            <div className="font-semibold line-clamp-1">{l.title}</div>
            <div className="text-xs text-gray-500 line-clamp-1">{l.address}</div>
            <div className="mt-1 text-sm">
              {l.type === 'MONTHLY'
                ? <>보증금 {l.deposit?.toLocaleString()} / 월세 {l.rentMonthly?.toLocaleString()} 만원</>
                : (l.type === 'SALE'
                    ? <>매매가 {l.price?.toLocaleString()} 만원</>
                    : <>전세 {l.price?.toLocaleString()} 만원</>)}
              {typeof l.area === 'number' && <span className="text-gray-500"> · {l.area}㎡</span>}
            </div>
          </div>
        </Link>
      ))}
    </div>
  )
}

function MineGrid({ items }: { items: Listing[] }) {
  if (!items.length) {
    return <div className="text-sm text-gray-500">내가 등록한 매물이 없습니다.</div>
  }
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {items.map(l => (
        <Link
          key={l.id}
          to={`/listings/${l.id}`}
          className="block overflow-hidden rounded-2xl border bg-white hover:shadow-md transition"
        >
          {l.images?.[0] && (
            <img src={l.images[0]} alt={l.title} className="w-full h-44 object-cover" />
          )}
          <div className="p-3">
            <div className="font-semibold line-clamp-1">{l.title}</div>
            <div className="text-xs text-gray-500 line-clamp-1">{l.address}</div>
            <div className="mt-1 text-sm">
              {l.type === 'MONTHLY'
                ? <>보증금 {l.deposit?.toLocaleString()} / 월세 {l.rentMonthly?.toLocaleString()} 만원</>
                : (l.type === 'SALE'
                    ? <>매매가 {l.price?.toLocaleString()} 만원</>
                    : <>전세 {l.price?.toLocaleString()} 만원</>)}
              {typeof l.area === 'number' && <span className="text-gray-500"> · {l.area}㎡</span>}
            </div>
          </div>
        </Link>
      ))}
    </div>
  )
}

export default function Account() {
  const saved = useStore(s => s.saved)

  const authKey = useMemo(() => ensureAuthKey(), [])
  const [fav, setFav] = useState<Listing[]>([])
  const [mine, setMine] = useState<Listing[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    ;(async () => {
      setLoading(true)
      try {
        // 1) 즐겨찾기 ID들 (쿠키키 기반)
        const favIds = listFavIds(authKey)
        let favItems: Listing[] = []
        if (favIds.length) {
          // 서버가 ids 필터를 지원한다면 우선 시도
          let byIds: Listing[] | null = null
          try {
            const r = await getListings({ ids: favIds.join(',') })
            if (Array.isArray(r)) byIds = r
          } catch { /* ignore */ }

          if (byIds && byIds.length) {
            favItems = byIds
          } else {
            // 폴백: 전체 목록 받아서 필터
            const all = await getListings().catch(() => []) as Listing[]
            const setIds = new Set(favIds.map(String))
            favItems = all.filter(l => setIds.has(String(l.id)))
          }
        }

        // 2) 내가 등록한 매물 (오너키 기반)
        const mineResp = await getMyListings(authKey).catch(() => ({ items: [] as Listing[] }))

        if (!mounted) return
        setFav(favItems)
        setMine(mineResp.items || [])
      } finally {
        if (mounted) setLoading(false)
      }
    })()
    return () => { mounted = false }
  }, [authKey])

  return (
    // ✅ 컨테이너 폭 제한 (다른 페이지와 동일: 약 900px)
    <div className="mx-auto px-4 w-[94vw] md:w-[80vw] xl:max-w-[1050px]">
      <div className="space-y-6 pt-4">
        {/* 내 키 안내 */}
        <Card>
          <div className="flex items-center gap-2">
            <div className="font-semibold">내 식별 키 (쿠키)</div>
            <code className="px-2 py-1 bg-slate-100 rounded text-xs">{maskKey(authKey)}</code>
            <button
              className="btn-outline ml-auto"
              onClick={() => navigator.clipboard.writeText(authKey)}
              title="전체 키 복사"
            >
              키 복사
            </button>
          </div>
          <p className="mt-2 text-sm text-gray-500">
            로그인 없이도 이 키로 <b>매물 등록/수정/삭제</b>, <b>리뷰 수정/삭제</b>, <b>즐겨찾기</b>를 관리합니다.
            브라우저/기기를 바꾸면 키가 달라지니 주의하세요.
          </p>
        </Card>

        {/* 즐겨찾기한 매물 */}
        <Card>
          <div className="font-semibold mb-3">즐겨찾기한 매물</div>
          {loading ? <div className="text-sm text-gray-500">불러오는 중…</div> : <FavoriteGrid items={fav} />}
        </Card>

        {/* 내가 등록한 매물 */}
        <Card>
          <div className="font-semibold mb-3">내가 등록한 매물</div>
          {loading ? <div className="text-sm text-gray-500">불러오는 중…</div> : <MineGrid items={mine} />}
        </Card>

        {/* 저장한 추천 결과 (기존 섹션 유지) */}
        <Card>
          <div className="font-semibold mb-2">저장한 추천 결과</div>
          <ul className="text-sm list-disc pl-5 space-y-2">
            {saved.length === 0 && (
              <li className="list-none text-gray-500">저장된 항목이 없습니다.</li>
            )}
            {saved.map((x: any, i: number) => (
              <li key={i}>
                추천 {new Date(x.ts).toLocaleString()} — 상가 {x.result.listings.length}건 / 업종 {x.result.categories.length}개
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </div>
  )
}
