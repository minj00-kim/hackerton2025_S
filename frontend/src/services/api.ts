// src/services/api.ts
import axios from 'axios'

// 외부 유틸(중복 방지: 정의하지 말고 사용/재수출만)
import { ensureAuthKey } from '../lib/auth'
import { addFavId, removeFavId, isFavId } from '../lib/fav'
export { ensureAuthKey as getAuthKey } from '../lib/auth'

// =======================================
// axios 인스턴스
// =======================================
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5050/api'
export const api = axios.create({ baseURL })

// 공통 유틸
const sleep = (ms: number) => new Promise(res => setTimeout(res, ms))
const ownerHeaders = (ownerKey?: string) => ({
  headers: { 'X-Owner-Key': ownerKey || ensureAuthKey() },
})

// =======================================
// 타입들
// =======================================
export type TradeType = 'SALE' | 'JEONSE' | 'MONTHLY'

export type Listing = {
  id: string
  title: string
  region?: string
  address: string
  category?: string
  // 거래/가격
  type: TradeType
  price?: number          // 매매가 or 전세보증금(만원)
  deposit?: number        // (월세) 보증금(만원)
  rentMonthly?: number    // (월세) 월세(만원)
  maintenanceFee?: number // 관리비(만원)
  // 면적/좌표
  area?: number           // m²
  lat?: number
  lng?: number
  // 행정코드
  sido?: string; sigungu?: string; dong?: string
  sidoCode?: string; sigunguCode?: string; dongCode?: string
  // 기타
  images?: string[]
  description?: string
  phone?: string               // 매물 주인 연락처(상세에 표시)
  availableFrom?: string       // ISO(YYYY-MM-DD) 입주 가능일
  // 메타(백엔드가 내려줄 수 있음)
  ownerId?: string
  status?: string
  postId?: string
  createdAt?: string
  updatedAt?: string
  // 구형 호환
  rent?: number
  theme?: string[]
  score?: number
  // 인기순 보조(백엔드/프론트 정렬용)
  viewsCount?: number
  favoritesCount?: number
}

export type NewListingPayload = {
  title: string
  description?: string
  address: string
  category: string
  images: string[]
  type: TradeType
  price?: number
  deposit?: number
  rentMonthly?: number
  maintenanceFee?: number
  area?: number
  lat?: number
  lng?: number
  sido?: string; sigungu?: string; dong?: string
  sidoCode?: string; sigunguCode?: string; dongCode?: string
  phone?: string
  availableFrom?: string
}

export type CategoryCount = { key: string; label: string; count: number }

export type Review = {
  id: string
  text: string
  rating?: number
  nickname?: string
  ownerKey?: string
  createdAt?: string
  updatedAt?: string
  canEdit?: boolean
}

// =======================================
// 기본 매물 리스트/상세
// =======================================

/** 리스트 조회 쿼리(백엔드 연동용) */
export type ListingsQuery = {
  q?: string
  region?: string
  type?: string
  theme?: string
  dealType?: 'all' | 'sale' | 'jeonse' | 'wolse'
  /** 가격순, 면적순, 인기순(popular) */
  sort?: 'price' | 'area' | 'popular'
  /** 특정 ID 모음으로 필터링 (예: "1,2,3") */
  ids?: string
}

export const getListings = (params: ListingsQuery = {}) =>
  api.get('/listings', { params }).then(r => r.data as Listing[])

export const getListing = (id: string | number) =>
  api.get(`/listings/${id}`).then(r => r.data as Listing)

// 등록/수정/삭제/내 매물
export const createListing = (body: NewListingPayload, ownerKey?: string) =>
  api.post('/listings', { ...body, ownerKey }, ownerHeaders(ownerKey)).then(r => r.data as Listing)

export const updateListing = (id: string | number, body: Partial<NewListingPayload>, ownerKey?: string) =>
  api.put(`/listings/${id}`, { ...body, ownerKey }, ownerHeaders(ownerKey)).then(r => r.data as Listing)

export const deleteListing = (id: string | number, ownerKey?: string) =>
  api.delete(`/listings/${id}`, {
    ...ownerHeaders(ownerKey),
    data: { ownerKey: ownerKey || ensureAuthKey() }, // axios DELETE body
  }).then(r => r.data as { ok: boolean } | { deleted: string | number })

