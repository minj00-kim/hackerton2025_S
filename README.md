# Seosan Vacant.AI — PRO (팀 공유용, 주석 풍부)

마이프랜차이즈(myfranchise.kr) 느낌의 깔끔한 레이아웃으로, **AI 추천/공실 매물/시뮬레이터/BEP/A vs B** 기능을 제공합니다.
외부 데이터는 Kakao Local API(검색/지오코딩)를 기본으로 연동하며, 필요시 Naver Geocoding 및 OpenAI 재랭킹을 선택적으로 활성화할 수 있습니다.

## 필수 설치
- Node.js 18+ (Windows: https://nodejs.org/)
- Kakao REST API 키 (https://developers.kakao.com/)
  - 앱 생성 → 플랫폼 "웹" 추가 → 로컬 개발 도메인 http://localhost:5173 등록
- (선택) Naver Cloud Maps Geocoding 키
- (선택) OPENAI_API_KEY

## 실행 (터미널 2개 권장)
```bash
# 서버
cd server
cp .env.sample .env  # 키 입력
npm i
npm run dev

# 프론트엔드
cd ../frontend
cp .env.sample .env  # API URL, Kakao 앱키 입력
npm i
npm run dev
```

- 서버: http://localhost:5050
- 웹:   http://localhost:3000