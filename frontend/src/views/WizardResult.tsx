// src/views/WizardResult.tsx
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import Card from '../components/Card'
import { api, aiSimulate, aiMarketAnalysis } from '../services/api'

type Params = {
  location: string
  industries: string[]
  budget: string | null
  experience: string | null
  target?: string
  analyses: string[]
}

type Result = {
  summary?: string
  recommendedCategories?: string[]
  hotZones?: { name: string; score: number }[]
  estimatedSales?: number
  bepSales?: number
  insights?: string[]
}

export default function WizardResult() {
  const nav = useNavigate()
  const { state } = useLocation() as {
    state?:
      | Params
      | { params?: Params; result?: Result }
      | undefined
  }

  const [loading, setLoading] = useState(true)
  const [params, setParams] = useState<Params | null>(null)
  const [res, setRes] = useState<Result | null>(null)
  const [error, setError] = useState<string | null>(null)

  // 상태/세션에서 파라미터 복구
  useEffect(() => {
    let p: Params | null = null
    let r: Result | null = null

    if (state) {
      if ('location' in (state as any)) {
        // Wizard가 직접 Params를 넘긴 경우
        p = state as Params
      } else {
        // Wizard가 { params, result }로 넘긴 경우
        const box = state as { params?: Params; result?: Result }
        p = box.params ?? null
        r = box.result ?? null
      }
    }

    // 새로고침 폴백
    if (!p) {
      try {
        const sp = sessionStorage.getItem('wizard.params')
        if (sp) p = JSON.parse(sp) as Params
        const sr = sessionStorage.getItem('wizard.result')
        if (sr) r = JSON.parse(sr) as Result
      } catch {/* ignore */}
    }

    if (!p) {
      nav('/wizard', { replace: true })
      return
    }

    setParams(p)
    if (r) {
      setRes(r)
      setLoading(false)
      return
    }

    // 결과 없으면 서버 호출
    ;(async () => {
      setLoading(true); setError(null)
      try {
        // 1) 정식 백엔드 우선
        const r1 = await aiMarketAnalysis(p).catch(async () => {
          // 2) 폴백: 기존 시뮬레이터
          const sim = await aiSimulate({
            region: p.location,
            category: p.industries?.[0] || '일반',
            area: 60,
            rent: 150,
          })
          const fallback: Result = {
            summary: `${p.location}에서 ${p.industries?.[0] || '선호 업종'} 중심의 창업 적합도 요약입니다.`,
            recommendedCategories: [sim.recommendedCategory],
            estimatedSales: sim.estimatedSales,
            bepSales: sim.bepSales,
            hotZones: [{ name: p.location, score: 84 }],
            insights: [
              '반경 800m 내 유사업종 밀집도가 보통입니다.',
              '임대료 대비 매출잠재력이 양호합니다.',
              '가시성 좋은 1층 코너 자리를 우선 검토하세요.',
            ],
          }
          return fallback
        })
        setRes(r1)
        // 결과도 세션에 저장(새로고침 대비)
        sessionStorage.setItem('wizard.params', JSON.stringify(p))
        sessionStorage.setItem('wizard.result', JSON.stringify(r1))
      } catch {
        setError('AI 분석을 불러오지 못했습니다.')
      } finally {
        setLoading(false)
      }
    })()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state, nav])

  if (!params) return null

  return (
    <div className="mx-auto px-4 w-[94vw] md:w-[80vw] xl:max-w-[980px] py-6 space-y-6">
      <div className="flex items-start gap-3">
        <div>
          <h1 className="text-2xl font-bold">AI 상권분석 결과</h1>
          <div className="text-sm text-gray-500 mt-1">
            지역: <b>{params.location}</b> · 업종:{' '}
            <b>{(params.industries ?? []).join(', ') || '-'}</b>
          </div>
        </div>
        <button
          className="ml-auto btn-outline"
          onClick={() => {
            // 입력 다시 시 시작 전 값 복구를 위해 파라미터만 남김
            sessionStorage.setItem('wizard.params', JSON.stringify(params))
            sessionStorage.removeItem('wizard.result')
            nav('/wizard')
          }}
        >
          다시 입력
        </button>
      </div>

      {loading && <div className="text-gray-500 text-sm">AI가 분석 중…</div>}
      {error && <div className="text-rose-600 text-sm">{error}</div>}

      {!loading && !error && res && (
        <>
          {/* 요약 */}
          <Card>
            <div className="font-semibold mb-2">핵심 요약</div>
            <p className="text-[15px] leading-relaxed whitespace-pre-wrap">
              {res.summary || '해당 지역과 업종에 대한 요약 결과입니다.'}
            </p>
          </Card>

          {/* 추천 업종 & 매출 */}
          <div className="grid md:grid-cols-2 gap-4">
            <Card>
              <div className="font-semibold mb-2">추천 업종</div>
              <div className="flex gap-2 flex-wrap">
                {(res.recommendedCategories || []).map((c) => (
                  <span key={c} className="chip">{c}</span>
                ))}
                {(!res.recommendedCategories || res.recommendedCategories.length === 0) && (
                  <div className="text-sm text-gray-500">추천 업종 데이터가 없습니다.</div>
                )}
              </div>
            </Card>
            <Card>
              <div className="font-semibold mb-2">매출 전망</div>
              <div className="text-[15px]">
                <div>예상 월매출: <b>{res.estimatedSales?.toLocaleString() ?? '-'} 만원</b></div>
                <div>BEP 매출: <b>{res.bepSales?.toLocaleString() ?? '-'} 만원</b></div>
              </div>
            </Card>
          </div>

          {/* 유망 구역 */}
          <Card>
            <div className="font-semibold mb-2">유망 구역</div>
            <div className="grid md:grid-cols-2 gap-2">
              {(res.hotZones || []).map((z) => (
                <div key={z.name} className="rounded-xl border p-3 flex items-center justify-between">
                  <div className="text-sm">{z.name}</div>
                  <div className="text-sm font-semibold">점수 {z.score}</div>
                </div>
              ))}
              {(!res.hotZones || res.hotZones.length === 0) && (
                <div className="text-sm text-gray-500">유망 구역 데이터가 없습니다.</div>
              )}
            </div>
          </Card>

          {/* 인사이트 */}
          <Card>
            <div className="font-semibold mb-2">인사이트</div>
            <ul className="list-disc pl-5 text-[15px] space-y-1">
              {(res.insights || []).map((t, i) => (<li key={i}>{t}</li>))}
              {(!res.insights || res.insights.length === 0) && (
                <li className="text-gray-500 list-none">추가 인사이트가 없습니다.</li>
              )}
            </ul>
          </Card>
        </>
      )}
    </div>
  )
}
