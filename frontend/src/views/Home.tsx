// src/views/Home.tsx
import { Link } from 'react-router-dom'

export default function Home() {
  return (
    <div
      className="
        relative left-1/2 right-1/2 -ml-[50vw] -mr-[50vw]
        w-screen min-h-screen overflow-x-hidden
        snap-y snap-mandatory
      "
    >
      {/* 1) HERO ─ 데이터로 검증된 창업 인사이트 */}
      <section
  className="
    snap-start min-h-screen flex items-center justify-center
    px-6 text-center
    bg-[radial-gradient(900px_520px_at_0%_100%,rgba(0,36,255,.28)_0%,rgba(0,36,255,.16)_38%,transparent_70%),radial-gradient(900px_520px_at_100%_100%,rgba(0,36,255,.28)_0%,rgba(0,36,255,.16)_38%,transparent_70%),radial-gradient(1200px_600px_at_50%_-12%,#ECF3FF_0%,transparent_60%),linear-gradient(180deg,#F3F7FF_0%,#E7EEFF_45%,#DCE6FF_100%)]
  "
>
        <div className="max-w-5xl mx-auto -mt-8 md:-mt-12">
          <div className="inline-flex items-center gap-3 px-4 py-2 rounded-full bg-black text-white text-[13px] font-semibold shadow-sm">
            <span>🚀</span>
            <span>AI 기반 상권분석 플랫폼</span>
          </div>
<br/><br/>
            <h1 className="mt-7 leading-[1.1] font-extrabold tracking-tight text-slate-900">
            <span className="block text-[38px] sm:text-[44px] md:text-[69px] tracking-[-0.05em] md:tracking-[-0.05em]">
              데이터로 검증된
            </span>
            {/* 3) 두 줄 사이 간격 살짝 넓히기 */}
    <span className="mt-2 block text-[48px] sm:text-[56px] md:text-[66px] tracking-[-0.05em] md:tracking-[-0.05em]
                     bg-clip-text text-transparent
                     bg-[linear-gradient(90deg,#3B82F6_0%,#6366F1_50%,#8B5CF6_100%)]">
      창업 인사이트
    </span>
          </h1>
<br/>
          <p className="mt-7 text-[15px] md:text-lg text-slate-600 max-w-3xl mx-auto">
            인공지능과 빅데이터를 활용한 정밀한 상권분석을 통해<br className="hidden md:inline" />
            성공적인 창업을 위한 전략적 정보를 제공합니다
          </p>
<br/>
          <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
            <Link
              to="/ai"
              className="inline-flex items-center justify-center h-14 px-8 rounded-full bg-black text-white text-[15px] font-semibold ring-1 ring-black/10 shadow-sm hover:brightness-110 transition"
              aria-label="상권 분석 시작하기(기존 AI 메이트)"
            >
              상권 분석 시작하기
            </Link>

            <Link
              to="/wizard"
              className="inline-flex items-center justify-center h-14 px-8 rounded-full bg-white text-slate-900 text-[15px] font-semibold ring-1 ring-slate-300 shadow-sm hover:bg-slate-50 hover:ring-slate-400 transition"
              aria-label="AI 창업 가이드(기존 AI 추천)"
            >
              AI 창업 가이드
            </Link>
          </div>
        </div>
      </section>

      {/* 2) WHY ─ 왜 AI 여긴어때 인가요? (로고와 동일한 스타일로 변경) */}
      <section className="snap-start min-h-screen flex items-center px-6 bg-[#F5F9FF]">
        <div className="max-w-6xl mx-auto w-full">
          <div className="text-center">
            <h2 className="text-3xl md:text-4xl font-extrabold text-slate-900">
              왜{' '}
              <span
                className="
                  font-line-seed logo-strong
                  text-transparent bg-clip-text bg-gradient-to-r
                  from-[#ff5bf1] to-[#79e4ff]
                  drop-shadow-[0_6px_24px_rgba(121,228,255,.18)]
                "
              >
                AI 여긴어때
              </span>{' '}
              인가요?<br/>
            </h2>
            <p className="mt-3 text-slate-600">
              정확하고 신뢰할 수 있는 데이터 기반의 상권분석을 통해 리스크를<br/> 최소화하고 성공 확률을 높입니다.<br/><br/><br/><br/><br/>
            </p>
          </div>

          <div className="mt-10 grid grid-cols-1 md:grid-cols-3 gap-5">
            <FeatureCard
              icon={
                <svg viewBox="0 0 24 24" className="w-7 h-7 text-sky-600">
                  <path d="M12 21s-6-5.2-6-10a6 6 0 1 1 12 0c0 4.8-6 10-6 10z" fill="none" stroke="currentColor" strokeWidth="1.8" />
                  <circle cx="12" cy="11" r="2.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
                </svg>
              }
              title="정밀한 위치분석"
              desc="실시간 유동인구 데이터와 교통 접근성, 경쟁업체 분포를 종합 분석하여 최적의 입지를 찾아드립니다"
            />
            <FeatureCard
              icon={
                <svg viewBox="0 0 24 24" className="w-7 h-7 text-sky-600">
                  <path d="M4 20h16" stroke="currentColor" strokeWidth="1.8" />
                  <rect x="6" y="12" width="3" height="6" rx="1" stroke="currentColor" strokeWidth="1.8" fill="none" />
                  <rect x="11" y="9" width="3" height="9" rx="1" stroke="currentColor" strokeWidth="1.8" fill="none" />
                  <rect x="16" y="6" width="3" height="12" rx="1" stroke="currentColor" strokeWidth="1.8" fill="none" />
                </svg>
              }
              title="AI 매출 예측"
              desc="업종별 빅데이터와 머신러닝 알고리즘을 통해 예상 매출과 수익성을 정확하게 예측합니다"
            />
            <FeatureCard
              icon={
                <svg viewBox="0 0 24 24" className="w-7 h-7 text-sky-600">
                  <circle cx="12" cy="12" r="8" fill="none" stroke="currentColor" strokeWidth="1.8" />
                  <circle cx="12" cy="12" r="2" fill="currentColor" />
                </svg>
              }
              title="맞춤형 전략"
              desc="개인의 예산, 경험, 업종을 고려한 개별화된 창업 전략과 실행 가이드를 제공합니다"
            />
          </div>
        </div>
      </section>

      {/* 3) CTA BAND */}
      <section
        className="
          snap-start min-h-[50vh] flex items-center justify-center text-center px-6
          bg-[linear-gradient(135deg,#4F46E5_0%,#7C3AED_50%,#06B6D4_100%)]
          text-white
        "
      >
        <div className="max-w-4xl mx-auto">
          <h3 className="text-3xl md:text-4xl font-extrabold leading-snug">
            지금 시작하여 성공적인 창업의 첫걸음을 내딛으세요
          </h3>
          <p className="mt-4 text-white/90">
            무료 상권분석으로 여러분의 아이디어가 얼마나 가치 있는지 확인해보세요
          </p>
          <div className="mt-8">
            <Link
              to="/ai"
              className="inline-flex items-center px-6 h-12 rounded-xl bg-white text-slate-900 font-semibold shadow hover:bg-white/90 transition"
            >
              무료로 분석 시작하기
            </Link>
          </div>
        </div>
      </section>

      {/* 4) INFO ─ 절반 높이 */}
      <section className="snap-start min-h-[50vh] flex items-center px-6 bg-slate-900 text-slate-200">
        <div className="max-w-6xl mx-auto w-full">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div>
              <h4 className="text-white font-bold">AI 여긴어때</h4>
              <p className="mt-3 text-sm text-slate-400">
                인공지능 기반 상권 분석으로 투자와 창업의 새 패러다임을 제시합니다.
              </p>
            </div>
            <div>
              <h4 className="text-white font-bold">서비스</h4>
              <ul className="mt-3 space-y-2 text-sm text-slate-300">
                <li>매물검색</li>
                <li>상권분석</li>
                <li>투자전략</li>
                <li>시장동향</li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-bold">고객지원</h4>
              <ul className="mt-3 space-y-2 text-sm text-slate-300">
                <li>도움말</li>
                <li>문의하기</li>
                <li>자주묻는질문</li>
                <li>고객센터</li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-bold">연락처</h4>
              <ul className="mt-3 space-y-2 text-sm text-slate-300">
                <li>1588-0000</li>
                <li>info@smartestate.com</li>
                <li>서울시 강남구 테헤란로</li>
              </ul>
            </div>
          </div>

          <div className="mt-10 border-t border-white/10 pt-6 text-sm text-slate-400">
            © 2025 AI여긴어때
          </div>
        </div>
      </section>
    </div>
  )
}

/* helpers */
function FeatureCard({
  icon,
  title,
  desc,
}: {
  icon: React.ReactNode
  title: string
  desc: string
}) {
  return (
    <div className="rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 p-6">
      <div className="w-12 h-12 rounded-xl bg-sky-50 flex items-center justify-center">
        {icon}
      </div>
      <div className="mt-4 font-semibold text-slate-900">{title}</div>
      <p className="mt-2 text-sm text-slate-600">{desc}</p>
    </div>
  )
}
