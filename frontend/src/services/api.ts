import axios from 'axios'
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5050/api'
export const api = axios.create({ baseURL })

export type Listing = {
  id: string; title: string; region: string; address: string; type: string;
  area: number; deposit: number; rent: number; images: string[];
  lat?: number; lng?: number; theme?: string[]; score?: number;
}

export const getListings = (params?: any) => api.get('/listings', { params }).then(r=>r.data as Listing[])
export const getListing = (id: string) => api.get('/listings/' + id).then(r=>r.data as Listing)
export const createListing = (body: Partial<Listing>) => api.post('/listings', body).then(r=>r.data as Listing)

export const aiRecommend = (body:any) => api.post('/ai/recommend', body).then(r=>r.data)
export const aiSimulate = (body:any) => api.post('/ai/simulate', body).then(r=>r.data)
export const aiCompare = (a: string, b: string) => api.post('/ai/compare', { regionA:a, regionB:b }).then(r=>r.data)

export const adminUploadCSV = (file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return api.post('/admin/upload-csv', fd, { headers: { 'Content-Type': 'multipart/form-data' }}).then(r=>r.data)
}

// ▶ 추가: 대화형 AI (백엔드가 있으면 /api/ai/chat 호출, 없으면 안전한 로컬 Fallback)
export async function aiChat(prompt: string): Promise<{ answer: string; listings?: Listing[] }> {
  try {
    const r = await api.post('/ai/chat', { prompt })
    return r.data
  } catch (e) {
    // Fallback: 로컬 규칙 기반 간단 응답 + 존재하는 매물 일부 첨부
    const all = await getListings().catch(()=>[]) as Listing[]
    const picks = all
      .filter(l => {
        const bag = [l.title,l.address,l.region,l.type,(l.theme||[]).join(',')].join(' ').toLowerCase()
        return bag.includes(prompt.toLowerCase())
      })
      .slice(0,4)

    const answer = [
      `요청하신 “${prompt}”에 대해 기본 정보를 정리했어요.`,
      `• 먼저 관심 지역/업종/예산을 더 구체화하면 추천의 정확도가 올라갑니다.`,
      picks.length ? `• 관련 매물 ${picks.length}건을 같이 보여드릴게요.` : `• 관련 매물은 아직 못찾았어요. 키워드를 바꿔보실래요?`,
      `\n※ 서버형 AI(/api/ai/chat)이 연결되면 더 풍부한 분석과 지도를 함께 드릴 수 있어요.`,
    ].join('\n')
    return { answer, listings: picks.length ? picks : undefined }
  }
}