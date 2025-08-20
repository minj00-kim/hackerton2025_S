import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getListing, aiSimulate } from '../services/api'
import Card from '../components/Card'
import Map from '../components/Map'

export default function Detail(){
  const { id } = useParams()
  const [item, setItem] = useState<any>(null)
  const [sim, setSim] = useState<any>(null)
  useEffect(()=>{ if(id) getListing(id).then(setItem) }, [id])
  const runSim = async () => {
    if(!item) return
    const r = await aiSimulate({ region: item.region, category: '카페', area: item.area, rent: item.rent })
    setSim(r)
  }
  if(!item) return <div className="text-sm text-gray-500">불러오는 중…</div>
  return (
    <div className="space-y-4">
      <Card>
        <img src={item.images?.[0]} className="w-full h-72 object-cover rounded-xl" />
        <h1 className="text-2xl font-bold mt-3">{item.title}</h1>
        <div className="text-sm text-gray-500">{item.region} · {item.address}</div>
        <div className="text-sm mt-2">{item.area}㎡ · 보증금 {item.deposit}만원 · 월세 {item.rent}만원</div>
        <div className="mt-3 flex gap-2 flex-wrap">
          {(item.theme||[]).map((t:string)=>(<span key={t} className="chip">{t}</span>))}
        </div>
        <button className="btn-primary mt-4" onClick={runSim}>추천 업종·예상 매출</button>
        {sim && (
          <div className="mt-3 text-sm">
            <div>추천 업종: {sim.recommendedCategory}</div>
            <div>예상 월매출: {sim.estimatedSales} 만원</div>
            <div>BEP 매출: {sim.bepSales} 만원</div>
          </div>
        )}
      </Card>
      {item.lat && item.lng && (
        <Card>
          <div className="font-semibold mb-2">지도</div>
          <Map lat={item.lat} lng={item.lng} />
        </Card>
      )}
    </div>
  )
}