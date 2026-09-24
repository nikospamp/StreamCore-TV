import { writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  acquireProfileLock, parseOptions, readCredentials, requireExpectedOrigin,
  reserveOutput, ReviewError, safeFailure, verifyReviewServer,
} from "./review-common.mjs";

const usage = `Capture an existing StreamCoreTV web preview (PowerShell examples in docs/agent-workflow.md).
  node webApp/e2e/review-web.mjs --screen profiles|home|details [options]
  --base-url URL                 Loopback preview origin (default http://127.0.0.1:8080)
  --profile NAME                Exact profile name or "Select NAME profile" label
  --asset TITLE                 Exact catalogue title; required for details
  --viewport WIDTHxHEIGHT       Default 1920x1080
  --channel chrome|chromium|msedge  Default chrome; chromium uses the pinned installation
  --headless                    Hide the browser
  --user-data-dir PATH          Persistent profile (default build/review/browser)
  --output PATH.png             Default build/review/web-<screen>.png; JSON beside it; both must be new
  --credentials-file PATH       Read only if sign-in is required (default docs/credentials/tmdb.txt)
  --timeout-ms NUMBER           Per-step timeout, 1000–300000 (default 60000)
  --settle-ms NUMBER            Bounded final canvas/artwork settlement, 0–30000 (default 1500)
  --allow-unverified-artifacts  Explicitly permit stale/unknown build provenance
  --keep-open                   Emit the result, then wait for browser close or Ctrl+C
Relative file paths resolve from the repository, regardless of your working directory.
Multiple profiles require --profile. Original screenshots require visual review; route readiness
and settlement do not prove canvas artwork finished loading. No builds or servers are started.`;

async function waitForRoute(page, path, options) {
  await page.waitForURL((url) => url.origin === options.baseUrl && url.pathname === path);
  await page.waitForFunction((expected) => {
    return document.body?.getAttribute("data-product-route") === expected
      && document.body?.getAttribute("data-product-visual-state") === "ready";
  }, path);
  requireExpectedOrigin(page.url(), options.baseUrl);
}

async function waitForFrames(page, count = 4) {
  await page.waitForFunction(() => document.visibilityState === "visible");
  // A finite frame count gives Compose its next paint without a model/tool roundtrip.
  await page.evaluate((frames) => new Promise((done, reject) => {
    const timeout = setTimeout(() => reject(new Error("Animation frames did not advance")), 5000);
    let remaining = frames;
    function tick() {
      remaining -= 1;
      if (remaining === 0) {
        clearTimeout(timeout);
        done();
      } else {
        requestAnimationFrame(tick);
      }
    }
    requestAnimationFrame(tick);
  }), count);
}

function clickablePoint(bounds, viewport) {
  if (!bounds || bounds.width <= 0 || bounds.height <= 0) return null;
  const left = Math.max(0, bounds.x);
  const right = Math.min(viewport.width, bounds.x + bounds.width);
  const top = Math.max(0, bounds.y);
  const bottom = Math.min(viewport.height, bounds.y + bounds.height);
  if (right <= left || bottom <= top) return null;
  return { x: (left + right) / 2, y: (top + bottom) / 2 };
}

async function activateSemanticButton(page, candidates, options) {
  await candidates.first().waitFor({ state: "attached" });
  await waitForFrames(page);
  for (let index = 0; index < await candidates.count(); index += 1) {
    const candidate = candidates.nth(index);
    if (!await candidate.isVisible()) continue;
    let point = clickablePoint(await candidate.boundingBox(), options.viewport);
    if (!point) continue;
    // Compose's semantic overlay can intercept locator.click; use its current painted bounds.
    await page.mouse.move(point.x, point.y);
    await waitForFrames(page, 2);
    point = clickablePoint(await candidate.boundingBox(), options.viewport);
    if (!point) continue;
    requireExpectedOrigin(page.url(), options.baseUrl);
    await page.mouse.click(point.x, point.y);
    return;
  }
  throw new ReviewError("TARGET_NOT_VISIBLE", "The selected control has no visible viewport bounds. Choose visible content or adjust the viewport; no coordinate-based fallback was attempted.");
}

