import { useEffect, useState } from 'react'
import { getRegionCountsByAdmin } from '../services/api'

type AdminLevel = 'sig' | 'emd'
type RegionItem = {
  code: string
  name: string
  count: number
  lat?: number
  lng?: number
  level: AdminLevel
}

interface Props {
  open: boolean
  onClose: () => void
  /** 지도의 포커스/패닝을 위해 클릭된 지역의 좌표/코드 전달 */
  onFocusRegion?: (r: RegionItem) => void
  /** 기본 광역: 충남 고정 (필요시 바꿀 수 있게 prop 노출) */
  province?: string
}

export default function RegionCountsPanel({
  open,
  onClose,
  province = '충남',
  onFocusRegion,
}: Props) {
  const [step, setStep] = useState<AdminLevel>('sig')
  const [sigList, setSigList] = useState<RegionItem[]>([])
  const [emdList, setEmdList] = useState<RegionItem[]>([])
  const [selectedSig, setSelectedSig] = useState<RegionItem | null>(null)
  const [loading, setLoading] = useState(false)

  // 1단계: 시군구 불러오기
  useEffect(() => {
    if (!open) return
    setStep('sig')
    setSelectedSig(null)
    setEmdList([])
    ;(async () => {
      try {
        setLoading(true)
        const rows = await getRegionCountsByAdmin({ province, level: 'sig' })
        setSigList(rows || [])
      } finally {
        setLoading(false)
      }
    })()
  }, [open, province])

  // 2단계: 읍면동 불러오기
  const enterEmd = async (sig: RegionItem) => {
    setSelectedSig(sig)
    setStep('emd')
    if (onFocusRegion && (sig.lat || sig.lng)) onFocusRegion(sig)
    try {
      setLoading(true)
      const rows = await getRegionCountsByAdmin({
        province,
        level: 'emd',
        parentCode: sig.code,
      })
      setEmdList(rows || [])
    } finally {
      setLoading(false)
    }
  }

  const Row = ({ item, onClick }: { item: RegionItem; onClick?: () => void }) => (
    <button
      onClick={onClick}
      className="w-full flex items-center justify-between px-4 py-3 rounded-xl border hover:bg-slate-50"
    >
      <span className="truncate">{item.name}</span>
      <span className="ml-3 shrink-0 inline-flex items-center justify-center min-w-[40px] h-7 px-2 rounded-full bg-slate-100 text-slate-700 text-sm">
        {item.count}
      </span>
    </button>
  )

  return (
    <aside
      className={[
        'fixed top-0 right-0 h-screen w-[340px] max-w-[85vw] bg-white shadow-2xl border-l z-30 transition-transform',
        open ? 'translate-x-0' : 'translate-x-full',
      ].join(' ')}
      aria-hidden={!open}
    >
      {/* 헤더 */}
      <div className="px-4 py-3 border-b flex items-center justify-between">
        <div className="font-semibold">지역 집계</div>
        <button
          onClick={onClose}
          className="rounded-full w-8 h-8 inline-flex items-center justify-center hover:bg-slate-100"
          aria-label="닫기"
        >
          ✕
        </button>
      </div>

      {/* 스텝 인디케이터 */}
      <div className="px-4 py-3 border-b">
        <div className="text-xs text-gray-500 mb-1">기준: {province}</div>
        <div className="flex items-center gap-2 text-sm">
          <span className={'px-2 py-1 rounded ' + (step === 'sig' ? 'bg-blue-50 text-blue-700' : 'bg-slate-100 text-slate-600')}>
            1) 시·군·구
          </span>
          <span className="text-slate-400">→</span>
          <span className={'px-2 py-1 rounded ' + (step === 'emd' ? 'bg-blue-50 text-blue-700' : 'bg-slate-100 text-slate-600')}>
            2) 읍·면·동
          </span>
        </div>
      </div>

      {/* 컨텐츠 */}
      <div className="p-4 overflow-y-auto h-[calc(100vh-120px)] space-y-3">
        {loading && <div className="text-sm text-gray-500">불러오는 중…</div>}

        {!loading && step === 'sig' && (
          <>
            {sigList.map((r) => (
              <Row key={r.code} item={r} onClick={() => enterEmd(r)} />
            ))}
            {!sigList.length && (
              <div className="text-sm text-gray-500">표시할 시·군·구가 없습니다.</div>
            )}
          </>
        )}

        {!loading && step === 'emd' && (
          <>
            <div className="flex items-center gap-2 mb-2">
              <button
                className="px-2 py-1 rounded border text-sm hover:bg-slate-50"
                onClick={() => {
                  setStep('sig')
                  setSelectedSig(null)
                  setEmdList([])
                }}
              >
                ← 뒤로
              </button>
              <div className="text-sm text-gray-700">
                <b>{selectedSig?.name}</b>의 읍·면·동
              </div>
            </div>

            {emdList.map((r) => (
              <Row
                key={r.code}
                item={r}
                onClick={() => {
                  if (onFocusRegion) onFocusRegion(r)
                }}
              />
            ))}
            {!emdList.length && (
              <div className="text-sm text-gray-500">표시할 읍·면·동이 없습니다.</div>
            )}
          </>
        )}
      </div>
    </aside>
  )
}
