import { defineConfig, devices } from "@playwright/test";

// SDK migration acceptance uses installed Chromium tooling without preparing unrelated browser engines.
export default defineConfig({
  testDir: "./tests",
  testMatch: ["product.spec.ts", "runtime.spec.ts", "player.spec.ts"],
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: "http://127.0.0.1:4173",
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  webServer: {
    command: "node server.mjs",
    port: 4173,
    reuseExistingServer: false,
    timeout: 30_000,
    env: { STREAMCORE_WEB_DISTRIBUTION: process.env.STREAMCORE_SDK_DISTRIBUTION ?? "production", STREAMCORE_WEB_PORT: "4173" },
  },
  projects: [
    { name: "chromium-1280", use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 720 } } },
    { name: "chromium-1920", use: { ...devices["Desktop Chrome"], viewport: { width: 1920, height: 1080 } } },
  ],
});
