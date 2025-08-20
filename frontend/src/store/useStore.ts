import { create } from 'zustand'
type State = {
  saved: any[]
  save: (item:any)=>void
  favorites: string[]
  toggleFav: (id:string)=>void
}
export const useStore = create<State>((set)=>({
  saved: [],
  save: (item)=> set(s=>({ saved:[item, ...s.saved].slice(0,50) })),
  favorites: [],
  toggleFav: (id) => set(s=>({ favorites: s.favorites.includes(id) ? s.favorites.filter(x=>x!==id) : [...s.favorites, id] }))
}))