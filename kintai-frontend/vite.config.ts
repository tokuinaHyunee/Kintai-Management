import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import type { ServerResponse } from "http";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080/kintai-backend",
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on("error", (_err, _req, res) => {
            const serverRes = res as ServerResponse;
            if (!serverRes.headersSent) {
              serverRes.writeHead(503, { "Content-Type": "application/json" });
              serverRes.end(
                JSON.stringify({ message: "バックエンドサーバーに接続できません（localhost:8080）" }),
              );
            }
          });
        },
      },
    },
  },
});
