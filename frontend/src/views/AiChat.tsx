import React, { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { aiChat } from '../services/api'
import ListingCard from '../components/ListingCard'
import type { Listing } from '../services/api'

type Msg = { role: 'user' | 'assistant', text: string, listings?: Listing[] }

export default function AiChat(){
  const [params] = useSearchParams()
  const firstQ = params.get('q') || ''
  const [input, setInput] = useState(firstQ)
  const [msgs, setMsgs] = useState<Msg[]>(firstQ ? [{ role:'user', text:firstQ }] : [])
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  const ask = async (q: string) => {
    const text = q.trim()
    if(!text) return
    setMsgs(m=>[...m, { role:'user', text }])
    setInput('')
    setLoading(true)
    try {
      const r = await aiChat(text) // 서버가 없으면 fallback 동작(services/api.ts에 구현)
      setMsgs(m=>[...m, { role:'assistant', text: r.answer || '응답을 생성했어요.', listings: r.listings }])
    } catch(e){
      setMsgs(m=>[...m, { role:'assistant', text: '서버 연결이 원활하지 않습니다. 잠시 후 다시 시도해주세요.' }])
    } finally { setLoading(false) }
  }

  useEffect(()=>{ if(firstQ) ask(firstQ) /* 최초 진입 시 한번만 */  // eslint-disable-next-line
  }, [])

  useEffect(()=>{ bottomRef.current?.scrollIntoView({ behavior:'smooth' }) }, [msgs, loading])

  return (
    <div className="center-col">
      <div className="grid gap-4">
        {/* 헤더 */}
        <div className="card px-4 py-3 flex items-center justify-between">
          <div className="font-semibold">AI 메이트 · 로컬</div>
          <div className="text-xs text-gray-500">β demo</div>
        </div>

        {/* 메시지 영역 */}
        <div className="card p-4 h-[65vh] overflow-auto">
          {msgs.map((m, i)=>(
            <div key={i} className={"mb-4 flex " + (m.role==='user' ? 'justify-end' : 'justify-start')}>
              <div className={(m.role==='user'?'bg-brand-600 text-white':'bg-gray-100 text-gray-900')+" max-w-[75%] rounded-2xl px-4 py-2"}>
                <div className="whitespace-pre-wrap leading-relaxed">{m.text}</div>

                {/* 어시스턴트가 매물도 같이 주는 경우 카드 그리드 */}
                {m.role==='assistant' && m.listings?.length ? (
                  <div className="mt-3 grid sm:grid-cols-2 gap-3">
                    {m.listings.slice(0,4).map(l => <ListingCard key={l.id} item={l} />)}
                  </div>
                ) : null}
              </div>
            </div>
          ))}
          {loading && (
            <div className="mb-4 flex justify-start">
              <div className="bg-gray-100 text-gray-900 rounded-2xl px-4 py-2">
                <span className="inline-flex gap-1">
                  <i className="w-2 h-2 bg-gray-500 rounded-full animate-bounce [animation-delay:-.2s]" />
                  <i className="w-2 h-2 bg-gray-500 rounded-full animate-bounce [animation-delay:-.1s]" />
                  <i className="w-2 h-2 bg-gray-500 rounded-full animate-bounce" />
                </span>
              </div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {/* 입력창 */}
        <div className="card p-3">
          <div className="flex gap-2">
            <input
              value={input}
              onChange={e=>setInput(e.target.value)}
              onKeyDown={e=>{ if(e.key==='Enter') ask(input) }}
              placeholder="예) 서산 카페 상권 추천해줘"
              className="flex-1 border rounded-xl px-3 py-2 focus:outline-brand-400"
            />
            <button onClick={()=>ask(input)} className="btn-primary">전송</button>
          </div>
          <div className="text-xs text-gray-500 mt-1">
            팁: “동문1동 야식 많은 곳”, “임대 200 이하 카페” 처럼 구체적으로 물어보면 더 좋아요.
          </div>
        </div>
      </div>
    </div>
  )
}
