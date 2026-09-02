import { defineConfig, devices } from "@playwright/test";
import { cpSync, existsSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { firefox } from "@playwright/test";

const firefoxSourceDirectory = dirname(firefox.executablePath());
const relocatedFirefoxDirectory = join(import.meta.dirname, ".playwright-browsers", "firefox");
const relocatedFirefoxExecutable = join(relocatedFirefoxDirectory, "firefox.exe");
if (process.platform === "win32" && !existsSync(relocatedFirefoxExecutable)) {
  mkdirSync(dirname(relocatedFirefoxDirectory), { recursive: true });
  cpSync(firefoxSourceDirectory, relocatedFirefoxDirectory, { recursive: true });
}

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  retries: 0,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: "http://127.0.0.1:4173",
    viewport: { width: 1280, height: 720 },
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  webServer: {
    command: "node server.mjs",
    port: 4173,
    reuseExistingServer: false,
    timeout: 30_000,
  },
  projects: [
    { name: "chromium-1280", use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 720 } } },
    { name: "chromium-1920", use: { ...devices["Desktop Chrome"], viewport: { width: 1920, height: 1080 } } },
    {
      name: "firefox-1280",
      use: {
        ...devices["Desktop Firefox"],
        viewport: { width: 1280, height: 720 },
        launchOptions: process.platform === "win32"
          ? { executablePath: relocatedFirefoxExecutable }
          : undefined,
      },
    },
    {
      name: "firefox-1920",
      use: {
        ...devices["Desktop Firefox"],
        viewport: { width: 1920, height: 1080 },
        launchOptions: process.platform === "win32"
          ? { executablePath: relocatedFirefoxExecutable }
          : undefined,
      },
    },
    { name: "webkit-1280", use: { ...devices["Desktop Safari"], viewport: { width: 1280, height: 720 } } },
    { name: "webkit-1920", use: { ...devices["Desktop Safari"], viewport: { width: 1920, height: 1080 } } },
  ],
});
