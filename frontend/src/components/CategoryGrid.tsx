// src/components/CategoryGrid.tsx
import { Link } from 'react-router-dom'

type Cat = { key: string; label: string; to?: string; icon: JSX.Element }

const ico = {
  coffee: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="8" width="14" height="10" rx="2" />
      <path d="M17 10h2a3 3 0 0 1 0 6h-2" />
    </svg>
  ),
  food: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 3v8" />
      <path d="M8 3v8" />
      <path d="M4 7h4" />
      <path d="M12 3v8" />
      <path d="M16 5h4" />
      <path d="M18 5v6" />
    </svg>
  ),
  beer: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="6" y="8" width="9" height="10" rx="2" />
      <path d="M15 10h2a2 2 0 0 1 0 4h-2" />
      <path d="M6 8a3 3 0 0 1 3-3h3a3 3 0 0 1 3 3" />
    </svg>
  ),
  store: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 7h16l-1 4H5L4 7Z" />
      <path d="M6 11v6a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-6" />
      <path d="M9 17h6" />
    </svg>
  ),
  fashion: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M8 4l4 2 4-2 3 4-3 2v8H8V10L5 8l3-4z" />
    </svg>
  ),
  beauty: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 3v3" />
      <path d="M8 7l2 2-2 2" />
      <path d="M16 7l-2 2 2 2" />
      <path d="M12 12v9" />
    </svg>
  ),
  medical: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="10" width="10" height="7" rx="3" />
      <rect x="11" y="7" width="10" height="7" rx="3" />
    </svg>
  ),
  culture: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 5h8v14H3z" />
      <path d="M13 5h8v14h-8z" />
      <path d="M7 5v14" />
      <path d="M17 5v14" />
    </svg>
  ),
  leisure: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="6" cy="12" r="2" />
      <circle cx="18" cy="12" r="2" />
      <path d="M8 12h8" />
    </svg>
  ),
  office: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="6" y="3" width="12" height="18" rx="2" />
      <path d="M9 7h2M13 7h2M9 11h2M13 11h2M9 15h2M13 15h2" />
    </svg>
  ),
  hotel: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 12h18v6H3z" />
      <path d="M6 12V9a2 2 0 0 1 2-2h4a3 3 0 0 1 3 3v2" />
    </svg>
  ),
  box: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 7l9-4 9 4-9 4-9-4z" />
      <path d="M3 7v10l9 4 9-4V7" />
      <path d="M12 11v10" />
    </svg>
  ),
  megaphone: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 11l12-5v12L3 13z" />
      <path d="M9 16v3a2 2 0 0 0 2 2h1" />
    </svg>
  ),
  etc: (
    <svg viewBox="0 0 24 24" className="w-7 h-7" fill="currentColor">
      <circle cx="6" cy="12" r="2" />
      <circle cx="12" cy="12" r="2" />
      <circle cx="18" cy="12" r="2" />
    </svg>
  ),
}

const cats: Cat[] = [
  { key: 'cafe', label: '카페/디저트', icon: ico.coffee },
  { key: 'food', label: '식당', icon: ico.food },
  { key: 'pub', label: '주점/호프', icon: ico.beer },
  { key: 'convenience', label: '편의', icon: ico.store },
  { key: 'fashion', label: '패션/액세서리', icon: ico.fashion },
  { key: 'beauty', label: '뷰티/미용', icon: ico.beauty },
  { key: 'medical', label: '의료/약국', icon: ico.medical },
  { key: 'culture', label: '문화/취미', icon: ico.culture },
  { key: 'leisure', label: '레저/스포츠', icon: ico.leisure },
  { key: 'office', label: '사무/공유오피스', icon: ico.office },
  { key: 'hotel', label: '숙박', icon: ico.hotel },
  { key: 'warehouse', label: '창고/물류', icon: ico.box },
  { key: 'popup', label: '팝업/쇼룸', icon: ico.megaphone },
  { key: 'etc', label: '기타', icon: ico.etc },
]

export default function CategoryGrid() {
  return (
    <section className="card p-5 md:p-6">
      <div className="font-semibold text-lg mb-1">카테고리</div>
      <p className="text-sm text-gray-600 mb-4">
        아이콘만 클릭해도 <b>상세 페이지</b>로 이동합니다.
      </p>

      {/* 두 줄 정렬: XL 기준 7열 → 14개가 2줄로 정갈하게 배치 */}
      <div className="grid grid-cols-4 sm:grid-cols-5 md:grid-cols-6 xl:grid-cols-7 gap-3 md:gap-4 place-items-center">
        {cats.map((c) => (
          <Link
            key={c.key}
            to={`/listings?cat=${encodeURIComponent(c.key)}`}
            className="group w-[84px] h-[84px] md:w-[92px] md:h-[92px] rounded-2xl bg-white border border-slate-200 hover:border-slate-300 hover:shadow-sm transition flex flex-col items-center justify-center"
            title={c.label}
          >
            <span className="text-gray-800 group-hover:text-gray-900">{c.icon}</span>
            <span className="mt-2 text-[11px] md:text-xs text-gray-700">{c.label}</span>
          </Link>
        ))}
      </div>
    </section>
  )
}
