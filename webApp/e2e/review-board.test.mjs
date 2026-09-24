import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { deflateSync } from "node:zlib";
import test from "node:test";
import { boardHtml, parseArguments, pngDimensions, prepareBoard, renderBoard } from "./review-board.mjs";

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const testRoot = join(repositoryRoot, "build/review/tests");
mkdirSync(testRoot, { recursive: true });

function pngChunk(type, data) {
  const chunk = Buffer.alloc(data.length + 12);
  chunk.writeUInt32BE(data.length, 0);
  chunk.write(type, 4, "ascii");
  data.copy(chunk, 8);
  let crc = 0xffffffff;
  for (const byte of chunk.subarray(4, -4)) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
  }
  chunk.writeUInt32BE((crc ^ 0xffffffff) >>> 0, chunk.length - 4);
  return chunk;
}

function png(width, height) {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header[8] = 8;
  header[9] = 6;
  const pixels = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const index = y * (width * 4 + 1) + 1 + x * 4;
      pixels[index] = x === 0 || x === width - 1 ? 255 : 20;
      pixels[index + 1] = y === 0 || y === height - 1 ? 255 : 100;
      pixels[index + 2] = 170;
      pixels[index + 3] = 255;
    }
  }
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), pngChunk("IHDR", header),
    pngChunk("IDAT", deflateSync(pixels)), pngChunk("IEND", Buffer.alloc(0)),
  ]);
}

function fixture(entries = [{ label: "Mobile", path: "mobile.png" }, { label: "TV", path: "tv.png" }]) {
  const directory = mkdtempSync(join(testRoot, "board-"));
  writeFileSync(join(directory, "mobile.png"), png(300, 900));
  writeFileSync(join(directory, "tv.png"), png(640, 360));
  const manifest = join(directory, "manifest.json");
  writeFileSync(manifest, JSON.stringify({ heading: "Review", entries }));
  return { directory, options: { manifest, output: join(directory, "board.png"), cellWidth: 240, columns: 2 } };
}

test("CLI paths are repository relative and options are bounded", () => {
  assert.deepEqual(parseArguments(["--help"]), { help: true });
  const options = parseArguments(["--manifest", "build/review/manifest.json"]);
  assert.equal(options.manifest, join(repositoryRoot, "build/review/manifest.json"));
  assert.equal(options.output, join(repositoryRoot, "build/review/board.png"));
  assert.equal(options.headless, true);
  assert.throws(() => parseArguments([]), /manifest is required/);
  assert.throws(() => parseArguments(["--manifest", "a", "--columns", "5"]), /columns must/);
  assert.throws(() => parseArguments(["--manifest", "a", "--cell-width", "10000"]), /cell-width must/);
  assert.throws(() => parseArguments(["--manifest", "a", "--channel", "unknown"]), /channel must/);
  assert.throws(() => parseArguments(["--manifest", "a", "--headless", "--headed"]), /Choose either/);
});

test("manifest paths resolve relative to the manifest; original bytes and dimensions remain intact", () => {
  const { directory, options } = fixture();
  const original = readFileSync(join(directory, "mobile.png"));
  const board = prepareBoard(options);
  assert.equal(board.entries[0].width, 300);
  assert.equal(board.entries[0].height, 900);
  assert.equal(board.entries[0].displayWidth, 160);
  assert.equal(board.entries[0].displayHeight, 480);
  assert.equal(board.entries[1].displayWidth, 240);
  assert.equal(board.entries[1].displayHeight, 135);
  assert.equal(board.entries[0].sha256, createHash("sha256").update(original).digest("hex"));
  assert.deepEqual(readFileSync(join(directory, "mobile.png")), original);
  assert.equal(board.height, 120 + 480 + 72);
});

test("untrusted headings and labels cannot insert markup or fetch remote content", () => {
  const { options } = fixture([{ label: '<img src="https://invalid.example/">', path: "mobile.png" }]);
  const manifest = JSON.parse(readFileSync(options.manifest, "utf8"));
  manifest.heading = "<script>alert(1)</script>";
  writeFileSync(options.manifest, JSON.stringify(manifest));
  const html = boardHtml(prepareBoard(options));
  assert.ok(html.includes("&lt;script&gt;alert(1)&lt;/script&gt;"));
  assert.ok(!html.includes("<script>"));
  assert.ok(!html.includes('<img src="https://'));
  assert.ok(html.includes("default-src 'none'; img-src data:"));
  assert.equal((html.match(/<img /g) ?? []).length, 1);
});

test("network sources, existing outputs and source overwrite are rejected", () => {
  const { options } = fixture([{ label: "Remote", path: "https://invalid.example/image.png" }]);
  assert.throws(() => prepareBoard(options), /local file path/);
  const local = fixture();
  assert.throws(() => prepareBoard({ ...local.options, output: join(local.directory, "mobile.png") }), /never overwritten/);
  writeFileSync(join(local.directory, "board.json"), "{}");
  assert.throws(() => prepareBoard(local.options), /sidecar already exists/);
});

test("invalid and excessive PNG dimensions fail before decoding or browser launch", () => {
  assert.throws(() => pngDimensions(Buffer.alloc(33)), /not a PNG/);
  const header = png(1, 1);
  header.writeUInt32BE(20000, 16);
  assert.throws(() => pngDimensions(header), /dimensions must/);
  header.writeUInt32BE(16000, 16);
  header.writeUInt32BE(16000, 20);
  assert.throws(() => pngDimensions(header), /pixel limit/);
  const { options } = fixture(Array.from({ length: 13 }, () => ({ label: "TV", path: "tv.png" })));
  assert.throws(() => prepareBoard(options), /1 to 12 screenshots/);
});

test("small captures are not enlarged", () => {
  const { directory, options } = fixture([{ label: "Small", path: "small.png" }]);
  writeFileSync(join(directory, "small.png"), png(20, 30));
  const board = prepareBoard(options);
  assert.equal(board.entries[0].displayWidth, 20);
  assert.equal(board.entries[0].displayHeight, 30);
});

test("real browser rendering preserves originals and records accurate board dimensions", {
  skip: process.env.STREAMCORE_REVIEW_BROWSER_TEST !== "1" ? "Set STREAMCORE_REVIEW_BROWSER_TEST=1 for the installed-browser integration check." : false,
}, async () => {
  const { directory, options } = fixture();
  const original = readFileSync(join(directory, "mobile.png"));
  const board = prepareBoard(options);
  const result = await renderBoard(board, { channel: process.env.STREAMCORE_REVIEW_BROWSER_CHANNEL ?? "chrome" });
  assert.equal(result.success, true);
  assert.deepEqual(pngDimensions(readFileSync(result.screenshot)), { width: board.width, height: board.height });
  assert.deepEqual(readFileSync(join(directory, "mobile.png")), original);
  const metadata = JSON.parse(readFileSync(result.metadata, "utf8"));
  assert.equal(metadata.entries[0].sha256, createHash("sha256").update(original).digest("hex"));
  assert.equal(metadata.entries[0].scaleX, 160 / 300);
  assert.equal(metadata.entries[0].scaleY, 480 / 900);
  assert.equal(metadata.entries[0].dataUrl, undefined);
});
