import { randomUUID } from "node:crypto";
import { lstat, mkdir, open, readFile, realpath, rmdir, unlink, writeFile } from "node:fs/promises";
import { hostname } from "node:os";
import { dirname, extname, isAbsolute, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

export const repositoryRoot = fileURLToPath(new URL("../../", import.meta.url));

export class ReviewError extends Error {
  constructor(code, message) {
    super(message);
    this.name = "ReviewError";
    this.code = code;
  }
}

export function parseOptions(args) {
  const values = new Map();
  const flags = new Set(["headless", "keep-open", "allow-unverified-artifacts", "help"]);
  const names = new Set([
    ...flags, "screen", "base-url", "profile", "asset", "viewport", "channel",
    "user-data-dir", "output", "credentials-file", "timeout-ms", "settle-ms",
  ]);
  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    const name = argument.startsWith("--") ? argument.slice(2) : "";
    if (!names.has(name) || values.has(name)) {
      throw new ReviewError("INVALID_ARGUMENT", "Unknown or repeated option. Run with --help for usage.");
    }
    if (flags.has(name)) {
      values.set(name, true);
    } else {
      const value = args[++index];
      if (!value || value.startsWith("--")) {
        throw new ReviewError("INVALID_ARGUMENT", "An option is missing its value. Run with --help for usage.");
      }
      values.set(name, value);
    }
  }
  if (values.has("help")) {
    return { help: true };
  }
  const screen = values.get("screen");
  if (!["profiles", "home", "details"].includes(screen)) {
    throw new ReviewError("INVALID_SCREEN", "Use --screen profiles, home, or details.");
  }
  if (screen === "details" && !values.get("asset")) {
    throw new ReviewError("ASSET_REQUIRED", "Details capture requires --asset with an exact catalogue title.");
  }
  const viewportMatch = /^(\d+)x(\d+)$/.exec(values.get("viewport") ?? "1920x1080");
  const viewport = viewportMatch && { width: Number(viewportMatch[1]), height: Number(viewportMatch[2]) };
  if (!viewport || Object.values(viewport).some((value) => value < 320 || value > 8192)) {
    throw new ReviewError("INVALID_VIEWPORT", "Use --viewport WIDTHxHEIGHT, with dimensions between 320 and 8192.");
  }
  const channel = values.get("channel") ?? "chrome";
  if (!["chrome", "chromium", "msedge"].includes(channel)) {
    throw new ReviewError("INVALID_CHANNEL", "Use --channel chrome, chromium, or msedge.");
  }
  const output = resolve(repositoryRoot, values.get("output") ?? `build/review/web-${screen}.png`);
  if (extname(output).toLowerCase() !== ".png") {
    throw new ReviewError("INVALID_OUTPUT", "The screenshot output must have a .png extension.");
  }
  return {
    screen,
    baseUrl: validateBaseUrl(values.get("base-url") ?? "http://127.0.0.1:8080"),
    profile: values.get("profile"),
    asset: values.get("asset"),
    viewport,
    channel,
    headless: values.has("headless"),
    keepOpen: values.has("keep-open"),
    allowUnverifiedArtifacts: values.has("allow-unverified-artifacts"),
    userDataDir: resolve(repositoryRoot, values.get("user-data-dir") ?? "build/review/browser"),
    output,
    credentialsFile: resolve(repositoryRoot, values.get("credentials-file") ?? "docs/credentials/tmdb.txt"),
    timeoutMs: boundedInteger(values.get("timeout-ms") ?? "60000", 1000, 300000, "timeout"),
    settleMs: boundedInteger(values.get("settle-ms") ?? "1500", 0, 30000, "settlement"),
  };
}

function boundedInteger(value, minimum, maximum, label) {
  const number = Number(value);
  if (!/^\d+$/.test(value) || !Number.isSafeInteger(number) || number < minimum || number > maximum) {
    throw new ReviewError("INVALID_ARGUMENT", `Invalid ${label} duration; expected ${minimum}–${maximum} milliseconds.`);
  }
  return number;
}

export function validateBaseUrl(value) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new ReviewError("INVALID_ORIGIN", "The preview URL must be an HTTP loopback origin.");
  }
  if (url.protocol !== "http:" || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname)
    || url.username || url.password || url.pathname !== "/" || url.search || url.hash) {
    throw new ReviewError("INVALID_ORIGIN", "The preview URL must be an HTTP loopback origin without credentials, path, query, or fragment.");
  }
  return url.origin;
}

