// Kakao Local REST 연동: 주소지오코딩 + 키워드/카테고리 장소검색
// 문서: https://developers.kakao.com/docs/latest/ko/local/dev-guide
import axios from 'axios'
const KAKAO = process.env.KAKAO_REST_KEY

const client = axios.create({
  baseURL: 'https://dapi.kakao.com/v2/local',
  headers: KAKAO ? { Authorization: 'KakaoAK ' + KAKAO } : {}
})

// 주소 → 좌표
export async function geocodeAddressKakao(address){
  if(!KAKAO) return null
  const r = await client.get('/search/address.json', { params: { query: address } })
  const doc = r.data?.documents?.[0]
  if(!doc) return null
  return { lat: Number(doc.y), lng: Number(doc.x), provider: 'kakao' }
}

// 키워드 장소검색 (예: '카페', '치킨')
export async function searchKeyword({ query, x, y, radius=1000 }){
  if(!KAKAO || !(x && y)) return { total: 0, items: [] }
  const r = await client.get('/search/keyword.json', { params: { query, x, y, radius, sort: 'distance' } })
  const items = r.data?.documents || []
  return { total: Number(r.data?.meta?.total_count||items.length), items }
}

// 카테고리 코드 기반 장소검색 (예: CE7=카페, FD6=음식점)
export async function searchCategory({ code, x, y, radius=1000 }){
  if(!KAKAO || !(x && y)) return { total: 0, items: [] }
  const r = await client.get('/search/category.json', { params: { category_group_code: code, x, y, radius, sort: 'distance' } })
  const items = r.data?.documents || []
  return { total: Number(r.data?.meta?.total_count||items.length), items }
}