export const getMyListings = (ownerKey?: string) =>
  api.get('/listings/mine', {
    params: { ownerKey: ownerKey || ensureAuthKey() },
    ...ownerHeaders(ownerKey),
  }).then(r => r.data as { items: Listing[] })

// =======================================
// AI
// =======================================
export const aiRecommend = (body:any) =>
  api.post('/ai/recommend', body).then(r => r.data)

export const aiSimulate = (body:any) =>
  api.post('/ai/simulate', body).then(r => r.data)

export const aiCompare = (a: string, b: string) =>
  api.post('/ai/compare', { regionA:a, regionB:b }).then(r => r.data)

/** 상권 분석 요청 타입 */
export type MarketAnalysisRequest = {
  location: string
  industries: string[]
  budget: string | null
  experience: string | null
  target?: string
  analyses: string[]
}
/** 상권 분석 응답 타입 */
export type MarketAnalysisResponse = {
  summary?: string
  recommendedCategories?: string[]
  hotZones?: { name: string; score: number }[]
  estimatedSales?: number
  bepSales?: number
  insights?: string[]
}
/** AI 상권분석 API */
export async function aiMarketAnalysis(body: MarketAnalysisRequest) {
  const r = await api.post<MarketAnalysisResponse>('/ai/market-analysis', body)
  return r.data
}

export async function aiChat(prompt: string): Promise<{ answer: string; listings?: Listing[] }> {
  try {
    const r = await api.post('/ai/chat', { prompt })
    return r.data
  } catch {
    const all = await getListings().catch(()=>[]) as Listing[]
    const picks = all
      .filter(l => {
        const bag = [l.title,l.address,l.region,l.type,(l.theme||[]).join(',')].join(' ').toLowerCase()
        return bag.includes(prompt.toLowerCase())
      })
      .slice(0,4)

    const answer = [
      `요청하신 “${prompt}”에 대해 기본 정보를 정리했어요.`,
      `• 관심 지역/업종/예산을 더 구체화하면 추천 정확도가 올라갑니다.`,
      picks.length ? `• 관련 매물 ${picks.length}건을 같이 보여드릴게요.` : `• 관련 매물을 못찾았어요. 키워드를 바꿔보실래요?`,
      `\n※ 서버형 AI(/api/ai/chat) 연결 시 더 풍부한 분석을 제공합니다.`,
    ].join('\n')
    return { answer, listings: picks.length ? picks : undefined }
  }
}

// =======================================
// 지도 연동(집계/반경/지역별 매물)
// =======================================
export type RegionLevel = 'sig' | 'emd'

export type RegionCount = {
  code: string
  name: string
  level: RegionLevel
  lat: number
  lng: number
  count: number
  parentCode?: string
}

export async function getRegionCounts(params: {
  level: RegionLevel
  sw: { lat:number, lng:number }
  ne: { lat:number, lng:number }
  center?: { lat:number, lng:number }
}): Promise<RegionCount[]> {
  try {
    const r = await api.get('/map/count-by-level', { params })
    return r.data as RegionCount[]
  } catch {
    await sleep(150)
    if (params.level === 'sig') {
      return [
        { code: '44210', name: '서산시', level: 'sig', lat: 36.781, lng: 126.452, count: 123 },
        { code: '44230', name: '당진시', level: 'sig', lat: 36.890, lng: 126.630, count: 98  },
        { code: '44150', name: '아산시', level: 'sig', lat: 36.790, lng: 127.000, count: 214 },
      ]
    }
    const c = params.center ?? { lat: 36.781, lng: 126.452 }
    return Array.from({ length: 12 }).map((_, i) => ({
      code: `44210-${i + 1}`,
      parentCode: '44210',
      name: `동문${i + 1}동`,
      level: 'emd' as const,
      lat: c.lat + (Math.random() - 0.5) * 0.12,
      lng: c.lng + (Math.random() - 0.5) * 0.12,
      count: Math.floor(Math.random() * 120) + 10,
    }))
  }
}

export async function getNearbyBizCounts(params: {
  code?: string
  lat?: number
  lng?: number
  radius?: number
}): Promise<Record<string, number>> {
  try {
    const r = await api.get('/map/nearby-biz-counts', { params })
    return r.data as Record<string, number>
  } catch {
    await sleep(120)
    const labels = [
      '카페/디저트','식당','주점/호프','편의','패션/액세서리',
      '뷰티/미용','의료/약국','문화/취미','레저/스포츠','사무/공유오피스',
      '숙박','창고/물류','팝업/쇼룸','기타'
    ]
    const out: Record<string, number> = {}
    labels.forEach(l => { out[l] = Math.floor(Math.random()*50) })
    return out
  }
}

