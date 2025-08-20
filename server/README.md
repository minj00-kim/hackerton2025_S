# Server (Express) — API 설명

## 라우트 개요
- `GET /api/health` 헬스체크
- `GET /api/listings` 매물 리스트 (쿼리 필터 지원)
- `GET /api/listings/:id` 매물 상세
- `POST /api/listings` 매물 생성 (주소 있으면 지오코딩 시도)
- `POST /api/ai/recommend` 추천 (Kakao Places로 주변 경쟁도 집계 → 점수화)
- `POST /api/ai/simulate` BEP/매출 추정
- `POST /api/ai/compare` 지역 비교 (두 지역의 근사 지표 비교)
- `POST /admin/upload-csv` CSV로 다건 등록

## 외부 API 키
- Kakao REST: `KAKAO_REST_KEY` (주소검색/키워드/카테고리 장소검색)
- Naver Geocode(선택): `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`
- OpenAI(선택): `OPENAI_API_KEY`