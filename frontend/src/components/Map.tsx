// src/components/Map.tsx
import { useEffect, useRef } from 'react'

type Props = {
  lat: number
  lng: number
  /** 카카오맵 level (작을수록 확대) 기본 4 */
  level?: number
  /** px 또는 '420px', '50vh' 같은 CSS 문자열 */
  height?: number | string
  /** Tailwind 등으로 높이를 주고 싶을 때 */
  className?: string
}

export default function Map({ lat, lng, level = 4, height, className }: Props) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = ref.current
    const k = (window as any).kakao
    if (!el || !k?.maps) return

    const center = new k.maps.LatLng(lat, lng)
    const map = new k.maps.Map(el, { center, level })
    new k.maps.Marker({ position: center, map })

    const onResize = () => {
      // 컨테이너 크기 변경 반영
      setTimeout(() => {
        map.relayout()
        map.setCenter(center)
      }, 0)
    }
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [lat, lng, level])

  const style = height
    ? { height: typeof height === 'number' ? `${height}px` : height }
    : undefined

  // height를 안 주면 기본 h-64, 주면 className의 높이보다 style이 우선
  const base =
    'w-full rounded-xl bg-slate-100 ' + (className ? className : 'h-64')

  return <div ref={ref} className={base} style={style} />
}
