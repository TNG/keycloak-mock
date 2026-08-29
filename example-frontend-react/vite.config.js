/// <reference types="vitest" />
import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // keep the historic dev server port so the keycloak client redirect still matches
    port: 3000,
  },
  build: {
    // the gradle build copies this directory into the example backend's static resources
    outDir: 'dist',
  },
  test: {
    environment: 'jsdom',
    globals: true,
  },
});
