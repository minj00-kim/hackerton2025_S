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