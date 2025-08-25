import { definePreset } from '@primeng/themes';
import Lara from '@primeng/themes/lara';

export const SharedFinancesPreset = definePreset(Lara, {
  semantic: {
    primary: {
      50: '#f8faf3',
      100: '#eff5e4',
      200: '#e2edce',
      300: '#d2e4b3',
      400: '#c3da99',
      500: '#91bd45',
      600: 'oklch(26.2% 0.051 172.552)',
      700: '#81a93c',
      800: '#64832e',
      900: '#485d21',
      950: '#334318',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '{gray.50}',
          100: '{gray.100}',
          200: '{gray.200}',
          300: '{gray.300}',
          400: '{gray.400}',
          500: '{gray.500}',
          600: '{gray.600}',
          700: '{gray.700}',
          800: '{gray.800}',
          900: '{gray.900}',
          950: '{gray.950}',
        },
      },
      dark: {
        surface: {
          0: '#ffffff',
          50: '{zinc.50}',
          100: '{zinc.100}',
          200: '{zinc.200}',
          300: '{zinc.300}',
          400: '{zinc.400}',
          500: '{zinc.500}',
          600: '{zinc.600}',
          700: '{zinc.700}',
          800: '{zinc.800}',
          900: '{zinc.900}',
          950: '{zinc.950}',
        },
      },
    },
  },
});
