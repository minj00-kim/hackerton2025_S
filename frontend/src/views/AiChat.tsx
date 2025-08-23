// src/views/AiChat.tsx
import React, { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { aiChat } from '../services/api'
import ListingCard from '../components/ListingCard'
import type { Listing } from '../services/api'

type Msg = { role: 'user' | 'assistant', text: string, listings?: Listing[] }

function formatKoreanDate(d: Date) {
  const yoil = ['일','월','화','수','목','금','토'][d.getDay()]
  return `${d.getFullYear()}년 ${d.getMonth()+1}월 ${d.getDate()}일 ${yoil}`
}

/** 제미나이 느낌의 파란 스파클 아이콘 */
const GeminiSparkle = ({ className = '' }) => (
  <svg
    className={className}
    width="18"
    height="18"
    viewBox="0 0 24 24"
    role="img"
    aria-label="assistant"
  >
    <defs>
      <linearGradient id="gem-blue" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="#60A5FA" />
        <stop offset="100%" stopColor="#2563EB" />
      </linearGradient>
    </defs>
    {/* 라운드 다이아몬드 모양(부드럽게) */}
    <path
      fill="url(#gem-blue)"
      d="M12 2.5c.6 0 1.2.3 1.6.8l2.9 3.4 3.9 1.6c1 .4 1 1.8 0 2.3L16.5 12l-1.9 4.7c-.4 1-1.8 1-2.3 0L10.4 12 3.6 10.6c-1-.2-1.2-1.6-.3-2.2l4.1-2.2 2.5-3.1c.3-.4.9-.6 1.4-.6Z"
    />
  </svg>
)

export default function AiChat() {
  const [params] = useSearchParams()
  const firstQ = params.get('q') || ''

  const [input, setInput] = useState(firstQ)
  const [msgs, setMsgs] = useState<Msg[]>([])
  const [pending, setPending] = useState(false)

  const bottomRef = useRef<HTMLDivElement>(null)
  const didInit = useRef(false)
  const composingRef = useRef(false)

  const ask = async (q: string) => {
    const text = q.trim()
    if (!text || pending) return
    setPending(true)
    setMsgs(m => [...m, { role: 'user', text }])
    setInput('')

    try {
      const r = await aiChat(text) // 서버 없으면 services/api.ts의 fallback 동작
      setMsgs(m => [
        ...m,
        { role: 'assistant', text: r.answer || '', listings: r.listings }
      ])
    } catch {
      setMsgs(m => [
        ...m,
        { role: 'assistant', text: '서버 연결이 원활하지 않습니다. 잠시 후 다시 시도해주세요.' }
      ])
    } finally {
      setPending(false)
    }
  }

  // 최초 1회 firstQ 자동 전송
  useEffect(() => {
    if (didInit.current) return
    didInit.current = true
    if (firstQ) ask(firstQ)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [firstQ])

  // 새 메시지/로딩 시 맨 아래로 스크롤
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [msgs, pending])

  return (
    <div className="relative">
      {/* 메시지 영역: 중앙 고정 폭 + 입력창 높이만큼 패딩(겹침 방지) */}
      <div className="mx-auto w-[min(1000px,92vw)] pt-6 pb-[140px]">
        {/* 날짜 칩 (상단 중앙) — 메시지가 있을 때만 표시 */}
        {msgs.length > 0 && (
          <div className="relative my-4">
            <hr className="border-gray-200" />
            <div className="absolute left-1/2 -translate-x-1/2 -top-3 bg-white px-3 py-1 rounded-full text-xs text-gray-600 ring-1 ring-gray-200">
              {formatKoreanDate(new Date())}
            </div>
          </div>
        )}

        {/* 대화 리스트 */}
        <div className="mt-2">
          {msgs.map((m, i) => (
            <div
              key={i}
              className={
                'mb-4 ' +
                (m.role === 'user'
                  ? 'flex justify-end'
                  : 'flex items-start gap-2')
              }
            >
              {m.role === 'assistant' && <GeminiSparkle className="mt-1 shrink-0" />}

              {/* 사용자 버블은 파란색 유지 / 어시스턴트는 배경 제거(텍스트만) */}
              <div
                className={
                  m.role === 'user'
                    ? 'bg-brand-600 text-white rounded-2xl px-4 py-2 max-w-[70%]'
                    : 'text-gray-900 max-w-[78%] leading-relaxed'
                }
              >
                <div className="whitespace-pre-wrap">{m.text}</div>

                {m.role === 'assistant' && m.listings?.length ? (
                  <div className="mt-3 grid sm:grid-cols-2 gap-3">
                    {m.listings.slice(0, 4).map((l) => (
                      <ListingCard key={l.id} item={l} />
                    ))}
                  </div>
                ) : null}
              </div>
            </div>
          ))}

          {pending && (
            <div className="mb-4 flex items-start gap-2">
              <GeminiSparkle className="mt-1 shrink-0" />
              <div className="text-gray-900">
                <span className="inline-flex gap-1">
                  <i className="w-2 h-2 bg-gray-400 rounded-full animate-bounce [animation-delay:-.2s]" />
                  <i className="w-2 h-2 bg-gray-400 rounded-full animate-bounce [animation-delay:-.1s]" />
                  <i className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" />
                </span>
              </div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>
      </div>

      {/* 입력창: 하단 고정 + 글로우, 바닥에서 살짝 띄움 */}
      <div className="fixed bottom-8 left-1/2 -translate-x-1/2 z-20 w-[min(1000px,92vw)]">
        <div className="rounded-full bg-white ring-1 ring-gray-200 shadow-[0_18px_70px_rgba(29,78,216,.18)]">
          <div className="flex items-center gap-2 px-4 py-2.5">
            <input
              value={input}
              onChange={(e) => {
                if (composingRef.current) return
                setInput(e.target.value)
              }}
              onCompositionStart={() => {
                composingRef.current = true
              }}
              onCompositionEnd={(e) => {
                composingRef.current = false
                setInput(e.currentTarget.value)
              }}
              onKeyDown={(e) => {
                // @ts-ignore
                if (e.key === 'Enter' && !e.nativeEvent.isComposing) ask(input)
              }}
              placeholder="예) 서산 카페 상권 추천해줘"
              className="flex-1 bg-transparent outline-none text-[15px] placeholder:text-gray-400"
            />
            <button
              onClick={() => ask(input)}
              disabled={pending}
              className="shrink-0 px-4 py-1.5 rounded-full bg-brand-600 text-white disabled:opacity-60"
            >
              {pending ? '전송중…' : '전송'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
