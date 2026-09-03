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
const shakaPlaybackAdapterPath = requireFile("shaka-playback-adapter.mjs");
if (existsSync(join(distributionDirectory, "shaka-adapter.mjs"))) {
  failures.push("obsolete WEB-01 shaka-adapter.mjs must not be packaged");
}
if (existsSync(join(distributionDirectory, "config.json"))) {
  failures.push("real config.json must not be packaged in the production distribution");
}

const files = listFiles(distributionDirectory);
const jsFiles = files.filter((path) => extname(path) === ".js");
const applicationModuleFiles = files.filter((path) => [".js", ".mjs"].includes(extname(path)));
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

if (existsSync(shakaPlaybackAdapterPath)) {
  const adapter = readFileSync(shakaPlaybackAdapterPath, "utf8");
  const staticPackageImport = /^\s*import\s+\w+\s+from\s+["']shaka-player\/dist\/shaka-player\.compiled\.js["'];/m;
  if (!staticPackageImport.test(adapter)) {
    failures.push("shaka-playback-adapter.mjs does not statically import the pinned module-local Shaka package");
  }
  if (/https?:\/\//i.test(adapter)) {
    failures.push("shaka-playback-adapter.mjs must not load Shaka from a remote origin");
  }
  if (/\b(?:globalThis|window|self)\s*(?:\.\s*shaka|\[\s*["']shaka["']\s*\])/i.test(adapter)) {
    failures.push("shaka-playback-adapter.mjs must not use a global Shaka fallback");
  }
}

const remoteShakaLoadPatterns = [
  /\bimport\s+(?:[^"'`\r\n;]+?\s+from\s+)?["'`](?:https?:)?\/\/[^"'`\r\n]*shaka[^"'`\r\n]*["'`]/i,
  /\bimport\s*\(\s*["'`](?:https?:)?\/\/[^"'`\r\n]*shaka[^"'`\r\n]*["'`]\s*\)/i,
  /\b(?:fetch|importScripts|loadScript|loadModule|require)\s*\(\s*["'`](?:https?:)?\/\/[^"'`\r\n]*shaka[^"'`\r\n]*["'`]/i,
  /\b(?:src|href)\s*=\s*["'`](?:https?:)?\/\/[^"'`\r\n]*shaka[^"'`\r\n]*["'`]/i,
  /\.setAttribute\s*\(\s*["'](?:src|href)["']\s*,\s*["'`](?:https?:)?\/\/[^"'`\r\n]*shaka[^"'`\r\n]*["'`]/i,
  /\bnew\s+(?:SharedWorker|Worker|URL)\s*\(\s*["'`](?:https?:)?\/\/[^"'`\r\n]*shaka[^"'`\r\n]*["'`]/i,
];
const globalShakaFallbackPattern =
  /\b(?:globalThis|window|self)\s*(?:(?:\?\.|\.)\s*shaka\b|(?:\?\.)?\s*\[\s*["']shaka["']\s*\])/i;
for (const path of applicationModuleFiles) {
  const moduleSource = readFileSync(path, "utf8");
  const executableMask = javascriptExecutableMask(moduleSource);
  if (hasExecutableMatch(moduleSource, executableMask, remoteShakaLoadPatterns)) {
    failures.push(`remote/CDN Shaka load found in ${safeRelativePath(path)}`);
  }
  if (hasExecutableMatch(moduleSource, executableMask, [globalShakaFallbackPattern])) {
    failures.push(`global Shaka fallback found in ${safeRelativePath(path)}`);
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

function hasExecutableMatch(source, executableMask, patterns) {
  return patterns.some((pattern) => {
    const flags = pattern.flags.includes("g") ? pattern.flags : `${pattern.flags}g`;
    const matcher = new RegExp(pattern.source, flags);
    for (const match of source.matchAll(matcher)) {
      if (executableMask[match.index] !== " ") {
        return true;
      }
    }
    return false;
  });
}

function javascriptExecutableMask(source) {
  const characters = source.split("");
  let state = "code";
  let escaped = false;
  for (let index = 0; index < characters.length; index += 1) {
    const character = characters[index];
    const next = characters[index + 1];
    if (state === "code") {
      if (character === "'" || character === '"' || character === "`") {
        state = character;
        characters[index] = " ";
      } else if (character === "/" && next === "/") {
        state = "line-comment";
        characters[index] = " ";
        characters[index + 1] = " ";
        index += 1;
      } else if (character === "/" && next === "*") {
        state = "block-comment";
        characters[index] = " ";
        characters[index + 1] = " ";
        index += 1;
      }
      continue;
    }
    if (state === "line-comment") {
      if (character === "\n" || character === "\r") {
        state = "code";
      } else {
        characters[index] = " ";
      }
      continue;
    }
    if (state === "block-comment") {
      characters[index] = " ";
      if (character === "*" && next === "/") {
        characters[index + 1] = " ";
        index += 1;
        state = "code";
      }
      continue;
    }
    characters[index] = " ";
    if (escaped) {
      escaped = false;
    } else if (character === "\\") {
      escaped = true;
    } else if (character === state) {
      state = "code";
    }
  }
  return characters.join("");
}
