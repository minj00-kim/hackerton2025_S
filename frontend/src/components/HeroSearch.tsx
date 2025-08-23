import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function HeroSearch(){
  const [mode, setMode] = useState<'listings'|'deal'|'insight'>('listings')
  const [q, setQ] = useState('')
  const go = useNavigate()

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const query = q.trim()
    if(!query) return
    if(mode === 'listings') go('/listings?q=' + encodeURIComponent(query))
    else if(mode === 'deal')  go('/listings?q=' + encodeURIComponent(query))
    else go('/wizard')
  }

  return (
    // ⬇⬇ 세로 패딩을 더 줄였습니다 (이전보다 약 25~35% 얕음)
    <section className="w-full bg-[#ffdf65] py-[clamp(18px,20vh,112px)]">
      <div className="mx-auto max-w-5xl text-center">
        <form onSubmit={onSubmit}>
          <div className="
            bg-white rounded-full flex items-center gap-2 px-2.5 py-1.5
            shadow-[0_10px_24px_rgba(0,0,0,.08)] mx-auto max-w-3xl
          ">
            <select
              value={mode}
              onChange={e=>setMode(e.target.value as any)}
              className="text-gray-700 text-sm md:text-base font-medium pl-3 pr-6 py-2 bg-transparent rounded-full hover:bg-slate-50 focus:outline-none"
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
              className="flex-1 text-gray-800 placeholder:text-gray-400 px-3 md:px-4 py-3 md:py-3.5 rounded-l-full focus:outline-none text-sm md:text-base"
            />

            <button
              className="ml-2 text-sm md:text-base font-semibold bg-brand-600 hover:bg-brand-700 text-white px-5 md:px-7 py-2.5 md:py-3 rounded-full transition"
              type="submit"
            >
              검색
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}