export function requireExpectedOrigin(value, baseUrl) {
  let matches = false;
  try {
    matches = new URL(value).origin === baseUrl;
  } catch {
    // Never include the untrusted URL in an error: it can carry credentials.
  }
  if (!matches) {
    throw new ReviewError("UNEXPECTED_ORIGIN", "The page left the verified preview origin. Capture and credential entry were stopped.");
  }
}

export async function verifyReviewServer(options, {
  root = repositoryRoot,
  getStatus = async (target, statusOptions) => {
    const { reviewStatus } = await import("./review-provenance.mjs");
    return reviewStatus(target, statusOptions);
  },
} = {}) {
  let identity;
  try {
    const response = await fetch(`${options.baseUrl}/__streamcore_review`, {
      redirect: "error",
      signal: AbortSignal.timeout(Math.min(options.timeoutMs, 10000)),
    });
    if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) {
      throw new Error("identity unavailable");
    }
    identity = await response.json();
  } catch {
    throw new ReviewError("SERVER_UNVERIFIED", "The preview identity endpoint is unavailable. Start this checkout's review server and check its port.");
  }
  let checkoutMatches = false;
  if (typeof identity?.checkout === "string") {
    try {
      checkoutMatches = normalizePath(await realpath(identity.checkout)) === normalizePath(await realpath(root));
    } catch {
      // Invalid, missing, and inaccessible checkout paths cannot establish server ownership.
    }
  }
  if (identity?.schemaVersion !== 2 || identity?.service !== "streamcore-review" || !checkoutMatches) {
    throw new ReviewError("SERVER_MISMATCH", "The server does not identify this checkout. Use the review server started from this repository.");
  }
  const validDistribution = ["productionExecutable", "developmentExecutable"].includes(identity.distribution);
  const validRuntimePath = identity.runtimeConfigPath === null
    || (typeof identity.runtimeConfigPath === "string" && isAbsolute(identity.runtimeConfigPath));
  let artifactMatches = false;
  if (validDistribution && typeof identity.artifactPath === "string" && isAbsolute(identity.artifactPath)) {
    try {
      const expected = resolve(root, "webApp/build/dist/wasmJs", identity.distribution);
      artifactMatches = normalizePath(await canonicalDestination(identity.artifactPath))
        === normalizePath(await canonicalDestination(expected));
    } catch {
      // A path that cannot be resolved must not be read for provenance validation.
    }
  }
  if (!validDistribution || !validRuntimePath || !artifactMatches) {
    throw new ReviewError("SERVER_MISMATCH", "The server's distribution or runtime configuration path is invalid for this checkout.");
  }
  // The server identifies the files it serves; it does not certify their freshness.
  // Development distributions remain usable with the explicit unverified override.
  const provenance = identity.distribution === "productionExecutable" && identity.runtimeConfigPath
    ? await getStatus("web", { root, artifact: identity.artifactPath, runtimeConfig: identity.runtimeConfigPath })
    : { status: "unknown" };
  const artifactStatus = ["fresh", "stale", "unknown"].includes(provenance?.status) ? provenance.status : "unknown";
  if (artifactStatus !== "fresh" && !options.allowUnverifiedArtifacts) {
    throw new ReviewError("ARTIFACT_UNVERIFIED", "Web artifact freshness is not verified. Build with the review build command, or explicitly use --allow-unverified-artifacts.");
  }
  return { artifactStatus, unverifiedArtifactsAllowed: options.allowUnverifiedArtifacts };
}

function normalizePath(value) {
  const normalized = resolve(value);
  return process.platform === "win32" ? normalized.toLowerCase() : normalized;
}

export async function acquireProfileLock(userDataDir) {
  await mkdir(dirname(userDataDir), { recursive: true });
  // Canonicalize the parent so alternate path spellings/symlinks share the same lock.
  const canonicalProfile = await realpath(userDataDir).catch(() => {
    return realpath(dirname(userDataDir)).then((parent) => resolve(parent, userDataDir.split(/[\\/]/).at(-1)));
  });
  const lockDirectory = `${canonicalProfile}.review-lock`;
  try {
    await mkdir(lockDirectory);
  } catch (error) {
    if (error.code === "EEXIST") {
      throw new ReviewError("PROFILE_LOCKED", "The browser profile has a review lock. Close its owning review process; if it crashed, verify its browser is closed before manually removing the adjacent .review-lock directory. Alternatively select a different --user-data-dir.");
    }
    throw new ReviewError("PROFILE_UNWRITABLE", "Cannot create the browser profile lock. Check permissions or select a writable --user-data-dir.");
  }
  const ownerFile = resolve(lockDirectory, "owner.json");
  const token = randomUUID();
  try {
    await writeFile(ownerFile, JSON.stringify({ token, pid: process.pid, host: hostname(), startedAt: new Date().toISOString() }), { flag: "wx" });
  } catch {
    await rmdir(lockDirectory).catch(() => {});
    throw new ReviewError("PROFILE_UNWRITABLE", "Cannot write the browser profile lock. Check permissions or select a writable --user-data-dir.");
  }
  return async function release() {
    const owner = JSON.parse(await readFile(ownerFile, "utf8"));
    if (owner.token !== token) {
      throw new ReviewError("LOCK_OWNER_CHANGED", "Profile lock ownership changed; the lock was preserved for inspection.");
    }
    await unlink(ownerFile);
    await rmdir(lockDirectory);
  };
}

