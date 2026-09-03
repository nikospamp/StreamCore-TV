import { createReadStream, existsSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const e2eDirectory = fileURLToPath(new URL(".", import.meta.url));
const distributionVariant = process.env.STREAMCORE_WEB_DISTRIBUTION === "development"
  ? "developmentExecutable"
  : "productionExecutable";
const distributionDirectory = normalize(
  join(e2eDirectory, "..", "build", "dist", "wasmJs", distributionVariant),
);
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
  const requestPath = decodeURIComponent(new URL(request.url ?? "/", "http://localhost").pathname);
  const relativePath = requestPath === "/" ? "index.html" : requestPath.slice(1);
  let candidate = normalize(join(distributionDirectory, relativePath));
  if (!candidate.startsWith(distributionDirectory)) {
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
}).listen(4173, "127.0.0.1");
