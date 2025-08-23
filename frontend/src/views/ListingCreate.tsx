// src/views/ListingCreate.tsx
import React, { useEffect, useMemo, useState } from 'react'
import { createListing, deleteListing, getMyListings } from '../services/api'

function getCookie(name: string) {
  const m = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)')
  return m ? decodeURIComponent(m.pop()!) : ''
}
function setCookie(name: string, value: string, days = 365) {
  const d = new Date()
  d.setTime(d.getTime() + days*24*60*60*1000)
  document.cookie = `${name}=${encodeURIComponent(value)}; expires=${d.toUTCString()}; path=/`
}
const ensureOwnerKey = (v?: string) =>
  v && v.trim() ? v.trim() : ('ok-' + Math.random().toString(36).slice(2, 10))

type TradeType = 'SALE' | 'JEONSE' | 'MONTHLY'

export default function ListingCreate() {
  const [tab, setTab] = useState<'create'|'manage'>('create')

  // 쿠키 기반 관리키
  const [ownerKey, setOwnerKey] = useState(getCookie('ownerKey') || '')
  useEffect(() => { if (ownerKey) setCookie('ownerKey', ownerKey) }, [ownerKey])

  // 폼 상태
  const [form, setForm] = useState({
    title: '',
    description: '',
    address: '',
    category: '',
    imagesText: '',
    type: 'SALE' as TradeType,
    price: '' as string | number,       // 매매/전세 공용
    deposit: '' as string | number,     // 월세용
    rentMonthly: '' as string | number, // 월세용
    maintenanceFee: '' as string | number,
    area: '' as string | number,        // m²

    lat: '', lng: '',
    sido: '', sigungu: '', dong: '',
    sidoCode: '', sigunguCode: '', dongCode: '',

    phone: '',
    availableFrom: '', // YYYY-MM-DD
  })

  const needPrice = form.type === 'SALE' || form.type === 'JEONSE'
  const needMonthly = form.type === 'MONTHLY'

  const images = useMemo(() =>
    form.imagesText.split('\n').map(s => s.trim()).filter(Boolean)
  , [form.imagesText])

  const onSubmit = async () => {
    // ownerKey 보장
    let key = ownerKey
    if (!key) {
      key = ensureOwnerKey()
      setOwnerKey(key)
      setCookie('ownerKey', key)
      alert(`관리 키(쿠키)에 자동 저장했어요.\n관리 키: ${key}\n이 키로 추후 수정·삭제가 가능합니다.`)
    }

    // 최소 검증
    if (!form.title.trim()) return alert('제목을 입력해주세요.')
    if (!form.address.trim()) return alert('주소를 입력해주세요.')
    if (!form.category.trim()) return alert('카테고리를 선택해주세요.')
    if (needPrice && !String(form.price).trim()) return alert('가격을 입력해주세요.')
    if (needMonthly && (!String(form.deposit).trim() || !String(form.rentMonthly).trim()))
      return alert('보증금/월세를 입력해주세요.')

    const payload = {
      title: form.title,
      description: form.description,
      address: form.address,
      category: form.category,
      images,                            // ← URL 배열
      type: form.type,
      price: needPrice ? Number(form.price) : undefined,
      deposit: needMonthly ? Number(form.deposit) : undefined,
      rentMonthly: needMonthly ? Number(form.rentMonthly) : undefined,
      maintenanceFee: form.maintenanceFee ? Number(form.maintenanceFee) : 0,
      area: form.area ? Number(form.area) : undefined,
      lat: form.lat ? Number(form.lat) : undefined,
      lng: form.lng ? Number(form.lng) : undefined,
      sido: form.sido, sigungu: form.sigungu, dong: form.dong,
      sidoCode: form.sidoCode, sigunguCode: form.sigunguCode, dongCode: form.dongCode,
      phone: form.phone,
      availableFrom: form.availableFrom || undefined,
    }

    const res = await createListing(payload, key)
    if ((res as any)?.id) {
      alert(`등록되었습니다! (id: ${(res as any).id})`)
      setForm({ ...form, title:'', description:'', imagesText:'', address:'', price:'', deposit:'', rentMonthly:'' })
    } else {
      alert('등록 실패. 잠시 후 다시 시도해주세요.')
    }
  }

  // 내 매물 관리
  const [myItems, setMyItems] = useState<any[]>([])
  const fetchMine = async () => {
    const key = (ownerKey || '').trim()
    if (!key) return alert('관리 키를 입력해주세요.')
    const r = await getMyListings(key)
    setMyItems(r?.items || [])
  }
  const handleDelete = async (id: number) => {
    const key = (ownerKey || '').trim()
    if (!key) return alert('관리 키를 입력해주세요.')
    if (!confirm('정말 삭제하시겠습니까?')) return
    const r = await deleteListing(id, key)
    if ((r as any)?.ok || (r as any)?.deleted) {
      alert('삭제되었습니다.')
      fetchMine()
    } else {
      alert('삭제 실패. 권한 또는 서버 상태를 확인해주세요.')
    }
  }

  return (
    <div className="center-col px-3 py-6">
      <h1 className="text-2xl font-bold mb-4">매물 등록 / 관리</h1>

      {/* 관리 키(쿠키) */}
      <div className="card p-4 mb-4">
        <div className="flex items-end gap-3">
          <div className="flex-1">
            <label className="text-sm text-gray-600">관리 키(쿠키)</label>
            <input
              value={ownerKey}
              onChange={e => setOwnerKey(e.target.value)}
              placeholder="예: my-secret-1234 (수정/삭제 권한)"
              className="w-full mt-1 border rounded-xl px-3 py-2"
            />
          </div>
          <button
            className="btn-outline"
            onClick={() => {
              const k = ensureOwnerKey()
              setOwnerKey(k)
              setCookie('ownerKey', k)
              alert(`새 관리 키를 발급하고 쿠키에 저장했습니다.\n관리 키: ${k}`)
            }}
          >
            새 키 발급
          </button>
        </div>
        <p className="text-xs text-gray-500 mt-2">
          로그인 없이 권한을 확인하기 위해 쿠키 기반 관리 키를 사용합니다.
          키를 잊지 마세요! (브라우저에 1년간 저장)
        </p>
      </div>

      {/* 탭 */}
      <div className="flex gap-2 mb-3">
        <button
          className={`px-3 py-2 rounded-xl ${tab==='create'?'bg-brand-600 text-white':'bg-white border'}`}
          onClick={()=>setTab('create')}
        >매물 등록</button>
        <button
          className={`px-3 py-2 rounded-xl ${tab==='manage'?'bg-brand-600 text-white':'bg-white border'}`}
          onClick={()=>setTab('manage')}
        >내 매물 관리</button>
      </div>

      {tab==='create' ? (
        <div className="card p-4 space-y-4">
          {/* 기본 */}
          <section>
            <div className="font-semibold mb-2">기본 정보</div>
            <div className="grid md:grid-cols-2 gap-3">
              <div>
                <label className="text-sm text-gray-600">제목</label>
                <input className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.title} onChange={e=>setForm({...form, title:e.target.value})}/>
              </div>
              <div>
                <label className="text-sm text-gray-600">카테고리</label>
                <select className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.category} onChange={e=>setForm({...form, category:e.target.value})}>
                  <option value="">선택하세요</option>
                  <option>카페/디저트</option>
                  <option>식당</option>
                  <option>주점/호프</option>
                  <option>편의</option>
                  <option>패션/액세서리</option>
                  <option>뷰티/미용</option>
                  <option>의료/약국</option>
                  <option>문화/취미</option>
                  <option>레저/스포츠</option>
                  <option>사무/공유오피스</option>
                  <option>숙박</option>
                  <option>창고/물류</option>
                  <option>팝업/쇼룸</option>
                  <option>기타</option>
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="text-sm text-gray-600">설명</label>
                <textarea className="w-full mt-1 border rounded-xl px-3 py-2 min-h-[90px]"
                  value={form.description} onChange={e=>setForm({...form, description:e.target.value})}/>
              </div>
            </div>
          </section>

          {/* 거래/가격 */}
          <section>
            <div className="font-semibold mb-2">거래 / 가격</div>
            <div className="grid md:grid-cols-3 gap-3">
              <div>
                <label className="text-sm text-gray-600">거래유형</label>
                <select
                  className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.type}
                  onChange={e=>setForm({...form, type: e.target.value as TradeType})}
                >
                  <option value="SALE">매매</option>
                  <option value="JEONSE">전세</option>
                  <option value="MONTHLY">월세</option>
                </select>
              </div>

              {needPrice && (
                <div>
                  <label className="text-sm text-gray-600">{form.type==='SALE'?'매매가(만원)':'전세보증금(만원)'}</label>
                  <input type="number" className="w-full mt-1 border rounded-xl px-3 py-2"
                    value={form.price as any} onChange={e=>setForm({...form, price:e.target.value})}/>
                </div>
              )}

              {needMonthly && (
                <>
                  <div>
                    <label className="text-sm text-gray-600">보증금(만원)</label>
                    <input type="number" className="w-full mt-1 border rounded-xl px-3 py-2"
                      value={form.deposit as any} onChange={e=>setForm({...form, deposit:e.target.value})}/>
                  </div>
                  <div>
                    <label className="text-sm text-gray-600">월세(만원)</label>
                    <input type="number" className="w-full mt-1 border rounded-xl px-3 py-2"
                      value={form.rentMonthly as any} onChange={e=>setForm({...form, rentMonthly:e.target.value})}/>
                  </div>
                </>
              )}

              <div>
                <label className="text-sm text-gray-600">관리비(만원)</label>
                <input type="number" className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.maintenanceFee as any} onChange={e=>setForm({...form, maintenanceFee:e.target.value})}/>
              </div>

              <div>
                <label className="text-sm text-gray-600">면적(m²)</label>
                <input type="number" className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.area as any} onChange={e=>setForm({...form, area:e.target.value})}/>
              </div>
            </div>
          </section>

          {/* 위치 */}
          <section>
            <div className="font-semibold mb-2">위치</div>
            <div className="grid md:grid-cols-2 gap-3">
              <div className="md:col-span-2">
                <label className="text-sm text-gray-600">주소</label>
                <input className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.address} onChange={e=>setForm({...form, address:e.target.value})}/>
              </div>
              <div>
                <label className="text-sm text-gray-600">위도</label>
                <input className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.lat} onChange={e=>setForm({...form, lat:e.target.value})}/>
              </div>
              <div>
                <label className="text-sm text-gray-600">경도</label>
                <input className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.lng} onChange={e=>setForm({...form, lng:e.target.value})}/>
              </div>

              <div>
                <label className="text-sm text-gray-600">시도 / 코드</label>
                <div className="flex gap-2">
                  <input className="flex-1 mt-1 border rounded-xl px-3 py-2"
                    value={form.sido} onChange={e=>setForm({...form, sido:e.target.value})}/>
                  <input className="w-40 mt-1 border rounded-xl px-3 py-2"
                    value={form.sidoCode} onChange={e=>setForm({...form, sidoCode:e.target.value})}/>
                </div>
              </div>
              <div>
                <label className="text-sm text-gray-600">시군구 / 코드</label>
                <div className="flex gap-2">
                  <input className="flex-1 mt-1 border rounded-xl px-3 py-2"
                    value={form.sigungu} onChange={e=>setForm({...form, sigungu:e.target.value})}/>
                  <input className="w-40 mt-1 border rounded-xl px-3 py-2"
                    value={form.sigunguCode} onChange={e=>setForm({...form, sigunguCode:e.target.value})}/>
                </div>
              </div>
              <div>
                <label className="text-sm text-gray-600">동 / 코드</label>
                <div className="flex gap-2">
                  <input className="flex-1 mt-1 border rounded-xl px-3 py-2"
                    value={form.dong} onChange={e=>setForm({...form, dong:e.target.value})}/>
                  <input className="w-40 mt-1 border rounded-xl px-3 py-2"
                    value={form.dongCode} onChange={e=>setForm({...form, dongCode:e.target.value})}/>
                </div>
              </div>
            </div>
          </section>

          {/* 연락/입주 */}
          <section>
            <div className="font-semibold mb-2">연락 & 입주</div>
            <div className="grid md:grid-cols-2 gap-3">
              <div>
                <label className="text-sm text-gray-600">전화번호</label>
                <input className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.phone} onChange={e=>setForm({...form, phone:e.target.value})}/>
              </div>
              <div>
                <label className="text-sm text-gray-600">입주 가능일</label>
                <input type="date" className="w-full mt-1 border rounded-xl px-3 py-2"
                  value={form.availableFrom} onChange={e=>setForm({...form, availableFrom:e.target.value})}/>
              </div>
            </div>
          </section>

          {/* 이미지 URL */}
          <section>
            <div className="font-semibold mb-2">이미지 URL (줄바꿈으로 여러 개)</div>
            <textarea className="w-full mt-1 border rounded-xl px-3 py-2 min-h-[90px]"
              placeholder="https://...jpg\nhttps://...png"
              value={form.imagesText} onChange={e=>setForm({...form, imagesText:e.target.value})}/>
          </section>

          <div className="flex justify-end gap-2">
            <button className="btn-outline" onClick={()=>setForm({
              ...form, title:'', description:'', address:'', imagesText:'',
              price:'', deposit:'', rentMonthly:''
            })}>초기화</button>
            <button className="btn-primary" onClick={onSubmit}>등록</button>
          </div>
        </div>
      ) : (
        <div className="card p-4">
          <div className="flex items-center gap-2">
            <button className="btn-primary" onClick={fetchMine}>내 매물 불러오기</button>
            <span className="text-sm text-gray-500">관리 키로 등록한 매물만 조회됩니다.</span>
          </div>

          <div className="mt-4 grid md:grid-cols-2 gap-3">
            {myItems.map((it) => (
              <div key={it.id} className="border rounded-xl p-3 bg-white">
                <div className="font-medium">{it.title}</div>
                <div className="text-xs text-gray-500">{it.address}</div>
                <div className="text-xs text-gray-500 mt-1">
                  {it.type === 'MONTHLY'
                    ? `보증금 ${it.deposit ?? '-'} / 월세 ${it.rentMonthly ?? '-'}`
                    : `가격 ${it.price ?? '-'}`
                  }
                </div>
                <div className="mt-2 flex gap-2">
                  {/* 수정 페이지를 따로 만들 계획이면 여기서 이동 */}
                  {/* <Link to={`/listings/edit/${it.id}`} className="btn-outline">수정</Link> */}
                  <button className="btn-outline" onClick={()=>handleDelete(it.id)}>삭제</button>
                </div>
              </div>
            ))}
            {!myItems.length && (
              <div className="text-sm text-gray-500">목록이 없습니다.</div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
