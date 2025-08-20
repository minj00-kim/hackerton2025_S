import { useState } from 'react'
import { aiCompare } from '../services/api'
import Card from '../components/Card'

export default function Compare(){
  const [a, setA] = useState('부춘동')
  const [b, setB] = useState('동문1동')
  const [res, setRes] = useState<any>(null)
  const run = async () => setRes(await aiCompare(a,b))
  return (
    <Card>
      <div className="grid sm:grid-cols-4 gap-3">
        <label className="text-sm"><div className="text-gray-600 mb-1">지역 A</div><input value={a} onChange={e=>setA(e.target.value)} className="w-full border rounded-xl px-3 py-2" /></label>
        <label className="text-sm"><div className="text-gray-600 mb-1">지역 B</div><input value={b} onChange={e=>setB(e.target.value)} className="w-full border rounded-xl px-3 py-2" /></label>
        <button className="btn-primary self-end" onClick={run}>분석</button>
      </div>
      {res && (
        <div className="mt-4 grid gap-4 text-sm">
          <div>
            <div className="font-semibold mb-2">지표</div>
            <ul className="space-y-1">
              <li>유동(근사): A {res.A.footTraffic} / B {res.B.footTraffic}</li>
              <li>임대료지수(근사): A {res.A.rentIndex} / B {res.B.rentIndex}</li>
              <li>경쟁도(주변 업소 수): A {res.A.competition} / B {res.B.competition}</li>
              <li>청년비율(근사): A {res.A.youth} / B {res.B.youth}</li>
            </ul>
          </div>
          <div>
            <div className="font-semibold mb-2">요약</div>
            <div>{res.summary}</div>
          </div>
        </div>
      )}
    </Card>
  )
}