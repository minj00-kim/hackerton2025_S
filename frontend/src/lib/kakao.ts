// src/lib/kakao.ts
declare global { interface Window { kakao: any } }

let pending: Promise<void> | null = null;

export function loadKakaoMaps(): Promise<void> {
  // 이미 로드됨
  if (window.kakao?.maps) return Promise.resolve();
  if (pending) return pending;

  const key = import.meta.env.VITE_KAKAO_API_KEY;
  console.log('[KAKAO] key present?', !!key);
  if (!key) {
    return Promise.reject(new Error('VITE_KAKAO_API_KEY is missing in .env'));
  }

  // 기존 스크립트가 있으면 그걸로 대기
  const existing = document.querySelector<HTMLScriptElement>('#kakao-maps-sdk');
  const url =
    `https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey=${key}` +
    `&libraries=services,clusterer`;

  pending = new Promise<void>((resolve, reject) => {
    const onLoad = () => {
      // 도메인 미등록/잘못된 키면 onload는 되는데 window.kakao가 비어있을 수 있어
      if (window.kakao?.maps) return resolve();
      reject(new Error(
        'Kakao SDK loaded but window.kakao is undefined. ' +
        '→ JavaScript 키인지, 허용 도메인에 현재 주소가 등록됐는지 확인하세요.'
      ));
    };
    const onError = () => reject(new Error('Failed to load Kakao SDK'));

    if (existing) {
      existing.addEventListener('load', onLoad, { once: true });
      existing.addEventListener('error', onError, { once: true });
      // 이미 붙어있으면 onload가 바로 올 수도 있으니 한 번 체크
      if (window.kakao?.maps) resolve();
      return;
    }

    const s = document.createElement('script');
    s.id = 'kakao-maps-sdk';
    s.async = true;
    s.defer = true;
    s.src = url;
    s.addEventListener('load', onLoad, { once: true });
    s.addEventListener('error', onError, { once: true });
    document.head.appendChild(s);
  });

  // 10초 타임아웃
  const timeout = new Promise<void>((_, rej) =>
    setTimeout(() => rej(new Error('Kakao SDK load timeout')), 10000)
  );

  return Promise.race([pending, timeout]);
}
