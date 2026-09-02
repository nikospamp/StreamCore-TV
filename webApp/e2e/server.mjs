import { createReadStream, existsSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const e2eDirectory = fileURLToPath(new URL(".", import.meta.url));
const distributionDirectory = normalize(
  join(e2eDirectory, "..", "build", "dist", "wasmJs", "productionExecutable"),
);
const mimeTypes = new Map([
  [".html", "text/html; charset=utf-8"],
  [".js", "text/javascript; charset=utf-8"],
  [".mjs", "text/javascript; charset=utf-8"],
  [".wasm", "application/wasm"],
  [".json", "application/json; charset=utf-8"],
  [".png", "image/png"],
]);

if (!existsSync(join(distributionDirectory, "index.html"))) {
  throw new Error(
    "Production distribution is missing. Run :webApp:wasmJsBrowserDistribution first.",
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
    "Cache-Control": "no-store",
  });
  createReadStream(candidate).pipe(response);
}).listen(4173, "127.0.0.1");
