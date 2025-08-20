import { Link } from 'react-router-dom'
import Card from './Card'

type Cat = { slug: string; label: string; icon: string }
const CATS: Cat[] = [
  { slug: 'coffee',     label: '커피',     icon: '☕' },
  { slug: 'kfood',      label: '한식',     icon: '🍚' },
  { slug: 'pub',        label: '주점',     icon: '🍺' },
  { slug: 'life',       label: '생활서비스', icon: '🧰' },
  { slug: 'taxlaw',     label: '법무·세무', icon: '⚖️' },
  { slug: 'logi',       label: '운송물류',  icon: '🚚' },
  { slug: 'estate',     label: '부동산',    icon: '🏢' },
  { slug: 'law',        label: '법률',     icon: '🔨' },
  { slug: 'chicken',    label: '치킨',     icon: '🍗' },
  { slug: 'pizza',      label: '피자',     icon: '🍕' },
  { slug: 'beauty',     label: '미용',     icon: '💇' },
  { slug: 'bakery',     label: '베이커리',  icon: '🥐' },
  { slug: 'security',   label: '보안',     icon: '🛡️' },
  { slug: 'finance',    label: '금융',     icon: '💳' },
  { slug: 'academy',    label: '학원',     icon: '📘' },
  { slug: 'pet',        label: '반려동물',  icon: '🐾' },
]

export default function CategoryGrid(){
  return (
    <Card>
      <div className="mb-4">
        <div className="text-2xl font-bold">카테고리</div>
        <div className="text-gray-500 text-sm">
          아이콘만 클릭해도 <span className="font-semibold">상세 페이지</span>로 이동합니다.
        </div>
      </div>

      <ul className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-8 gap-3">
        {CATS.map(c => (
          <li key={c.slug}>
            {/* 필요에 맞게 이동 경로 수정하세요. 예: /category/:slug 또는 /listings?type= */}
            <Link
              to={`/listings?type=${encodeURIComponent(c.label)}`}
              className="group flex flex-col items-center gap-2 rounded-2xl border bg-white hover:bg-slate-50 shadow-sm px-5 py-6 transition"
            >
              <div className="text-4xl">{c.icon}</div>
              <div className="text-sm font-medium text-gray-800">{c.label}</div>
            </Link>
          </li>
        ))}
      </ul>
    </Card>
  )
}