export async function readCredentials(path) {
  let text;
  try {
    text = await readFile(path, "utf8");
  } catch {
    throw new ReviewError("CREDENTIALS_UNAVAILABLE", "Sign-in is required. Supply a readable --credentials-file with username/password fields, or sign in interactively using this browser profile.");
  }
  const credentials = {};
  for (const line of text.split(/\r?\n/)) {
    const match = /^\s*(username|password)\s*[:=](.*)$/i.exec(line);
    if (match) {
      credentials[match[1].toLowerCase()] = match[2].trim();
    }
  }
  if (!credentials.username || !credentials.password) {
    throw new ReviewError("CREDENTIALS_UNAVAILABLE", "Sign-in requires username and password fields in --credentials-file. Values were not logged.");
  }
  return credentials;
}

export async function reserveOutput(options) {
  const screenshot = await canonicalDestination(options.output);
  const result = screenshot.replace(/\.png$/i, ".json");
  const profile = await canonicalDestination(options.userDataDir);
  const credentials = await canonicalDestination(options.credentialsFile);
  const normalizedProfile = `${normalizePath(profile)}${sep}`;
  if (normalizePath(screenshot) === normalizePath(profile)
    || normalizePath(screenshot).startsWith(normalizedProfile)
    || normalizePath(screenshot) === normalizePath(credentials)
    || normalizePath(result) === normalizePath(credentials)) {
    throw new ReviewError("OUTPUT_CONFLICT", "Output must be separate from browser-profile and credential files.");
  }
  if (await lstat(screenshot).then(() => true, (error) => { if (error.code === "ENOENT") return false; throw error; })) {
    throw new ReviewError("OUTPUT_EXISTS", "Screenshot or JSON output already exists. Choose a new --output path to preserve earlier evidence.");
  }
  await mkdir(dirname(result), { recursive: true });
  try {
    return await open(result, "wx");
  } catch (error) {
    if (error.code === "EEXIST") {
      throw new ReviewError("OUTPUT_EXISTS", "Screenshot or JSON output already exists. Choose a new --output path to preserve earlier evidence.");
    }
    throw error;
  }
}

async function canonicalDestination(path) {
  try {
    return await realpath(path);
  } catch (error) {
    if (error.code !== "ENOENT") throw error;
    const parent = dirname(path);
    if (parent === path) throw error;
    return resolve(await canonicalDestination(parent), relative(parent, path));
  }
}

export function safeFailure(error, phase) {
  if (error instanceof ReviewError) {
    return { code: error.code, message: error.message };
  }
  if (error?.name === "TimeoutError") {
    return { code: "TIMEOUT", message: "The current step did not become ready before its timeout. Check the app state and target selection; increase --timeout-ms only if needed." };
  }
  if (phase === "launch") {
    if (/EACCES|EPERM|access is denied|permission denied/i.test(String(error?.message))) {
      return { code: "BROWSER_PERMISSION", message: "Browser launch was denied. Check the execution permissions for this environment." };
    }
    if (/executable.*(?:doesn't exist|not found)|distribution.*not found|ENOENT/i.test(String(error?.message))) {
      return { code: "BROWSER_MISSING", message: "The selected browser is unavailable. Check --channel; install project-pinned Chromium explicitly if needed." };
    }
    return { code: "BROWSER_LAUNCH_FAILED", message: "Browser launch failed. Check channel availability and whether another browser already owns --user-data-dir." };
  }
  if (["EACCES", "EPERM"].includes(error?.code)) {
    return { code: "FILESYSTEM_PERMISSION", message: "A local file operation was denied. Check profile/output directory permissions." };
  }
  return { code: "REVIEW_FAILED", message: "The review step failed. Inspect the app interactively with the same parameters; raw errors and page contents were omitted to protect credentials." };
}
