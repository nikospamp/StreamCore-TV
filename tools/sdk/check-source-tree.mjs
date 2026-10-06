import {gunzipSync} from 'node:zlib';
// Real repository coverage and browser acceptance for the complete Kotlin tree.
import {createRequire} from 'node:module';
import {fileURLToPath,pathToFileURL} from 'node:url';
import path from 'node:path';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const require=createRequire(path.join(root,'webApp/e2e/package.json'));
const {chromium}=require('playwright');
const output=path.join(root,'build/sdk-explorer');
await fs.mkdir(output,{recursive:true});
const browser=await chromium.launch({channel:'chrome',headless:true});
const page=await browser.newPage({viewport:{width:1600,height:1000},reducedMotion:'reduce'});
page.setDefaultTimeout(15000);
const errors=[],requests=[];
page.on('pageerror',error=>errors.push(error.message));
page.on('request',request=>{if(/^https?:/.test(request.url()))requests.push(request.url());});
await page.route(/^https?:/,route=>route.abort());
try {
  await page.goto(pathToFileURL(path.join(root,'docs/sdk/streamcore-sdk-explorer.html')).href);
  await page.waitForFunction(()=>window.SDK_EXPLORER);
  const data=JSON.parse(gunzipSync(Buffer.from(await page.locator('#explorer-data').textContent(),'base64')).toString('utf8'));
  const nodes=new Map(data.nodes.map(node=>[node.id,node]));
  const byKey=new Map(data.nodes.map(node=>[node.key,node]));
  assert.equal(data.index.formalParameters,data.index.representedParameters);
  assert.equal(data.index.missingParameters.length,0);
  assert.ok(data.index.formalParameters>2000,'Source-wide parameter coverage, not a storage-specific patch');
  for(const callable of data.nodes.filter(node=>node.signatureParameterIds)) {
    for(const id of callable.signatureParameterIds) {
      assert.ok(nodes.has(id),`${callable.symbol} exposes its complete signature`);
      assert.ok(nodes.get(id).isParameter || nodes.get(id).kind==='parameter');
    }
  }
  async function select(key) {
    const node=byKey.get(key);assert.ok(node,`Node ${key}`);
    await page.evaluate(id=>window.SDK_EXPLORER.selectNode(id),node.id);
    assert.equal((await page.evaluate(()=>window.SDK_EXPLORER.readState())).treeRoot,node.id);
    return node;
  }
  function reachable(id,context,direction) {
    const adj=new Map();
    for(const edge of data.edges) {
      if(!edge.contextIds.includes(context) || !edge.traces.includes('value') || edge[direction]===false)continue;
      const from=direction==='upstream'?edge.to:edge.from,to=direction==='upstream'?edge.from:edge.to;
      if(!adj.has(from))adj.set(from,[]);adj.get(from).push(to);
    }
    const found=new Set([id]),pending=[id];
    for(let i=0;i<pending.length;i++)for(const target of adj.get(pending[i]) || [])if(!found.has(target)){found.add(target);pending.push(target);}
    return found;
  }
  const storage=await select('storage.android.factory');
  const parameters=storage.signatureParameterIds.map(id=>nodes.get(id));
  assert.deepEqual(parameters.map(node=>node.label),['context','config','authFileName']);
  const config=parameters.find(node=>node.label==='config');
  assert.match(config.typeName,/StreamCoreConfiguration/);
  assert.equal(await page.locator('.relation-tree-content > .relation-tree-member').filter({has:page.locator('h3').filter({hasText:/^Inputs · 3$/})}).count(),1);
  await page.screenshot({path:path.join(output,'full-tree-storage-inputs.png')});
  await page.evaluate(id=>window.SDK_EXPLORER.selectNode(id),config.id);
  const origins=reachable(config.id,'tmdb-android','upstream');
  assert.ok(origins.has(byKey.get('app.tmdb.config.common').id),'Storage input reaches the real common configuration');
  assert.ok(!origins.has(byKey.get('app.tmdb.connection').id),'Storage config excludes HTTP connection settings');
  const uses=reachable(config.id,'tmdb-android','downstream');
  for(const field of ['persistence','backend','storageNamespace'])assert.ok([...uses].some(id=>nodes.get(id)?.label.endsWith('.'+field)),`Config reaches ${field}`);
  assert.ok(uses.has(byKey.get('storage.android.names').id),'Namespace contributes to filename prefix');
  assert.ok([...uses].some(id=>nodes.get(id)?.label==='files'),'Namespace contributes to canonical files');
  for(const field of ['auth','library','search','progress'])assert.ok(uses.has(byKey.get('storage.platform.'+field).id),`Full tree reaches ${field} store`);
  await page.screenshot({path:path.join(output,'full-tree-storage-config.png')});
  for(const key of ['tmdb.auth.login','clientb.search.search','clientb.profiles.createProfile','runtime.playback.reportEvent']) {
    const node=byKey.get(key);assert.ok(node,key);
    const targetContext=node.contextIds.includes('tmdb-android')?'tmdb-android':'clientb-android';
    await page.locator('#provider').selectOption(targetContext.split('-')[0]);
    await select(key);
    assert.ok(node.signatureParameterIds.length>0,`${key} exposes formerly missing parameters`);
    const inputs=page.locator('.relation-tree-content > .relation-tree-member').filter({has:page.locator('h3').filter({hasText:/^Inputs/})});
    for(const parameter of node.signatureParameterIds.map(id=>nodes.get(id)))assert.ok((await inputs.textContent()).includes(parameter.label));
  }
  await page.locator('#provider').selectOption('tmdb');
  await page.evaluate(id=>window.SDK_EXPLORER.selectNode(id),config.id);
  for(const width of [1440,390,360]) {
    await page.setViewportSize({width,height:900});
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,`Tree fits ${width}`);
  }
  await page.screenshot({path:path.join(output,'full-tree-storage-mobile.png')});
  assert.deepEqual(errors,[]);assert.deepEqual(requests,[]);
  const report={snapshot:data.snapshot,index:{files:data.index.sourceFileCount,parameters:data.index.formalParameters,represented:data.index.representedParameters,unresolvedCalls:data.index.unresolvedCount},checks:'all signature parameters, real storage origins and uses, field precision, auth/search/profile/playback inputs, responsive tree, zero network',errors};
  await fs.writeFile(path.join(output,'source-tree-verification.json'),JSON.stringify(report,null,2));
  console.log(JSON.stringify(report,null,2));
} catch(error) {
  await page.screenshot({path:path.join(output,'source-tree-failure.png')}).catch(()=>{});
  throw error;
} finally {await browser.close();}
