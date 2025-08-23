// src/views/Detail.tsx
import { useEffect, useMemo, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api, getListing, aiSimulate, type Listing } from '../services/api'
import Card from '../components/Card'
import Map from '../components/Map'
import RadiusStats from '../components/RadiusStats'
import { ensureAuthKey as getAuthKey, maskKey } from '../lib/auth'
import { isFav, toggleFav, type MinimalListing } from '../lib/fav'

/* ---------- 유틸/타입 ---------- */
const tradeLabel: Record<string, string> = { SALE: '매매', JEONSE: '전세', MONTHLY: '월세' }
const fmt = (n?: number) => (n || n === 0) ? n.toLocaleString() : '-'
const fmtDate = (iso?: string) => (iso ? new Date(iso).toLocaleString() : '-')

// deal 타입이 명시되지 않으면 필드로 유추
type Deal = 'SALE' | 'JEONSE' | 'MONTHLY'
function inferDealType(item: Listing): Deal {
  const t = (item as any).type as Deal | undefined
  if (t === 'SALE' || t === 'JEONSE' || t === 'MONTHLY') return t
  const rent = (item.rentMonthly ?? (item as any).rent) ?? 0
  const price = item.price ?? 0
  const deposit = item.deposit ?? 0
  if (rent > 0) return 'MONTHLY'
  if (rent === 0 && deposit > 0 && price === 0) return 'JEONSE'
  return 'SALE'
}

function formatPhone(p?: string) {
  if (!p) return '-'
  const d = p.replace(/\D/g, '')
  if (d.length === 11) return `${d.slice(0,3)}-${d.slice(3,7)}-${d.slice(7)}`
  if (d.length === 10) return `${d.slice(0,3)}-${d.slice(3,6)}-${d.slice(6)}`
  return p
}
function telHref(p?: string) {
  if (!p) return undefined
  const d = p.replace(/\D/g, '')
  return d ? `tel:${d}` : undefined
}
function fmtAvail(s?: string) {
  if (!s) return '-'
  const d = new Date(s)
  return isNaN(+d) ? s : d.toLocaleDateString()
}

/* ---------- 가격/면적/관리비 + 연락처/입주일 블록 ---------- */
function PriceBlock({ item }: { item: Listing }) {
  const deal = inferDealType(item)
  const phone = (item as any).ownerPhone ?? (item as any).phone as string | undefined
  const availableFrom = (item as any).availableFrom as string | undefined

  return (
    <>
      <div className="flex flex-wrap gap-x-6 gap-y-1 text-[15px]">
        <div><span className="text-gray-500 mr-1">거래유형</span><b>{tradeLabel[deal] ?? deal}</b></div>
        {deal === 'MONTHLY' ? (
          <>
            <div><span className="text-gray-500 mr-1">보증금</span><b>{fmt(item.deposit)}만원</b></div>
            <div><span className="text-gray-500 mr-1">월세</span><b>{fmt(item.rentMonthly ?? (item as any).rent)}만원</b></div>
          </>
        ) : deal === 'SALE' ? (
          <div><span className="text-gray-500 mr-1">매매가</span><b>{fmt(item.price)}만원</b></div>
        ) : (
          <div><span className="text-gray-500 mr-1">전세보증금</span><b>{fmt(item.deposit)}만원</b></div>
        )}
        <div><span className="text-gray-500 mr-1">관리비</span><b>{fmt(item.maintenanceFee)}만원</b></div>
        <div><span className="text-gray-500 mr-1">면적</span><b>{fmt(item.area)}㎡</b></div>
      </div>

      {/* 추가 정보: 연락처/입주 가능일 */}
      <div className="mt-2 flex flex-wrap gap-x-6 gap-y-1 text-[15px]">
        <div>
          <span className="text-gray-500 mr-1">연락처</span>
          {phone ? (
            <a className="font-semibold underline-offset-2 hover:underline" href={telHref(phone)}>{formatPhone(phone)}</a>
          ) : <b>-</b>}
        </div>
        <div>
          <span className="text-gray-500 mr-1">입주 가능일</span>
          <b>{fmtAvail(availableFrom)}</b>
        </div>
      </div>
    </>
  )
}