async function ensureProfiles(page, options, setPhase) {
  setPhase("open");
  await page.goto(`${options.baseUrl}/profiles`, { waitUntil: "domcontentloaded" });
  requireExpectedOrigin(page.url(), options.baseUrl);
  await page.waitForFunction(() => {
    return ["/login", "/profiles"].includes(document.body?.getAttribute("data-product-route"))
      && document.body?.getAttribute("data-product-visual-state") === "ready";
  });
  requireExpectedOrigin(page.url(), options.baseUrl);
  if (new URL(page.url()).pathname === "/login") {
    setPhase("login");
    const credentials = await readCredentials(options.credentialsFile);
    try {
      requireExpectedOrigin(page.url(), options.baseUrl);
      await page.getByTestId("login:identifier").fill(credentials.username);
      requireExpectedOrigin(page.url(), options.baseUrl);
      await page.getByTestId("login:password").fill(credentials.password);
      requireExpectedOrigin(page.url(), options.baseUrl);
      await page.getByTestId("login:password").press("Enter");
    } finally {
      credentials.username = "";
      credentials.password = "";
    }
    const loginOutcome = await page.waitForFunction(() => {
      if (document.body?.getAttribute("data-product-route") === "/profiles"
        && document.body?.getAttribute("data-product-visual-state") === "ready") return "accepted";
      if (location.pathname !== "/login") return false;
      // The Compose error dialog is in its own viewport; WebProductShell exposes
      // these body attributes as the portable application error signal.
      if (document.body?.getAttribute("data-product-error-kind")
        && (document.body?.getAttribute("data-product-error-title")?.trim()
          || document.body?.getAttribute("data-product-error-message")?.trim())) return "rejected";
      for (const alert of document.querySelectorAll('[role="alert"]')) {
        const bounds = alert.getBoundingClientRect();
        const style = getComputedStyle(alert);
        if (alert.textContent?.trim() && bounds.width > 0 && bounds.height > 0
          && style.visibility !== "hidden" && style.display !== "none") return "rejected";
      }
      return false;
    });
    try {
      if (await loginOutcome.jsonValue() === "rejected") {
        throw new ReviewError("LOGIN_REJECTED", "The login page displayed an error after sign-in. Inspect authentication interactively; error text and credentials were not logged.");
      }
    } finally {
      await loginOutcome.dispose();
    }
  }
  setPhase("profiles");
  await waitForRoute(page, "/profiles", options);
  await page.getByRole("button", { name: /^Select .* profile$/ }).first().waitFor({ state: "attached" });
}

async function selectProfile(page, options) {
  const profiles = page.getByRole("button", { name: /^Select .* profile$/ });
  let selected;
  if (options.profile) {
    const label = options.profile.startsWith("Select ") && options.profile.endsWith(" profile")
      ? options.profile : `Select ${options.profile} profile`;
    selected = page.getByRole("button", { name: label, exact: true });
    await selected.first().waitFor({ state: "attached" });
    if (await selected.count() !== 1) {
      throw new ReviewError("PROFILE_AMBIGUOUS", "The requested profile label is not unique. Resolve duplicate profile names before capturing.");
    }
  } else {
    if (await profiles.count() !== 1) {
      throw new ReviewError("PROFILE_SELECTION_REQUIRED", "Multiple profiles are available. Supply --profile with an exact profile name or accessible label.");
    }
    selected = profiles;
  }
  const label = await selected.getAttribute("aria-label") ?? options.profile ?? "single available profile";
  await activateSemanticButton(page, selected, options);
  await waitForRoute(page, "/home", options);
  return label;
}

