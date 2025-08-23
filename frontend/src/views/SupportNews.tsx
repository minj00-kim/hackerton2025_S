import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getSupportNews, type SupportNews } from '../services/api'

const TAGS = ['전체', '청년창업', '지원금', '정책', '교육/멘토링'] as const

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString('ko-KR', {
      year: 'numeric', month: 'long', day: 'numeric', weekday: 'short',
    })
  } catch {
    return iso
  }
}

function NewsCard({ item }: { item: SupportNews }) {
  return (
    <a
      href={item.url}
      target="_blank"
      rel="noreferrer"
      className="card hover:shadow-md transition block"
    >
      {item.thumbnail ? (
        <img
          src={item.thumbnail}
          alt=""
          className="w-full h-40 object-cover rounded-t-2xl border-b"
        />
      ) : null}
      <div className="p-4">
        <div className="text-xs text-gray-500 flex items-center gap-2">
          <span className="font-medium">{item.source}</span>
          <span>·</span>
          <span>{formatDate(item.publishedAt)}</span>
        </div>
        <div className="mt-1 font-semibold line-clamp-2">{item.title}</div>
        <div className="mt-1 text-sm text-gray-700 line-clamp-2">{item.summary}</div>
        {item.tags?.length ? (
          <div className="mt-2 flex flex-wrap gap-1">
            {item.tags.map(t => (
              <span key={t} className="chip">{t}</span>
            ))}
          </div>
        ) : null}
      </div>
    </a>
  )
}

export default function SupportNews() {
  const [sp, setSp] = useSearchParams()
  const qParam = sp.get('q') ?? ''
  const tagParam = sp.get('tag') ?? '전체'

  const [q, setQ] = useState(qParam)
  const [tag, setTag] = useState<typeof TAGS[number]>(tagParam as any)
  const [items, setItems] = useState<SupportNews[]>([])
  const [page, setPage] = useState(1)
  const [nextPage, setNextPage] = useState<number | undefined>(undefined)
  const [loading, setLoading] = useState(false)
  const [firstLoaded, setFirstLoaded] = useState(false)

  // URL ↔ 상태 동기화
  useEffect(() => {
    const now = new URLSearchParams()
    if (q) now.set('q', q)
    if (tag && tag !== '전체') now.set('tag', tag)
    setSp(now, { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, tag])

  const refresh = async () => {
    setLoading(true)
    try {
      const r = await getSupportNews({ page: 1, pageSize: 12, q, tag: tag === '전체' ? undefined : tag })
      setItems(r.items || [])
      setPage(1)
      setNextPage(r.nextPage)
    } finally {
      setLoading(false)
      setFirstLoaded(true)
    }
  }

  const loadMore = async () => {
    if (!nextPage || loading) return
    setLoading(true)
    try {
      const r = await getSupportNews({ page: nextPage, pageSize: 12, q, tag: tag === '전체' ? undefined : tag })
      setItems(prev => [...prev, ...(r.items || [])])
      setPage(nextPage)
      setNextPage(r.nextPage)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { refresh() }, [/* 처음/필터 변경 시 */ tag, q]) // eslint-disable-line

  const empty = firstLoaded && !loading && items.length === 0

  return (
    <div className="mx-auto px-4 w-[94vw] md:w-[80vw] xl:max-w-[1050px] py-6">
      {/* 헤더 */}
      <div className="mb-3 flex items-center justify-between">
        <h1 className="text-xl font-semibold">창업 지원 · 뉴스</h1>
        <span className="text-xs text-gray-500">실시간 크롤링/백엔드 연동</span>
      </div>

      {/* 필터 바 */}
      <div className="card p-3 mb-4">
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex gap-1">
            {TAGS.map(t => (
              <button
                key={t}
                onClick={() => setTag(t)}
                className={`px-3 py-1.5 rounded-full text-sm border ${
                  tag === t ? 'bg-brand-600 text-white border-brand-600' : 'bg-white hover:bg-slate-50'
                }`}
              >
                {t}
              </button>
            ))}
          </div>
          <div className="ml-auto flex items-center gap-2">
            <input
              value={q}
              onChange={e => setQ(e.target.value)}
              placeholder="키워드: 청년·지원금·멘토링…"
              className="border rounded-xl px-3 py-2 w-64"
            />
            <button onClick={refresh} className="btn btn-outline">검색</button>
          </div>
        </div>
      </div>

      {/* 리스트 */}
      {empty ? (
        <div className="card p-6 text-center text-gray-600">조건에 맞는 기사가 없어요.</div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map(it => <NewsCard key={it.id} item={it} />)}
          {loading && Array.from({ length: 3 }).map((_, i) => (
            <div key={'sk'+i} className="card p-4 animate-pulse h-56" />
          ))}
        </div>
      )}

      {/* 더보기 */}
      <div className="flex justify-center mt-6 mb-10">
        {nextPage ? (
          <button onClick={loadMore} disabled={loading} className="btn btn-outline">
            {loading ? '불러오는 중…' : '더보기'}
          </button>
        ) : (
          firstLoaded && <div className="text-sm text-gray-500">마지막 기사까지 확인했어요.</div>
        )}
      </div>
    </div>
  )
}
