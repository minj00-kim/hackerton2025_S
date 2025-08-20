// 모바일 앱처럼 보이게 가운데 큰 프레임을 만들어 주는 래퍼
// - children을 화면 내용으로 렌더
// - 헤더 좌측: 햄버거, 중앙: 타이틀, 우측: 닫기(X) 아이콘 스타일

import React from 'react'

export default function PhoneShell({
  title = 'AI 메이트',
  highlight = '로컬',
  children,
}: { title?: string; highlight?: string; children: React.ReactNode }) {
  return (
    <div className="min-h-[calc(100dvh-120px)] flex items-start justify-center">
      <div className="relative w-[402px] h-[729px] bg-white rounded-[28px] shadow-soft border overflow-hidden">
        {/* 노치 */}
        <div className="absolute top-2 left-1/2 -translate-x-1/2 w-24 h-1.5 rounded-full bg-gray-300/80" />
        {/* 헤더 */}
        <div className="absolute top-6 left-0 right-0 h-14 px-4 flex items-center justify-between">
          <button className="w-8 h-8 grid place-content-center">
            {/* 햄버거 */}
            <div className="w-6 h-[2px] bg-gray-900 mb-1.5" />
            <div className="w-6 h-[2px] bg-gray-900 mb-1.5" />
            <div className="w-6 h-[2px] bg-gray-900" />
          </button>
          <div className="text-[17px] font-semibold tracking-[.2px]">
            {title}{' '}
            <span className="text-brand-600 font-extrabold">{highlight}</span>
          </div>
          <button className="w-8 h-8 grid place-content-center">
            {/* X 아이콘 */}
            <div className="relative w-5 h-5">
              <span className="absolute inset-0 rotate-45 block w-[2px] bg-gray-900 m-auto"></span>
              <span className="absolute inset-0 -rotate-45 block w-[2px] bg-gray-900 m-auto"></span>
            </div>
          </button>
        </div>

        {/* 내용 */}
        <div className="absolute inset-0 pt-24 pb-6 px-5 overflow-auto">
          {children}
        </div>
      </div>
    </div>
  )
}
