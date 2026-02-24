import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  define: {
    global: "window",
  },
  optimizeDeps: {
    // sockjs-client is CJS-only; telling Vite to pre-bundle it with esbuild
    // ensures the default export is available at runtime
    include: ["sockjs-client"],
  },
});
