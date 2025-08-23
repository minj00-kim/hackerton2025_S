// src/lib/fav.ts
import { ensureAuthKey } from './auth';

export type MinimalListing = {
  id: string | number;
  title?: string;
  address?: string;
  type?: string;
  price?: number;
  rentMonthly?: number;
  thumb?: string;
};

type FavId = string | number;

const MAP_KEY = 'fav:listings';          // { [authKey]: string[] }
const LEGACY_PREFIX = 'fav:';            // 'fav:<authKey>' -> string[]
const ITEMS_PREFIX = 'fav:items:';       // 'fav:items:<authKey>' -> { [id]: MinimalListing }

function canUseStorage() {
  return typeof window !== 'undefined' && !!window.localStorage;
}

/* ---------- low-level storage ---------- */
function readMap(): Record<string, string[]> {
  if (!canUseStorage()) return {};
  const raw = window.localStorage.getItem(MAP_KEY);
  if (!raw) return {};
  try {
    const m = JSON.parse(raw);
    return m && typeof m === 'object' ? (m as Record<string, string[]>) : {};
  } catch { return {}; }
}
function writeMap(m: Record<string, string[]>) {
  if (!canUseStorage()) return;
  window.localStorage.setItem(MAP_KEY, JSON.stringify(m));
}

function readItems(key: string): Record<string, MinimalListing> {
  if (!canUseStorage()) return {};
  const raw = window.localStorage.getItem(ITEMS_PREFIX + key);
  if (!raw) return {};
  try {
    const m = JSON.parse(raw);
    return m && typeof m === 'object' ? (m as Record<string, MinimalListing>) : {};
  } catch { return {}; }
}
function writeItems(key: string, m: Record<string, MinimalListing>) {
  if (!canUseStorage()) return;
  window.localStorage.setItem(ITEMS_PREFIX + key, JSON.stringify(m));
}

/* ---------- public APIs (ids) ---------- */
export function listFavIds(authKey?: string): string[] {
  if (!canUseStorage()) return [];
  const key = authKey || ensureAuthKey();
  const map = readMap();
  const arr1 = Array.isArray(map[key]) ? map[key] : [];

  // legacy merge
  const legacyRaw = window.localStorage.getItem(`${LEGACY_PREFIX}${key}`);
  let arr2: string[] = [];
  if (legacyRaw) { try { arr2 = JSON.parse(legacyRaw) as string[]; } catch {} }

  const set = new Set([...arr1.map(String), ...arr2.map(String)]);
  const merged = Array.from(set);

  map[key] = merged; writeMap(map);
  window.localStorage.setItem(`${LEGACY_PREFIX}${key}`, JSON.stringify(merged));
  return merged;
}

export function isFavId(id: FavId, authKey?: string): boolean {
  const key = authKey || ensureAuthKey();
  return listFavIds(key).includes(String(id));
}

export function addFavId(id: FavId, authKey?: string) {
  const key = authKey || ensureAuthKey();
  const set = new Set(listFavIds(key));
  set.add(String(id));
  const merged = Array.from(set);
  const map = readMap(); map[key] = merged; writeMap(map);
  if (canUseStorage()) {
    window.localStorage.setItem(`${LEGACY_PREFIX}${key}`, JSON.stringify(merged));
  }
}

export function removeFavId(id: FavId, authKey?: string) {
  const key = authKey || ensureAuthKey();
  const set = new Set(listFavIds(key));
  set.delete(String(id));
  const merged = Array.from(set);
  const map = readMap(); map[key] = merged; writeMap(map);
  if (canUseStorage()) {
    window.localStorage.setItem(`${LEGACY_PREFIX}${key}`, JSON.stringify(merged));
  }
}

/** id만 토글 (호환용) */
export function toggleFavorite(id: FavId, authKey?: string): boolean {
  if (isFavId(id, authKey)) {
    removeFavId(id, authKey);
    return false;
  } else {
    addFavId(id, authKey);
    return true;
  }
}

/* ---------- public APIs (items) ---------- */
/** 즐겨찾기 여부(then 체인 호환 위해 Promise로 래핑) */
export async function isFav(id: FavId, authKey?: string): Promise<boolean> {
  return isFavId(id, authKey);
}

/** 카드 기반 토글: true=저장됨, false=해제됨 */
export async function toggleFav(card: MinimalListing, authKey?: string): Promise<boolean> {
  const key = authKey || ensureAuthKey();
  const id = String(card.id);

  if (isFavId(id, key)) {
    // off
    removeFavId(id, key);
    const items = readItems(key);
    if (items[id]) { delete items[id]; writeItems(key, items); }
    return false;
  } else {
    // on
    addFavId(id, key);
    const items = readItems(key);
    // 최소 정보만 저장(안전)
    items[id] = {
      id,
      title: card.title,
      address: card.address,
      type: card.type,
      price: card.price,
      rentMonthly: card.rentMonthly,
      thumb: card.thumb,
    };
    writeItems(key, items);
    return true;
  }
}

/** 미니 카드 목록 반환(없으면 id만으로 채움) */
export function getFavoriteItems(authKey?: string): MinimalListing[] {
  const key = authKey || ensureAuthKey();
  const ids = listFavIds(key);
  const items = readItems(key);
  return ids.map(id => items[id] ?? ({ id } as MinimalListing));
}

/* ---------- compatibility named exports ---------- */
export const addFavorite = (id: FavId, authKey?: string) => addFavId(id, authKey);
export const removeFavorite = (id: FavId, authKey?: string) => removeFavId(id, authKey);
// (toggleFavorite는 이미 export 되어 있음)
