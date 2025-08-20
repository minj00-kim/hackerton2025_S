// Multer v1/v2/v3 상호운용을 위한 파일 버퍼 읽기 도우미
export async function fileToBuffer(file){
  if(!file) return null
  if(file.buffer) return file.buffer
  if(file.stream){
    const chunks = []
    for await (const c of file.stream) chunks.push(Buffer.from(c))
    return Buffer.concat(chunks)
  }
  return null
}