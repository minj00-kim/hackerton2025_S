// src/views/More.tsx
import { Link } from 'react-router-dom'
import Card from '../components/Card'
import React, { useEffect } from 'react'

export default function More() {

  // 이 화면에서도 푸터 숨김 (AiChat과 동일 정책)
  useEffect(() => {
    document.body.classList.add('chat-compact')
    return () => document.body.classList.remove('chat-compact')
  }, [])

  return (
    <div className="mx-auto w-full max-w-5xl px-4 py-8 space-y-6">
      <header className="mb-2">
        <h1 className="text-2xl font-bold">더보기</h1>
        <p className="text-gray-600 mt-1">자주 쓰는 도구들을 한 곳에서 모았습니다.</p>
      </header>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Card className="p-5 flex flex-col">
          <div className="text-lg font-semibold">BEP 계산기</div>
          <p className="text-sm text-gray-600 mt-1 flex-1">
            지역/업종/면적/원가를 입력해 손익분기점을 계산합니다.
          </p>
          <Link to="/consulting/simulate" className="btn-primary mt-4 self-start">열기</Link>
        </Card>

        <Card className="p-5 flex flex-col">
          <div className="text-lg font-semibold">A vs B 비교</div>
          <p className="text-sm text-gray-600 mt-1 flex-1">
            두 지역을 선택해 지표를 비교 분석합니다.
          </p>
          <Link to="/consulting/compare" className="btn-primary mt-4 self-start">열기</Link>
        </Card>

        <Card className="p-5 flex flex-col">
          <div className="text-lg font-semibold">CSV 업로드(관리자)</div>
          <p className="text-sm text-gray-600 mt-1 flex-1">
            매물/좌표 데이터를 일괄 업로드합니다.
          </p>
          <Link to="/admin/upload" className="btn-primary mt-4 self-start">열기</Link>
        </Card>
      </div>
    </div>
  )
}
