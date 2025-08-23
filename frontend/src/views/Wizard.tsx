import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Card from '../components/Card'

type Budget =
  | 'lt_100m' | '100_300m' | '300_500m' | '500m_1b' | '1_3b' | 'gt_3b'

type Experience =
  | 'newbie' | '0_1y' | '1_3y' | '3_5y' | 'gt_5y'

const INDUSTRIES = [
  '카페/베이커리','음식점/주점','소매/편의점','미용/헬스케어',
  '의료/약국','교육/학원','부동산/금융','서비스업','기타',
]

const BUDGETS: { key: Budget; label: string }[] = [
  { key: 'lt_100m',  label: '1천만원 미만' },
  { key: '100_300m', label: '1천만원~3천만원' },
  { key: '300_500m', label: '3천만원~5천만원' },
  { key: '500m_1b',  label: '5천만원~1억원' },
  { key: '1_3b',     label: '1억~3억원' },
  { key: 'gt_3b',    label: '3억원 이상' },
]

const EXPERIENCES: { key: Experience; label: string }[] = [
  { key: 'newbie', label: '초보 창업자' },
  { key: '0_1y',   label: '1년 이내' },
  { key: '1_3y',   label: '1-3년' },
  { key: '3_5y',   label: '3-5년' },
  { key: 'gt_5y',  label: '5년 이상' },
]

const ANALYSES = [
  '유동인구 분석','경쟁업체 분석','임대료 분석','매출 예측',
  '고객 특성 분석','상권 트렌드','입지 평가','리스크 분석',
]

function Chip({
  active, children, onClick, as='button',
}: { active?: boolean; children: React.ReactNode; onClick?: () => void; as?: 'button' | 'div' }) {
  const cls =
    'px-3 py-2 rounded-xl border text-sm transition ' +
    (active
      ? 'bg-brand-50 border-brand-200 text-brand-700'
      : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-50')
  if (as === 'div') return <div className={cls}>{children}</div>
  return <button type="button" className={cls} onClick={onClick}>{children}</button>
}

export default function Wizard() {
  const nav = useNavigate()

  // 폼 상태
  const [location, setLocation] = useState('')
  const [industries, setIndustries] = useState<string[]>([])
  const [budget, setBudget] = useState<Budget | null>(null)
  const [exp, setExp] = useState<Experience | null>(null)
  const [targetDesc, setTargetDesc] = useState('')
  const [want, setWant] = useState<string[]>([])

  const ready = useMemo(() =>
    location.trim().length > 0 && industries.length > 0 && !!budget && !!exp && want.length > 0
  , [location, industries, budget, exp, want])

  const toggle = (arr: string[], v: string, setter: (n: string[]) => void, single=false) => {
    if (single) return setter(arr[0] === v ? [] : [v])
    setter(arr.includes(v) ? arr.filter(x => x !== v) : [...arr, v])
  }

  const submit = (e?: React.FormEvent) => {
    e?.preventDefault()
    const payload = {
      location: location.trim(),
      industries,
      budget,
      experience: exp,
      target: targetDesc.trim(),
      analyses: want,
    }
    nav('/wizard/result', { state: payload })
  }

  return (
    <div className="mx-auto px-4 w-[94vw] md:w-[80vw] xl:max-w-[980px] py-6">
      <div className="text-center">
        <h1 className="text-2xl md:text-3xl font-extrabold">AI 상권분석 서비스</h1>
        <p className="text-gray-600 mt-2">원하는 지역과 업종을 입력하면, AI가 상권을 분석해 창업에 필요한 인사이트를 제공합니다.</p>
      </div>

      <Card className="mt-6">
        <form onSubmit={submit} className="grid gap-6">
          {/* 1행: 관심 지역 / 업종 */}
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <div className="font-semibold mb-2">관심 지역 설정</div>
              <input
                value={location}
                onChange={e=>setLocation(e.target.value)}
                placeholder="예: 서울시 강남구 역삼동"
                className="w-full h-11 rounded-xl border px-3 text-sm bg-white"
              />
              <div className="text-xs text-blue-600 mt-1">정확한 주소일수록 더 정밀한 분석이 가능해요.</div>
            </div>
            <div>
              <div className="font-semibold mb-2">관심 업종 분야</div>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {INDUSTRIES.map(it => (
                  <Chip
                    key={it}
                    active={industries.includes(it)}
                    onClick={()=>toggle(industries, it, setIndustries)}
                  >{it}</Chip>
                ))}
              </div>
            </div>
          </div>

          {/* 2행: 예산 / 경험 */}
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <div className="font-semibold mb-2">투자 예산 범위</div>
              <div className="grid grid-cols-2 gap-2">
                {BUDGETS.map(b => (
                  <Chip key={b.key} active={budget===b.key} onClick={()=>setBudget(b.key)}>{b.label}</Chip>
                ))}
              </div>
            </div>
            <div>
              <div className="font-semibold mb-2">사업 경험</div>
              <div className="grid grid-cols-2 gap-2">
                {EXPERIENCES.map(x => (
                  <Chip key={x.key} active={exp===x.key} onClick={()=>setExp(x.key)}>{x.label}</Chip>
                ))}
              </div>
            </div>
          </div>

          {/* 3행: 타겟 고객층 */}
          <div>
            <div className="font-semibold mb-2">타겟 고객층</div>
            <textarea
              value={targetDesc}
              onChange={e=>setTargetDesc(e.target.value)}
              placeholder="예: 20-30대 직장인, 가족단위 고객, 학생층 등 (선택)"
              className="w-full rounded-xl border px-3 py-2 text-sm bg-white"
              rows={3}
            />
            <div className="text-xs text-gray-500 mt-1">{targetDesc.length}/500자</div>
          </div>

          {/* 4행: 원하는 분석 유형 */}
          <div>
            <div className="font-semibold mb-2">원하는 분석 유형 (복수 선택 가능)</div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              {ANALYSES.map(a => (
                <Chip key={a} active={want.includes(a)} onClick={()=>toggle(want, a, setWant)}>{a}</Chip>
              ))}
            </div>
          </div>

        </form>
      </Card>

      <div className="mt-5 flex justify-center">
        <button
          onClick={submit}
          disabled={!ready}
          className={'h-11 px-6 rounded-xl text-white font-semibold ' +
            (ready ? 'bg-brand-600 hover:bg-brand-700' : 'bg-gray-300 cursor-not-allowed')}
        >
          AI 상권분석 시작하기
        </button>
      </div>
    </div>
  )
}
