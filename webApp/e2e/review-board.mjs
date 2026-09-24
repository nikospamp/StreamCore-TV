import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, realpathSync, statSync, writeFileSync } from "node:fs";
import { dirname, extname, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
const limits = Object.freeze({
  entries: 12,
  sourceBytes: 20 * 1024 * 1024,
  totalBytes: 60 * 1024 * 1024,
  sourcePixels: 24_000_000,
  totalPixels: 64_000_000,
  boardPixels: 32_000_000,
  dimension: 16_384,
});

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function boundedInteger(value, name, minimum, maximum) {
  const number = Number(value);
  assert(Number.isInteger(number) && number >= minimum && number <= maximum,
    `${name} must be an integer from ${minimum} to ${maximum}.`);
  return number;
}

function localPath(value, base, name) {
  assert(typeof value === "string" && value.length > 0, `${name} must be a local file path.`);
  assert(!/^(?:\\\\|\/\/)/.test(value) && !/^[a-z][a-z\d+.-]*:/i.test(value.replace(/^[a-z]:[\\/]/i, "")),
    `${name} must be a local file path, not a URL or network share.`);
  return resolve(base, value);
}

export function parseArguments(args) {
  if (args.length === 1 && args[0] === "--help") return { help: true };
  const options = { columns: 2, cellWidth: 600, channel: "chrome", headless: true };
  const names = new Map([
    ["--manifest", "manifest"], ["--output", "output"], ["--columns", "columns"],
    ["--cell-width", "cellWidth"], ["--channel", "channel"],
  ]);
  const seen = new Set();
  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    assert(!seen.has(argument), `Duplicate option: ${argument}`);
    seen.add(argument);
    if (argument === "--headed" || argument === "--headless") {
      assert(!seen.has(argument === "--headed" ? "--headless" : "--headed"), "Choose either --headed or --headless.");
      options.headless = argument !== "--headed";
      continue;
    }
    assert(names.has(argument), `Unknown option: ${argument}`);
    const value = args[++index];
    assert(value !== undefined && !value.startsWith("--"), `Missing value for ${argument}.`);
    options[names.get(argument)] = value;
  }
  assert(options.manifest, "--manifest is required.");
  options.columns = boundedInteger(options.columns, "columns", 1, 4);
  options.cellWidth = boundedInteger(options.cellWidth, "cell-width", 240, 1200);
  assert(["chrome", "chromium", "msedge"].includes(options.channel), "channel must be chrome, chromium, or msedge.");
  options.manifest = localPath(options.manifest, repositoryRoot, "manifest");
  options.output = localPath(options.output ?? "build/review/board.png", repositoryRoot, "output");
  return options;
}

export function pngDimensions(buffer, maximumPixels = limits.sourcePixels) {
  assert(buffer.length >= 33 && buffer.subarray(0, 8).equals(signature), "Source is not a PNG image.");
  assert(buffer.readUInt32BE(8) === 13 && buffer.toString("ascii", 12, 16) === "IHDR", "PNG has no valid IHDR header.");
  const width = buffer.readUInt32BE(16);
  const height = buffer.readUInt32BE(20);
  assert(width > 0 && height > 0 && width <= limits.dimension && height <= limits.dimension,
    `PNG dimensions must be positive and at most ${limits.dimension}.`);
  assert(width * height <= maximumPixels, `PNG exceeds the ${maximumPixels} pixel limit.`);
  return { width, height };
}

