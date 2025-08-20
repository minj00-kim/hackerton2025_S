/** Tailwind: 마이프랜차이즈 느낌의 담백 블루 + 카드 그림자 */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx,js,jsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eff6ff",100: "#dbeafe",200: "#bfdbfe",300: "#93c5fd",400: "#60a5fa",
          500: "#3b82f6",600: "#2563eb",700: "#1d4ed8",800: "#1e40af",900: "#1e3a8a"
        }
      },
      boxShadow: { soft: "0 12px 32px -14px rgba(0,0,0,.18)" },
      fontFamily: { sans: ['system-ui', 'Arial', 'Apple SD Gothic Neo', 'Noto Sans KR', 'sans-serif'] }
    },
  },
  plugins: [],
}