// src/lib/auth.ts
const COOKIE_KEY = 'sv_auth';
const LOCAL_KEY  = 'svai-auth-key';
const ONE_YEAR = 60 * 60 * 24 * 365;

function setCookie(name: string, value: string, maxAgeSec = ONE_YEAR) {
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=${maxAgeSec}`;
}
function getCookie(name: string): string | undefined {
  return document.cookie
    .split(';')
    .map(s => s.trim())
    .find(s => s.startsWith(name + '='))?.split('=')[1];
}
function randomKey(n = 24) {
  try {
    const bytes = new Uint8Array(n);
    crypto.getRandomValues(bytes);
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    return Array.from(bytes).map(b => chars[b % chars.length]).join('');
  } catch {
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let s = '';
    for (let i = 0; i < n; i++) s += chars[Math.floor(Math.random() * chars.length)];
    return s;
  }
}

/** 방문자 고유키(쿠키 우선). 없으면 생성하고 쿠키+localStorage 모두에 동기화 */
export function ensureAuthKey(): string {
  let v = getCookie(COOKIE_KEY);
  if (!v) {
    v = localStorage.getItem(LOCAL_KEY) || randomKey();
    setCookie(COOKIE_KEY, v);
    localStorage.setItem(LOCAL_KEY, v);
  } else {
    localStorage.setItem(LOCAL_KEY, decodeURIComponent(v));
  }
  return decodeURIComponent(v);
}

// 구 코드 호환용 별칭
export const getAuthKey = ensureAuthKey;

export function maskKey(k: string, prefix = 6) {
  if (!k) return '';
  return `${k.slice(0, prefix)}•••`;
}

export function authHeaders() {
  return { 'X-Owner-Key': ensureAuthKey() };
}