export function prepareBoard(options) {
  const manifestPath = localPath(options.manifest, repositoryRoot, "manifest");
  assert(statSync(manifestPath).size <= 64 * 1024, "Manifest exceeds 64 KiB.");
  const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));
  assert(manifest !== null && typeof manifest === "object" && !Array.isArray(manifest), "Manifest must be an object.");
  const heading = manifest.heading ?? "StreamCoreTV review";
  assert(typeof heading === "string" && heading.length <= 200, "heading must contain at most 200 characters.");
  assert(Array.isArray(manifest.entries) && manifest.entries.length > 0 && manifest.entries.length <= limits.entries,
    `entries must contain 1 to ${limits.entries} screenshots.`);
  const columns = Math.min(boundedInteger(options.columns ?? 2, "columns", 1, 4), manifest.entries.length);
  const cellWidth = boundedInteger(options.cellWidth ?? 600, "cell-width", 240, 1200);
  const output = localPath(options.output ?? "build/review/board.png", repositoryRoot, "output");
  assert(extname(output).toLowerCase() === ".png", "output must end in .png.");
  const sidecar = `${output.slice(0, -4)}.json`;
  assert(!existsSync(output) && !existsSync(sidecar), "Output or sidecar already exists; choose a new output path. Originals are never overwritten.");
  let totalBytes = 0;
  let totalPixels = 0;
  const entries = manifest.entries.map((entry, index) => {
    assert(entry !== null && typeof entry === "object" && !Array.isArray(entry), `Entry ${index + 1} must be an object.`);
    assert(typeof entry.label === "string" && entry.label.trim().length > 0 && entry.label.length <= 120,
      `Entry ${index + 1} label must contain 1 to 120 characters.`);
    const path = localPath(entry.path, dirname(manifestPath), `Entry ${index + 1} path`);
    assert(extname(path).toLowerCase() === ".png", `Entry ${index + 1} must reference a PNG file.`);
    const file = statSync(path);
    assert(file.isFile() && file.size <= limits.sourceBytes, `Entry ${index + 1} must be a file no larger than 20 MiB.`);
    totalBytes += file.size;
    assert(totalBytes <= limits.totalBytes, "Screenshots exceed the 60 MiB combined file-size limit.");
    const buffer = readFileSync(path);
    const dimensions = pngDimensions(buffer);
    totalPixels += dimensions.width * dimensions.height;
    assert(totalPixels <= limits.totalPixels, `Screenshots exceed the ${limits.totalPixels} combined source-pixel limit.`);
    const scale = Math.min(1, cellWidth / dimensions.width, cellWidth * 2 / dimensions.height);
    return {
      label: entry.label,
      path: realpathSync(path),
      sha256: createHash("sha256").update(buffer).digest("hex"),
      ...dimensions,
      displayWidth: Math.max(1, Math.floor(dimensions.width * scale)),
      displayHeight: Math.max(1, Math.floor(dimensions.height * scale)),
      dataUrl: `data:image/png;base64,${buffer.toString("base64")}`,
    };
  });
  const rowHeights = [];
  for (let index = 0; index < entries.length; index += columns) {
    rowHeights.push(Math.max(...entries.slice(index, index + columns).map((entry) => entry.displayHeight)) + 72);
  }
  const width = 48 + columns * cellWidth + (columns - 1) * 24;
  const height = 120 + rowHeights.reduce((total, value) => total + value, 0) + (rowHeights.length - 1) * 24;
  assert(width <= limits.dimension && height <= limits.dimension && width * height <= limits.boardPixels,
    `Board exceeds dimension or ${limits.boardPixels}-pixel limits. Reduce columns/cell-width or split the manifest.`);
  return { heading, manifestPath, output, sidecar, entries, columns, cellWidth, width, height, rowHeights };
}

function escapeHtml(value) {
  return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#39;");
}

export function boardHtml(board) {
  const cards = board.entries.map((entry, index) => {
    const rowHeight = board.rowHeights[Math.floor(index / board.columns)];
    return `<figure style="height:${rowHeight}px"><figcaption>${escapeHtml(entry.label)}</figcaption>` +
      `<p>${entry.width} × ${entry.height} px · displayed ${entry.displayWidth} × ${entry.displayHeight} px</p>` +
      `<div class="capture" style="height:${rowHeight - 72}px"><img alt="${escapeHtml(entry.label)}" ` +
      `width="${entry.displayWidth}" height="${entry.displayHeight}" src="${entry.dataUrl}"></div></figure>`;
  }).join("");
  return `<!doctype html><html><head><meta charset="utf-8">` +
    `<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'">` +
    `<style>*{box-sizing:border-box}html,body{margin:0;width:${board.width}px;height:${board.height}px;overflow:hidden}` +
    `body{padding:24px;background:#11151c;color:#f3f5f8;font-family:Arial,sans-serif}` +
    `header{height:72px}h1{font-size:24px;line-height:28px;margin:0 0 8px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}` +
    `header p{font-size:12px;line-height:16px;color:#c3cbd6;margin:0}` +
    `main{display:grid;grid-template-columns:repeat(${board.columns},${board.cellWidth}px);gap:24px}` +
    `figure{width:${board.cellWidth}px;margin:0;overflow:hidden}` +
    `figcaption{font-size:18px;line-height:24px;height:24px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}` +
    `figure p{height:36px;margin:0;padding-top:5px;font-size:12px;line-height:18px;color:#c3cbd6}` +
    `.capture{display:flex;align-items:flex-start;justify-content:center}img{display:block;flex-shrink:0;object-fit:contain}` +
    `</style></head><body><header><h1>${escapeHtml(board.heading)}</h1>` +
    `<p>Original captures · scaled to fit, never cropped.</p>` +
    `</header><main>${cards}</main></body></html>`;
}

