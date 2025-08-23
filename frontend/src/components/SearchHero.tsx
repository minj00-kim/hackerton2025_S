// src/components/SearchHero.tsx
import { FormEvent, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

type Props = {
  /** true면 노란 배경 없이 폼만 렌더링 */
  noBg?: boolean;
  className?: string;
};

export default function SearchHero({ noBg = false, className = '' }: Props) {
  const [kw, setKw] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams(location.search);
    const v = kw.trim();
    if (v) params.set('q', v); else params.delete('q');
    params.delete('page'); // 페이지네이션 초기화
    navigate({ pathname: '/listings', search: params.toString() });
  };

  const Wrapper = (noBg ? 'div' : 'section') as keyof JSX.IntrinsicElements;
  const wrapClass = noBg ? 'py-0' : 'bg-[#ffdf65] py-12';

  return (
    <Wrapper className={`${wrapClass} ${className}`}>
      <div className="max-w-5xl mx-auto">
        <form
          onSubmit={onSubmit}
          className="rounded-full bg-white shadow-xl px-3 py-2 flex items-center gap-2"
        >
          <div className="px-3 py-1 text-sm border rounded-full bg-white">매물</div>
          <input
            className="flex-1 px-3 py-2 outline-none"
            value={kw}
            onChange={(e) => setKw(e.target.value)}
            placeholder="지역, 지하철, 건물명, 학교명으로 검색해 보세요."
          />
          <button type="submit" className="btn-primary rounded-full px-5 py-2">
            검색
          </button>
        </form>
      </div>
    </Wrapper>
  );
}
