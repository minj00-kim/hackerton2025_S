// 간단 지표 계산기: Kakao 주변 업소 수를 이용해 경쟁도 등을 근사
import { searchKeyword, searchCategory } from './kakao.js'

const CATEGORY_CODES = {
  cafe: 'CE7',        // 카페
  restaurant: 'FD6',  // 음식점 전체
  convenience: 'CS2', // 편의점
}

/** 특정 좌표 주변 경쟁도 측정 (반경 m, 키워드 배열 기준) */
export async function measureCompetition({ lat, lng, keywords=['카페','분식','치킨'], radius=700 }){
  if(!(lat && lng)) return { total: 0, byKeyword: {}, byCategory:{} }
  const x = lng, y = lat
  const byKeyword = {}
  for(const q of keywords){
    try {
      const r = await searchKeyword({ query:q, x, y, radius })
      byKeyword[q] = r.total
    } catch{ byKeyword[q] = 0 }
  }
  const byCategory = {}
  for(const [name, code] of Object.entries(CATEGORY_CODES)){
    try {
      const r = await searchCategory({ code, x, y, radius })
      byCategory[name] = r.total
    } catch{ byCategory[name] = 0 }
  }
  const total = Object.values(byKeyword).reduce((a,b)=>a+(b||0),0)
  return { total, byKeyword, byCategory }
}

export function scoreByCompetition(total){
  if(!total || total<=5) return 85
  if(total<=20) return 70
  if(total<=50) return 55
  if(total<=120) return 40
  return 25
}

export function rentIndex({ rent, area }){
  if(!(rent && area)) return 50
  const per = rent / Math.max(20, area)
  const idx = Math.min(6, Math.max(2, per))
  return Math.round( (idx-2)/(6-2) * 70 + 20 )
}

export function footTrafficHeuristic(region){
  const table = { '부춘동':80,'동문1동':75,'동문2동':68,'수석동':60,'석남동':65,'해미읍':50 }
  return table[region] ?? 55
}
export function youthHeuristic(region){
  const table = { '부춘동':65,'동문1동':70,'동문2동':62,'수석동':58,'석남동':60,'해미읍':45 }
  return table[region] ?? 55
}