export async function renderBoard(board, options = {}) {
  const { chromium } = await import("playwright");
  let browser;
  try {
    browser = await chromium.launch({
      channel: (options.channel ?? "chrome") === "chromium" ? undefined : (options.channel ?? "chrome"),
      headless: options.headless ?? true,
      timeout: 30_000,
    });
    const context = await browser.newContext({
      viewport: { width: board.width, height: Math.min(board.height, 2000) },
      deviceScaleFactor: 1, reducedMotion: "reduce", serviceWorkers: "block",
    });
    await context.route("**/*", (route) => route.abort());
    const page = await context.newPage();
    page.setDefaultTimeout(30_000);
    await page.setContent(boardHtml(board), { waitUntil: "load", timeout: 30_000 });
    await page.waitForFunction(() => [...document.images].every((image) => image.complete), undefined, { timeout: 30_000 });
    await page.evaluate(async () => {
      await document.fonts.ready;
      await Promise.all([...document.images].map((image) => image.decode()));
    });
    const images = await page.locator("img").evaluateAll((elements) => elements.map((element) => ({
      width: element.naturalWidth, height: element.naturalHeight,
    })));
    assert(images.every((entry, index) => entry.width === board.entries[index].width && entry.height === board.entries[index].height),
      "Decoded image dimensions do not match their PNG headers.");
    const screenshot = await page.screenshot({ fullPage: true, animations: "disabled", timeout: 30_000 });
    const rendered = pngDimensions(screenshot, limits.boardPixels);
    assert(rendered.width === board.width && rendered.height === board.height, "Rendered board dimensions do not match the planned layout.");
    const metadata = {
      schemaVersion: 1,
      heading: board.heading,
      output: board.output,
      composition: "Original PNG captures, contained without cropping; display scaling only. Inputs remain unchanged.",
      layout: { width: board.width, height: board.height, columns: board.columns, cellWidth: board.cellWidth, deviceScaleFactor: 1 },
      entries: board.entries.map(({ dataUrl, ...entry }) => ({
        ...entry,
        scaleX: entry.displayWidth / entry.width,
        scaleY: entry.displayHeight / entry.height,
      })),
    };
    mkdirSync(dirname(board.output), { recursive: true });
    // Exclusive creation also prevents a concurrent process replacing evidence since validation.
    writeFileSync(board.output, screenshot, { flag: "wx" });
    writeFileSync(board.sidecar, `${JSON.stringify(metadata, null, 2)}\n`, { flag: "wx" });
    return { success: true, screenshot: board.output, metadata: board.sidecar, entries: board.entries.length, width: board.width, height: board.height };
  } finally {
    if (browser) await browser.close();
  }
}

async function main() {
  let phase = "arguments";
  try {
    const options = parseArguments(process.argv.slice(2));
    if (options.help) {
      console.log(JSON.stringify({
        success: true,
        usage: "node webApp/e2e/review-board.mjs --manifest <path> [--output <path.png>] [--columns 1..4] [--cell-width 240..1200] [--channel chrome|chromium|msedge] [--headless|--headed]",
        manifest: { heading: "Optional heading", entries: [{ label: "Mobile", path: "mobile.png" }] },
        paths: "Manifest and output CLI paths resolve from the repository; screenshot paths resolve from the manifest directory.",
        defaults: { output: "build/review/board.png", columns: 2, cellWidth: 600, channel: "chrome", headless: true },
        output: "PNG plus a JSON sidecar with original hashes, dimensions and display scaling; existing files are never overwritten.",
      }));
      return;
    }
    phase = "validation";
    const board = prepareBoard(options);
    phase = "render";
    console.log(JSON.stringify(await renderBoard(board, options)));
  } catch (error) {
    console.log(JSON.stringify({ success: false, phase, error: error instanceof Error ? error.message : String(error) }));
    process.exitCode = 1;
  }
}

if (process.argv[1] && pathToFileURL(resolve(process.argv[1])).href === import.meta.url) {
  await main();
}