export async function runWebReview(options, emit = (value) => console.log(JSON.stringify(value))) {
  let phase = "output";
  let resultFile;
  let context;
  let releaseLock;
  let interrupted = false;
  let emitted = false;
  let result;
  let observedFailure;
  let browserClosed = false;
  let notifyClosed;
  const closed = new Promise((done) => { notifyClosed = done; });
  const onSignal = () => {
    interrupted = true;
    if (context) void context.close().catch(() => {});
  };
  process.once("SIGINT", onSignal);
  process.once("SIGTERM", onSignal);
  try {
    resultFile = await reserveOutput(options);
    phase = "server";
    const provenance = await verifyReviewServer(options);
    phase = "profile-lock";
    releaseLock = await acquireProfileLock(options.userDataDir);
    if (interrupted) throw new ReviewError("INTERRUPTED", "Review interrupted before browser launch.");
    phase = "launch";
    let chromium;
    try {
      ({ chromium } = await import("playwright"));
    } catch {
      throw new ReviewError("DEPENDENCIES_MISSING", "Project Playwright is unavailable. Run npm ci in webApp/e2e explicitly before capturing.");
    }
    context = await chromium.launchPersistentContext(options.userDataDir, {
      ...(options.channel === "chromium" ? {} : { channel: options.channel }),
      headless: options.headless,
      viewport: options.viewport,
      timeout: options.timeoutMs,
      args: [`--window-size=${options.viewport.width},${options.viewport.height + 80}`],
    });
    context.once("close", () => { browserClosed = true; notifyClosed(); });
    if (interrupted) throw new ReviewError("INTERRUPTED", "Review interrupted during browser launch.");
    context.setDefaultTimeout(options.timeoutMs);
    context.setDefaultNavigationTimeout(options.timeoutMs);
    // Block navigation away from the verified app origin, while allowing its API/artwork requests.
    await context.route("**/*", async (route) => {
      const request = route.request();
      if (request.isNavigationRequest() && new URL(request.url()).origin !== options.baseUrl) {
        await route.abort("blockedbyclient");
      } else {
        await route.continue();
      }
    });
    const page = context.pages()[0] ?? await context.newPage();
    page.on("requestfailed", (request) => {
      // The sandbox can deny backend traffic even when the loopback preview is reachable.
      // Keep URLs, headers, and browser error text out of the result: they may contain tokens.
      if (result?.success || observedFailure
        || !/\bERR_(?:NETWORK_)?ACCESS_DENIED\b/.test(request.failure()?.errorText ?? "")) return;
      observedFailure = {
        phase,
        error: new ReviewError("NETWORK_ACCESS_DENIED", "Browser network access was denied by the execution environment. Resolve the host network permission and retry; no request details or credentials were logged."),
      };
      // Closing only this owned context cancels pending waits. Never throw from an event callback.
      void context.close().catch(() => {});
    });
    await page.bringToFront();
    await ensureProfiles(page, options, (value) => { phase = value; });
    let selectedProfile;
    if (options.screen !== "profiles") {
      phase = "select-profile";
      selectedProfile = await selectProfile(page, options);
      phase = "home-content";
      await page.getByRole("button", { name: /^Open details for / }).first().waitFor({ state: "attached" });
    }
    if (options.screen === "details") {
      phase = "select-asset";
      await activateSemanticButton(page, page.getByRole("button", { name: `Open details for ${options.asset}`, exact: true }), options);
      phase = "details-content";
      await page.waitForURL((url) => url.origin === options.baseUrl && /^\/details\/[^/]+$/.test(url.pathname));
      await waitForRoute(page, new URL(page.url()).pathname, options);
      await page.getByRole("button", { name: "Play", exact: true }).first().waitFor({ state: "attached" });
    }
    const capturePath = new URL(page.url()).pathname;
    phase = "settlement";
    await page.mouse.move(options.viewport.width - 2, options.viewport.height - 2);
    await waitForFrames(page);
    // Compose canvas image completion has no DOM image signal. Expose this bounded allowance
    // explicitly and retain final human/model screenshot inspection as the acceptance check.
    await page.waitForTimeout(options.settleMs);
    requireExpectedOrigin(page.url(), options.baseUrl);
    await waitForRoute(page, capturePath, options);
    phase = "capture";
    const screenshot = await page.screenshot({ type: "png", timeout: options.timeoutMs });
    try {
      await writeFile(options.output, screenshot, { flag: "wx" });
    } catch (error) {
      if (error.code === "EEXIST") {
        throw new ReviewError("OUTPUT_EXISTS", "Screenshot output was created by another process. Choose a new --output path.");
      }
      throw error;
    }
    result = {
      success: true,
      screen: options.screen,
      url: `${options.baseUrl}${new URL(page.url()).pathname}`,
      screenshot: options.output,
      viewport: options.viewport,
      selectedProfile,
      selectedAsset: options.screen === "details" ? options.asset : undefined,
      ...provenance,
      settleMs: options.settleMs,
      requiresVisualInspection: true,
    };
    if (options.keepOpen) {
      await resultFile.writeFile(`${JSON.stringify(result, null, 2)}\n`);
      emit({ ...result, browserKeptOpen: true });
      emitted = true;
      phase = "keep-open";
      if (!browserClosed) await closed;
    }
  } catch (error) {
    const failurePhase = observedFailure?.phase ?? phase;
    const failure = interrupted ? { code: "INTERRUPTED", message: "Review interrupted." } : safeFailure(observedFailure?.error ?? error, failurePhase);
    result = { success: false, phase: failurePhase, ...failure };
  } finally {
    let cleanupFailed = false;
    if (context && !browserClosed) {
      try {
        await context.close();
        browserClosed = true;
      } catch {
        cleanupFailed = true;
      }
    }
    if (releaseLock && !cleanupFailed) {
      try {
        await releaseLock();
      } catch {
        cleanupFailed = true;
      }
    }
    process.removeListener("SIGINT", onSignal);
    process.removeListener("SIGTERM", onSignal);
    if (cleanupFailed && result?.success) {
      result = { ...result, success: false, phase: "cleanup", code: "CLEANUP_FAILED", message: "Capture completed, but browser/profile cleanup could not be confirmed. Inspect the profile lock before reuse." };
    }
  }
  if (resultFile) {
    try {
      const content = Buffer.from(`${JSON.stringify(result, null, 2)}\n`);
      await resultFile.truncate(0);
      await resultFile.write(content, 0, content.length, 0);
    } catch {
      if (result.success) {
        result = { ...result, success: false, phase: "result", code: "RESULT_UNWRITABLE", message: "Capture completed, but the JSON result could not be written beside the screenshot." };
      }
    } finally {
      await resultFile.close().catch(() => {});
    }
  }
  if (!emitted) emit(result);
  return result;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const options = parseOptions(process.argv.slice(2));
    if (options.help) {
      console.log(usage);
    } else {
      const result = await runWebReview(options);
      process.exitCode = result.success ? 0 : 1;
    }
  } catch (error) {
    console.log(JSON.stringify({ success: false, phase: "arguments", ...safeFailure(error, "arguments") }));
    process.exitCode = 1;
  }
}
