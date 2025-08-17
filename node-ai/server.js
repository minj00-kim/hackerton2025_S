import express from 'express'
import 'dotenv/config'
import { geocodeAddress } from './lib/geocode.js'
import { recommend, simulate, compareRegions } from './lib/recommend.js'

const app = express()
app.use(express.json())

// 단순 헬스체크
app.get('/health', (_,res)=>res.json({ok:true}))

// 지오코딩 프록시
app.get('/geocode', async (req,res)=>{
    try{
        const { address } = req.query
        if(!address) return res.status(400).json({ ok:false, message:'address required'})
        const r = await geocodeAddress(address)
        if(!r) return res.status(404).json({ ok:false, message:'not found'})
        res.json({ ok:true, data:r })
    }catch(e){
        res.status(500).json({ ok:false, message:String(e) })
    }
})

// 추천
app.post('/recommend', async (req,res)=>{
    try{
        const { listings, options } = req.body || {}
        if(!Array.isArray(listings)) return res.status(400).json({ ok:false, message:'listings[] required'})
        const r = await recommend(listings, options)
        res.json({ ok:true, data:r })
    }catch(e){
        res.status(500).json({ ok:false, message:String(e) })
    }
})

// 시뮬레이터
app.post('/simulate', async (req,res)=>{
    try{
        const r = await simulate(req.body || {})
        res.json({ ok:true, data:r })
    }catch(e){
        res.status(500).json({ ok:false, message:String(e) })
    }
})

// 지역 비교
app.get('/compare', async (req,res)=>{
    try{
        const { a, b } = req.query
        if(!a || !b) return res.status(400).json({ ok:false, message:'a, b required'})
        const r = await compareRegions(a, b)
        res.json({ ok:true, data:r })
    }catch(e){
        res.status(500).json({ ok:false, message:String(e) })
    }
})

const PORT = process.env.PORT || 3001
app.listen(PORT, ()=> console.log(`node-ai listening on :${PORT}`))
