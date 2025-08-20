// AI 로직: 추천/시뮬/비교 — Kakao 기반 경쟁도 + 휴리스틱
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { measureCompetition, scoreByCompetition, rentIndex, footTrafficHeuristic, youthHeuristic } from './metrics.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const DATA_PATH = path.join(__dirname, '..', 'data', 'listings.json')

function loadListings(){
  try { return JSON.parse(fs.readFileSync(DATA_PATH, 'utf-8')) }
  catch { return [] }
}
function saveListings(rows){
  fs.writeFileSync(DATA_PATH, JSON.stringify(rows, null, 2), 'utf-8')
}

export async function recommend(profile){
  const rows = loadListings()
  const target = rows.filter(r => (profile?.regions||[]).includes(r.region))
  const rated = []
  for(const it of target){
    const comp = await measureCompetition({ lat: it.lat, lng: it.lng })
    const compScore = scoreByCompetition(comp.total)
    const rentIdx = rentIndex({ rent: it.rent, area: it.area })
    const ft = footTrafficHeuristic(it.region)
    const youth = youthHeuristic(it.region)

    const p = profile?.preferences || { footTraffic:3, rentSensitivity:3, competitionTolerance:3, youthPreference:3 }
    const score =
      compScore * (p.competitionTolerance/5) +
      (100-rentIdx) * (p.rentSensitivity/5) +
      ft * (p.footTraffic/5) +
      youth * (p.youthPreference/5)

    rated.push({ ...it, score: Math.round(score) })
  }
  rated.sort((a,b)=> (b.score||0)-(a.score||0))

  const categories = Array.from(new Set([...(profile?.desiredCategories||['카페','분식','치킨'])]))
  const top = rated.slice(0, 4)
  const reasons = [
    '주변 경쟁도/임대/유동/청년 지표를 반영했습니다.',
    'Kakao 장소검색 기반 근사치이므로 참고용으로 활용하세요.'
  ]
  return { categories, listings: top, reasons }
}

export async function simulate(payload){
  const ft = footTrafficHeuristic(payload.region)
  const base = 1200 + ft*10
  const estimatedSales = Math.max(800, Math.round(base + (payload.area||50)*3))
  const cogsRate = payload.cogsRate ?? 0.33
  const grossProfit = Math.round(estimatedSales * (1 - cogsRate))
  const fixedCosts = Math.round((payload.rent||200) + (payload.labor||300) + (payload.misc||100))
  const operatingProfit = grossProfit - fixedCosts
  const bepSales = Math.round(fixedCosts / Math.max(0.05, (1 - cogsRate - 0.25)))
  return { estimatedSales, operatingProfit, bepSales, recommendedCategory: payload.category || '카페' }
}

export async function compare(regionA, regionB){
  const ftA = footTrafficHeuristic(regionA), ftB = footTrafficHeuristic(regionB)
  const rentA = 60 + Math.abs(regionA.charCodeAt(0)%20), rentB = 60 + Math.abs(regionB.charCodeAt(0)%20)
  const compA = 30 + (regionA.length*3)%50, compB = 30 + (regionB.length*3)%50
  const youthA = youthHeuristic(regionA), youthB = youthHeuristic(regionB)

  const summary = `${regionA}는 유동 ${ftA}, 임대 ${rentA}, 경쟁 ${compA} / ${regionB}는 유동 ${ftB}, 임대 ${rentB}, 경쟁 ${compB} 입니다.`
  return {
    A: { footTraffic: ftA, rentIndex: rentA, competition: compA, youth: youthA },
    B: { footTraffic: ftB, rentIndex: rentB, competition: compB, youth: youthB },
    summary
  }
}

export const ListingStore = { load: loadListings, save: saveListings }