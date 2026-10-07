import {gunzipSync} from 'node:zlib';
// Verify the standalone SDK explorer with the existing project Playwright install.
// Run: node tools/sdk/check-explorer.mjs
import {createRequire} from 'node:module';
import {fileURLToPath, pathToFileURL} from 'node:url';
import path from 'node:path';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const require = createRequire(path.join(root, 'webApp/e2e/package.json'));
const {chromium} = require('playwright');
const output = path.join(root, 'build/sdk-explorer');
await fs.mkdir(output, {recursive:true});
const browser = await chromium.launch({channel:'chrome', headless:true});
const context = await browser.newContext({viewport:{width:1600,height:1000}, colorScheme:'light', reducedMotion:'reduce'});
const page = await context.newPage();
page.setDefaultTimeout(10000);
const errors = [], external = [];
page.on('pageerror', error => errors.push(error.message));
page.on('request', request => { if (/^https?:/.test(request.url())) external.push(request.url()); });
await context.route(/^https?:/, route => route.abort());
const url = pathToFileURL(path.join(root, 'build/sdk-docs/streamcore-sdk-explorer.html')).href;
const state = async () => {
  await page.waitForFunction(() => window.SDK_EXPLORER);
  return page.evaluate(() => window.SDK_EXPLORER.readState());
};