export async function getListingsByRegion(params: {
  code: string
  level: RegionLevel
  limit?: number
}): Promise<Listing[]> {
  try {
    const r = await api.get('/map/listings-by-region', { params })
    return r.data as Listing[]
  } catch {
    const all = await getListings().catch(()=>[]) as Listing[]
    return all.slice(0, params.limit ?? 20)
  }
}

// 구버전 호환
export async function fetchRegionCounts(params: {
  level: 'sgg' | 'emd'
  parentCode?: string
}): Promise<RegionCount[]> {
  const level: RegionLevel = (params.level === 'sgg' ? 'sig' : 'emd')
  const sw = { lat: 36.4, lng: 126.0 }
  const ne = { lat: 37.1, lng: 127.2 }
  return getRegionCounts({ level, sw, ne })
}

export async function fetchCategoryCounts(params: {
  lat: number
  lng: number
  radius: number
  category?: string
}): Promise<CategoryCount[]> {
  try {
    const r = await api.get('/agg/radius-categories', { params })
    if (Array.isArray(r.data)) return r.data as CategoryCount[]
    if (r.data && typeof r.data === 'object') {
      return Object.entries(r.data).map(([label, count], i) => ({
        key: String(i),
        label,
        count: Number(count),
      }))
    }
  } catch { /* fallthrough */ }
  await sleep(180)
  const base: CategoryCount[] = [
    { key: 'cafe',        label: '카페/디저트', count: 12 },
    { key: 'food',        label: '식당',       count: 34 },
    { key: 'pub',         label: '주점/호프',  count: 7  },
    { key: 'convenience', label: '편의',       count: 9  },
    { key: 'beauty',      label: '뷰티/미용',  count: 5  },
  ]
  return base.map(c => ({ ...c, count: Math.max(0, Math.floor(c.count * (0.6 + Math.random()))) }))
}

// =======================================
// 리뷰 API
// =======================================
export async function getReviews(listingId: string | number): Promise<Review[]> {
  try {
    const r = await api.get(`/listings/${listingId}/reviews`)
    return r.data as Review[]
  } catch {
    return []
  }
}
export async function addReview(listingId: string | number, body: { text: string; rating?: number; nickname?: string }): Promise<Review> {
  const r = await api.post(`/listings/${listingId}/reviews`, body, ownerHeaders())
  return r.data as Review
}
export async function updateReview(listingId: string | number, reviewId: string | number, body: { text?: string; rating?: number }): Promise<Review> {
  const r = await api.put(`/listings/${listingId}/reviews/${reviewId}`, body, ownerHeaders())
  return r.data as Review
}
export async function deleteReview(listingId: string | number, reviewId: string | number): Promise<{ ok: boolean }> {
  const r = await api.delete(`/listings/${listingId}/reviews/${reviewId}`, ownerHeaders())
  return r.data as { ok: boolean }
}

// =======================================
// 즐겨찾기 API (서버 우선, 실패 시 로컬 폴백)
// =======================================
export async function addFavorite(listingId: string | number, ownerKey?: string) {
  const key = ownerKey || ensureAuthKey()
  try {
    const r = await api.post('/favorites', { listingId }, { headers: { 'X-Owner-Key': key } })
    return r.data
  } catch {
    addFavId(listingId, key)
    return { ok: true }
  }
}

export async function removeFavorite(listingId: string | number, ownerKey?: string) {
  const key = ownerKey || ensureAuthKey()
  try {
    const r = await api.delete(`/favorites/${listingId}`, { headers: { 'X-Owner-Key': key } })
    return r.data
  } catch {
    removeFavId(listingId, key)
    return { ok: true }
  }
}

export async function isFavorite(listingId: string | number, ownerKey?: string): Promise<boolean> {
  const key = ownerKey || ensureAuthKey()
  try {
    const r = await api.get(`/favorites/${listingId}`, { headers: { 'X-Owner-Key': key } })
    if (typeof r.data?.isFav === 'boolean') return r.data.isFav
  } catch { /* 폴백 */ }
  return isFavId(listingId, key)
}

// =======================================
// 창업 지원 뉴스(크롤링 연동)
// =======================================
export type SupportNews = {
  id: string
  title: string
  summary: string
  url: string
  source: string
  publishedAt: string
  tags?: string[]
  thumbnail?: string
}

