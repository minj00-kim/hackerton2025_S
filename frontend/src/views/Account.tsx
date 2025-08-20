import Card from '../components/Card'
import { useStore } from '../store/useStore'

export default function Account(){
  const saved = useStore(s=>s.saved)
  return (
    <Card>
      <div className="font-semibold mb-2">저장한 추천 결과</div>
      <ul className="text-sm list-disc pl-5 space-y-2">
        {saved.map((x:any, i:number)=>(
          <li key={i}>
            추천 {new Date(x.ts).toLocaleString()} — 상가 {x.result.listings.length}건 / 업종 {x.result.categories.length}개
          </li>
        ))}
      </ul>
    </Card>
  )
}