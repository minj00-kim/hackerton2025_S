// env를 window로 노출 (SDK 로더가 사용)
;(window as any).VITE_KAKAO_API_KEY = import.meta.env.VITE_KAKAO_API_KEY

// 디버그: 키가 존재하는지만 확인 (값은 콘솔에 찍지 않음)
console.log('[ENV] kakao key present?', !!import.meta.env.VITE_KAKAO_API_KEY)

// frontend/src/main.tsx
import React, { Suspense } from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import './index.css'
import { router } from './router'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Suspense fallback={<div className="center-col p-6 text-gray-500">로딩 중…</div>}>
      <RouterProvider router={router} />
    </Suspense>
  </React.StrictMode>
)
