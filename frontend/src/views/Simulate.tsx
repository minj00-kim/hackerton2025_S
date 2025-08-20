import { useState } from 'react'
import { aiSimulate } from '../services/api'
import Card from '../components/Card'

export default function Simulate(){
  const [form, setForm] = useState({ region:'부춘동', category:'카페', area:70, rent:250, cogsRate:0.35, labor:350, misc:120 })
  const [res, setRes] = useState<any>(null)
  const run = async () => setRes(await aiSimulate(form))
  return (
    <Card>
      <div className="grid sm:grid-cols-3 gap-3">
        {Object.entries(form).map(([k,v]) => (
          <label key={k} className="text-sm">
            <div className="text-gray-600 mb-1">{k}</div>
            <input value={v as any} onChange={e=>setForm(s=>({...s, [k]: isNaN(Number(e.target.value))? e.target.value : Number(e.target.value)}))}
              className="w-full border rounded-xl px-3 py-2" />
          </label>
        ))}
      </div>
      <button className="btn-primary mt-3" onClick={run}>계산</button>
      {res && (
        <div className="mt-3 text-sm">
          <div>예상 매출: {res.estimatedSales} 만원</div>
          <div>영업이익: {res.operatingProfit} 만원</div>
          <div>BEP 매출: {res.bepSales} 만원</div>
        </div>
      )}
    </Card>
  )
}