import assert from "node:assert/strict";
import { mkdir, mkdtemp, readFile, readdir, rm, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import test from "node:test";
import {
  acquireProfileLock, parseOptions, readCredentials, repositoryRoot,
  requireExpectedOrigin, reserveOutput, safeFailure, validateBaseUrl, verifyReviewServer,
} from "./review-common.mjs";
import { runWebReview } from "./review-web.mjs";
import { createReviewServer } from "./server.mjs";

test("CLI paths resolve from the checkout and ambiguous/invalid arguments fail before launching", () => {
  const options = parseOptions(["--screen", "details", "--asset", "Example Film", "--headless", "--output", "build/review/sample.png"]);
  assert.equal(options.output, resolve(repositoryRoot, "build/review/sample.png"));
  assert.equal(options.credentialsFile, resolve(repositoryRoot, "docs/credentials/tmdb.txt"));
  assert.deepEqual(options.viewport, { width: 1920, height: 1080 });
  assert.equal(options.headless, true);
  for (const args of [
    ["--screen", "details"],
    ["--screen", "profiles", "--unknown"],
    ["--screen", "profiles", "--screen", "home"],
    ["--screen", "profiles", "--viewport", "0x1080"],
    ["--screen", "profiles", "--timeout-ms", "Infinity"],
    ["--screen", "profiles", "--output", "sample.txt"],
    ["--screen", "profiles", "--channel", "firefox"],
  ]) assert.throws(() => parseOptions(args));
});

test("only loopback origins are accepted; paths, credentials and redirected origins are rejected", () => {
  assert.equal(validateBaseUrl("http://localhost:8080/"), "http://localhost:8080");
  assert.equal(validateBaseUrl("http://[::1]:8080"), "http://[::1]:8080");
  for (const url of ["https://example.com", "http://example.com", "http://localhost:8080/profiles", "http://user:secret@localhost:8080", "http://localhost:8080/?token=secret"]) {
    assert.throws(() => validateBaseUrl(url), { code: "INVALID_ORIGIN" });
  }
  assert.throws(() => requireExpectedOrigin("https://example.com/login?token=secret", "http://localhost:8080"), { code: "UNEXPECTED_ORIGIN" });
  assert.doesNotThrow(() => requireExpectedOrigin("http://localhost:8080/login", "http://localhost:8080"));
});

test("profile locks reject competing owners and can be reacquired only after release", async (t) => {
  const directory = await mkdtemp(join(tmpdir(), "streamcore-lock-"));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const profile = join(directory, "browser");
  const release = await acquireProfileLock(profile);
  await assert.rejects(acquireProfileLock(profile), { code: "PROFILE_LOCKED" });
  assert.equal((await readdir(`${profile}.review-lock`)).length, 1);
  await release();
  const nextRelease = await acquireProfileLock(profile);
  await nextRelease();
});

test("changed lock ownership is preserved, never silently removed", async (t) => {
  const directory = await mkdtemp(join(tmpdir(), "streamcore-lock-owner-"));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const profile = join(directory, "browser");
  const release = await acquireProfileLock(profile);
  const ownerPath = join(`${profile}.review-lock`, "owner.json");
  await writeFile(ownerPath, JSON.stringify({ token: "replacement-owner" }));
  await assert.rejects(release(), { code: "LOCK_OWNER_CHANGED" });
  assert.equal(JSON.parse(await readFile(ownerPath, "utf8")).token, "replacement-owner");
});

test("credential parser reads only the needed fields and failures never echo contents", async (t) => {
  const directory = await mkdtemp(join(tmpdir(), "streamcore-credential-fixture-"));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const path = join(directory, "credentials.txt");
  await writeFile(path, "Username: synthetic-user\r\nPassword=synthetic:password=value\r\nOther=ignore-me\r\n");
  assert.deepEqual(await readCredentials(path), { username: "synthetic-user", password: "synthetic:password=value" });
  await writeFile(path, "token=do-not-print\n");
  await assert.rejects(readCredentials(path), (error) => error.code === "CREDENTIALS_UNAVAILABLE" && !error.message.includes("do-not-print"));
});

test("server identity is metadata only and serves pages without source or artifact hashing", async (t) => {
  // A fixture without Git, a manifest, or readable runtime configuration must still
  // serve identity and pages. Provenance is a separate client operation.
  const root = await mkdtemp(join(tmpdir(), "streamcore-review-server-"));
  t.after(() => rm(root, { recursive: true, force: true }));
  const artifactPath = join(root, "webApp/build/dist/wasmJs/productionExecutable");
  await mkdir(artifactPath, { recursive: true });
  await writeFile(join(artifactPath, "index.html"), "<!doctype html><title>Preview fixture</title>");
  const runtimeConfigPath = join(root, "private-config.json");
  const server = createReviewServer({ root, runtimeConfig: runtimeConfigPath });
  await new Promise((done) => server.listen(0, "127.0.0.1", done));
  t.after(() => new Promise((done) => server.close(done)));
  const baseUrl = `http://127.0.0.1:${server.address().port}`;
  const response = await fetch(`${baseUrl}/__streamcore_review`);
  assert.equal(response.headers.get("cache-control"), "no-store");
  const identity = await response.json();
  assert.deepEqual(identity, {
    schemaVersion: 2, service: "streamcore-review", checkout: root,
    distribution: "productionExecutable", pid: process.pid, artifactPath, runtimeConfigPath,
  });
  assert.equal(await (await fetch(`${baseUrl}/profiles`)).text(), "<!doctype html><title>Preview fixture</title>");
  // Even an invalid manifest cannot turn the HTTP endpoint into a hash/error path.
  await mkdir(join(root, "build/review/provenance"), { recursive: true });
  await writeFile(join(root, "build/review/provenance/web.json"), "malformed manifest");
  assert.deepEqual(await (await fetch(`${baseUrl}/__streamcore_review`)).json(), identity);
  await assert.rejects(verifyReviewServer({ baseUrl, timeoutMs: 1000, allowUnverifiedArtifacts: false }, { root }), { code: "ARTIFACT_UNVERIFIED" });
  await writeFile(runtimeConfigPath, JSON.stringify({ fixture: true }));
  const configResponse = await fetch(`${baseUrl}/config.json`);
  assert.equal(configResponse.headers.get("cache-control"), "no-store");
  assert.deepEqual(await configResponse.json(), { fixture: true });
  assert.equal((await fetch(`${baseUrl}/missing.js`)).status, 404);
  assert.deepEqual(await (await fetch(`${baseUrl}/__streamcore_review`)).json(), identity, "identity must not include runtime configuration contents");
});

test("verified paths are mandatory; freshness is checked locally and never trusted from identity", async (t) => {
  const initial = {
    schemaVersion: 2, service: "streamcore-review", checkout: repositoryRoot,
    distribution: "productionExecutable", pid: process.pid,
    artifactPath: resolve(repositoryRoot, "webApp/build/dist/wasmJs/productionExecutable"),
    runtimeConfigPath: resolve(repositoryRoot, "webApp/build/generated/webDevelopmentConfig/config.json"),
  };
  let identity = { ...initial, provenance: { status: "fresh" } };
  let contentType = "application/json";
  const server = createServer((request, response) => {
    response.writeHead(200, { "Content-Type": contentType });
    response.end(contentType === "application/json" ? JSON.stringify(identity) : "<!doctype html><title>SPA fallback</title>");
  });
  await new Promise((done) => server.listen(0, "127.0.0.1", done));
  t.after(() => new Promise((done) => server.close(done)));
  const options = { baseUrl: `http://127.0.0.1:${server.address().port}`, timeoutMs: 1000, allowUnverifiedArtifacts: false };
  let localStatus = "fresh";
  let statusCalls = 0;
  const dependencies = { getStatus: (target, selection) => {
    statusCalls += 1;
    assert.equal(target, "web");
    assert.deepEqual(selection, { root: repositoryRoot, artifact: initial.artifactPath, runtimeConfig: initial.runtimeConfigPath });
    return { status: localStatus };
  } };
  assert.deepEqual(await verifyReviewServer(options, dependencies), { artifactStatus: "fresh", unverifiedArtifactsAllowed: false });
  localStatus = "stale";
  await assert.rejects(verifyReviewServer(options, dependencies), { code: "ARTIFACT_UNVERIFIED" });
  options.allowUnverifiedArtifacts = true;
  assert.equal((await verifyReviewServer(options, dependencies)).artifactStatus, "stale");
  assert.equal(statusCalls, 3);
  for (const invalid of [
    { checkout: tmpdir() }, { service: "other-service" }, { schemaVersion: 1 },
    { distribution: "otherExecutable" }, { artifactPath: tmpdir() },
    { artifactPath: "webApp/build/dist/wasmJs/productionExecutable" },
    { runtimeConfigPath: "relative/config.json" }, { runtimeConfigPath: 42 },
  ]) {
    identity = { ...initial, ...invalid };
    await assert.rejects(verifyReviewServer(options, dependencies), { code: "SERVER_MISMATCH" });
  }
  assert.equal(statusCalls, 3, "invalid identity must fail before local files are hashed");
  identity = { ...initial, runtimeConfigPath: null, provenance: { status: "fresh" } };
  assert.equal((await verifyReviewServer(options, dependencies)).artifactStatus, "unknown");
  identity = { ...initial, distribution: "developmentExecutable", artifactPath: resolve(repositoryRoot, "webApp/build/dist/wasmJs/developmentExecutable"), provenance: { status: "fresh" } };
  assert.equal((await verifyReviewServer(options, dependencies)).artifactStatus, "unknown");
  options.allowUnverifiedArtifacts = false;
  await assert.rejects(verifyReviewServer(options, dependencies), { code: "ARTIFACT_UNVERIFIED" });
  assert.equal(statusCalls, 3, "development distributions and missing runtime selections are not certified as production");
  contentType = "text/html";
  await assert.rejects(verifyReviewServer(options, dependencies), { code: "SERVER_UNVERIFIED" });
});

test("unverified server fails before browser/profile work and emits one redacted result", async (t) => {
  const directory = await mkdtemp(join(tmpdir(), "streamcore-review-failure-"));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const server = createServer((request, response) => { response.writeHead(404).end(); });
  await new Promise((done) => server.listen(0, "127.0.0.1", done));
  t.after(() => new Promise((done) => server.close(done)));
  const output = join(directory, "capture.png");
  const options = parseOptions(["--screen", "profiles", "--base-url", `http://127.0.0.1:${server.address().port}`, "--user-data-dir", join(directory, "browser"), "--output", output]);
  const emissions = [];
  const result = await runWebReview(options, (value) => emissions.push(value));
  assert.equal(result.code, "SERVER_UNVERIFIED");
  assert.equal(emissions.length, 1);
  assert.deepEqual(await readdir(directory), ["capture.json"]);
  assert.equal(JSON.parse(await readFile(join(directory, "capture.json"), "utf8")).success, false);
});

test("browser launch diagnostics distinguish missing executables from permission denial without exposing raw errors", () => {
  assert.equal(safeFailure(new Error("spawn EPERM secret-token"), "launch").code, "BROWSER_PERMISSION");
  assert.equal(safeFailure(new Error("Executable doesn't exist at secret-path"), "launch").code, "BROWSER_MISSING");
  const failure = safeFailure(new Error("page.fill(secret-password) failed with secret-token"), "login");
  assert.equal(failure.code, "REVIEW_FAILED");
  assert.equal(JSON.stringify(failure).includes("secret-password"), false);
  assert.equal(JSON.stringify(failure).includes("secret-token"), false);
});

test("output reservation preserves existing evidence and rejects profile/credential destinations", async (t) => {
  const directory = await mkdtemp(join(tmpdir(), "streamcore-review-output-"));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const options = {
    output: join(directory, "capture.png"),
    userDataDir: join(directory, "browser"),
    credentialsFile: join(directory, "credentials.txt"),
  };
  await writeFile(options.output, "original");
  await assert.rejects(reserveOutput(options), { code: "OUTPUT_EXISTS" });
  assert.equal(await readFile(options.output, "utf8"), "original");
  await rm(options.output);
  const reserved = await reserveOutput(options);
  await assert.rejects(reserveOutput(options), { code: "OUTPUT_EXISTS" });
  await reserved.close();
  await assert.rejects(reserveOutput({ ...options, output: join(options.userDataDir, "capture.png") }), { code: "OUTPUT_CONFLICT" });
  await assert.rejects(reserveOutput({ ...options, output: join(directory, "credentials.png"), credentialsFile: join(directory, "credentials.json") }), { code: "OUTPUT_CONFLICT" });
});
