// 큰 카카오 지도에 매물을 그리는 화면
// - Kakao SDK는 index.html에서 env 키가 있으면 로드됩니다.
// - /api/listings 에서 매물 읽어와 lat/lng가 있는 것만 마커로 표기
// - 마커 클릭 → 인포윈도우(썸네일/제목/가격) + 상세페이지로 이동 링크
// - 상단에 간단한 필터(키워드/지역/테마) 제공

import { useEffect, useMemo, useRef, useState } from 'react'
import { getListings } from '../services/api'
import Card from '../components/Card'
import { Link } from 'react-router-dom'
import { loadKakaoMaps } from '../lib/kakao'

declare global { interface Window { kakao:any } }

type Listing = {
  id:string; title:string; address:string; region:string; type:string;
  area:number; deposit:number; rent:number; images:string[]; theme?:string[];
  lat?:number; lng?:number;
}

export default function MapView(){
  const [all, setAll] = useState<Listing[]>([])
  const [q, setQ] = useState('')
  const [region, setRegion] = useState('')
  const [theme, setTheme] = useState('')
  const [onlyGeo, setOnlyGeo] = useState(true)

  // 카카오 맵 객체/클러스터러/마커 보관
  const mapRef = useRef<HTMLDivElement>(null)
  const mapObj = useRef<any>(null)
  const clusterer = useRef<any>(null)
  const markersRef = useRef<any[]>([])
  const infoRef = useRef<any>(null)

  // 필터 적용
  const rows = useMemo(()=>{
    const k = q.trim().toLowerCase()
    return all.filter(l=>{
      if(onlyGeo && !(typeof l.lat==='number' && typeof l.lng==='number')) return false
      if(region && l.region !== region) return false
      if(theme && !(l.theme||[]).includes(theme)) return false
      if(k){
        const hay = [l.title,l.address,l.region,l.type,(l.theme||[]).join(',')].join(' ').toLowerCase()
        if(!hay.includes(k)) return false
      }
      return true
    })
  }, [all,q,region,theme,onlyGeo])

  useEffect(() => {
  const run = async () => {
    await loadKakaoMaps();           // ← 이게 실행되어야 script가 head에 붙음
    // ... 지도 생성 코드 ...
  }
  run().catch(e => console.error(e))
}, [])

  // 최초 데이터 로드
  useEffect(()=>{ (async()=>{
    const list = await getListings()
    setAll(list)
  })() }, [])

  // 카카오맵 초기화 (서산 중심으로 시작)
  useEffect(()=>{
    const tryInit = () => {
      if(!window.kakao || !window.kakao.maps) return false
      window.kakao.maps.load(()=>{
        const center = new window.kakao.maps.LatLng(36.7818, 126.4528) // 서산시청 근처
        const map = new window.kakao.maps.Map(mapRef.current, { center, level: 6 })
        mapObj.current = map
        // 클러스터러 (라이브러리 로드된 경우)
        if(window.kakao.maps.MarkerClusterer){
          clusterer.current = new window.kakao.maps.MarkerClusterer({ map, averageCenter: true, minLevel: 6 })
        }
        renderMarkers(rows)
      })
      return true
    }
    // SDK가 늦게 로드될 수 있어 폴링
    const id = setInterval(()=>{ if(tryInit()) clearInterval(id) }, 300)
    return ()=> clearInterval(id)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 필터 변경 시 마커 다시 그림
  useEffect(()=>{
    if(!mapObj.current || !window.kakao?.maps) return
    renderMarkers(rows)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rows])

  const renderMarkers = (list:Listing[]) => {
    const kakao = window.kakao
    // 기존 마커/클러스터 제거
    if(clusterer.current){ clusterer.current.clear() }
    markersRef.current.forEach(m=>m.setMap(null))
    markersRef.current = []
    if(infoRef.current){ infoRef.current.close(); infoRef.current = null }

    const markers:any[] = []
    const bounds = new kakao.maps.LatLngBounds()

    list.forEach(item=>{
      if(typeof item.lat !== 'number' || typeof item.lng !== 'number') return
      const pos = new kakao.maps.LatLng(item.lat, item.lng)
      const marker = new kakao.maps.Marker({ position: pos })
      markers.push(marker)
      bounds.extend(pos)

      kakao.maps.event.addListener(marker, 'click', ()=>{
        // 인포윈도우 콘텐츠 (간단 카드)
        const html = `
          <div style="min-width:260px">
            <div style="display:flex;gap:8px">
              <img src="${item.images?.[0]||''}" style="width:80px;height:80px;object-fit:cover;border-radius:8px;border:1px solid #eee"/>
              <div style="font-size:13px;line-height:1.35">
                <div style="font-weight:700;margin-bottom:2px">${escapeHtml(item.title)}</div>
                <div style="color:#666">${escapeHtml(item.region)} · ${escapeHtml(item.address||'')}</div>
                <div style="margin-top:4px">보증금 ${fmt(item.deposit)} · 월세 ${fmt(item.rent)}</div>
                <div style="margin-top:6px">
                  <a href="/listings/${item.id}" style="color:#2563eb;text-decoration:underline" target="_blank">상세 보기</a>
                </div>
              </div>
            </div>
          </div>
        `
        if(!infoRef.current) infoRef.current = new kakao.maps.InfoWindow({ removable:true })
        infoRef.current.setContent(html)
        infoRef.current.open(mapObj.current, marker)
      })
    })

    // 지도에 올리기
    if(clusterer.current){ clusterer.current.addMarkers(markers) }
    else { markers.forEach(m=>m.setMap(mapObj.current)) }
    markersRef.current = markers

    // 영역 맞추기
    if(!bounds.isEmpty()){ mapObj.current.setBounds(bounds) }
  }

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex flex-wrap items-end gap-2">
          <label className="text-sm">
            <div className="text-gray-600 mb-1">키워드</div>
            <input className="border rounded-xl px-3 py-2" placeholder="제목/주소/테마"
              value={q} onChange={e=>setQ(e.target.value)} />
          </label>
          <label className="text-sm">
            <div className="text-gray-600 mb-1">지역</div>
            <input className="border rounded-xl px-3 py-2" placeholder="예: 부춘동"
              value={region} onChange={e=>setRegion(e.target.value)} />
          </label>
          <label className="text-sm">
            <div className="text-gray-600 mb-1">테마</div>
            <input className="border rounded-xl px-3 py-2" placeholder="예: 역세권"
              value={theme} onChange={e=>setTheme(e.target.value)} />
          </label>
          <label className="text-sm inline-flex items-center gap-2 ml-auto">
            <input type="checkbox" checked={onlyGeo} onChange={e=>setOnlyGeo(e.target.checked)} />
            좌표가 있는 매물만
          </label>
          <span className="text-xs text-gray-500">표시 {rows.length} / 총 {all.length}</span>
        </div>
      </Card>

      {/* 큰 지도 영역 */}
      <div ref={mapRef} className="w-full h-[70vh] rounded-2xl border bg-white" />

      {/* 간단한 리스트(지금 보고 있는 필터 결과) */}
      <Card>
        <div className="font-semibold mb-2">현재 표시 중인 매물</div>
        <ul className="text-sm grid md:grid-cols-2 gap-2">
          {rows.slice(0,20).map(l=>(
            <li key={l.id} className="flex items-center gap-3">
              <img src={l.images?.[0]||''} className="w-16 h-16 object-cover rounded-lg border" />
              <div>
                <div className="font-medium">{l.title}</div>
                <div className="text-gray-500">{l.region} · {l.address}</div>
                <div className="text-gray-700">보증금 {fmt(l.deposit)} / 월세 {fmt(l.rent)}</div>
                <Link to={`/listings/${l.id}`} className="text-brand-700 underline text-xs">상세보기</Link>
              </div>
            </li>
          ))}
        </ul>
        {rows.length>20 && <div className="text-xs text-gray-500 mt-2">…외 {rows.length-20}건</div>}
      </Card>
    </div>
  )
}

function fmt(n?:number){ return typeof n==='number' ? `${n}만원` : '-' }
function escapeHtml(s?:string){
  if(!s) return ''
  return s.replace(/[&<>"]/g, c=>({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;' } as any)[c])
}
