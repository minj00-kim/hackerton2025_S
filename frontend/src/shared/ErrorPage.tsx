// 사용자가 보게 될 친화적인 에러 화면
import { isRouteErrorResponse, useRouteError, Link } from 'react-router-dom'

export default function ErrorPage(){
  const err = useRouteError() as any
  const title = isRouteErrorResponse(err) ? `${err.status} ${err.statusText}` : '문제가 발생했습니다'
  const msg = err?.message || '잠시 후 다시 시도해 주세요.'
  return (
    <div className="center-col card p-6 my-8">
      <h1 className="text-xl font-bold mb-2">{title}</h1>
      <p className="text-sm text-gray-600 mb-4">{msg}</p>
      <Link to="/" className="btn-primary text-sm">홈으로</Link>
    </div>
  )
}
