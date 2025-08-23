// src/components/RadiusStats.tsx
import { useEffect, useMemo, useState } from 'react'
import { api } from '../services/api'

type Props = {
  lat: number
  lng: number
  /** 최초 반경(m). 기본 800 */
  defaultRadius?: number
  /** 최대 반경(m). 기본 800 */
  maxRadius?: number
  /** 표기할 카테고리 목록 (백엔드 분류명과 일치) */
  categories?: string[]
}

const DEFAULT_CATEGORIES = [
  '카페/디저트','식당','주점/호프','편의','패션/액세서리',
  '뷰티/미용','의료/약국','문화/취미','레저/스포츠','사무/공유오피스',
  '숙박','창고/물류','팝업/쇼룸','기타'
]

export default function RadiusStats({
  lat, lng,
  defaultRadius = 800,
  maxRadius = 800,
  categories = DEFAULT_CATEGORIES,
}: Props) {
  const [radius, setRadius] = useState<number>(Math.min(defaultRadius, maxRadius))
  const [loading, setLoading] = useState(false)
  const [counts, setCounts] = useState<Record<string, number>>({})

  const total = useMemo(
    () => categories.reduce((s, c) => s + (counts[c] ?? 0), 0),
    [counts, categories]
  )

  useEffect(() => {
    let alive = true
    ;(async () => {
      setLoading(true)
      try {
        // 백엔드가 다양한 응답 형태를 줄 수 있어 유연하게 처리
        // 1) { counts: { 카테고리: 숫자, ... } }
        // 2) [{ category: '식당', count: 12 }, ...]
        // 3) 키가 영어일 경우도 있으니 서버에서 한글로 맞춰 내려오는 게 이상적
        const r = await api.get('/stats/nearby', {
          params: { lat, lng, radius, categories: categories.join(',') },
        })
        const data = r?.data
        let map: Record<string, number> = {}
        if (data && typeof data === 'object') {
          if (Array.isArray(data)) {
            data.forEach((row: any) => { map[row.category] = Number(row.count) || 0 })
          } else if (Array.isArray(data.items)) {
            data.items.forEach((row: any) => { map[row.category] = Number(row.count) || 0 })
          } else if (data.counts && typeof data.counts === 'object') {
            map = Object.fromEntries(Object.entries(data.counts).map(([k, v]) => [k, Number(v) || 0]))
          }
        }
        if (alive) setCounts(map)
      } catch {
        if (alive) setCounts({})
      } finally {
        if (alive) setLoading(false)
      }
    })()
    return () => { alive = false }
  }, [lat, lng, radius, categories])

  return (
    <div className="rounded-2xl border bg-white p-3">
      <div className="flex items-center justify-between mb-2">
        <div className="font-semibold">주변 업소 집계</div>
        <div className="text-sm text-gray-600">반경 {radius}m</div>
      </div>

      {/* 반경 슬라이더: 최대 800m */}
      <div className="px-1 py-2">
        <input
          type="range"
          min={100}
          max={maxRadius}
          step={50}
          value={radius}
          onChange={e => setRadius(Number(e.target.value))}
          className="w-full"
        />
      </div>

      <div className="grid gap-2 sm:grid-cols-2">
        {categories.map(cat => (
          <div
            key={cat}
            className="flex items-center justify-between rounded-xl border px-3 py-2 bg-white"
          >
            <span className="text-sm">{cat}</span>
            <span className="text-sm font-semibold">{counts[cat] ?? 0}</span>
          </div>
        ))}
        {loading && (
          <div className="text-sm text-gray-500 col-span-full">불러오는 중…</div>
        )}
        {(!loading && total === 0) && (
          <div className="text-sm text-gray-500 col-span-full">데이터가 없습니다.</div>
        )}
      </div>
    </div>
  )
}
