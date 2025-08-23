// src/router.tsx 
import { lazy } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'  // ← Navigate 추가!
import MainLayout from './shared/MainLayout'
import ErrorPage from './shared/ErrorPage'

const Home           = lazy(() => import('./views/Home'))
const Wizard         = lazy(() => import('./views/Wizard'))
const WizardResult   = lazy(() => import('./views/WizardResult'))   // ★ 추가
const Listings       = lazy(() => import('./views/Listings'))
const Detail         = lazy(() => import('./views/Detail'))
const MapView        = lazy(() => import('./views/MapView'))
const Simulate       = lazy(() => import('./views/Simulate'))
const Compare        = lazy(() => import('./views/Compare'))
const Account        = lazy(() => import('./views/Account'))
const AdminUpload    = lazy(() => import('./views/AdminUpload'))
const AiLanding      = lazy(() => import('./views/AiLanding'))
const AiChat         = lazy(() => import('./views/AiChat'))
const More           = lazy(() => import('./views/More'))
const ListingCreate  = lazy(() => import('./views/ListingCreate')) 
const SupportNews    = lazy(() => import('./views/SupportNews'))

export const router = createBrowserRouter([
  {
    path: '/',                          // ← 레이아웃 라우트에 명시적으로 경로 지정
    element: <MainLayout />,
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <Home /> },         // ← 홈은 index 라우트로
      { path: 'wizard', element: <Wizard /> },
      { path: 'wizard/result', element: <WizardResult /> }, // ★ 결과 페이지 라우트
      { path: 'listings', element: <Listings /> },
      { path: 'listings/:id', element: <Detail /> },
      { path: 'listings/new', element: <ListingCreate /> },
      { path: 'map', element: <MapView /> },
      { path: 'consulting/simulate', element: <Simulate /> },
      { path: 'consulting/compare', element: <Compare /> },
      { path: 'account', element: <Account /> },
      { path: 'admin/upload', element: <AdminUpload /> },
      { path: 'ai', element: <AiLanding /> },
      { path: 'ai/chat', element: <AiChat /> },
      { path: 'mate', element: <Navigate to="/ai" replace /> },   // ← 이제 정상 작동
      { path: 'more', element: <More /> },                        // 경로 정규화(자식 라우트)
      // ▼ 추가: 창업 지원(뉴스)
      { path: 'support', element: <SupportNews /> },
      // 맨 마지막: 404 대응
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
])
