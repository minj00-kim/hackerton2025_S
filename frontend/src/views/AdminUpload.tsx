// 관리자 업로드: CSV로 매물 대량 등록
// CSV 헤더: id,title,address,region,type,area,deposit,rent,images,theme,lat,lng
import { useState } from 'react'
import Card from '../components/Card'
import { adminUploadCSV } from '../services/api'

export default function AdminUpload(){
  const [file, setFile] = useState<File|null>(null)
  const [report, setReport] = useState<any>(null)
  const onSubmit = async () => {
    if(!file) return alert('파일을 선택하세요')
    const res = await adminUploadCSV(file)
    setReport(res)
  }
  return (
    <Card>
      <div className="font-semibold mb-2">CSV 업로드(관리자)</div>
      <p className="text-xs text-gray-500 mb-2">CSV 헤더: id,title,address,region,type,area,deposit,rent,images,theme,lat,lng</p>
      <input type="file" accept=".csv" onChange={e=>setFile(e.target.files?.[0]||null)} />
      <button className="btn-primary ml-2 text-sm" onClick={onSubmit}>업로드</button>
      {report && (
        <div className="text-sm mt-3">
          <div>총 {report.total}건 / 성공 {report.success} / 실패 {report.failed}</div>
          {report.errors?.length>0 && <ul className="list-disc pl-5 mt-2">{report.errors.map((e:string,i:number)=>(<li key={i}>{e}</li>))}</ul>}
        </div>
      )}
    </Card>
  )
}