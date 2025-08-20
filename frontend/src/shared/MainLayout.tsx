// frontend/src/shared/MainLayout.tsx
// 메인 레이아웃: 상단 네비 + 가운데 컬럼. 검색 submit 시 startTransition으로 전환 렌더링 처리.
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState, startTransition } from 'react'

const nav = ({ isActive }: any) =>
  'px-3 py-2 rounded-xl text-sm font-medium ' +
  (isActive ? 'bg-white text-brand-700 shadow-soft' : 'text-gray-700 hover:bg-white/70')

export default function MainLayout(){
  const [q, setQ] = useState('')
  const go = useNavigate()

  // 동기 입력 직후 라우팅으로 인한 "A component suspended…" 경고를 방지
  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    startTransition(() => {
      go('/listings?q=' + encodeURIComponent(q))
    })
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-20 bg-white/90 backdrop-blur border-b">
        <div className="center-col px-4 py-3 flex items-center gap-4">
          <Link to="/" className="text-xl font-extrabold text-brand-700">Seosan Vacant.AI</Link>
          <nav className="hidden md:flex gap-1">
            <NavLink to="/wizard" className={nav}>AI 추천</NavLink>
            <NavLink to="/listings" className={nav}>공실 매물</NavLink>
            <NavLink to="/map" className={nav}>지도</NavLink>
            <NavLink to="/consulting/simulate" className={nav}>BEP</NavLink>
            <NavLink to="/consulting/compare" className={nav}>A vs B</NavLink>
            <NavLink to="/admin/upload" className={nav}>업로더</NavLink>
          </nav>
          <form onSubmit={onSubmit} className="ml-auto flex items-center gap-2">
            <input
              value={q}
              onChange={e=>setQ(e.target.value)}
              placeholder="지역/업종 검색"
              className="px-3 py-2 border rounded-xl text-sm w-56 focus:outline-brand-400 bg-white"
            />
            <button className="btn-primary text-sm">검색</button>
          </form>
          <Link to="/account" className="ml-2 text-sm text-gray-700">마이페이지</Link>
        </div>
      </header>
      <main className="px-4 py-8">
        <div className="center-col">
          <Outlet />
        </div>
      </main>
      <footer className="border-t py-8 text-center text-sm text-gray-500 bg-white">
        © 2025 Seosan Vacant.AI
      </footer>
    </div>
  )
}
