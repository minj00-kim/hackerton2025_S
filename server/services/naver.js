// Naver Cloud Maps Geocoding (선택): Kakao 실패시 대안
// 문서: https://api.ncloud-docs.com/docs/en/ai-naver-mapsgeocoding
import axios from 'axios'
const NAVER_ID = process.env.NAVER_CLIENT_ID
const NAVER_SECRET = process.env.NAVER_CLIENT_SECRET

export async function geocodeAddressNaver(address){
  if(!(NAVER_ID && NAVER_SECRET)) return null
  const r = await axios.get('https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode', {
    params: { query: address },
    headers: {
      'X-NCP-APIGW-API-KEY-ID': NAVER_ID,
      'X-NCP-APIGW-API-KEY': NAVER_SECRET
    }
  })
  const item = r.data?.addresses?.[0]
  if(!item) return null
  return { lat: Number(item.y), lng: Number(item.x), provider: 'naver' }
}