import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'

const nav = ({ isActive }: { isActive: boolean }) =>
  [
    'inline-flex items-center h-10 px-3 rounded-xl',
    'text-sm transition-colors',
    isActive
      ? 'bg-white text-brand-700 shadow-soft font-semibold'
      : 'text-gray-700 hover:bg-white/70 font-normal',
  ].join(' ')

export default function MainLayout() {
  const location = useLocation()
  // 채팅/더보기 화면에서는 푸터를 숨김
  const hideFooter =
    location.pathname.startsWith('/ai/chat') ||
    location.pathname.startsWith('/more')

  // 홈에서는 헤더를 숨기도록 되어 있으면 기존 로직 유지
  const isHome = location.pathname === '/'

  return (
    <div className="min-h-screen bg-slate-50">
      {/* 상단바 */}
      <header
        className={
          'sticky top-0 z-20 bg-white/90 backdrop-blur border-b ' +
          (isHome ? 'hidden' : '')
        }
      >
        {/* ✅ 풀폭 컨테이너 + 좌측 그룹 / 우측 마이페이지 */}
        <div className="w-full max-w-none px-3 sm:px-4 md:px-6 py-3 flex items-center">
          <div className="flex items-center gap-4">
            {/* 로고 (그라데이션 텍스트) */}
            <Link
              to="/"
              aria-label="AI여긴어때 홈"
              className="group flex items-end gap-1.5 select-none whitespace-nowrap"
            >
            
              <span
                className={[
                  'ml-1 font-line-seed logo-strong leading-none',
                  'text-[34px] md:text-[42px]',
                  'text-transparent bg-clip-text bg-gradient-to-r',
                  'from-[#ff5bf1] to-[#79e4ff]',
                  'drop-shadow-[0_6px_24px_rgba(121,228,255,.18)]',
                ].join(' ')}
              >
                AI여긴어때
              </span>
            </Link>

            {/* 네비게이션 */}
            <nav className="flex items-center gap-1 no-scrollbar overflow-x-auto">
              <NavLink to="/ai" className={nav}>AI 메이트</NavLink>
              <NavLink to="/map" className={nav}>지도</NavLink>
              <NavLink to="/listings" className={nav}>공실 매물</NavLink>
              <NavLink to="/support" className={nav}>창업 지원</NavLink>
              <NavLink to="/listings/new" className={nav}>매물 등록</NavLink>
              <NavLink to="/more" className={nav}>더보기</NavLink>
            </nav>
          </div>

          {/* 우측 끝: 마이페이지 */}
          <div className="ml-auto">
            <Link to="/account" className="text-sm text-gray-700">마이페이지</Link>
          </div>
        </div>
      </header>

      {/* 페이지 콘텐츠 */}
      <main className="pt-0 pb-8">
        <Outlet />
      </main>

      {/* 채팅/더보기에서는 푸터 숨김 */}
      {!hideFooter && (
        <footer className="border-t py-8 text-center text-sm text-gray-500 bg-white">
          © 2025 AI여긴어때
        </footer>
      )}
    </div>
  )
}