/* ---------- 하트(즐겨찾기) 버튼 ---------- */
function FavButton({ card, onChanged }: { card: MinimalListing, onChanged?: (v:boolean)=>void }) {
  const [fav, setFav] = useState<boolean>(false)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    isFav(card.id).then(setFav).catch(()=>setFav(false))
  }, [card.id])

  const toggle = async () => {
    if (busy) return
    setBusy(true)
    try {
      const next = await toggleFav(card)
      setFav(next)
      onChanged?.(next)
    } finally {
      setBusy(false)
    }
  }

  return (
    <button
      onClick={toggle}
      disabled={busy}
      aria-label={fav ? '즐겨찾기 해제' : '즐겨찾기 추가'}
      className={'ml-auto inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-sm border ' +
        (fav ? 'bg-rose-50 text-rose-600 border-rose-200' : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50')}
    >
      <svg width="18" height="18" viewBox="0 0 24 24" className={fav ? 'fill-rose-500' : 'fill-none stroke-current'}>
        <path d="M12 21s-6.716-4.273-9.2-7.2C.8 10.8 2.4 6 6.8 6c2.345 0 3.6 1.6 3.6 1.6S11.655 6 14 6c4.4 0 6 4.8 4 7.8C18.716 16.727 12 21 12 21Z" strokeWidth="1.8" />
      </svg>
      {fav ? '저장됨' : '즐겨찾기'}
    </button>
  )
}

/* ---------- 리뷰 아이템 ---------- */
type Review = {
  id: string
  ownerKey: string
  text: string
  rating?: number
  nickname?: string
  createdAt?: string
  updatedAt?: string
  canEdit?: boolean
}

function ReviewItem({
  v, onEdit, onDelete,
}: {
  v: Review; onEdit: (id: string, text: string, rating?: number) => void; onDelete: (id: string) => void
}) {
  const [editing, setEditing] = useState(false)
  const [text, setText] = useState(v.text)
  const [rating, setRating] = useState<number | undefined>(v.rating)
  const save = async () => { await onEdit(v.id, text.trim(), rating); setEditing(false) }
  return (
    <div className="p-3 rounded-xl border bg-white">
      <div className="flex items-center gap-2 text-sm text-gray-600">
        <span className="font-medium text-gray-800">{v.nickname || '익명'}</span>
        {typeof v.rating === 'number' &&
          <span className="text-amber-500">★ {v.rating}</span>}
        <span className="ml-auto">{fmtDate(v.createdAt)}</span>
      </div>
      {editing ? (
        <div className="mt-2 space-y-2">
          <textarea value={text} onChange={e=>setText(e.target.value)} rows={3}
            className="w-full border rounded-lg px-3 py-2 text-sm outline-brand-400" />
          <div className="flex items-center gap-2">
            <select value={rating ?? ''} onChange={e=>setRating(e.target.value?Number(e.target.value):undefined)}
              className="border rounded px-2 py-1 text-sm">
              <option value="">평점 없음</option>
              {[1,2,3,4,5].map(n=> <option key={n} value={n}>{n}</option>)}
            </select>
            <button className="btn-primary" onClick={save}>저장</button>
            <button className="btn-outline" onClick={()=>{ setEditing(false); setText(v.text); setRating(v.rating) }}>취소</button>
          </div>
        </div>
      ) : (
        <p className="mt-2 text-[15px] leading-relaxed whitespace-pre-wrap">{v.text}</p>
      )}
      {v.canEdit && !editing && (
        <div className="mt-2 flex gap-2">
          <button className="btn-outline" onClick={()=>setEditing(true)}>수정</button>
          <button className="btn-outline text-rose-600 border-rose-200" onClick={()=>onDelete(v.id)}>삭제</button>
        </div>
      )}
    </div>
  )
}

/* ---------- 상세 페이지 ---------- */
export default function Detail(){
  const { id } = useParams()
  const [item, setItem] = useState<Listing | null>(null)
  const [sim, setSim] = useState<any>(null)

  // 리뷰
  const [reviews, setReviews] = useState<Review[]>([])
  const [rvText, setRvText] = useState('')
  const [rvNick, setRvNick] = useState('')
  const [rvRating, setRvRating] = useState<number | undefined>()
  const myKey = useMemo(() => getAuthKey(), [])

  // 즐겨찾기 카드 구성
  const favCard: MinimalListing | null = useMemo(() => {
    if (!item) return null
    return {
      id: String(item.id),
      title: item.title,
      address: item.address,
      type: (item as any).type,
      price: item.price ?? item.deposit,
      rentMonthly: item.rentMonthly ?? (item as any).rent,
      thumb: item.images?.[0],
    }
  }, [item])

  useEffect(() => { if (id) getListing(id).then(setItem) }, [id])

  useEffect(() => {
    if (!id) return
    loadReviews(id)
  }, [id])

  async function loadReviews(listingId: string) {
    try {
      const r = await api.get(`/listings/${listingId}/reviews`)
      setReviews(Array.isArray(r.data) ? r.data : [])
    } catch {
      setReviews([])
    }
  }

  const runSim = async () => {
    if(!item) return
    const r = await aiSimulate({
      region: item.region,
      category: item.category || '카테고리',
      area: item.area,
      rent: item.rentMonthly ?? (item as any).rent ?? 0
    })
    setSim(r)
  }

  // 리뷰 CRUD (쿠키키 헤더)
  const submitReview = async () => {
    if (!id || !rvText.trim()) return
    const body = { text: rvText.trim(), rating: rvRating, nickname: rvNick || undefined }
    const r = await api.post(`/listings/${id}/reviews`, body, {
      headers: { 'X-Owner-Key': myKey },
    })
    const created: Review = r.data
    setReviews(prev => [created, ...prev])
    setRvText(''); setRvNick(''); setRvRating(undefined)
  }
  const editReview = async (rid: string, text: string, rating?: number) => {
    if (!id) return
    const r = await api.put(`/listings/${id}/reviews/${rid}`, { text, rating }, {
      headers: { 'X-Owner-Key': myKey },
    })
    const updated: Review = r.data
    setReviews(rs => rs.map(rv => rv.id === rid ? updated : rv))
  }
  const removeReview = async (rid: string) => {
    if (!id) return
    await api.delete(`/listings/${id}/reviews/${rid}`, {
      headers: { 'X-Owner-Key': myKey },
    })
    setReviews(rs => rs.filter(rv => rv.id !== rid))
  }

  if(!item) return <div className="text-sm text-gray-500">불러오는 중…</div>

  return (
    // ✅ 상세 페이지 폭 제한
    <div className="mx-auto px-4 w-[94vw] md:w-[80vw] xl:max-w-[1050px] space-y-6">
      {/* 헤더 카드(이미지, 타이틀, 즐겨찾기) */}
      <Card>
        {item.images?.[0] && (
          <img src={item.images[0]} alt={item.title} className="w-full h-72 object-cover rounded-xl" />
        )}

        <div className="mt-3 flex items-start gap-3">
          <h1 className="text-2xl font-bold">{item.title}</h1>
          {favCard && <FavButton card={favCard} />}
        </div>

        <div className="text-sm text-gray-500">
          {item.region ? `${item.region} · ` : ''}{item.address}
        </div>

        {/* 카테고리/태그 */}
        <div className="mt-2 flex flex-wrap gap-2">
          {item.category && <span className="chip">{item.category}</span>}
          {(item.theme||[]).map(t => <span key={t} className="chip">{t}</span>)}
        </div>

        {/* 가격/면적/관리비 + 연락처/입주일 */}
        <div className="mt-3"><PriceBlock item={item} /></div>

        {/* 설명 */}
        {item.description && (
          <p className="mt-3 text-[15px] leading-relaxed whitespace-pre-wrap">{item.description}</p>
        )}
      </Card>

      {/* 지도 (세로 높이 확장) */}
{item.lat && item.lng && (
  <Card>
    <div className="py-4">
      <div className="font-semibold mb-3">지도</div>
      {/* md 이상에서 더 크게: 380px → 520px */}
      <Map lat={item.lat} lng={item.lng} className="h-[380px] md:h-[520px]" />
      {/* 또는 픽셀로 고정하고 싶다면: <Map lat={...} lng={...} height={520} /> */}
    </div>
  </Card>
)}

      {/* 반경 내 카테고리 집계 (기본/최대 800m) */}
      {(item?.lat && item?.lng) && (
        <section className="mt-2">
          <RadiusStats
            lat={item.lat!}
            lng={item.lng!}
            defaultRadius={800}
            maxRadius={800}
            categories={[
              '카페/디저트','식당','주점/호프','편의','패션/액세서리',
              '뷰티/미용','의료/약국','문화/취미','레저/스포츠','사무/공유오피스',
              '숙박','창고/물류','팝업/쇼룸','기타'
            ]}
          />
        </section>
      )}

      {/* 리뷰 섹션 */}
      <Card>
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">매물 리뷰</h2>
          <span className="text-xs text-gray-500">내 식별키: {maskKey(myKey)}</span>
        </div>

        {/* 작성 폼 */}
        <div className="mt-3 p-3 rounded-xl border bg-white">
          <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
            <input
              value={rvNick}
              onChange={e=>setRvNick(e.target.value)}
              placeholder="닉네임(선택)"
              className="border rounded-lg px-3 py-2 text-sm outline-brand-400"
            />
            <select
              value={rvRating ?? ''}
              onChange={e=>setRvRating(e.target.value ? Number(e.target.value) : undefined)}
              className="border rounded-lg px-3 py-2 text-sm"
            >
              <option value="">평점 없음</option>
              {[1,2,3,4,5].map(n => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
          <textarea
            value={rvText}
            onChange={e=>setRvText(e.target.value)}
            placeholder="리뷰를 입력하세요. (쿠키로 작성자 보호, 로그인 불필요)"
            rows={3}
            className="mt-2 w-full border rounded-lg px-3 py-2 text-sm outline-brand-400"
          />
          <div className="mt-2 flex justify-end">
            <button className="btn-primary" onClick={submitReview} disabled={!rvText.trim()}>등록</button>
          </div>
        </div>

        {/* 목록 */}
        <div className="mt-4 grid gap-3">
          {reviews.length === 0 && (
            <div className="text-sm text-gray-500">아직 작성된 리뷰가 없습니다.</div>
          )}
          {reviews.map(rv => (
            <ReviewItem key={rv.id} v={rv} onEdit={editReview} onDelete={removeReview} />
          ))}
        </div>
      </Card>

      {/* 안내 */}
      <div className="text-xs text-gray-500">
        ※ 즐겨찾기는 <Link to="/account" className="underline">마이페이지</Link> &gt; 즐겨찾기에서 확인할 수 있어요.
      </div>
    </div>
  )
}
