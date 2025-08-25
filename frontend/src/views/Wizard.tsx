// src/views/Wizard.tsx
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { aiMarketAnalysis, type MarketAnalysisRequest } from '../services/api'

/* ───────── 2D Blue Icons (inline SVG) ───────── */
function Icon({ name, className = 'w-5 h-5' }: { name:
  'location' | 'compass' | 'money' | 'briefcase' | 'target' | 'chart'; className?: string }) {
  const common = 'stroke-current text-blue-600'
  switch (name) {
    case 'location':
      return (
        <svg viewBox="0 0 24 24" fill="none" className={`${className} ${common}`} strokeWidth="1.8">
          <path d="M12 21s-6-5.2-6-10a6 6 0 1 1 12 0c0 4.8-6 10-6 10z" />
          <circle cx="12" cy="11" r="2.5" />
        </svg>
      )
    case 'compass':
      return (
        <svg viewBox="0 0 24 24" fill="none" className={`${className} ${common}`} strokeWidth="1.8">
          <circle cx="12" cy="12" r="9" />
          <path d="M14.8 9.2l-2.9 6-6 2.9 2.9-6 6-2.9z" />
        </svg>
      )
    case 'money':
      return (
        <svg viewBox="0 0 24 24" fill="none" className={`${className} ${common}`} strokeWidth="1.8">
          <rect x="3" y="7" width="18" height="10" rx="2" />
          <circle cx="12" cy="12" r="2.5" />
          <path d="M6 10h1M17 14h1" />
        </svg>
      )
    case 'briefcase':
      return (
        <svg viewBox="0 0 24 24" fill="none" className={`${className} ${common}`} strokeWidth="1.8">
          <rect x="3" y="8" width="18" height="11" rx="2" />
          <path d="M9 8V6a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2" />
          <path d="M3 12h18" />
        </svg>
      )
    case 'target':
      return (
        <svg viewBox="0 0 24 24" fill="none" className={`${className} ${common}`} strokeWidth="1.8">
          <circle cx="12" cy="12" r="8" />
          <circle cx="12" cy="12" r="4" />
          <circle cx="12" cy="12" r="1.2" fill="currentColor" stroke="none" />
        </svg>
      )
    case 'chart':
      return (
        <svg viewBox="0 0 24 24" fill="none" className={`${className} ${common}`} strokeWidth="1.8">
          <path d="M4 20h16" />
          <rect x="6" y="12" width="3" height="6" rx="1" />
          <rect x="11" y="9" width="3" height="9" rx="1" />
          <rect x="16" y="6" width="3" height="12" rx="1" />
        </svg>
      )
  }
}

/** 섹션 타이틀(아이콘 + 텍스트) */
function SectionTitle({ icon, children }:{
  icon: React.ComponentProps<typeof Icon>['name'],
  children: React.ReactNode
}) {
  return (
    <div className="flex items-center gap-2 mb-3 md:mb-4">
      <Icon name={icon} />
      <h2 className="font-semibold">{children}</h2>
    </div>
  )
}

/** 칩(토글) 버튼 */
function Chip({
  selected,
  onClick,
  children,
}: {
  selected?: boolean
  onClick?: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        'h-10 px-4 rounded-xl border text-sm transition',
        selected
          ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
          : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50',
      ].join(' ')}
    >
      {children}
    </button>
  )
}

const INDUSTRY_OPTIONS = [
  '카페/베이커리','음식점/주점','소매/편의점','미용/헬스케어',
  '의료/약국','교육/학원','부동산/금융','서비스업','기타',
]
const BUDGET_OPTIONS = [
  '1천만원 미만','1천만원~3천만원','3천만원~5천만원',
  '5천만원~1억원','1억~3억원','3억원 이상',
]
const EXP_OPTIONS = ['초보 창업자','1년 이내','1-3년','3-5년','5년 이상']
const ANALYSES = [
  '유동인구 분석','경쟁업체 분석','임대료 분석','매출 예측',
  '고객 특성 분석','상권 트렌드','입지 평가','리스크 분석',
]

