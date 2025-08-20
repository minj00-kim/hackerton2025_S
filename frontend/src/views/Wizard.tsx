import { useState } from 'react'
import { aiRecommend } from '../services/api'
import ListingCard from '../components/ListingCard'
import Card from '../components/Card'
import { useStore } from '../store/useStore'

const REGIONS = ['부춘동','동문1동','동문2동','수석동','석남동','해미읍']

export default function Wizard(){
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<any>(null)
  const save = useStore(s=>s.save)

  const [profile, setProfile] = useState({
    capital: 3000,
    desiredCategories: ['카페','분식'],
    regions: ['부춘동','동문1동'],
    preferences: { footTraffic: 4, rentSensitivity: 3, competitionTolerance: 3, youthPreference: 4 }
  })

  const run = async () => {
    setLoading(true)
    try {
      const r = await aiRecommend(profile as any)
      setResult(r)
      save({ ts: Date.now(), profile, result: r })
    } finally { setLoading(false) }
  }

  return (
    <div className="grid gap-6">
      <Card>
        <div className="font-semibold mb-2">창업자 정보 입력</div>
        <div className="grid sm:grid-cols-2 gap-3">
          <Number label="자본금(만원)" value={profile.capital} onChange={v=>setProfile(s=>({...s, capital:v}))} />
          <Multi label="희망 업종" value={profile.desiredCategories} onChange={v=>setProfile(s=>({...s, desiredCategories:v}))} options={['카페','치킨','분식','피자','편의점','독서실']} />
          <Multi label="선호 지역(서산)" value={profile.regions} onChange={v=>setProfile(s=>({...s, regions:v}))} options={REGIONS} />
        </div>
        <div className="grid sm:grid-cols-4 gap-3 mt-3">
          <Slider label="유동인구 선호" value={profile.preferences.footTraffic} onChange={v=>setProfile(s=>({...s, preferences:{...s.preferences, footTraffic:v}}))} />
          <Slider label="임대료 민감도" value={profile.preferences.rentSensitivity} onChange={v=>setProfile(s=>({...s, preferences:{...s.preferences, rentSensitivity:v}}))} />
          <Slider label="경쟁 허용도" value={profile.preferences.competitionTolerance} onChange={v=>setProfile(s=>({...s, preferences:{...s.preferences, competitionTolerance:v}}))} />
          <Slider label="청년 상권 선호" value={profile.preferences.youthPreference} onChange={v=>setProfile(s=>({...s, preferences:{...s.preferences, youthPreference:v}}))} />
        </div>
        <button className="btn-primary mt-4" onClick={run} disabled={loading}>{loading?'분석 중…':'AI 추천 받기'}</button>
      </Card>

      {result && (
        <>
          <Card>
            <div className="font-semibold mb-2">업종 추천</div>
            <div className="flex flex-wrap gap-2">
              {result.categories.map((c:string)=>(<span key={c} className="chip">{c}</span>))}
            </div>
          </Card>
          <Card>
            <div className="font-semibold mb-2">추천 상가 리스트</div>
            <div className="grid sm:grid-cols-2 gap-4">
              {result.listings.map((it:any) => <ListingCard key={it.id} item={it} />)}
            </div>
          </Card>
          <Card>
            <div className="font-semibold mb-2">추천 근거</div>
            <ul className="text-sm text-gray-700 list-disc pl-5 space-y-1">
              {result.reasons.map((r:string, i:number)=>(<li key={i}>{r}</li>))}
            </ul>
          </Card>
        </>
      )}
    </div>
  )
}

function Number({label, value, onChange}:{label:string; value:number; onChange:(v:number)=>void}){
  return <label className="block text-sm">
    <div className="text-gray-600 mb-1">{label}</div>
    <input type="number" value={value} onChange={e=>onChange(Number(e.target.value))} className="w-full border rounded-xl px-3 py-2"/>
  </label>
}

function Multi({label, value, options, onChange}:{label:string; value:string[]; options:string[]; onChange:(v:string[])=>void}){
  return <label className="block text-sm">
    <div className="text-gray-600 mb-1">{label}</div>
    <select multiple value={value} onChange={e=>onChange(Array.from(e.target.selectedOptions).map(o=>o.value))}
      className="w-full border rounded-xl px-3 py-2 h-28">
      {options.map(o=> <option key={o} value={o}>{o}</option>)}
    </select>
  </label>
}

function Slider({label, value, onChange}:{label:string; value:number; onChange:(v:number)=>void}){
  return <label className="block text-sm">
    <div className="text-gray-600 mb-1">{label}: {value}</div>
    <input type="range" min={1} max={5} value={value} onChange={e=>onChange(Number(e.target.value))} className="w-full"/>
  </label>
}