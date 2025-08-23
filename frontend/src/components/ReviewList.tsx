import { useEffect, useState } from 'react'
import { Review, addReview, deleteReview, getReviews, updateReview } from '../services/api'
import { ensureAuthKey as getAuthKey } from '../lib/auth'

export default function ReviewList({ listingId }: { listingId: string }) {
  const [items, setItems] = useState<Review[]>([])
  const [text, setText] = useState('')
  const [rating, setRating] = useState<number | undefined>(undefined)
  const [nickname, setNickname] = useState('')
  const [busy, setBusy] = useState(false)
  const me = getAuthKey()

  useEffect(() => {
    let alive = true
    getReviews(listingId).then(r => alive && setItems(r))
    return () => { alive = false }
  }, [listingId])

  const submit = async () => {
    const t = text.trim()
    if (!t || busy) return
    setBusy(true)
    try {
      const rv = await addReview(listingId, { text: t, rating, nickname })
      setItems([rv, ...items])
      setText('')
      setRating(undefined)
    } finally {
      setBusy(false)
    }
  }

  const onEdit = async (id: string, s: string) => {
    const rv = await updateReview(listingId, id, { text: s })
    setItems(items.map(v => v.id === id ? rv : v))
  }

  const onDelete = async (id: string) => {
    await deleteReview(listingId, id)
    setItems(items.filter(v => v.id !== id))
  }

  return (
    <div className="mt-6">
      <h3 className="text-lg font-semibold mb-2">매물 리뷰</h3>

      {/* 작성 */}
      <div className="rounded-2xl border bg-white p-3 mb-4">
        <div className="flex gap-2 mb-2">
          <input
            value={nickname}
            onChange={e => setNickname(e.target.value)}
            placeholder="닉네임(선택)"
            className="w-40 border rounded-lg px-2 py-1"
          />
          <select
            value={rating ?? ''}
            onChange={e => setRating(e.target.value ? Number(e.target.value) : undefined)}
            className="w-28 border rounded-lg px-2 py-1"
          >
            <option value="">별점(선택)</option>
            {[1,2,3,4,5].map(n => <option key={n} value={n}>{'⭐'.repeat(n)}</option>)}
          </select>
        </div>
        <div className="flex gap-2">
          <textarea
            value={text}
            onChange={e => setText(e.target.value)}
            placeholder="이 매물에 대한 의견을 남겨주세요."
            className="flex-1 border rounded-xl px-3 py-2"
            rows={3}
          />
          <button onClick={submit} disabled={busy} className="btn-primary">
            등록
          </button>
        </div>
        <div className="text-xs text-gray-500 mt-1">작성 보호키: <code className="font-mono">{me.slice(0,8)}…</code></div>
      </div>

      {/* 리스트 */}
      <div className="space-y-3">
        {items.map(rv => (
          <Item key={rv.id} rv={rv} onEdit={onEdit} onDelete={onDelete} />
        ))}
        {!items.length && <div className="text-sm text-gray-500">아직 리뷰가 없어요.</div>}
      </div>
    </div>
  )
}

function Item({ rv, onEdit, onDelete }: {
  rv: Review
  onEdit: (id: string, text: string) => void
  onDelete: (id: string) => void
}) {
  const [editing, setEditing] = useState(false)
  const [val, setVal] = useState(rv.text)
  const save = () => { onEdit(rv.id, val); setEditing(false) }

  return (
    <div className="rounded-2xl border bg-white p-3">
      <div className="flex items-center justify-between">
        <div className="text-sm text-gray-600">
          <b>{rv.nickname || '익명'}</b>
          {rv.rating ? <span className="ml-2 text-yellow-500">{'⭐'.repeat(rv.rating)}</span> : null}
          <span className="ml-2">{new Date(rv.createdAt).toLocaleString()}</span>
        </div>
        {rv.canEdit && (
          <div className="text-xs flex gap-2">
            {!editing ? (
              <>
                <button className="text-blue-600" onClick={() => setEditing(true)}>수정</button>
                <button className="text-rose-600" onClick={() => onDelete(rv.id)}>삭제</button>
              </>
            ) : (
              <>
                <button className="text-blue-600" onClick={save}>저장</button>
                <button className="text-gray-600" onClick={() => { setVal(rv.text); setEditing(false) }}>취소</button>
              </>
            )}
          </div>
        )}
      </div>
      <div className="mt-2 whitespace-pre-wrap">
        {!editing ? rv.text : (
          <textarea className="w-full border rounded-lg px-2 py-1" rows={3} value={val} onChange={e => setVal(e.target.value)} />
        )}
      </div>
    </div>
  )
}