export default function Wizard() {
  const nav = useNavigate()

  const [location, setLocation] = useState('')
  const [industries, setIndustries] = useState<string[]>([])
  const [budget, setBudget] = useState<string | null>(null)
  const [experience, setExperience] = useState<string | null>(null)
  const [targetDesc, setTargetDesc] = useState('')
  const [checked, setChecked] = useState<string[]>([])
  const [loading, setLoading] = useState(false)

  const toggle = (arr: string[], v: string, set: (x: string[]) => void) => {
    set(arr.includes(v) ? arr.filter((x) => x !== v) : [...arr, v])
  }

  const onSubmit = async () => {
    if (loading) return
    setLoading(true)

    const body: MarketAnalysisRequest = {
      location: location.trim(),
      industries,
      budget,
      experience,
      target: targetDesc.trim() || undefined,
      analyses: checked,
    }

    // 결과 페이지 폴백을 위해 선 저장
    sessionStorage.setItem('wizard.params', JSON.stringify(body))

    try {
      const result = await aiMarketAnalysis(body).catch(() => null)
      if (result) sessionStorage.setItem('wizard.result', JSON.stringify(result))
      nav('/wizard/result', { state: { params: body, result } })
    } catch {
      nav('/wizard/result', { state: { params: body } })
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {/* ▽ 전체를 감싸는 풀-블리드 하늘색 그라데이션 배경 (헤더 포함) */}
      <section className="relative left-1/2 right-1/2 -ml-[50vw] -mr-[50vw] w-screen bg-gradient-to-br from-[#eff6ff] to-[#e0e7ff]">

        <div className="mx-auto w-[94vw] md:w-[86vw] xl:max-w-[980px] py-10 md:py-14">
          {/* 헤더: 네비에서 여유롭게 띄움 */}
          <div className="text-center mb-6 md:mb-8">
            <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight">AI 상권분석 서비스</h1>
            <p className="mt-3 text-sm md:text-base text-gray-600">
              원하는 지역과 업종을 입력하면, AI가 상권을 분석해 창업에 필요한 인사이트를 제공합니다.
            </p>
          </div>

          {/* 흰색 카드 */}
          <div className="bg-white rounded-2xl shadow-xl ring-1 ring-slate-100 p-5 md:p-7">
            {/* 그룹 간 간격 넉넉히 */}
            <div className="space-y-8 md:space-y-10">
              {/* 1행: 관심 지역 / 관심 업종 */}
              <div className="grid md:grid-cols-2 gap-6 md:gap-8">
                <section>
                  <SectionTitle icon="location">관심 지역 설정</SectionTitle>
                  <input
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    placeholder="예: 서울시 강남구 역삼동"
                    className="w-full h-11 rounded-xl border px-3 text-sm bg-white"
                  />
                  <p className="mt-2 text-xs text-sky-600">
                    정확한 주소일수록 더 정밀한 분석이 가능해요.
                  </p>
                </section>

                <section>
                  <SectionTitle icon="compass">관심 업종 분야</SectionTitle>
                  <div className="flex flex-wrap gap-2">
                    {INDUSTRY_OPTIONS.map((opt) => (
                      <Chip
                        key={opt}
                        selected={industries.includes(opt)}
                        onClick={() => toggle(industries, opt, setIndustries)}
                      >
                        {opt}
                      </Chip>
                    ))}
                  </div>
                </section>
              </div>

              {/* 2행: 투자 예산 / 사업 경험 */}
              <div className="grid md:grid-cols-2 gap-6 md:gap-8">
                <section>
                  <SectionTitle icon="money">투자 예산 범위</SectionTitle>
                  <div className="grid grid-cols-2 gap-2">
                    {BUDGET_OPTIONS.map((b) => (
                      <Chip key={b} selected={budget === b} onClick={() => setBudget(b)}>
                        {b}
                      </Chip>
                    ))}
                  </div>
                </section>

                <section>
                  <SectionTitle icon="briefcase">사업 경험</SectionTitle>
                  <div className="grid grid-cols-2 gap-2">
                    {EXP_OPTIONS.map((e) => (
                      <Chip key={e} selected={experience === e} onClick={() => setExperience(e)}>
                        {e}
                      </Chip>
                    ))}
                  </div>
                </section>
              </div>

              {/* 3행: 타겟 고객층 */}
              <section>
                <SectionTitle icon="target">타겟 고객층</SectionTitle>
                <textarea
                  value={targetDesc}
                  onChange={(e) => setTargetDesc(e.target.value)}
                  placeholder="예: 20~30대 직장인, 가족단위 고객, 학생층 등 (선택)"
                  rows={3}
                  className="w-full rounded-xl border px-3 py-2 text-sm bg-white"
                />
                <div className="mt-1 text-xs text-gray-400">0/500자</div>
              </section>

              {/* 4행: 원하는 분석 유형 */}
              <section>
                <SectionTitle icon="chart">원하는 분석 유형 (복수 선택 가능)</SectionTitle>
                <div className="grid md:grid-cols-2 gap-2">
                  {ANALYSES.map((a) => {
                    const selected = checked.includes(a)
                    return (
                      <label
                        key={a}
                        className={[
                          'flex items-center gap-2 h-11 rounded-xl border px-3 text-sm cursor-pointer transition',
                          selected
                            ? 'bg-blue-50 border-blue-300 text-blue-700'
                            : 'bg-white border-gray-200 hover:bg-gray-50',
                        ].join(' ')}
                      >
                        <input
                          type="checkbox"
                          checked={selected}
                          onChange={() => toggle(checked, a, setChecked)}
                        />
                        <span>{a}</span>
                      </label>
                    )
                  })}
                </div>
              </section>

              {/* CTA */}
              <div className="pt-2 flex justify-center">
                <button
                  onClick={onSubmit}
                  disabled={loading}
                  className={`h-11 px-6 rounded-xl font-semibold shadow text-white ${
                    loading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'
                  }`}
                >
                  {loading ? '분석 중…' : 'AI 상권분석 시작하기'}
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
      {/* ▴ 헤더 포함, 흰 카드 바깥 전체 하늘색(부드러운 그라데이션) */}
    </>
  )
}
