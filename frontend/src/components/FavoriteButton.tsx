import { useEffect, useState } from 'react'
import { addFavorite, isFavorite, removeFavorite } from '../services/api'

export default function FavoriteButton({ listingId }: { listingId: string }) {
  const [on, setOn] = useState(false)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let alive = true
    isFavorite(listingId).then(v => alive && setOn(v))
    return () => { alive = false }
  }, [listingId])

  const toggle = async () => {
    if (busy) return
    setBusy(true)
    try {
      if (on) await removeFavorite(listingId)
      else await addFavorite(listingId)
      setOn(!on)
    } finally {
      setBusy(false)
    }
  }

  return (
    <button
      onClick={toggle}
      disabled={busy}
      className={'inline-flex items-center gap-2 rounded-full px-3 py-1.5 ' +
        (on ? 'bg-rose-50 text-rose-600' : 'bg-white border text-gray-700')}
      title={on ? '즐겨찾기 해제' : '즐겨찾기 추가'}
    >
      <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden>
        <path
          d="M12 21s-6.7-4.33-9.33-7.5A5.5 5.5 0 1 1 12 6a5.5 5.5 0 1 1 9.33 7.5C18.7 16.67 12 21 12 21z"
          fill={on ? '#ef4444' : 'none'}
          stroke={on ? '#ef4444' : '#111827'}
          strokeWidth="1.5"
        />
      </svg>
      {on ? '저장됨' : '즐겨찾기'}
    </button>
  )
}
