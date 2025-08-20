import HeroSearch from '../components/HeroSearch'
import CategoryGrid from '../components/CategoryGrid'
import Card from '../components/Card'
import { Link } from 'react-router-dom'

export default function Home(){
  return (
    <div className="space-y-10">
      {/* ⬇⬇ 보라색 히어로 영역: 페이지 폭에서 독립적으로 좌우 꽉 채우기 */}
  <section
    className="
      full-bleed
      bg-[#ffdf65]
      py-16 sm:py-24
    "
  >
        {/* 내부 내용은 다시 컨테이너로 중앙 정렬 */}
        <div className="center-col px-4 sm:px-6 lg:px-8">
          <HeroSearch />
        </div>
      </section>

      {/* ⬇⬇ 메인 콘텐츠(컨테이너 폭 유지) */}
      <section className="center-col px-4 sm:px-6 lg:px-8 space-y-8">
        {/* 카테고리 그리드 복귀 */}
        <CategoryGrid />

        {/* (아래는 예시 섹션 유지) */}
        <div className="grid sm:grid-cols-2 gap-4">
          <Card>
            <div className="font-semibold mb-2">유지보수 가이드</div>
            <ul className="text-sm text-gray-700 list-disc pl-5 space-y-1">
              <li><code>frontend/src/services/api.ts</code> — 프론트 API 정의</li>
              <li><code>server/services/*.js</code> — 외부 API 연동 로직</li>
              <li><code>server/server.js</code> — Express 라우팅/시작</li>
            </ul>
          </Card>
          <Card>
            <div className="font-semibold mb-2">빠른 이동</div>
            <div className="flex gap-2 text-sm">
              <Link to="/listings" className="btn-outline">공실 매물</Link>
              <Link to="/wizard" className="btn-outline">AI 추천</Link>
              <Link to="/map" className="btn-outline">지도</Link>
            </div>
          </Card>
        </div>
      </section>
    </div>
  )
}
