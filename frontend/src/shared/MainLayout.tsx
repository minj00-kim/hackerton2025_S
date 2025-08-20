// frontend/src/shared/MainLayout.tsx
import { Outlet, NavLink, Link } from 'react-router-dom'

const nav = ({ isActive }: any) =>
  'px-3 py-2 rounded-xl text-sm font-medium ' +
  (isActive ? 'bg-white text-brand-700 shadow-soft' : 'text-gray-700 hover:bg-white/70')

export default function MainLayout(){
  return (
    <div className="min-h-screen bg-slate-50">
      {/* 상단바 */}
      <header className="sticky top-0 z-20 bg-white/90 backdrop-blur border-b">
        <div className="center-col px-4 py-3 flex items-center gap-3">
          <Link to="/" className="text-xl font-extrabold text-brand-700 whitespace-nowrap">
            AI여긴어때
          </Link>
          <nav className="flex gap-1">
            <NavLink to="/wizard" className={nav}>AI 추천</NavLink>
            <NavLink to="/mate" className={nav}>AI 메이트</NavLink>
            <NavLink to="/listings" className={nav}>공실 매물</NavLink>
            <NavLink to="/consulting/simulate" className={nav}>BEP</NavLink>
            <NavLink to="/consulting/compare" className={nav}>A vs B</NavLink>
            <NavLink to="/admin/upload" className={nav}>업로더</NavLink>
            <NavLink to="/map" className={nav}>지도</NavLink>
          </nav>
          <div className="ml-auto" />
          <Link to="/account" className="text-sm text-gray-700">마이페이지</Link>
        </div>
      </header>

      {/* ⬇⬇ 상단 여백 제거(네비와 히어로가 공백 없이 맞닿게) */}
      <main className="pt-0">
        <Outlet />
      </main>

      <footer className="border-t py-8 text-center text-sm text-gray-500 bg-white">
        © 2025 Seosan Vacant.AI
      </footer>
    </div>
  )
}
