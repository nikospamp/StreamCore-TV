import { createReadStream, existsSync, realpathSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, isAbsolute, join, normalize, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
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

export function createReviewServer({
  root = fileURLToPath(new URL("../..", import.meta.url)),
  distribution = process.env.STREAMCORE_WEB_DISTRIBUTION === "development"
    ? "developmentExecutable" : "productionExecutable",
  runtimeConfig = process.env.STREAMCORE_WEB_RUNTIME_CONFIG,
} = {}) {
  const checkout = realpathSync(root);
  if (!["productionExecutable", "developmentExecutable"].includes(distribution)) {
    throw new Error("Unsupported preview distribution.");
  }
  const distributionDirectory = resolve(checkout, "webApp/build/dist/wasmJs", distribution);
  // Optional local preview configuration stays outside the deployable distribution.
  const runtimeConfigPath = runtimeConfig ? resolve(runtimeConfig) : null;
  const identity = JSON.stringify({
    schemaVersion: 2,
    service: "streamcore-review",
    checkout,
    distribution,
    pid: process.pid,
    artifactPath: distributionDirectory,
    runtimeConfigPath,
  });
  if (!existsSync(join(distributionDirectory, "index.html"))) {
    throw new Error(
      `${distribution} distribution is missing. Build it before running browser tests.`,
    );
  }

  return createServer((request, response) => {
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
    // Separate identity endpoint: an SPA fallback returning 200 is not a readiness check.
    if (requestPath === "/__streamcore_review") {
      response.writeHead(200, {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": "no-store",
        "X-Content-Type-Options": "nosniff",
      });
      // Identity is fixed startup metadata. Hashing belongs in the review client so
      // readiness requests never block serving the application on source/artifact IO.
      response.end(identity);
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
  });
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  createReviewServer().listen(Number(process.env.STREAMCORE_WEB_PORT ?? 4173), "127.0.0.1");
}