try {
  await page.goto(url);
  await page.waitForFunction(() => window.SDK_EXPLORER);
  assert.equal((await state()).surface,'tree','Full source tree is the default');
  await page.locator('#graph-view').click();
  const data = JSON.parse(gunzipSync(Buffer.from(await page.locator('#explorer-data').textContent(),'base64')).toString('utf8'));
  const node = key => { const value = data.nodes.find(n => n.key === key); assert.ok(value, `Mapped node ${key}`); return value; };
  const select = async key => {
    await page.evaluate(id => window.SDK_EXPLORER.selectNode(id), node(key).id);
    assert.equal((await state()).node, node(key).id);
  };
  const setContext = async (provider,platform) => {
    await page.locator('#provider').selectOption(provider);
    await page.locator('#platform').selectOption(platform);
    assert.equal((await state()).context, `${provider}-${platform}`);
  };
  const has = (values,key) => values.includes(node(key).id);
  const settleGraph = () => page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
  const graphSnapshot = async () => {
    await settleGraph();
    return page.evaluate(() => ({
      state:window.SDK_EXPLORER.readState(),
      transforms:Object.fromEntries([...document.querySelectorAll('.graph-node')].map(element => [element.dataset.nodeId,element.getAttribute('transform')])),
    }));
  };
  const assertStationary = async (before,label) => {
    const after = await graphSnapshot();
    assert.deepEqual(after.state.view,before.state.view,`${label}: camera stays exactly unchanged`);
    const common = Object.keys(before.transforms).filter(id => id in after.transforms);
    assert.ok(common.length > 0,`${label}: existing graph nodes remain comparable`);
    for (const id of common) assert.equal(after.transforms[id],before.transforms[id],`${label}: existing position stays unchanged for ${id}`);
    return after;
  };
  const assertSelected = async id => {
    const selection = await page.locator('.graph-node.is-selected').evaluateAll(elements => elements.map(element => ({id:element.dataset.nodeId,pressed:element.getAttribute('aria-pressed')})));
    assert.deepEqual(selection,[{id,pressed:'true'}],'Exactly the requested graph node is highlighted and announced as selected');
    assert.equal((await state()).node,id);
  };
  const visibleGraphCandidate = async kinds => {
    const candidate = await page.evaluate(allowedKinds => {
      const current = window.SDK_EXPLORER.readState();
      const bounds = document.querySelector('#graph').getBoundingClientRect();
      return [...document.querySelectorAll('.graph-node')].map(element => {
        const box = element.querySelector('.node-box').getBoundingClientRect();
        const x = box.left + box.width / 2, y = box.top + box.height / 2;
        const hit = document.elementFromPoint(x,y)?.closest('.graph-node');
        return {id:element.dataset.nodeId,key:element.dataset.key,kind:element.dataset.kind,x,y,
          visible:box.left>bounds.left && box.right<bounds.right && box.top>bounds.top && box.bottom<bounds.bottom && hit===element};
      }).find(candidate => candidate.visible && candidate.id!==current.node && allowedKinds.includes(candidate.kind));
    },kinds);
    assert.ok(candidate,`A different unobscured ${kinds.join('/')} node is available in the graph viewport`);
    return candidate;
  };
  const pointerSelect = async (kinds,label) => {
    const candidate = await visibleGraphCandidate(kinds);
    const before = await graphSnapshot();
    await page.mouse.click(candidate.x,candidate.y);
    const after = await assertStationary(before,label);
    assert.equal(after.state.mode,before.state.mode,`${label}: selecting a node never changes the explicit trace lens`);
    if (before.state.mode!=='auto') assert.equal(after.state.effectiveMode,before.state.mode,`${label}: explicit lens remains effective for every node kind`);
    await assertSelected(candidate.id);
    return candidate;
  };
  assert.equal((await state()).context,'tmdb-android');
  assert.equal((await state()).journey,'construction');
  assert.equal((await state()).mode,'auto');
  assert.equal((await state()).effectiveMode,'value','Auto traces the default HTTP client as a value');
  assert.equal((await state()).direction,'both');
  await page.screenshot({path:path.join(output,'desktop-auto-default.png')});
  await select('tmdb.api.httpClient');
  let current = await state();
  for (const key of ['tmdb.android.client','tmdb.factory.httpClient','tmdb.http.factory','external.okhttp.engine']) {
    assert.ok(has(current.upstream,key), `Android origin includes ${key}`);
  }
  assert.ok(!has(current.upstream,'external.js.engine'),'Android origin excludes browser engine');
  assert.ok(!has(current.upstream,'tmdb.web.client'),'Android origin excludes browser local');
  assert.ok(await page.locator('.graph-node.is-origin').count());
  await page.screenshot({path:path.join(output,'desktop-http-origin.png')});

  // Auto follows functions as calls and values as provenance without moving the graph.
  await page.locator('#fit-graph').click();
  let autoGeometry = await graphSnapshot();
  await page.locator('.graph-node[data-key="tmdb.android.factory"] .node-box').click();
  await assertStationary(autoGeometry,'Auto selection of createAndroid');
  await assertSelected(node('tmdb.android.factory').id);
  current = await state();
  assert.equal(current.mode,'auto');
  assert.equal(current.effectiveMode,'call');
  for (const key of ['storage.android.factory','tmdb.http.factory','tmdb.factory']) {
    assert.ok(has(current.downstream,key),`Auto function tracing includes ${key}`);
    assert.ok(await page.locator(`.graph-node[data-key="${key}"].is-consumer`).count(),`${key} is visibly highlighted`);
  }
  assert.ok(current.downstreamEdges.length>0,'Auto function selection shows a nonempty call path');
  autoGeometry = await graphSnapshot();
  await select('tmdb.api.httpClient');
  await assertStationary(autoGeometry,'Auto selection of the HTTP client property');
  assert.equal((await state()).mode,'auto');
  assert.equal((await state()).effectiveMode,'value');
  assert.ok(has((await state()).upstream,'tmdb.http.factory'));
  await select('session.contract');
  assert.equal((await state()).effectiveMode,'call','Auto treats callable contracts as call paths');
  await select('tmdb.api.httpClient');

  // Type cues use distinct geometry as well as accessible labels and color.
  const shapeExamples = [
    ['function','tmdb.android.factory'],['constructor','tmdb.api'],['parameter','tmdb.factory.httpClient'],
    ['variable','tmdb.android.client'],['property','tmdb.api.httpClient'],['contract','session.contract'],
    ['external','external.ktor.client'],
  ];
  const shapeSignatures = [];
  for (const [shape,key] of shapeExamples) {
    const element = page.locator(`.graph-node[data-key="${key}"]`);
    assert.equal(await element.getAttribute('data-shape'),shape,`${key} has the ${shape} shape`);
    assert.equal(await element.locator('.node-box').count(),1,`${shape} retains one clickable primary outline`);
    const signature = await element.evaluate(node => {
      const outline=node.querySelector('.node-box');
      return JSON.stringify({tag:outline.tagName,rx:outline.getAttribute('rx'),ry:outline.getAttribute('ry'),
        path:outline.getAttribute('d'),points:outline.getAttribute('points'),
        details:[...node.querySelectorAll('.node-detail-line')].map(line=>line.getAttribute('d')),
        dash:getComputedStyle(outline).strokeDasharray});
    });
    shapeSignatures.push(signature);
    const legend = page.locator(`.node-shape-legend [data-shape="${shape}"]`);
    assert.equal(await legend.count(),1,`Legend explains the ${shape} geometry`);
    assert.match(await legend.textContent(),new RegExp(shape,'i'));
  }
  assert.equal(new Set(shapeSignatures).size,shapeExamples.length,'All seven graph types have distinguishable outlines/detail/stroke patterns');

  // Pin a function, then inspect a deep value through a directed, evidence-backed path.
  await select('tmdb.android.factory');
  await page.locator('#pin-function').click();
  assert.equal((await state()).pinnedFunction,node('tmdb.android.factory').id);
  autoGeometry = await graphSnapshot();
  await select('tmdb.api.httpClient');
  await assertStationary(autoGeometry,'Selecting a value while a function is pinned');
  assert.equal((await state()).effectiveMode,'value');
  const expectedPath = ['tmdb.android.factory','tmdb.http.factory','tmdb.android.client','tmdb.factory.httpClient','tmdb.api.httpClient'];
  const pathIds = await page.locator('#connection-path [data-path-node]').evaluateAll(elements=>elements.map(element=>element.dataset.pathNode));
  assert.equal(pathIds[0],node(expectedPath[0]).id,'Connection starts at the pinned function');
  assert.equal(pathIds.at(-1),node(expectedPath.at(-1)).id,'Connection ends at the selected value');
  let pathIndex = -1;
  for (const key of expectedPath) {
    const nextIndex = pathIds.indexOf(node(key).id);
    assert.ok(nextIndex>pathIndex,`Directed connection includes ${key} in source-flow order`);
    pathIndex = nextIndex;
  }
  const pathEvidence = await page.locator('#connection-path [data-evidence]').evaluateAll(elements=>elements.map(element=>element.dataset.evidence));
  assert.ok(pathEvidence.length>=pathIds.length-1,'Every directed step exposes relationship evidence');
  for (const edgeId of pathEvidence) {
    const relation=data.edges.find(edge=>edge.id===edgeId);
    assert.ok(relation,`Pinned path evidence ${edgeId} resolves`);
    assert.ok(relation.contextIds.includes('tmdb-android'),'Pinned path stays within the selected platform/provider');
  }
  const creatorOwners = await page.locator('#value-creators [data-owner]').evaluateAll(elements=>elements.map(element=>element.dataset.owner));
  assert.ok(creatorOwners.length>0 && creatorOwners.every(owner=>owner.trim()),'Value creators are grouped by their source owner');
  assert.ok((await page.locator('#value-creators').textContent()).includes(node('tmdb.http.factory').symbol),'Value creators identify the actual createTmdbHttpClient function');
  const consumerOwners = await page.locator('#value-consumers [data-owner]').evaluateAll(elements=>elements.map(element=>element.dataset.owner));
  assert.ok(consumerOwners.some(owner=>owner.includes('KtorTmdbApi')),'Value consumers are grouped under their owning class');
  for (const key of ['tmdb.ktor.getMovieDetails','tmdb.ktor.createSession']) {
    const consumer = page.locator(`#value-consumers [data-usage-node=${JSON.stringify(node(key).id)}]`);
    assert.equal(await consumer.count(),1,`Consumer summary includes ${key}`);
    assert.ok((await consumer.textContent()).includes(node(key).symbol),'Consumer uses the exact scoped symbol');
    assert.ok(await consumer.locator('[data-source], [data-evidence]').count(),'Consumer exposes source/evidence navigation');
  }
  const creatorEvidence = page.locator('#value-creators [data-source], #value-creators [data-evidence]').first();
  assert.ok(await creatorEvidence.count(),'Creator summary exposes source/evidence navigation');
  await creatorEvidence.click();
  assert.equal(await page.locator('#source-dialog').evaluate(element=>element.open),true);
  assert.ok(await page.locator('#source-code .is-evidence').count());
  await page.keyboard.press('Escape');
  await page.screenshot({path:path.join(output,'function-value-connection.png')});
  assert.equal(new URLSearchParams(new URL(page.url()).hash.slice(1)).get('from'),node('tmdb.android.factory').id);
  await page.reload();
  assert.equal((await state()).pinnedFunction,node('tmdb.android.factory').id,'Pinned function survives URL reload');
  assert.equal((await state()).node,node('tmdb.api.httpClient').id);
  assert.equal((await state()).mode,'auto');
  assert.ok(await page.locator('#connection-path [data-path-node]').count());

  await select('runtime.search.search');
  await page.locator('#pin-function').click();
  await select('tmdb.api.httpClient');
  // The source index can expose additional dependencies through search and
  // shared session operations that were absent from the original curated map.
  const expandedPath=await page.locator('#connection-path [data-path-node]').evaluateAll(elements=>elements.map(element=>element.dataset.pathNode));
  if(expandedPath.length)for(const id of expandedPath)assert.ok(data.nodes.some(node=>node.id===id),'Expanded path uses indexed source nodes');
  else assert.match(await page.locator('#connection-path').textContent(),/no directed.*path|no.*connection/i);
  await page.locator('#clear-function').click();
  assert.ok(!(await state()).pinnedFunction,'Clear function removes the pinned endpoint');
  assert.ok(!new URLSearchParams(new URL(page.url()).hash.slice(1)).get('from'));
  await select('tmdb.android.factory');
  await page.locator('#pin-function').click();
  await setContext('tmdb','web');
  assert.ok(!(await state()).pinnedFunction,'Changing platform clears an incompatible Android function pin');
  await select('tmdb.web.factory');
  await page.locator('#pin-function').click();
  await select('tmdb.api.httpClient');
  const browserPathIds = await page.locator('#connection-path [data-path-node]').evaluateAll(elements=>elements.map(element=>element.dataset.pathNode));
  assert.ok(browserPathIds.includes(node('tmdb.web.client').id),'Browser pin traces the browser-created value');
  assert.ok(!browserPathIds.includes(node('tmdb.android.client').id),'Browser path excludes Android values');
  for (const id of browserPathIds) assert.ok(data.nodes.find(node=>node.id===id).contextIds.includes('tmdb-web'));
  await setContext('clientb','web');
  assert.ok(!(await state()).pinnedFunction,'Changing provider clears the incompatible TMDB pin');
  await setContext('tmdb','android');
  await select('tmdb.api.httpClient');

  // Constructor inputs expose the actual supplied expression and a field-only origin chain.
  await select('runtime.client');
  const configurationInput = page.locator('[data-input-key="runtime.client.configuration"]');
  assert.match(await configurationInput.textContent(),/configuration = config\.common/);
  await page.screenshot({path:path.join(output,'constructor-inputs.png')});
  const beforeOriginTrace = await graphSnapshot();
  await configurationInput.locator('[data-trace-origin]').click();
  await assertStationary(beforeOriginTrace,'Explicit input-origin tracing');
  assert.equal((await state()).node,node('runtime.client.configuration').id);
  assert.equal((await state()).mode,'value');
  assert.equal((await state()).direction,'upstream');
  for (const key of ['tmdb.factory.config.common','tmdb.android.config.common','app.tmdb.config.common']) {
    assert.ok(await page.locator(`.origin-tree [data-origin-step="${key}"]`).count(),`Origin chain includes ${key}`);
    assert.ok(has((await state()).upstream,key));
  }
  for (const key of ['app.tmdb.connection','tmdb.http.config']) assert.ok(!has((await state()).upstream,key),'Common configuration excludes sibling connection settings');
  const originSource = page.locator('.origin-tree [data-origin-step="app.tmdb.config.common"] [data-source]').first();
  await originSource.click();
  assert.match(await page.locator('#source-code').textContent(),/backend = "tmdb-production"/);
  assert.ok(await page.locator('#source-code .is-evidence').count());
  await page.locator('#close-source').click();
  await page.screenshot({path:path.join(output,'configuration-origin.png')});
  await page.locator('#direction').selectOption('both');
  await select('tmdb.api.httpClient');

  // Evidence controls use real source excerpts and line numbers.
  const evidenceId = await page.locator('#node-detail [data-evidence]').first().getAttribute('data-evidence');
  const edge = data.edges.find(e => e.id === evidenceId);
  await page.locator('#node-detail [data-evidence]').first().click();
  assert.equal(await page.locator('#source-dialog').evaluate(el => el.open),true);
  assert.match(await page.locator('#source-path').textContent(),new RegExp(edge.evidence.file.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')));
  assert.ok(await page.locator(`#source-code [data-line="${edge.evidence.startLine}"].is-evidence`).count());
  await page.keyboard.press('Escape');

  // Ordinary selection changes highlighting, never the camera, layout, or chosen trace lens.
  // Fit is explicit setup so pointer events hit real unobscured SVG nodes, not test-only selection hooks.
  await page.locator('#fit-graph').click();
  await pointerSelect(['parameter','value','property'],'Pointer selection of a different value');
  await pointerSelect(['callable'],'Pointer selection of a function while using Value flow');
  let geometry = await graphSnapshot();
  await page.locator('#trace-mode').selectOption('call');
  await assertStationary(geometry,'Explicit Function calls lens change');
  assert.match(await page.locator('#trace-explanation').textContent(),/function calls/i);
  for (const id of (await state()).visibleEdges) assert.ok(data.edges.find(edge=>edge.id===id).traces.includes('call'),'Call view only draws call relationships');
  await pointerSelect(['parameter','value','property'],'Pointer selection of a value while using Function calls');
  const keyboardTarget = await visibleGraphCandidate(['callable']);
  geometry = await graphSnapshot();
  await page.locator(`.graph-node[data-key="${keyboardTarget.key}"]`).focus();
  await assertStationary(geometry,'Focusing a visible SVG node');
  await page.keyboard.press('Enter');
  await assertStationary(geometry,'Keyboard Enter selection');
  assert.equal((await state()).mode,'call','Keyboard selection preserves Function calls');
  await assertSelected(keyboardTarget.id);

  // Inspector connections use the same stable selection behavior, even when the target is offscreen.
  const connection = page.locator('#node-detail .connection-select').first();
  assert.ok(await connection.count(),'Selected function exposes an inspector connection');
  const connectionTarget = await connection.getAttribute('data-select-node');
  geometry = await graphSnapshot();
  await connection.click();
  await assertStationary(geometry,'Inspector connection selection');
  assert.equal((await state()).mode,'call','Inspector connections preserve the chosen lens');
  await assertSelected(connectionTarget);

  // An off-journey selection appends its path without relocating the construction map.
  geometry = await graphSnapshot();
  await select('runtime.playback.reportEvent');
  const expanded = await assertStationary(geometry,'Off-journey API selection');
  assert.equal(expanded.state.journey,'construction');
  assert.equal(expanded.state.mode,'call','Public selection hook preserves the chosen lens');
  assert.ok(expanded.state.visibleNodes.some(id => !geometry.state.visibleNodes.includes(id)),'Off-journey selection adds newly relevant symbols');
  geometry = await graphSnapshot();
  await page.locator('#expand-node').click();
  await assertStationary(geometry,'Expand immediate connections');

  // Repositioning is available through deliberate viewport/layout actions.
  geometry = await graphSnapshot();
  await page.locator('#focus-node').click();
  const centered = await graphSnapshot();
  assert.notDeepEqual(centered.state.view,geometry.state.view,'Center selection deliberately moves the camera to the newly selected node');
  for (const id of Object.keys(geometry.transforms)) {
    if (id in centered.transforms) assert.equal(centered.transforms[id],geometry.transforms[id],'Center selection does not rearrange node positions');
  }
  await page.locator('#arrange-trace').click();
  const arranged = await graphSnapshot();
  assert.equal(arranged.state.node,centered.state.node,'Arrange path preserves the selected symbol');
  assert.equal(arranged.state.mode,centered.state.mode,'Arrange path preserves the trace lens');
  assert.ok(Object.keys(centered.transforms).some(id => id in arranged.transforms && centered.transforms[id]!==arranged.transforms[id]),'Arrange path deliberately recomputes graph positions');

  geometry = await graphSnapshot();
  await page.locator('#trace-mode').selectOption('value');
  await assertStationary(geometry,'Explicit Value flow lens change');
  await select('tmdb.api.httpClient');
  assert.match(await page.locator('#trace-explanation').textContent(),/value flow/i);
  for (const id of (await state()).visibleEdges) assert.ok(data.edges.find(edge=>edge.id===id).traces.includes('value'),'Value view only draws value relationships');
  const argumentEdge = data.edges.find(edge => edge.to===node('tmdb.api.httpClient').id && edge.kind==='passes');
  assert.ok(argumentEdge,'HTTP client has a reviewed constructor argument relationship');
  const argumentSentence = await page.locator(`.connection-sentence[data-relationship="${argumentEdge.id}"]`).first().textContent();
  assert.match(argumentSentence,/httpClient/,'Relationship prose names the actual argument');
  assert.match(argumentSentence,/pass|argument|parameter|suppl|receive|stored/i,'Relationship prose explains what the edge means');
  assert.ok(argumentSentence.trim().length>25,'Relationship explanation is more than a raw edge-kind label');

  geometry = await graphSnapshot();
  await page.locator('#direction').selectOption('upstream');
  await assertStationary(geometry,'Upstream direction change');
  assert.equal((await state()).downstream.length,0);
  geometry = await graphSnapshot();
  await page.locator('#direction').selectOption('downstream');
  await assertStationary(geometry,'Downstream direction change');
  assert.equal((await state()).upstream.length,0);
  geometry = await graphSnapshot();
  await page.locator('#direction').selectOption('both');
  await assertStationary(geometry,'Both directions change');
  const beforeZoom = (await state()).view.scale;
  await page.locator('#zoom-in').click();
  assert.ok((await state()).view.scale > beforeZoom);
  await page.locator('#fit-graph').click();
  assert.ok((await state()).view.scale > 0);
  await page.locator('#focus-node').click();

  // Real SVG keyboard selection, plus browser/selection history round-trip.
  const selected = page.locator('.graph-node.is-selected');
  await selected.focus();
  await page.keyboard.press('Enter');
  assert.equal((await state()).node,node('tmdb.api.httpClient').id);
  await select('tmdb.android.client');
  const selectedUrl = page.url();
  await page.locator('#history-back').click();
  await page.waitForFunction(id => window.SDK_EXPLORER.readState().node === id,node('tmdb.api.httpClient').id);
  await page.locator('#history-forward').click();
  await page.waitForFunction(id => window.SDK_EXPLORER.readState().node === id,node('tmdb.android.client').id);
  await page.reload();
  assert.equal(page.url(),selectedUrl);
  assert.equal((await state()).node,node('tmdb.android.client').id);

  await setContext('tmdb','web');
  await select('tmdb.api.httpClient');
  current = await state();
  assert.ok(has(current.upstream,'external.js.engine'));
  assert.ok(has(current.upstream,'tmdb.web.client'));
  assert.ok(!has(current.upstream,'external.okhttp.engine'));
  assert.ok(!has(current.upstream,'tmdb.android.client'));

  // All supported contexts/journeys render without dangling nodes or cross-context paths.
  for (const sourceContext of data.contexts) {
    await setContext(sourceContext.provider,sourceContext.platform);
    for (const journey of data.journeys) {
      const button = page.locator(`#journeys [data-journey="${journey.id}"]`);
      if (!await button.count()) continue;
      await button.click();
      current = await state();
      assert.equal(current.journey,journey.id);
      assert.ok(current.visibleNodes.length > 0);
      const traced = new Set([...current.upstream,...current.downstream,current.node]);
      for (const id of traced) {
        const n = data.nodes.find(item => item.id === id);
        assert.ok(n.contextIds.includes(sourceContext.id),`${n.key} belongs to ${sourceContext.id}`);
        if (sourceContext.provider === 'clientb') assert.ok(!n.key.startsWith('tmdb.'),'ClientB trace excludes TMDB');
        if (sourceContext.id === 'clientb-web') assert.ok(!n.key.startsWith('app.'),'ClientB web is SDK-only');
      }
    }
  }
  assert.match(await page.locator('#context-note').textContent(),/not wired by the current app/);

  await setContext('tmdb','android');
  await page.locator('#journeys [data-journey="playback"]').click();
  await page.locator('#trace-mode').selectOption('call');
  await select('runtime.playback.createProgressRecorder');
  assert.ok(has((await state()).downstream,'runtime.playback.recorder'),'Recorder construction is traced');
  await page.locator('#trace-mode').selectOption('value');
  await select('runtime.playback.recorder.captured');
  assert.ok((await state()).upstream.length > 0,'Captured authorization has an origin');
  await page.locator('#show-values').uncheck();
  assert.ok((await state()).visibleNodes.includes(node('runtime.playback.recorder.captured').id),'Selected value remains visible');
  await page.locator('#expand-node').click();
  await page.locator('#show-values').check();

  await page.locator('#search').fill('StreamCoreContent');
  assert.ok(await page.locator('#file-results [data-source]').count(),'Search includes SDK source outside mapped nodes');
  await page.locator('#file-results [data-source]').first().click();
  assert.equal(await page.locator('#source-dialog').evaluate(el => el.open),true);
  await page.locator('#source-find').fill('definitely_no_such_source_symbol');
  assert.match(await page.locator('#source-status').textContent(),/No matches/);
  await page.keyboard.press('Escape');
  assert.equal(await page.locator('#source-dialog').evaluate(el => el.open),false);
  await page.locator('#search').fill('definitely_no_such_sdk_symbol');
  assert.match(await page.locator('#search-count').textContent(),/^0 symbols/);
  await page.locator('#search').fill('');
  await page.locator('#journeys [data-journey="construction"]').click();
  await select('tmdb.api.httpClient');

  await page.locator('.snapshot-details summary').click();
  await page.locator('#show-coverage').click();
  assert.equal(await page.locator('#coverage-dialog').evaluate(el => el.open),true);
  assert.match(await page.locator('#coverage-content').textContent(),/restoreSession/);
  await page.keyboard.press('Escape');
  await page.locator('.snapshot-details summary').click();
  await page.locator('#theme').selectOption('dark');
  assert.equal(await page.locator('html').getAttribute('data-theme'),'dark');
  await page.screenshot({path:path.join(output,'desktop-dark.png')});
  await page.locator('#theme').selectOption('light');
  for (const width of [1440,1024,390,360]) {
    await page.setViewportSize({width,height:width<700?844:1000});
    await page.locator('#focus-node').click();
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth>innerWidth+1),false,`No overflow at ${width}`);
    if (width<700) {
      await page.locator('#open-navigator').click();
      await page.locator('#search').fill('httpClient');
      assert.ok(await page.locator('#symbol-results [data-select-node]').count());
      await page.locator('[data-close-pane="navigator"]').click();
      await page.locator('#open-inspector').click();
      assert.equal(await page.locator('#open-inspector').getAttribute('aria-expanded'),'true');
      await page.screenshot({path:path.join(output,`mobile-inspector-${width}.png`)});
      await page.locator('[data-close-pane="inspector"]').click();
      assert.equal(await page.locator('#open-inspector').getAttribute('aria-expanded'),'false');
      await page.screenshot({path:path.join(output,`mobile-graph-${width}.png`)});
    }
  }
  await page.setViewportSize({width:1440,height:1000});
  await page.locator('.guide-link').click();
  await page.locator('.sidebar-tools a[href="streamcore-sdk-explorer.html"]').click();
  await page.waitForFunction(() => window.SDK_EXPLORER);
  assert.equal((await state()).context,'tmdb-android','Guide links back to the offline explorer');
  assert.deepEqual(errors,[],'No browser JavaScript errors');
  assert.deepEqual(external,[],'Offline explorer makes no external requests');
  const result = {snapshot:data.snapshot,nodes:data.nodes.length,relationships:data.edges.length,publicOperations:data.coverage.length,contexts:data.contexts.length,
    journeys:data.journeys.length,sourceFiles:data.files.length,viewports:[1600,1440,1024,390,360],errors,externalRequests:external.length,
    checks:'Auto function/value tracing, seven distinct type geometries, pinned directed function/value path with owner-grouped creators/consumers and source evidence, pin URL/clear/context boundaries, HTTP provenance, platform/provider isolation, evidence, stable camera/layout for pointer/keyboard/connection/API selection, explicit trace lens, stable direction changes, deliberate center/arrange, relationship prose, zoom, history/reload, all journeys, recorder capture, source search, coverage, themes, responsive panes'};
  await fs.writeFile(path.join(output,'verification.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify(result,null,2));
} catch (error) {
  await page.screenshot({path:path.join(output,'failure.png')}).catch(() => {});
  const diagnostics = await page.evaluate(() => ({search:document.querySelector('#search')?.value,
    navigationHidden:document.querySelector('#journey-navigation')?.hidden,panes:[...document.querySelectorAll('.is-open')].map(e=>e.id),
    node:window.SDK_EXPLORER?.readState().node,errors:document.querySelectorAll('dialog[open]').length})).catch(() => ({}));
  console.error(JSON.stringify(diagnostics,null,2));
  throw error;
} finally {
  await browser.close();
}
