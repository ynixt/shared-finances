const { guessProductionMode } = require('@ngneat/tailwind');

process.env.TAILWIND_MODE = guessProductionMode() ? 'build' : 'watch';

module.exports = {
  prefix: '',
  important: true,
  mode: 'jit',
  purge: {
    content: ['./src/**/*.{html,ts,css,scss,sass,less,styl}'],
  },
  darkMode: false, // or 'media' or 'class'
  theme: {
    screens: {
      sm: '600px',
      md: '960px',
      lg: '1280px',
      xl: '1920px',
    },
    extend: {},
  },
  variants: {
    extend: {},
  },
};
