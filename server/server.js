import 'dotenv/config'
import express from 'express'
import cors from 'cors'
import morgan from 'morgan'
import multer from 'multer'
import { parse } from 'csv-parse/sync'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'
import { v4 as uuidv4 } from 'uuid'
import { geocodeAddress } from './services/geocode.js'
import { recommend, simulate, compare, ListingStore } from './services/ai.js'
import { fileToBuffer } from './services/upload.js'

const app = express()
const PORT = process.env.PORT || 5050

app.use(cors())
app.use(express.json({ limit: '1mb' }))
app.use(morgan('dev'))

const upload = multer()

const __dirname = path.dirname(fileURLToPath(import.meta.url))

app.get('/api/health', (req,res)=> res.json({ ok:true }))

app.get('/api/listings', (req,res)=>{
  const rows = ListingStore.load()
  const q = (req.query.q||'').toString().trim()
  let out = rows
  if(q) out = out.filter(r => (r.title+r.address+r.region).includes(q))
  ;['region','type','theme'].forEach(k=>{
    const v = (req.query[k]||'').toString().trim()
    if(v) out = out.filter(r => (k==='theme' ? (r.theme||[]).some(t=>t.includes(v)) : (r[k]||'').includes(v)))
  })
  res.json(out)
})

app.get('/api/listings/:id', (req,res)=>{
  const rows = ListingStore.load()
  const found = rows.find(r => r.id === req.params.id)
  if(!found) return res.status(404).json({ error:'not found' })
  res.json(found)
})

app.post('/api/listings', async (req,res)=>{
  const rows = ListingStore.load()
  const id = req.body.id || uuidv4()
  const item = { id, title:'새 매물', images:[], theme:[], ...req.body }
  if(item.address && !(item.lat && item.lng)){
    const geo = await geocodeAddress(item.address).catch(()=>null)
    if(geo){ item.lat = geo.lat; item.lng = geo.lng }
  }
  rows.push(item)
  ListingStore.save(rows)
  res.json(item)
})

app.post('/api/ai/recommend', async (req,res)=>{
  try { res.json(await recommend(req.body||{})) }
  catch(e){ console.error(e); res.status(500).json({ error:'recommend failed' }) }
})
app.post('/api/ai/simulate', async (req,res)=>{
  try { res.json(await simulate(req.body||{})) }
  catch(e){ console.error(e); res.status(500).json({ error:'simulate failed' }) }
})
app.post('/api/ai/compare', async (req,res)=>{
  try { const { regionA, regionB } = req.body||{}; res.json(await compare(regionA, regionB)) }
  catch(e){ console.error(e); res.status(500).json({ error:'compare failed' }) }
})

// CSV 헤더: id,title,address,region,type,area,deposit,rent,images,theme,lat,lng
app.post('/admin/upload-csv', upload.single('file'), async (req,res)=>{
  try{
    const buf = await fileToBuffer(req.file)
    if(!buf) return res.status(400).json({ error:'파일을 읽을 수 없습니다.' })
    const text = buf.toString('utf-8')
    const rows = parse(text, { columns: true, skip_empty_lines: true, trim: true })
    const store = ListingStore.load()
    const report = { total: rows.length, success:0, failed:0, errors:[] }
    for(const r of rows){
      try{
        const id = r.id || uuidv4()
        const images = (r.images||'').split('|').map(s=>s.trim()).filter(Boolean)
        const theme = (r.theme||'').split('|').map(s=>s.trim()).filter(Boolean)
        const item = {
          id,
          title: r.title || '무제',
          address: r.address||'',
          region: r.region||'',
          type: r.type||'상가',
          area: Number(r.area)||0,
          deposit: Number(r.deposit)||0,
          rent: Number(r.rent)||0,
          images, theme
        }
        let lat = Number(r.lat)||null, lng = Number(r.lng)||null
        if(!(lat && lng) && item.address){
          const geo = await geocodeAddress(item.address).catch(()=>null)
          if(geo){ lat = geo.lat; lng = geo.lng }
        }
        if(lat && lng){ item.lat = lat; item.lng = lng }
        store.push(item); report.success++
      }catch(err){
        report.failed++; report.errors.push(String(err?.message||err))
      }
    }
    ListingStore.save(store)
    res.json(report)
  }catch(e){
    console.error(e)
    res.status(500).json({ error:'upload failed' })
  }
})

app.listen(PORT, ()=>{
  console.log('API listening on http://localhost:'+PORT)
})