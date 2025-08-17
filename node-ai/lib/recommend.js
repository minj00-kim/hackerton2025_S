import { searchCategory, searchKeyword } from './kakao.js';
import { geocodeAddress } from './geocode.js';
import fs from 'fs/promises';
import path from 'path';

// ✅ 실행 위치가 node-ai 폴더라면 이렇게 간단히:
const DATA_DIR = path.join(process.cwd(), 'data');     // ← node-ai/data
const LISTINGS_PATH = path.join(DATA_DIR, 'listings.json');

// 업종 아키타입
const ARCH = {
    '카페':   { needTraffic: 4, margin: 0.65, minArea: 35, rentWeight: 0.9,  targetYouth: true  },
    '치킨':   { needTraffic: 3, margin: 0.55, minArea: 60, rentWeight: 0.7,  targetYouth: false },
    '분식':   { needTraffic: 3, margin: 0.60, minArea: 30, rentWeight: 0.8,  targetYouth: true  },
    '피자':   { needTraffic: 3, margin: 0.52, minArea: 70, rentWeight: 0.7,  targetYouth: false },
    '편의점': { needTraffic: 4, margin: 0.35, minArea: 50, rentWeight: 0.95, targetYouth: false },
    '독서실': { needTraffic: 2, margin: 0.45, minArea: 100, rentWeight: 0.6,  targetYouth: true  }
};

// 간단 캐시
const mem = new Map();
const mkey = (...args) => args.join('::');

// ✅ Kakao 신호 집계(에러/누락 방어)
async function collectSignals({ lat, lng }) {
    const safe = (v) => v && Array.isArray(v.items) ? v : { total: 0, items: [] };

    const CE7 = safe(await searchCategory({ code: 'CE7', x: lng, y: lat, radius: 700 })); // 카페
    const FD6 = safe(await searchCategory({ code: 'FD6', x: lng, y: lat, radius: 700 })); // 음식점
    const CS2 = safe(await searchCategory({ code: 'CS2', x: lng, y: lat, radius: 700 })); // 편의점

    // 거리 가중 근사
    const decay = (items) => items.slice(0, 30).reduce((t, _, i) => t + 1 / Math.sqrt(i + 1), 0);
    const comp = decay(FD6.items) + 0.7 * decay(CE7.items);
    const attract = 0.6 * decay(CE7.items) + 0.4 * decay(CS2.items);

    return {
        counts: { cafe: CE7.total, food: FD6.total, cvs: CS2.total },
        compIndex: Math.min(1, comp / 15),
        attractIndex: Math.min(1, attract / 12),
    };
}

