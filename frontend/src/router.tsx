import { lazy } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import MainLayout from './shared/MainLayout'
import ErrorPage from './shared/ErrorPage'

const Home = lazy(() => import('./views/Home'))
const Wizard = lazy(() => import('./views/Wizard'))
const Listings = lazy(() => import('./views/Listings'))
const Detail = lazy(() => import('./views/Detail'))
const MapView = lazy(() => import('./views/MapView'))
const Simulate = lazy(() => import('./views/Simulate'))
const Compare = lazy(() => import('./views/Compare'))
const Account = lazy(() => import('./views/Account'))
const AdminUpload = lazy(() => import('./views/AdminUpload'))

const AiLanding = lazy(() => import('./views/AiLanding'))
const AiChat = lazy(() => import('./views/AiChat'))

export const router = createBrowserRouter([
  {
    element: <MainLayout />,
    errorElement: <ErrorPage />, // 여기 추가
    children: [
      { path: '/', element: <Home /> },
      { path: '/wizard', element: <Wizard /> },
      { path: '/listings', element: <Listings /> },
      { path: '/listings/:id', element: <Detail /> },
      { path: '/map', element: <MapView /> },
      { path: '/consulting/simulate', element: <Simulate /> },
      { path: '/consulting/compare', element: <Compare /> },
      { path: '/account', element: <Account /> },
      { path: '/admin/upload', element: <AdminUpload /> },

      { path: '/ai', element: <AiLanding /> },
      { path: '/ai/chat', element: <AiChat /> },
    ]
  }
])