import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const suggestions = [
  '이 근처 맛집 대신찾아드립니다.',
  '요즘 MZ 사이에서 뜨는 여름 보양음식',
]

export default function AiLanding(){
  const [q, setQ] = useState('')
  const nav = useNavigate()
  const submit = (text?: string) => {
    const query = (text ?? q).trim()
    if(!query) return
    nav('/ai/chat?q=' + encodeURIComponent(query))
  }

  return (
    <section className="py-12 md:py-16">
      {/* 가운데 정렬 + 데스크톱 50% 폭. 바깥 배경과 동일한 회색톤 */}
      <div
        className="
          mx-auto w-[94vw] md:w-[70vw] xl:w-[50vw]
          rounded-[28px]
          bg-slate-50     /* 바깥(body)의 bg와 동일하게 */
          px-6 md:px-10 py-14 md:py-20
          shadow-none     /* 그림자/테두리 제거 */
        "
      >
        {/* 아이콘 */}
        <div className="w-24 h-24 md:w-28 md:h-28 rounded-full bg-[#78b0fe] grid place-content-center mb-8 mx-auto">
          <svg width="52" height="52" viewBox="0 0 24 24" className="-rotate-45 fill-white">
            <path d="M2 21l20-9L2 3l5 9-5 9zm7.53-5.53l-2.12-3.18 9.19-4.14-7.07 7.32z"/>
          </svg>
        </div>

        {/* 제목 */}
        <h1 className="text-center font-extrabold leading-tight text-[28px] md:text-[40px] text-gray-900">
          가나다님,<br className="hidden md:block" />
          <span className="inline-block mt-1">어떤 장소를 찾으시나요?</span>
        </h1>

        {/* 추천 칩 */}
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          {suggestions.map((s, i) => (
            <button
              key={i}
              onClick={()=>submit(s)}
              className="px-5 py-5 rounded-3xl bg-gray-100 hover:bg-gray-200 text-gray-900 text-[15px] md:text-[16px] font-medium transition"
            >
              {s}
            </button>
          ))}
        </div>

        {/* 검색창 */}
        <div className="mt-6 w-full max-w-2xl mx-auto">
          <div className="relative">
            <input
              value={q}
              onChange={e=>setQ(e.target.value)}
              onKeyDown={e=>{ if(e.key==='Enter') submit() }}
              placeholder="어떤장소를 찾으시나요?"
              className="
                w-full h-[60px] md:h-[64px]
                rounded-[26px] bg-gray-100
                pl-5 pr-14 text-[16px] md:text-[17px]
                font-medium text-gray-600
                focus:outline-brand-400
              "
            />
            <button
              onClick={()=>submit()}
              aria-label="전송"
              className="absolute right-2 top-1/2 -translate-y-1/2 w-[42px] h-[42px] rounded-full bg-gray-400 hover:bg-gray-500 grid place-content-center transition"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" className="fill-white -rotate-45">
                <path d="M2 21l20-9L2 3l5 9-5 9zm7.53-5.53l-2.12-3.18 9.19-4.14-7.07 7.32z"/>
              </svg>
            </button>
          </div>
          <p className="text-center text-[12px] text-gray-500 mt-3">
            AI가 생성한 응답이니, 중요한 정보는 꼭 확인해주세요!
          </p>
        </div>
      </div>
    </section>
  )
}
