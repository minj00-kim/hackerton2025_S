import { useEffect, useState } from 'react'
import { getListings } from '../services/api'
import ListingCard from '../components/ListingCard'
import Card from '../components/Card'

export default function Listings(){
  const [rows, setRows] = useState<any[]>([])
  const [filters, setFilters] = useState({ q:'', region:'', type:'', theme:'', minArea:'', maxArea:'', minRent:'', maxRent:'' })

  // URL 파라미터에서 초기값 주입
  useEffect(()=>{
    const query = new URLSearchParams(location.search)
    const theme = query.get('theme') || ''
    const q = query.get('q') || ''
    setFilters((s)=>({ ...s, theme, q }))
  },[])

  // 필터 변경되면 자동 로드
  useEffect(()=>{
    const load = async () => {
      const params:any = {}
      Object.entries(filters).forEach(([k,v])=>{ if(String(v).length) params[k]=v })
      setRows(await getListings(params))
    }
    load()
  }, [filters])

  const selectedTheme = filters.theme

  return (
    <div className="space-y-4">
      <Card>
        <div className="text-lg font-semibold mb-1">공실 매물</div>
        {selectedTheme && (
          <div className="text-sm text-brand-700 mb-2">선택 카테고리: <b>{selectedTheme}</b></div>
        )}
        <div className="grid md:grid-cols-6 gap-2">
          <input className="border rounded-xl px-3 py-2 text-sm md:col-span-2" placeholder="키워드" value={filters.q} onChange={e=>setFilters(s=>({...s,q:e.target.value}))} />
          <input className="border rounded-xl px-3 py-2 text-sm" placeholder="지역" value={filters.region} onChange={e=>setFilters(s=>({...s,region:e.target.value}))} />
          <select className="border rounded-xl px-3 py-2 text-sm" value={filters.type} onChange={e=>setFilters(s=>({...s,type:e.target.value}))}>
            <option value="">유형</option>
            <option>상가</option><option>지하상가</option><option>사무/학원</option>
          </select>
          <input className="border rounded-xl px-3 py-2 text-sm" placeholder="테마" value={filters.theme} onChange={e=>setFilters(s=>({...s,theme:e.target.value}))} />
          <button className="btn-primary text-sm" onClick={()=>setFilters({...filters})}>검색</button>
        </div>
      </Card>

      <div className="grid gap-4">
        {rows.map((it:any)=>(<ListingCard key={it.id} item={it} />))}
      </div>
    </div>
  )
}
