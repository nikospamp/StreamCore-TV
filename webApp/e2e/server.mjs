import { createReadStream, existsSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, isAbsolute, join, normalize, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

const e2eDirectory = fileURLToPath(new URL(".", import.meta.url));
const distributionVariant = process.env.STREAMCORE_WEB_DISTRIBUTION === "development"
  ? "developmentExecutable"
  : "productionExecutable";
const distributionDirectory = normalize(
  join(e2eDirectory, "..", "build", "dist", "wasmJs", distributionVariant),
);
// Optional local preview configuration stays outside the deployable distribution.
const runtimeConfigPath = process.env.STREAMCORE_WEB_RUNTIME_CONFIG;
const port = Number(process.env.STREAMCORE_WEB_PORT ?? 4173);
const mimeTypes = new Map([
  [".html", "text/html; charset=utf-8"],
  [".js", "text/javascript; charset=utf-8"],
  [".mjs", "text/javascript; charset=utf-8"],
  [".wasm", "application/wasm"],
  [".json", "application/json; charset=utf-8"],
  [".xml", "application/xml; charset=utf-8"],
  [".css", "text/css; charset=utf-8"],
  [".svg", "image/svg+xml"],
  [".png", "image/png"],
  [".jpg", "image/jpeg"],
  [".jpeg", "image/jpeg"],
  [".webp", "image/webp"],
  [".woff2", "font/woff2"],
  [".mpd", "application/dash+xml"],
  [".m3u8", "application/vnd.apple.mpegurl"],
  [".mp4", "video/mp4"],
  [".m4s", "video/iso.segment"],
  [".webm", "video/webm"],
  [".vtt", "text/vtt; charset=utf-8"],
]);

if (!existsSync(join(distributionDirectory, "index.html"))) {
  throw new Error(
    `${distributionVariant} distribution is missing. Build it before running browser tests.`,
  );
}

createServer((request, response) => {
  const requestTarget = request.url ?? "/";
  const rawPath = requestTarget.split("?", 1)[0];
  if (rawPath.includes("\\")) {
    response.writeHead(400).end();
    return;
  }
  let requestPath;
  try {
    const encodedPath = new URL(requestTarget, "http://localhost").pathname;
    if (/%(?:2f|5c)/i.test(encodedPath)) {
      response.writeHead(400).end();
      return;
    }
    requestPath = decodeURIComponent(encodedPath);
  } catch (_) {
    response.writeHead(400).end();
    return;
  }
  if (requestPath.includes("\\")) {
    response.writeHead(400).end();
    return;
  }
  const relativePath = requestPath === "/" ? "index.html" : requestPath.slice(1);
  if (requestPath === "/config.json" && runtimeConfigPath && existsSync(runtimeConfigPath)) {
    response.writeHead(200, {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
    });
    createReadStream(runtimeConfigPath).pipe(response);
    return;
  }
  let candidate = normalize(join(distributionDirectory, relativePath));
  const candidateFromRoot = relative(distributionDirectory, candidate);
  if (
    isAbsolute(candidateFromRoot) ||
    candidateFromRoot === ".." ||
    candidateFromRoot.startsWith(`..${sep}`)
  ) {
    response.writeHead(400).end();
    return;
  }
  if (!existsSync(candidate) || statSync(candidate).isDirectory()) {
    if (extname(relativePath) !== "") {
      response.writeHead(404).end();
      return;
    }
    candidate = join(distributionDirectory, "index.html");
  }

  response.writeHead(200, {
    "Content-Type": mimeTypes.get(extname(candidate)) ?? "application/octet-stream",
    "Content-Length": statSync(candidate).size,
    "Cache-Control": "no-store",
  });
  createReadStream(candidate).pipe(response);
}).listen(port, "127.0.0.1");
