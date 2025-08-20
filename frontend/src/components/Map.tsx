// frontend/src/components/Map.tsx
import { useEffect, useRef, useState } from 'react'
import { loadKakaoMaps } from '../lib/kakao'

declare global { interface Window { kakao: any } }

export default function Map({lat,lng}:{lat:number; lng:number}){
  const ref = useRef<HTMLDivElement>(null)
  const [err, setErr] = useState<string>('')

  useEffect(() => {
    let cancelled = false
    loadKakaoMaps().then(() => {
      if (cancelled) return
      const container = ref.current
      if (!container) return
      const pos = new window.kakao.maps.LatLng(lat, lng)
      const map = new window.kakao.maps.Map(container, { center: pos, level: 3 })
      new window.kakao.maps.Marker({ position: pos, map })
    }).catch(e => {
      console.error('[KAKAO] load fail', e)
      setErr(e?.message || String(e))
    })
    return () => { cancelled = true }
  }, [lat, lng])

  return (
    <>
      <div ref={ref} className="w-full h-64 rounded-xl border bg-white" />
      {err && <div className="text-xs text-red-600 mt-1">지도 로드 실패: {err}</div>}
    </>
  )
}
