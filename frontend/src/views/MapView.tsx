// src/views/MapView.tsx
import { useEffect, useMemo, useRef, useState } from 'react'
import { getRegionCounts, getNearbyBizCounts, getListingsByRegion } from '../services/api'
import MapRightPanel, { type Cluster } from '../components/MapRightPanel'

const HEADER_H = 56
const kakao = (window as any).kakao

export default function MapView() {
  const wrapRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<any>(null)
  const overlaysRef = useRef<any[]>([])
  const searchMarkerRef = useRef<any>(null)

  const [zoomLevel, setZoomLevel] = useState(7)
  const [onlyWithCoords, setOnlyWithCoords] = useState(true)

  // ✅ 한글 조합도 잘 입력되도록 controlled input + composition 처리
  const [keyword, setKeyword] = useState('')
  const composingRef = useRef(false)
  const keywordRef = useRef<HTMLInputElement | null>(null)

  const [theme, setTheme] = useState('')

  // 단일 패널 + 3단계 스텝
  const [panelOpen, setPanelOpen] = useState(false)
  const [panelStep, setPanelStep] = useState<'sig' | 'emd' | 'category'>('sig')

  // 목록/선택 상태
  const [sigList, setSigList] = useState<Cluster[]>([])
  const [selectedSig, setSelectedSig] = useState<Cluster | null>(null)

  const [emdList, setEmdList] = useState<Cluster[]>([])
  const [selectedEmd, setSelectedEmd] = useState<Cluster | null>(null)

  const [bizCounts, setBizCounts] = useState<Record<string, number> | null>(null)
  const [regionListings, setRegionListings] = useState<any[]>([])

  const clusterScale = (cnt: number) => (cnt > 999 ? 1.25 : cnt > 199 ? 1.15 : cnt > 49 ? 1.05 : 1)
  const currentLevel = useMemo<'sig' | 'emd'>(() => (zoomLevel <= 6 ? 'emd' : 'sig'), [zoomLevel])

  // Kakao map
  useEffect(() => {
    if (!wrapRef.current || !kakao?.maps) return
    const center = new kakao.maps.LatLng(36.78, 126.45)
    const map = new kakao.maps.Map(wrapRef.current, { center, level: 7 })
    mapRef.current = map

    kakao.maps.event.addListener(map, 'zoom_changed', () => setZoomLevel(map.getLevel()))
    kakao.maps.event.addListener(map, 'dragend', fetchClusters)

    fetchClusters()
    return () => {
      overlaysRef.current.forEach((ov) => ov.setMap && ov.setMap(null))
      overlaysRef.current = []
      if (searchMarkerRef.current) {
        searchMarkerRef.current.setMap(null)
        searchMarkerRef.current = null
      }
      mapRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => { fetchClusters() }, [currentLevel, onlyWithCoords, theme]) // eslint-disable-line

  // 키워드 변경 시 서버 필터는 반영하되, IME 조합 중에는 딜레이
  useEffect(() => {
    if (composingRef.current) return
    const t = setTimeout(fetchClusters, 350)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword])

  // 지도 클러스터 렌더
  const renderClusters = (items: Cluster[]) => {
    overlaysRef.current.forEach((ov) => ov.setMap && ov.setMap(null))
    overlaysRef.current = []
    const map = mapRef.current
    if (!map) return

    items.forEach((c) => {
      const scale = clusterScale(c.count)
      const el = document.createElement('div')
      el.style.transform = `scale(${scale})`
      el.style.display = 'inline-flex'
      el.style.alignItems = 'center'
      el.style.gap = '6px'
      el.style.padding = '10px 14px'
      el.style.borderRadius = '9999px'
      el.style.background = '#2F6BFF'
      el.style.color = '#fff'
      el.style.fontWeight = '700'
      el.style.boxShadow = '0 6px 14px rgba(47,107,255,.25)'
      el.style.cursor = 'pointer'
      el.innerHTML = `
        <span style="background:rgba(255,255,255,.2);padding:4px 8px;border-radius:9999px;">${c.count}</span>
        <span>${c.name}</span>
      `

      const clickHandler = async () => {
        setPanelOpen(true)
        if (c.level === 'sig') {
          // 시·군·구 클릭 → 해당 시군구의 읍·면·동 목록
          setSelectedSig(c)
          setPanelStep('emd')
          await loadEmdList(c)
          map.panTo(new kakao.maps.LatLng(c.lat, c.lng))
        } else {
          // 읍·면·동 클릭 → 카테고리
          setSelectedEmd(c)
          setPanelStep('category')
          await loadCategoryForRegion(c)
          const curr = map.getLevel()
          map.setLevel(Math.max(1, curr - 1), { animate: true })
          map.panTo(new kakao.maps.LatLng(c.lat, c.lng))
        }
      }
      el.addEventListener('click', clickHandler)

      const ov = new kakao.maps.CustomOverlay({
        position: new kakao.maps.LatLng(c.lat, c.lng),
        content: el,
        yAnchor: 1.1,
        zIndex: 10,
        clickable: true,
      })
      ov.setMap(map)
      overlaysRef.current.push(ov)
      kakao.maps.event.addListener(ov, 'click', clickHandler)
    })
  }

  // 지도 클러스터 데이터
  const fetchClusters = async () => {
    const map = mapRef.current
    if (!map) return
    const b = map.getBounds()
    const center = map.getCenter()
    const params = {
      level: currentLevel,
      sw: { lat: b.getSouthWest().getLat(), lng: b.getSouthWest().getLng() },
      ne: { lat: b.getNorthEast().getLat(), lng: b.getNorthEast().getLng() },
      center: { lat: center.getLat(), lng: center.getLng() },
      onlyWithCoords, keyword, theme,
    }
    try {
      const list = await getRegionCounts(params as any)
      renderClusters(list || [])
    } catch {
      renderClusters([{ name: '서산시', code: 'TEST', lat: 36.781, lng: 126.45, count: 123, level: currentLevel }])
    }
  }

  // 시·군·구 목록 로드(패널용)
  const loadSigList = async () => {
    const map = mapRef.current
    if (!map) return
    const b = map.getBounds()
    const center = map.getCenter()
    try {
      const list = await getRegionCounts({
        level: 'sig',
        sw: { lat: b.getSouthWest().getLat(), lng: b.getSouthWest().getLng() },
        ne: { lat: b.getNorthEast().getLat(), lng: b.getNorthEast().getLng() },
        center: { lat: center.getLat(), lng: center.getLng() },
        onlyWithCoords, keyword, theme,
      } as any)
      setSigList(list || [])
    } catch {
      setSigList([])
    }
  }

  // 읍·면·동 목록 로드(상위코드 지정)
  const loadEmdList = async (sig: Cluster) => {
    try {
      const list = await getRegionCounts({
        level: 'emd',
        parentCode: sig.code, // ★ 백엔드에 parentCode 지원
      } as any)
      setEmdList(list || [])
    } catch {
      setEmdList([])
    }
  }

  // 카테고리/리스트 로드(읍·면·동 기준)
  const loadCategoryForRegion = async (emd: Cluster) => {
    try {
      const [biz, list] = await Promise.all([
        getNearbyBizCounts({ code: emd.code, radius: 800 }),
        getListingsByRegion({ code: emd.code, level: emd.level, limit: 20 }),
      ])
      setBizCounts(biz || {})
      setRegionListings(Array.isArray(list) ? list : [])
    } catch {
      setBizCounts(null)
      setRegionListings([])
    }
  }

  // ✅ 카카오 장소 검색 → 지도 이동 + 검색 마커 표시
  const doPlaceSearch = () => {
    const q = keyword.trim()
    const map = mapRef.current
    if (!q || !map || !kakao?.maps?.services) return

    const places = new kakao.maps.services.Places()
    places.keywordSearch(q, (results: any[], status: string) => {
      if (status !== kakao.maps.services.Status.OK || !results?.length) return
      const first = results[0]
      const latlng = new kakao.maps.LatLng(Number(first.y), Number(first.x))

      map.setLevel(4)
      map.setCenter(latlng)

      if (searchMarkerRef.current) searchMarkerRef.current.setMap(null)
      searchMarkerRef.current = new kakao.maps.Marker({ position: latlng, map })
    })
  }

  // 상단 필터바
  const FilterBar = () => (
    <div className="fixed left-1/2 -translate-x-1/2" style={{ top: HEADER_H + 8, zIndex: 30 }}>
      {/* 폼으로 묶어 Enter 제출 지원 */}
      <form
        className="bg-white/90 backdrop-blur rounded-full shadow px-3 py-1.5 flex items-center gap-3"
        onSubmit={(e) => { e.preventDefault(); doPlaceSearch() }}
      >
        <label className="inline-flex items-center gap-1 px-2">
          <input
            type="checkbox"
            checked={onlyWithCoords}
            onChange={(e) => setOnlyWithCoords(e.target.checked)}
          />
          <span className="text-sm text-gray-700">좌표가 있는 매물만</span>
        </label>

        <input
          ref={keywordRef}
          type="text"
          placeholder="지역, 지하철, 건물명, 학교명 등 검색"
          value={keyword}
          className="text-sm bg-transparent px-2 outline-none border-l pl-3 border-gray-200"
          style={{ minWidth: 220 }}
          autoCapitalize="off"
          autoCorrect="off"
          spellCheck={false}
          onChange={(e) => {
            // ✅ 여러 글자 자연스럽게 입력
            setKeyword(e.currentTarget.value)
          }}
          onCompositionStart={() => { composingRef.current = true }}
          onCompositionEnd={(e) => {
            composingRef.current = false
            setKeyword(e.currentTarget.value)
          }}
        />

        <button
          type="submit"
          className="text-sm px-3 h-7 rounded-full bg-brand-600 text-white hover:bg-brand-700"
          title="검색"
        >
          검색
        </button>

        <select
          value={theme}
          onChange={(e) => setTheme(e.target.value)}
          className="text-sm bg-transparent px-2 outline-none"
        >
          <option value="">테마 전체</option>
          <option value="역세권">역세권</option>
          <option value="먹자골목">먹자골목</option>
          <option value="대학가">대학가</option>
        </select>

        {/* 지역 집계(시·군·구부터) */}
        <button
          type="button"
          className="text-sm px-2 h-7 rounded-full bg-gray-100 hover:bg-gray-200"
          onClick={async () => {
            setPanelOpen(true)
            setPanelStep('sig')
            setSelectedSig(null)
            setSelectedEmd(null)
            await loadSigList()
          }}
        >
          지역 전체
        </button>
      </form>
    </div>
  )

  return (
    <div className="relative left-1/2 right-1/2 -ml-[50vw] -mr-[50vw] w-screen">
      <FilterBar />
      <div
        ref={wrapRef}
        className="w-screen"
        style={{ height: `calc(100vh - ${HEADER_H}px)`, zIndex: 0 }}
      />

      <MapRightPanel
        open={panelOpen}
        onClose={() => setPanelOpen(false)}
        step={panelStep}
        // SIG
        sigRegions={sigList}
        onSelectSig={async (sig) => {
          setSelectedSig(sig)
          setPanelStep('emd')
          await loadEmdList(sig)
          try {
            const map = mapRef.current
            map.panTo(new kakao.maps.LatLng(sig.lat, sig.lng))
          } catch {}
        }}
        // EMD
        selectedSig={selectedSig}
        emdRegions={emdList}
        onBackToSig={() => {
          setPanelStep('sig')
          setSelectedSig(null)
          setSelectedEmd(null)
          loadSigList()
        }}
        onSelectEmd={async (emd) => {
          setSelectedEmd(emd)
          setPanelStep('category')
          await loadCategoryForRegion(emd)
          try {
            const map = mapRef.current
            const curr = map.getLevel()
            map.setLevel(Math.max(1, curr - 1), { animate: true })
            map.panTo(new kakao.maps.LatLng(emd.lat, emd.lng))
          } catch {}
        }}
        // CATEGORY
        selectedEmd={selectedEmd ? { name: selectedEmd.name, code: selectedEmd.code } : null}
        bizCounts={bizCounts}
        listings={regionListings}
        onBackToEmd={() => {
          if (selectedSig) {
            setPanelStep('emd')
          } else {
            setPanelStep('sig')
          }
        }}
      />
    </div>
  )
}
