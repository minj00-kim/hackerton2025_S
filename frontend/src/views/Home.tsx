import { Link } from 'react-router-dom'
import Card from '../components/Card'
import CategoryGrid from '../components/CategoryGrid';

export default function Home(){
  return (
    <div className="space-y-8">
      <section className="card p-8 text-center bg-gradient-to-r from-brand-50 to-white">
        <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight">AI 기반 ‘공실 상가 + 청년 창업’ 매칭</h1>
        <p className="text-gray-600 mt-3">Kakao 로컬 데이터로 경쟁도/입지 지표를 집계하고, 시뮬레이터로 BEP를 계산합니다.</p>
        <div className="mt-5 flex gap-3 justify-center">
          <Link to="/wizard" className="btn-primary">AI 추천 받기</Link>
          <Link to="/listings" className="btn-outline">공실 매물 보기</Link>
        </div>
      </section>

<CategoryGrid />

      <section className="grid sm:grid-cols-2 gap-4">
        <Card>
          <div className="font-semibold mb-2">추천 근거</div>
          <ul className="text-sm text-gray-700 list-disc pl-5 space-y-1">
            <li>지역 지표: 유동/임대(근사)/경쟁(주변 업소 수)/청년</li>
            <li>업종 특성: 최소 면적/마진/임대료 민감도</li>
            <li>창업자 선호: 예산/희망 업종/선호 지역</li>
          </ul>
          <p className="text-xs text-gray-500 mt-3">* 외부 데이터 키가 없으면 샘플/휴리스틱으로 동작합니다.</p>
        </Card>
        <Card>
          <div className="font-semibold mb-2">유지보수 가이드</div>
          <ul className="text-sm text-gray-700 list-disc pl-5 space-y-1">
            <li><code>frontend/src/services/api.ts</code> — 프론트 API 정의</li>
            <li><code>server/services/*.js</code> — 외부 API 연동 로직</li>
            <li><code>server/server.js</code> — Express 라우팅/시작</li>
          </ul>
        </Card>
      </section>
    </div>
  )
}