import { Link, NavLink } from 'react-router-dom'

const nav = ({ isActive }: any) =>
  'px-3 py-2 rounded-xl text-sm font-medium whitespace-nowrap ' +
  (isActive
    ? 'bg-white text-brand-700 shadow-soft'
    : 'text-gray-700 hover:bg-white/70')

export default function Navbar() {
  return (
    <header className="sticky top-0 z-40 bg-white/90 backdrop-blur border-b">
      {/* 헤더 전용 폭 (본문과 분리) */}
      <div className="mx-auto w-full max-w-[1180px] px-4 py-3 flex items-center gap-3">
        {/* 좌측 로고/타이틀 */}
        <Link to="/" className="text-xl font-extrabold text-brand-700 whitespace-nowrap">
          AI여긴어때
        </Link>

        {/* 좌측에 항목 몰아넣기, 줄바꿈 금지 */}
        <nav className="flex gap-1 flex-nowrap overflow-x-auto">
          <NavLink to="/wizard" className={nav}>AI 추천</NavLink>
          <NavLink to="/listings" className={nav}>공실 매물</NavLink>
          <NavLink to="/consulting/simulate" className={nav}>BEP</NavLink>
          <NavLink to="/consulting/compare" className={nav}>A vs B</NavLink>
          <NavLink to="/admin/upload" className={nav}>업로더</NavLink>
          <NavLink to="/map" className={nav}>지도</NavLink>
        </nav>

        {/* 우측 여백/마이페이지 */}
        <div className="ml-auto" />
        <Link to="/account" className="text-sm text-gray-700">마이페이지</Link>
      </div>
    </header>
  )
}
