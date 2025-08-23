// src/components/MapRightPanel.tsx
import React from 'react'

export type Cluster = {
  name: string
  code: string
  lat: number
  lng: number
  count: number
  level: 'sig' | 'emd'
}

type Props = {
  open: boolean
  onClose: () => void

  step: 'sig' | 'emd' | 'category'

  // Step 1: 시·군·구
  sigRegions: Cluster[]
  onSelectSig: (c: Cluster) => void

  // Step 2: 읍·면·동
  selectedSig: Cluster | null
  emdRegions: Cluster[]
  onBackToSig: () => void
  onSelectEmd: (c: Cluster) => void

  // Step 3: 카테고리
  selectedEmd: { name: string; code: string } | null
  bizCounts: Record<string, number> | null
  listings: any[]
  onBackToEmd: () => void
}

export default function MapRightPanel({
  open,
  onClose,
  step,

  sigRegions,
  onSelectSig,

  selectedSig,
  emdRegions,
  onBackToSig,
  onSelectEmd,

  selectedEmd,
  bizCounts,
  listings,
  onBackToEmd,
}: Props) {
  return (
    <>
      <div
        className={`fixed inset-0 bg-black/10 transition-opacity ${
          open ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
        onClick={onClose}
        style={{ zIndex: 39 }}
      />
      <aside
        className={`fixed top-0 right-0 h-full w-[360px] bg-white border-l shadow-xl transform transition-transform ${
          open ? 'translate-x-0' : 'translate-x-full'
        }`}
        style={{ zIndex: 40 }}
      >
        <header className="h-12 px-4 flex items-center justify-between border-b">
          <div className="font-semibold">
            {step === 'sig' && '지역 집계 · 시·군·구'}
            {step === 'emd' && '지역 집계 · 읍·면·동'}
            {step === 'category' && '카테고리 집계'}
          </div>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-700">✕</button>
        </header>

        {/* Step 1: 시·군·구 */}
        {step === 'sig' && (
          <div className="p-4 space-y-2 overflow-y-auto h-[calc(100%-48px)]">
            <div className="text-xs text-gray-500 mb-1">기준: 충남 · 1) 시·군·구 → 2) 읍·면·동</div>
            {sigRegions.length ? sigRegions.map((r) => (
              <button
                key={r.code}
                onClick={() => onSelectSig(r)}
                className="w-full flex items-center justify-between px-3 py-3 rounded-xl border hover:bg-gray-50"
              >
                <div className="text-sm">{r.name}</div>
                <span className="text-xs text-gray-600 bg-gray-100 px-2 py-0.5 rounded-full">{r.count}</span>
              </button>
            )) : <div className="text-sm text-gray-500 py-8 text-center">표시할 지역이 없습니다.</div>}
          </div>
        )}

        {/* Step 2: 읍·면·동 */}
        {step === 'emd' && (
          <div className="p-4 space-y-2 overflow-y-auto h-[calc(100%-48px)]">
            <div className="flex items-center justify-between">
              <div className="text-sm">
                <span className="text-gray-500">상위 지역</span>{' '}
                <strong>{selectedSig?.name ?? '-'}</strong>
              </div>
              <button onClick={onBackToSig} className="text-xs text-blue-600 hover:underline">← 시·군·구로</button>
            </div>
            <div className="text-xs text-gray-500 mb-1">2) 읍·면·동 선택</div>
            {emdRegions.length ? emdRegions.map((r) => (
              <button
                key={r.code}
                onClick={() => onSelectEmd(r)}
                className="w-full flex items-center justify-between px-3 py-3 rounded-xl border hover:bg-gray-50"
              >
                <div className="text-sm">{r.name}</div>
                <span className="text-xs text-gray-600 bg-gray-100 px-2 py-0.5 rounded-full">{r.count}</span>
              </button>
            )) : <div className="text-sm text-gray-500 py-8 text-center">읍·면·동이 없습니다.</div>}
          </div>
        )}

        {/* Step 3: 카테고리 */}
        {step === 'category' && (
          <div className="p-4 space-y-4 overflow-y-auto h-[calc(100%-48px)]">
            <div className="flex items-center justify-between">
              <div className="text-sm">
                <span className="text-gray-500">선택 지역</span>{' '}
                <strong>{selectedEmd?.name ?? '-'}</strong>
              </div>
              <button onClick={onBackToEmd} className="text-xs text-blue-600 hover:underline">← 읍·면·동으로</button>
            </div>

            <section>
              <div className="text-xs text-gray-500 mb-2">카테고리 집계</div>
              <div className="space-y-2">
                {bizCounts && Object.keys(bizCounts).length ? (
                  Object.entries(bizCounts).map(([k, v]) => (
                    <div key={k} className="flex items-center justify-between px-3 py-2 rounded-xl border">
                      <span className="text-sm">{k}</span>
                      <span className="text-xs text-gray-600 bg-gray-100 px-2 py-0.5 rounded-full">{v}</span>
                    </div>
                  ))
                ) : (
                  <div className="text-sm text-gray-500 py-6 text-center">집계가 없습니다.</div>
                )}
              </div>
            </section>

            {!!listings?.length && (
              <section>
                <div className="text-xs text-gray-500 mb-2">매물 미리보기</div>
                <div className="space-y-2">
                  {listings.slice(0, 8).map((l: any) => (
                    <div key={l.id} className="px-3 py-2 rounded-xl border">
                      <div className="text-sm font-medium truncate">{l.title || l.name}</div>
                      <div className="text-xs text-gray-500 truncate">{l.address}</div>
                    </div>
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
      </aside>
    </>
  )
}
