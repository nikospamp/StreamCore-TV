import {
  existsSync,
  readdirSync,
  readFileSync,
  statSync,
} from "node:fs";
import { extname, isAbsolute, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

const e2eDirectory = fileURLToPath(new URL(".", import.meta.url));
const webAppDirectory = resolve(e2eDirectory, "..");
const distributionDirectory = resolve(
  process.env.STREAMCORE_WEB_RELEASE_DIRECTORY ??
    join(webAppDirectory, "build", "dist", "wasmJs", "productionExecutable"),
);
const displayDirectory = process.env.STREAMCORE_WEB_RELEASE_DIRECTORY === undefined
  ? "webApp/build/dist/wasmJs/productionExecutable"
  : "configured production distribution";

const failures = [];
const requireFile = (relativePath) => {
  const path = join(distributionDirectory, relativePath);
  if (!existsSync(path) || !statSync(path).isFile()) {
    failures.push(`missing file: ${relativePath}`);
  } else if (statSync(path).size === 0) {
    failures.push(`empty file: ${relativePath}`);
  }
  return path;
};

if (!existsSync(distributionDirectory) || !statSync(distributionDirectory).isDirectory()) {
  throw new Error(`${displayDirectory} is missing; build :webApp:wasmJsBrowserDistribution first.`);
}

const indexPath = requireFile("index.html");
requireFile("streamcore-web.js");
const configExamplePath = requireFile("config.example.json");
const shakaAdapterPath = requireFile("shaka-adapter.mjs");
if (existsSync(join(distributionDirectory, "config.json"))) {
  failures.push("real config.json must not be packaged in the production distribution");
}

const files = listFiles(distributionDirectory);
const jsFiles = files.filter((path) => extname(path) === ".js");
const wasmFiles = files.filter((path) => extname(path) === ".wasm");
const composeAssets = files.filter((path) => {
  return relative(distributionDirectory, path).split(sep)[0] === "composeResources";
});
if (jsFiles.length === 0) failures.push("no JavaScript application bundle found");
if (wasmFiles.length === 0) failures.push("no Wasm binary found");
if (composeAssets.length === 0) failures.push("composeResources contains no packaged assets");

if (existsSync(indexPath)) {
  const index = readFileSync(indexPath, "utf8");
  const scriptReferences = [...index.matchAll(/<script\b[^>]*\bsrc=["']([^"']+)["']/gi)]
    .map((match) => match[1]);
  if (scriptReferences.length === 0) {
    failures.push("index.html contains no script source");
  }
  for (const reference of scriptReferences) {
    if (/^(?:[a-z][a-z0-9+.-]*:)?\/\//i.test(reference) || /^[a-z][a-z0-9+.-]*:/i.test(reference)) {
      failures.push("index.html must not load an application script from another origin");
      continue;
    }
    let pathname;
    try {
      pathname = decodeURIComponent(reference.split(/[?#]/, 1)[0])
        .replaceAll("\\", "/")
        .replace(/^\/+/, "");
    } catch {
      failures.push("index.html contains a malformed encoded script source");
      continue;
    }
    const scriptPath = resolve(distributionDirectory, pathname);
    const relativeScriptPath = relative(distributionDirectory, scriptPath);
    const escapesDistribution = relativeScriptPath === ".." ||
      relativeScriptPath.startsWith(`..${sep}`) ||
      isAbsolute(relativeScriptPath);
    if (escapesDistribution) {
      failures.push("index.html script source escapes the production distribution");
      continue;
    }
    if (!existsSync(scriptPath) || !statSync(scriptPath).isFile()) {
      failures.push(`index.html references missing script: ${pathname}`);
    }
  }
}

if (existsSync(configExamplePath)) {
  try {
    const example = JSON.parse(readFileSync(configExamplePath, "utf8"));
    const exactKeys = Object.keys(example).sort();
    const expectedKeys = ["tmdbAccountId", "tmdbBaseUrl", "tmdbReadAccessToken"].sort();
    if (JSON.stringify(exactKeys) !== JSON.stringify(expectedKeys)) {
      failures.push("config.example.json must contain only the documented runtime keys");
    }
    if (typeof example.tmdbBaseUrl !== "string" || !example.tmdbBaseUrl.startsWith("https://")) {
      failures.push("config.example.json tmdbBaseUrl must be HTTPS");
    }
    for (const key of ["tmdbReadAccessToken", "tmdbAccountId"]) {
      if (typeof example[key] !== "string" || !/^replace-with-[a-z-]+$/.test(example[key])) {
        failures.push(`config.example.json ${key} must remain an explicit placeholder`);
      }
    }
  } catch {
    failures.push("config.example.json is not valid JSON");
  }
}

if (existsSync(shakaAdapterPath)) {
  const adapter = readFileSync(shakaAdapterPath, "utf8");
  if (!adapter.includes("shaka-player/dist/shaka-player.compiled.js")) {
    failures.push("shaka-adapter.mjs does not use the pinned module-local Shaka package adapter");
  }
  if (/https?:\/\//i.test(adapter)) {
    failures.push("shaka-adapter.mjs must not load Shaka from a remote origin");
  }
}

const hashedWasmName = /^[0-9a-f]{20,64}\.wasm$/;
const executableText = jsFiles
  .map((path) => readFileSync(path, "utf8"))
  .join("\n");
for (const wasmPath of wasmFiles) {
  const name = relative(distributionDirectory, wasmPath).replaceAll("\\", "/");
  if (!hashedWasmName.test(name)) {
    failures.push(`Wasm asset is not content-versioned: ${name}`);
  }
  if (!executableText.includes(name)) {
    failures.push(`JavaScript bundle does not reference packaged Wasm asset: ${name}`);
  }
}

const scannedArtifactFiles = files.filter((path) => {
  const extension = extname(path);
  return [".html", ".js", ".mjs", ".json", ".map", ".wasm"].includes(extension);
});
const forbiddenPatterns = [
  {
    label: "raw JWT/read token",
    pattern: /(?:^|[^A-Za-z0-9_-])eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}(?=$|[^A-Za-z0-9_-])/i,
  },
  {
    label: "authorization bearer value",
    pattern: /(?:^|[^A-Za-z0-9_-])bearer\s+[A-Za-z0-9._~+/=-]{16,}(?=$|[^A-Za-z0-9._~+/=-])/i,
  },
  {
    label: "TMDB session value",
    pattern: /(?:^|[^A-Za-z0-9_])session_id(?:%3[dD]|=|["']?\s*:\s*["'])[A-Za-z0-9._~-]{8,}(?=$|[^A-Za-z0-9._~-])/i,
  },
  {
    label: "embedded TMDB read token config value",
    pattern: /["']?tmdbReadAccessToken["']?\s*[:=]\s*["'](?!replace-with-)[^"'\s]{8,}["']/i,
  },
  {
    label: "embedded account config value",
    pattern: /["']?tmdbAccountId["']?\s*[:=]\s*(?:["'](?!replace-with-)[^"']+["']|\d{2,})/i,
  },
];
for (const path of scannedArtifactFiles) {
  const content = readFileSync(path).toString(extname(path) === ".wasm" ? "latin1" : "utf8");
  for (const forbidden of forbiddenPatterns) {
    if (forbidden.pattern.test(content)) {
      failures.push(`${forbidden.label} found in ${safeRelativePath(path)}`);
    }
  }
}

if (failures.length > 0) {
  for (const failure of failures) process.stderr.write(`FAIL: ${failure}\n`);
  process.exitCode = 1;
} else {
  process.stdout.write(
    `PASS: ${displayDirectory}; html=1 js=${jsFiles.length} wasm=${wasmFiles.length} ` +
      `assets=${composeAssets.length} configExample=1 realConfig=0\n`,
  );
}

function listFiles(directory) {
  const discovered = [];
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      discovered.push(...listFiles(path));
    } else if (entry.isFile()) {
      discovered.push(path);
    }
  }
  return discovered;
}

function safeRelativePath(path) {
  return relative(distributionDirectory, path).replaceAll("\\", "/");
}
