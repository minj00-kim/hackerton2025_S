import axios from 'axios'
import 'dotenv/config'
import { geocodeAddressKakao } from './kakao.js'

const NAVER_ID = process.env.NAVER_CLIENT_ID
const NAVER_SECRET = process.env.NAVER_CLIENT_SECRET

export async function geocodeAddress(address) {
    // 1) Kakao
    try {
        const k = await geocodeAddressKakao(address)
        if (k) return k
    } catch {}
    // 2) Naver
    try {
        if (!NAVER_ID || !NAVER_SECRET) return null
        const r = await axios.get('https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode', {
            params: { query: address },
            headers: {
                'X-NCP-APIGW-API-KEY-ID': NAVER_ID,
                'X-NCP-APIGW-API-KEY': NAVER_SECRET
            }
        })
        const item = r.data?.addresses?.[0]
        if (!item) return null
        return { lat: Number(item.y), lng: Number(item.x), provider: 'naver' }
    } catch {}
    return null
}
