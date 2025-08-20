import { Link } from 'react-router-dom'
import type { Listing } from '../services/api'

export default function ListingCard({ item }:{ item: Listing }){
  return (
    <Link to={`/listings/${item.id}`} className="block card overflow-hidden hover:shadow-lg transition">
      <div className="h-40 bg-gray-100" style={{backgroundImage:`url(${item.images?.[0]||""})`, backgroundSize:'cover', backgroundPosition:'center'}} />
      <div className="p-4">
        <div className="font-semibold">{item.title}</div>
        <div className="text-xs text-gray-500">{item.region} · {item.address}</div>
        <div className="text-sm mt-2">{item.area}㎡ · 보증금 {item.deposit}만원 · 월세 {item.rent}만원</div>
        <div className="mt-2 flex gap-1 flex-wrap">
          {(item.theme||[]).slice(0,3).map(t => <span key={t} className="chip">{t}</span>)}
        </div>
      </div>
    </Link>
  )
}