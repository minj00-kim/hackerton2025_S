// 지오코딩 통합: Kakao 우선, 실패 시 Naver로 폴백
import { geocodeAddressKakao } from './kakao.js'
import { geocodeAddressNaver } from './naver.js'

/** 주소 문자열을 받아 좌표를 반환합니다.
 *  - 우선 Kakao 주소검색을 시도
 *  - 실패/키없음이면 Naver로 폴백
 *  - 둘 다 실패하면 null
 */
export async function geocodeAddress(address){
  if(!address) return null
  try {
    const k = await geocodeAddressKakao(address)
    if(k) return k
  } catch(e){ /* ignore */ }
  try {
    const n = await geocodeAddressNaver(address)
    if(n) return n
  } catch(e){ /* ignore */ }
  return null
}