export async function getSupportNews(params?: {
  page?: number
  pageSize?: number
  q?: string
  tag?: string
  since?: string
}): Promise<{ items: SupportNews[]; nextPage?: number }> {
  try {
    const r = await api.get('/support/news', { params })
    return r.data as { items: SupportNews[]; nextPage?: number }
  } catch {
    const page = params?.page ?? 1
    const pageSize = params?.pageSize ?? 10
    const base = Array.from({ length: pageSize }).map((_, i) => {
      const n = (page - 1) * pageSize + i + 1
      const now = new Date()
      now.setDate(now.getDate() - n)
      return {
        id: `dummy-${n}`,
        title: `[더미] 청년창업·지원금 뉴스 ${n}`,
        summary: '백엔드 연결 시 실제 크롤링 기사 요약이 표시됩니다.',
        url: 'https://www.mss.go.kr/',
        source: '중소벤처기업부',
        publishedAt: now.toISOString(),
        tags: ['청년창업', '지원금'],
        thumbnail: `https://picsum.photos/seed/startup${n}/640/360`,
      } as SupportNews
    })
    return { items: base, nextPage: page < 5 ? page + 1 : undefined }
  }
}

export async function getSupportNewsById(id: string): Promise<SupportNews | null> {
  try {
    const r = await api.get('/support/news/' + id)
    return r.data as SupportNews
  } catch {
    return null
  }
}

// =======================================
// (선택) 관리자형 지역 집계 엔드포인트
// =======================================
export async function getRegionCountsByAdmin(params: {
  province?: string
  level: 'sig' | 'emd'
  parentCode?: string
}): Promise<Array<{ code: string; name: string; count: number; lat?: number; lng?: number; level: 'sig' | 'emd' }>> {
  try {
    const qs = new URLSearchParams()
    if (params.province) qs.set('province', params.province)
    qs.set('level', params.level)
    if (params.parentCode) qs.set('parent', params.parentCode)

    const res = await fetch(`/api/region/counts?${qs.toString()}`)
    if (!res.ok) throw new Error('bad status')
    const json = await res.json()
    return Array.isArray(json) ? json : []
  } catch {
    if (params.level === 'sig') {
      return [
        { code: '44210', name: '서산시', count: 123, lat: 36.781, lng: 126.45, level: 'sig' },
        { code: '44131', name: '천안시', count: 412, lat: 36.815, lng: 127.15, level: 'sig' },
        { code: '44200', name: '아산시', count: 208, lat: 36.789, lng: 127.001, level: 'sig' },
        { code: '44710', name: '공주시', count: 77,  lat: 36.445, lng: 127.119, level: 'sig' },
      ]
    }
    return [
      { code: (params.parentCode || '') + '-101', name: '동문1동', count: 67, lat: 36.78, lng: 126.47, level: 'emd' },
      { code: (params.parentCode || '') + '-102', name: '동문2동', count: 39, lat: 36.79, lng: 126.46, level: 'emd' },
      { code: (params.parentCode || '') + '-103', name: '부춘동',   count: 54, lat: 36.77, lng: 126.45, level: 'emd' },
    ]
  }
}

// =======================================
// 관리자 CSV 업로드 (AdminUpload.tsx 사용)
// =======================================
export type AdminCsvUploadResult = {
  ok: boolean
  imported?: number
  skipped?: number
  errors?: Array<{ line: number; message: string }>
  preview?: Listing[]
}

/** 관리자 CSV 업로드 (mode=preview/commit) */
export async function adminUploadCSV(
  file: File | Blob,
  opts?: { mode?: 'preview' | 'commit'; ownerKey?: string }
): Promise<AdminCsvUploadResult> {
  const fd = new FormData()
  fd.append('file', file)
  if (opts?.mode) fd.append('mode', opts.mode)

  try {
    const r = await api.post('/admin/upload-csv', fd, {
      headers: {
        ...(opts?.ownerKey ? { 'X-Owner-Key': opts.ownerKey } : {}),
        // Content-Type은 브라우저가 boundary 포함해서 자동 설정하게 두는 것이 안전
      } as any,
    })
    return r.data as AdminCsvUploadResult
  } catch {
    // 백엔드 미연동 시에도 UI가 죽지 않도록 최소 폴백
    return { ok: true, imported: 0, skipped: 0, errors: [] }
  }
}
