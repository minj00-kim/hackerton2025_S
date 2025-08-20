// 카테고리 아이콘 그리드
// - 아이콘(이모지) 클릭 시 /listings?theme=<카테고리> 로 이동
// - 백엔드 /api/listings 의 theme 필터가 매칭되어 해당 카테고리 매물만 보입니다.

import { Link } from "react-router-dom";

type Cat = { key: string; label: string; emoji: string };

const CATEGORIES: Cat[] = [
  { key: "coffee", label: "커피", emoji: "☕" },
  { key: "korean", label: "한식", emoji: "🍚" },
  { key: "pub", label: "주점", emoji: "🍺" },
  { key: "life", label: "생활서비스", emoji: "🧰" },
  { key: "taxlaw", label: "법무·세무", emoji: "⚖️" },
  { key: "logi", label: "운송물류", emoji: "🚚" },
  { key: "estate", label: "부동산", emoji: "🏢" },
  { key: "law", label: "법률", emoji: "🔨" },
  { key: "chicken", label: "치킨", emoji: "🍗" },
  { key: "pizza", label: "피자", emoji: "🍕" },
  { key: "beauty", label: "미용", emoji: "💇" },
  { key: "bakery", label: "베이커리", emoji: "🥐" },
  { key: "security", label: "보안", emoji: "🛡️" },
  { key: "finance", label: "금융", emoji: "💳" },
  { key: "academy", label: "학원", emoji: "📘" },
  { key: "pet", label: "반려동물", emoji: "🐾" },
];

export default function CategoryGrid() {
  return (
    <div className="card p-6">
      <div className="text-lg font-semibold mb-1">카테고리</div>
      <p className="text-sm text-gray-600 mb-4">
        아이콘만 클릭해도 <b>상세 페이지</b>로 이동합니다.
      </p>

      {/* 가운데 정렬 + 가로로 쭉(줄바꿈 허용) */}
      <div className="flex flex-wrap justify-center gap-3">
        {CATEGORIES.map((c) => (
          <Link
            key={c.key}
            to={`/listings?theme=${encodeURIComponent(c.label)}`}
            className="w-[92px] h-[92px] rounded-2xl border bg-white hover:bg-brand-50 hover:border-brand-200 transition flex flex-col items-center justify-center text-center"
            aria-label={`${c.label} 카테고리로 이동`}
            title={c.label}
          >
            <span className="text-3xl">{c.emoji}</span>
            <span className="text-xs mt-2 text-gray-800">{c.label}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
