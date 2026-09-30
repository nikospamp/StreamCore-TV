import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { spawnSync } from "node:child_process";
import { readCredentials } from "../../webApp/e2e/review-common.mjs";

const serialIndex = process.argv.indexOf("--serial");
const serial = serialIndex >= 0 ? process.argv[serialIndex + 1] : null;
if (!serial || !/^[a-zA-Z0-9_.:-]+$/.test(serial)) throw new Error("Supply --serial for an explicit test device.");
const live = process.argv.includes("--live-tmdb");
const adb = process.env.STREAMCORE_ADB || "adb";
const app = "com.example.streamcore.consumer";
const apk = resolve("samples/sdk-consumer/androidApp/build/outputs/apk/debug/androidApp-debug.apk");
function run(args, input) {
  const result = spawnSync(adb, ["-s", serial, ...args], { input, encoding: "utf8", timeout: 30_000 });
  if (result.status !== 0) throw new Error("Android consumer command failed (details withheld).");
  return result.stdout;
}
try {
  run(["install", "-r", apk]);
  run(["shell", "input", "keyevent", "KEYCODE_WAKEUP"]);
  run(["shell", "wm", "dismiss-keyguard"]);
  run(["shell", "am", "force-stop", app]);
  // install -r preserves private files. Never let an interrupted live run select this run's mode.
  run(["shell", "run-as", app, "rm", "-f", "files/journey-result.txt", "files/live-tmdb.json"]);
  if (live) {
    const config = JSON.parse(await readFile(resolve("webApp/build/generated/webDevelopmentConfig/config.json"), "utf8"));
    const credentials = await readCredentials(resolve("docs/credentials/tmdb.txt"));
    run(["shell", "run-as", app, "mkdir", "-p", "files"]);
    run(["shell", `run-as ${app} sh -c 'cat > files/live-tmdb.json'`], JSON.stringify({
      readAccessToken: config.tmdbReadAccessToken, username: credentials.username, password: credentials.password,
    }));
    credentials.username = "";
    credentials.password = "";
  }
  run(["shell", "am", "start", "-n", `${app}/.ConsumerActivity`]);
  const expected = live ? "Published SDK journey passed (TMDB live)" : "Published SDK journey passed (ClientB)";
  const deadline = Date.now() + 120_000;
  let passed = false;
  while (Date.now() < deadline) {
    // Read the app-owned outcome without competing with another active UI automation service.
    const result = spawnSync(adb, ["-s", serial, "shell", "run-as", app, "cat", "files/journey-result.txt"], { encoding: "utf8", timeout: 10_000 });
    if (result.status === 0 && result.stdout.includes(expected)) { passed = true; break; }
    if (result.status === 0 && result.stdout.includes("Published SDK journey failed")) throw new Error("Published Android consumer reported a failure.");
    await new Promise(resolve => setTimeout(resolve, 1500));
  }
  if (!passed) throw new Error("Timed out waiting for the published Android consumer.");
  console.log(JSON.stringify({ provider: live ? "tmdb-live" : "clientb-reference", serial, passed: true, expectedAccountRestriction: false }));
} finally {
  // Exact task-owned transient files only; the application also deletes credentials immediately after reading.
  run(["shell", "run-as", app, "rm", "-f", "files/live-tmdb.json"]);
}
