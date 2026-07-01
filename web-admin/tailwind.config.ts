import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // brand TiredCity
        brand: { DEFAULT: '#C0392B', dark: '#922B21', soft: '#F9EBEA' },
      },
    },
  },
  plugins: [],
};
export default config;
