import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { mkdir, mkdtemp, readFile, readdir, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { join } from "node:path";
import test from "node:test";
import { promisify } from "node:util";
import { parseOptions, repositoryRoot } from "./review-common.mjs";
import { runWebReview } from "./review-web.mjs";

const execFileAsync = promisify(execFile);
const fixtureHtml = `<!doctype html><html><head><meta charset="utf-8"><title>Review tool fixture</title>
<style>body{margin:40px;background:#17212c;color:white;font:24px sans-serif}button,input{display:block;margin:20px;padding:16px;font:20px sans-serif}button{width:360px;height:64px}</style></head>
<body><script>
const fixtureMode = 'normal';
const authenticated = localStorage.getItem('review-fixture-session') === 'yes' && fixtureMode !== 'login-rejected';
if (!authenticated) history.replaceState({}, '', '/login');
const path = location.pathname;
document.body.setAttribute('data-product-route', path);
if (path === '/login') {
  document.body.innerHTML = '<h1>Fixture login</h1><form><input data-testid="login:identifier" autocomplete="username"><input data-testid="login:password" type="password" autocomplete="current-password"><button>Sign in</button></form>';
  document.querySelector('form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = document.querySelector('[data-testid="login:identifier"]').value;
    const password = document.querySelector('[data-testid="login:password"]').value;
    if (username !== 'fixture-user' || password !== 'fixture-password') return;
    if (fixtureMode === 'login-rejected') {
      // Match WebProductShell's real Compose error-dialog contract, which does
      // not depend on an HTML role=alert being present in this viewport.
      document.body.setAttribute('data-product-error-kind', 'authentication');
      document.body.setAttribute('data-product-error-title', 'Sign-in failed');
      document.body.setAttribute('data-product-error-message', 'fixture-secret rejection details');
      return;
    }
    await fetch('/fixture-login', {method:'POST'});
    localStorage.setItem('review-fixture-session', 'yes');
    location.assign('/profiles');
  });
} else if (path === '/profiles') {
  document.body.innerHTML = '<h1>Fixture profiles</h1><button aria-label="Select Fixture profile">Fixture</button><button aria-label="Select Second profile">Second</button>';
  for (const button of document.querySelectorAll('button')) button.onclick = () => location.assign('/home');
  if (fixtureMode === 'network-denied') {
    document.body.innerHTML = '<h1>Loading fixture profiles</h1>';
    fetch('/fixture-network?token=fixture-secret').catch(() => {});
  }
} else if (path === '/home') {
  document.body.innerHTML = '<h1>Fixture home</h1><button style="display:none" aria-label="Open details for Example Feature">Hidden duplicate</button><button aria-label="Open details for Example Feature">Example Feature</button>';
  for (const button of document.querySelectorAll('button')) button.onclick = () => location.assign('/details/42');
} else if (path === '/details/42') {
  document.body.innerHTML = '<h1>Example Feature</h1><button aria-label="Play">Play</button>';
}
document.body.setAttribute('data-product-visual-state', 'ready');
</script></body></html>`;

test("real browser fixture covers all screens, auth reuse, output preservation and cleanup after errors", {
  skip: process.env.STREAMCORE_REVIEW_BROWSER_TEST !== "1" ? "Set STREAMCORE_REVIEW_BROWSER_TEST=1 for the installed-browser integration check." : false,
  timeout: 120000,
}, async (t) => {
  const testRoot = join(repositoryRoot, "build/review/tests");
  await mkdir(testRoot, { recursive: true });
  const directory = await mkdtemp(join(testRoot, "web-"));
  const credentialsFile = join(directory, "synthetic-credentials.txt");
  await writeFile(credentialsFile, "username=fixture-user\npassword=fixture-password\n");
  let loginCount = 0;
  let fixtureMode = "normal";
  const server = createServer((request, response) => {
    if (request.url === "/__streamcore_review") {
      response.writeHead(200, { "Content-Type": "application/json" });
      response.end(JSON.stringify({
        schemaVersion: 2, service: "streamcore-review", checkout: repositoryRoot,
        distribution: "developmentExecutable", pid: process.pid,
        artifactPath: join(repositoryRoot, "webApp/build/dist/wasmJs/developmentExecutable"), runtimeConfigPath: null,
      }));
    } else if (request.url === "/fixture-login") {
      loginCount += 1;
      response.writeHead(204).end();
    } else {
      response.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      response.end(fixtureHtml.replace("const fixtureMode = 'normal';", `const fixtureMode = ${JSON.stringify(fixtureMode)};`));
    }
  });
  await new Promise((done) => server.listen(0, "127.0.0.1", done));
  t.after(() => new Promise((done) => server.close(done)));
  const baseArgs = [
    "--base-url", `http://127.0.0.1:${server.address().port}`,
    "--user-data-dir", join(directory, "browser"),
    "--channel", process.env.STREAMCORE_REVIEW_BROWSER_CHANNEL ?? "chrome",
    "--headless", "--viewport", "1280x720", "--settle-ms", "0", "--timeout-ms", "5000", "--allow-unverified-artifacts",
  ];
  async function capture(name, args, credentials = join(directory, "missing-credentials.txt")) {
    const emissions = [];
    const result = await runWebReview(parseOptions([
      ...baseArgs, "--output", join(directory, `${name}.png`), "--credentials-file", credentials, ...args,
    ]), (value) => emissions.push(value));
    assert.equal(emissions.length, 1);
    assert.equal((await readdir(directory)).some((entry) => entry.endsWith(".review-lock")), false, "profile lock must be released after every run");
    return result;
  }
  const profiles = await capture("profiles", ["--screen", "profiles"], credentialsFile);
  assert.equal(profiles.success, true, JSON.stringify(profiles));
  assert.equal(profiles.artifactStatus, "unknown", "fixture identity cannot certify production freshness");
  assert.equal(loginCount, 1);
  const screenshot = await readFile(profiles.screenshot);
  assert.equal(screenshot.subarray(0, 8).toString("hex"), "89504e470d0a1a0a");
  const original = Buffer.from(screenshot);
  const duplicate = await capture("profiles", ["--screen", "profiles"]);
  assert.equal(duplicate.code, "OUTPUT_EXISTS");
  assert.deepEqual(await readFile(profiles.screenshot), original);
  const home = await capture("home", ["--screen", "home", "--profile", "Fixture"]);
  assert.equal(home.success, true, JSON.stringify(home));
  assert.equal(home.selectedProfile, "Select Fixture profile");
  const details = await capture("details", ["--screen", "details", "--profile", "Fixture", "--asset", "Example Feature"]);
  assert.equal(details.success, true, JSON.stringify(details));
  assert.equal(details.selectedAsset, "Example Feature");
  assert.match(details.url, /\/details\/42$/);
  assert.equal(loginCount, 1, "the persisted session must avoid reading missing credentials or logging in again");
  const ambiguous = await capture("ambiguous", ["--screen", "home"]);
  assert.equal(ambiguous.code, "PROFILE_SELECTION_REQUIRED");
  const missing = await capture("missing", ["--screen", "details", "--profile", "Fixture", "--asset", "Absent Feature"]);
  assert.equal(missing.code, "TIMEOUT");
  assert.equal(missing.phase, "select-asset");
  // The actual CLI must finish naturally from a foreign working directory, close Chrome,
  // release the shared profile, and emit exactly one JSON record without log chatter.
  const child = await execFileAsync(process.execPath, [
    join(repositoryRoot, "webApp/e2e/review-web.mjs"), ...baseArgs,
    "--screen", "profiles", "--output", join(directory, "cli.png"),
    "--credentials-file", join(directory, "missing-credentials.txt"),
  ], { cwd: directory, timeout: 30000, windowsHide: true });
  assert.equal(child.stderr.trim(), "");
  const records = child.stdout.trim().split(/\r?\n/);
  assert.equal(records.length, 1);
  assert.equal(JSON.parse(records[0]).success, true);
  assert.equal((await readdir(directory)).some((entry) => entry.endsWith(".review-lock")), false);

  fixtureMode = "login-rejected";
  const rejected = await capture("login-rejected", ["--screen", "profiles"], credentialsFile);
  assert.equal(rejected.code, "LOGIN_REJECTED");
  assert.equal(rejected.phase, "login");
  assert.equal(JSON.stringify(rejected).includes("fixture-secret"), false);

  fixtureMode = "network-denied";
  const { chromium } = await import("playwright");
  const originalLaunch = chromium.launchPersistentContext;
  // Page routes take precedence over the runner's origin guard. Simulate the Chromium
  // access-denied event without external traffic, credentials, or production-only hooks.
  chromium.launchPersistentContext = async function (...args) {
    const context = await originalLaunch.apply(this, args);
    for (const page of context.pages()) {
      await page.route("**/fixture-network?*", (route) => route.abort("accessdenied"));
    }
    return context;
  };
  try {
    const denied = await capture("network-denied", ["--screen", "profiles"]);
    assert.equal(denied.code, "NETWORK_ACCESS_DENIED");
    assert.equal(JSON.stringify(denied).includes("fixture-secret"), false);
    assert.equal(JSON.stringify(denied).includes("fixture-network"), false);
    assert.equal((await readdir(directory)).includes("network-denied.png"), false);
  } finally {
    chromium.launchPersistentContext = originalLaunch;
  }
});
