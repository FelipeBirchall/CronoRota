/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // Identidade visual definida na seção 1.2 do documento do projeto -
        // não são cores genéricas de template, são as cores que a equipe
        // já escolheu pro produto.
        petroleo: '#1F6F78',   // cor institucional - navegação, botões primários
        terracota: '#C2571A',  // alerta - tempo parado acima do esperado
        verdeok: '#2F6D3C',    // indicador dentro do esperado
        grafite: '#595959',    // texto secundário
        fundo: '#F7F6F3',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      fontFeatureSettings: {
        tabular: '"tnum"',
      },
    },
  },
  plugins: [],
};
