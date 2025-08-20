// frontend/src/components/HeroSearch.tsx
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function HeroSearch(){
  const [mode, setMode] = useState<'listings'|'deal'|'insight'>('listings')
  const [q, setQ] = useState('')
  const go = useNavigate()

  const onSubmit = (e:any) => {
    e.preventDefault()
    if(mode === 'listings' || mode === 'deal'){
      go('/listings?q=' + encodeURIComponent(q))
    }else{
      go('/wizard')
    }
  }

  return (
    // ⬇⬇ 전폭-풀블리드 + 노란 배경, 상단은 그대로(pt), 하단만 대폭 축소(pb)
    <section
      className="
        mx-[calc(50%-50vw)]
        px-[max(1rem,calc(50vw-680px))]
        bg-[#ffdf65]
        pt-24 md:pt-28      /* (위) 기존 느낌 유지 */
        pb-6  md:pb-8       /* (아래) 기존 대비 ~80% 축소 */
      "
    >
      {/* 중앙 검색 카드 */}
      <form onSubmit={onSubmit} className="max-w-5xl mx-auto">
        <div className="
          bg-white rounded-full flex items-center gap-1 md:gap-2
          px-3 md:px-4 py-2 md:py-3 shadow-xl
        ">
          {/* 모드 선택 */}
          <select
            value={mode}
            onChange={e=>setMode(e.target.value as any)}
            className="text-gray-700 text-sm md:text-base font-medium pl-1 pr-6 py-2 bg-transparent rounded-full hover:bg-slate-50 focus:outline-none"
            aria-label="검색 모드"
          >
            <option value="listings">매물</option>
            <option value="deal">실거래가</option>
            <option value="insight">인사이트</option>
          </select>

          <div className="w-px h-6 bg-slate-200 mx-1 md:mx-2" />

          <input
            value={q}
            onChange={e=>setQ(e.target.value)}
            placeholder="지역, 지하철, 건물명, 학교명으로 검색해 보세요."
            className="flex-1 text-gray-800 placeholder:text-gray-400 px-3 md:px-4 py-3 md:py-4 rounded-l-full focus:outline-none text-sm md:text-base"
          />

          <button
            className="ml-1 md:ml-2 text-sm md:text-base font-semibold bg-brand-600 hover:bg-brand-700 text-white px-5 md:px-7 py-2.5 md:py-3 rounded-full transition"
            type="submit"
            aria-label="검색"
          >
            검색
          </button>
        </div>
      </form>
    </section>
  )
}
