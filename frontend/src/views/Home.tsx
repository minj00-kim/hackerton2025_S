// src/views/Home.tsx
import { Link } from 'react-router-dom'

export default function Home() {
  return (
    <div className="landing theme-cyber relative left-1/2 right-1/2 -ml-[50vw] -mr-[50vw] w-screen min-h-screen overflow-hidden">
      {/* CYBER 배경: 3D 네온 그리드 */}
      <div className="bg-cyber" aria-hidden />

      {/* 본문 */}
      <section className="z-10 relative min-h-screen flex flex-col items-center justify-center text-center px-6">
        <h1 className="title">AI여긴어때</h1>
        <p className="tagline mt-3">
          데이터를 <b>빠르게 해석</b>하고 <b>행동 가능한 인사이트</b>로 바꿉니다
        </p>

        <div className="actions mt-10">
          {/* 글씨 흰색으로 고정됨 */}
          <Link to="/ai" className="cta primary" aria-label="AI 메이트로 이동">
            <span className="cta-label">AI 메이트</span>
            <span className="cta-desc">대화형 컨시어지 · 질문/리서치/요약</span>
          </Link>

          <Link to="/wizard" className="cta ghost" aria-label="AI 추천으로 이동">
            <span className="cta-label">AI 추천</span>
            <span className="cta-desc">조건 입력 → 맞춤 매물/상권 추천</span>
          </Link>
        </div>
      </section>

      <style>{`
        :root{
          --txt: #eaf3ff;
          --titleFrom:#ff5bf1; 
          --titleTo:#79e4ff; 
          --titleGlow: rgba(255,91,241,.25);

          /* 버튼(테마 고정: CYBER) */
          --btnPrimaryBg: linear-gradient(180deg, rgba(255,91,241,.25), rgba(121,228,255,.18));
          --btnPrimaryBorder: rgba(255,91,241,.5);
          --btnPrimaryText: #ffffff;           /* ★ 메이트 버튼 글씨 흰색으로 고정 */
          --btnPrimaryShadow: rgba(255,91,241,.25);

          --btnGhostBg: rgba(16,20,28,.6);
          --btnGhostBorder: rgba(121,228,255,.35);
          --btnGhostText: #dff7ff;
          --btnGhostShadow: rgba(121,228,255,.18);
        }

        .landing{ position:relative; background:#070a16; }

        /* 타이틀/카피 */
        .title{
          margin:0; user-select:none; white-space:nowrap;
          font-weight:900; letter-spacing:.05em;
          font-size: clamp(44px, 10vw, 120px);
          color:transparent;
          background: linear-gradient(90deg,var(--titleFrom),var(--titleTo));
          -webkit-background-clip:text; background-clip:text;
          text-shadow: 0 12px 48px var(--titleGlow);
        }
        .tagline{
          color: var(--txt); opacity:.94;
          text-shadow: 0 1px 0 rgba(0,0,0,.25);
          font-size: clamp(14px,2.2vw,18px);
        }

        /* CTA 공통 */
        .actions{ display:flex; gap:16px; flex-wrap:wrap; justify-content:center; }
        .cta{
          display:flex; flex-direction:column; align-items:flex-start;
          min-width:240px; padding:14px 18px; border-radius:16px;
          text-align:left; backdrop-filter: blur(10px);
          border:1px solid var(--btnBorder);
          background: var(--btnBg);
          color: var(--btnText);
          box-shadow: 0 12px 28px var(--btnShadow);
          transition: transform .2s ease, box-shadow .2s ease, filter .2s ease;
          position:relative; overflow:hidden;
        }
        .cta::after{
          content:''; position:absolute; inset:0;
          background: radial-gradient(140px 60px at -10% 50%, rgba(255,255,255,.30), transparent 60%);
          transform: translateX(-120%); transition: transform .6s ease;
        }
        .cta:hover{ transform: translateY(-2px); filter: brightness(1.04); }
        .cta:hover::after{ transform: translateX(120%); }
        .cta-label{ font-weight:900; letter-spacing:.02em; font-size:18px; line-height:1.1 }
        .cta-desc{ margin-top:4px; font-size:12.5px; opacity:.92 }

        .primary{
          --btnBg: var(--btnPrimaryBg);
          --btnBorder: var(--btnPrimaryBorder);
          --btnText: var(--btnPrimaryText);
          --btnShadow: var(--btnPrimaryShadow);
        }
        .ghost{
          --btnBg: var(--btnGhostBg);
          --btnBorder: var(--btnGhostBorder);
          --btnText: var(--btnGhostText);
          --btnShadow: var(--btnGhostShadow);
        }

        /* CYBER 배경(3D 네온 그리드) */
        .bg-cyber{
          position:absolute; left:-10vw; right:-10vw; bottom:-5vh; height: 140vh;
          background:
            linear-gradient(to bottom, transparent 0%, rgba(0,0,0,.7) 100%),
            repeating-linear-gradient(to right, rgba(121,228,255,.18) 0 1px, transparent 1px 80px),
            repeating-linear-gradient(to top,   rgba(121,228,255,.18) 0 1px, transparent 1px 80px),
            linear-gradient(180deg,#070a16,#04060c);
          transform: perspective(800px) rotateX(65deg) translateY(18vh);
          border-top:1px solid rgba(121,228,255,.45);
          mask-image: linear-gradient(to top, transparent 0%, black 22%);
          animation: gridScroll 30s linear infinite;
        }
        @keyframes gridScroll { 
          0%{background-position:0 0,0 0,0 0} 
          100%{background-position:0 0,0 600px,0 600px} 
        }
      `}</style>
    </div>
  )
}
