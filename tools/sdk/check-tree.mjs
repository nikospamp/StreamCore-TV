import {gunzipSync} from 'node:zlib';
// Verify the generic source tree and, by default, its generated explorer integration.
// Run: node tools/sdk/check-tree.mjs [--skip-real]
import {createRequire} from 'node:module';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {createHash} from 'node:crypto';
import path from 'node:path';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const require = createRequire(path.join(root, 'webApp/e2e/package.json'));
const {chromium} = require('playwright');
const output = path.join(root, 'build/sdk-explorer');
const [script, css] = await Promise.all([
  fs.readFile(path.join(root, 'docs/sdk/explorer-content/tree.js'), 'utf8'),
  fs.readFile(path.join(root, 'docs/sdk/explorer-content/explorer.css'), 'utf8'),
]);
await fs.mkdir(output, {recursive:true});
const browser = await chromium.launch({channel:'chrome', headless:true});
const context = await browser.newContext({viewport:{width:1440,height:1000},colorScheme:'light',reducedMotion:'reduce'});
const page = await context.newPage();
page.setDefaultTimeout(15000);
const errors = [];
const external = [];
const checks = [];
const screenshots = [];
page.on('pageerror', error => errors.push(error.message));
page.on('request', request => { if(/^https?:/.test(request.url()))external.push(request.url()); });
await context.route(/^https?:/, route => route.abort());
const report = {componentSha256:createHash('sha256').update(script).digest('hex'),checks,screenshots,errors,externalRequests:external,success:false};
const fixturePath = 'sdk/example/src/commonMain/kotlin/example/Example.kt';
const fixtureLines = Array.from({length:180},(_,index)=>`// example source line ${index+1}`);
fixtureLines.splice(7,3,'val created: HttpClient = createHttpClient(', '    configuration = configuration,', ')');
fixtureLines.splice(24,2,'fun consume(client: HttpClient) {','    client.get("movie/details")');
const fixtureFiles = [{path:fixturePath,name:'Example.kt',module:':sdk:example',sourceSet:'commonMain',test:false,binary:false,source:fixtureLines.join('\n'),lines:fixtureLines.length}];
const makeNode = (id,kind='value',extra={}) => ({id,key:id,label:id,symbol:id,kind,contextIds:['android'],journeys:['fixture'],column:0,file:fixturePath,startLine:1,endLine:1,...extra});
const makeEdge = (id,from,to,kind='derives',extra={}) => ({id,from,to,kind,contextIds:['android'],evidence:{file:fixturePath,startLine:8,endLine:10},...extra});

const signatureNodes = [
  makeNode('factory','callable',{label:'createAndroid',returnType:'StreamCoreClient'}),
  makeNode('factory.context','parameter',{label:'context',ownerId:'factory',typeName:'android.content.Context'}),
  makeNode('factory.configuration','parameter',{label:'configuration',ownerId:'factory',typeName:'TmdbSdkConfiguration'}),
  makeNode('factory.httpClient','parameter',{label:'httpClient',ownerId:'factory',typeName:'HttpClient'}),
  makeNode('factory.local','value',{label:'client',ownerId:'factory',typeName:'HttpClient'}),
];
const signature = {nodes:signatureNodes,edges:[],files:fixtureFiles};
const values = {
  nodes:[...signatureNodes,
    makeNode('producer','callable',{label:'createHttpClient',returnType:'HttpClient'}),
    makeNode('consumer','callable',{label:'KtorTmdbApi.getMovieDetails',returnType:'MovieDetailsDto',startLine:25,endLine:26}),
  ],
  edges:[makeEdge('produce-client','producer','factory.httpClient','produces'),makeEdge('read-client','factory.httpClient','consumer','reads',{evidence:{file:fixturePath,startLine:25,endLine:26}})],
  files:fixtureFiles,
};

async function check(name, callback) {
  const started = Date.now();
  try {
    await callback();
    checks.push({name,passed:true,durationMs:Date.now()-started});
  } catch(error) {
    checks.push({name,passed:false,durationMs:Date.now()-started,error:error.message});
    throw error;
  }
}

async function mount(data,rootId,options={}) {
  await page.setContent('<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Source tree verification</title></head><body><main id="tree-test-host" aria-label="Tree test host"></main></body></html>');
  await page.addStyleTag({content:css});
  await page.addStyleTag({content:'html,body{margin:0;min-width:0}#tree-test-host{height:100dvh;min-width:0;max-width:100%;overflow:auto}'});
  await page.addScriptTag({content:script});
  await page.evaluate(({data,rootId,options})=>{
    window.treeCalls={selected:[],sources:[]};
    window.treeFixture=data;
    window.treeInstance=window.SDK_RELATION_TREE.create({...data,
      onSelectNode:id=>window.treeCalls.selected.push(id),
      onOpenSource:evidence=>window.treeCalls.sources.push(evidence),
    });
    document.querySelector('#tree-test-host').append(window.treeInstance.render(rootId,'android',options));
  },{data,rootId,options});
}

async function expandAll() {
  await page.locator('[data-tree-expand="all"]').click();
  await page.waitForFunction(()=>document.querySelector('.relation-tree')?.getAttribute('aria-busy')==='false' && document.querySelector('.relation-tree-status')?.textContent.includes('no depth limit'));
}

async function screenshot(name) {
  await page.screenshot({path:path.join(output,name)});
  screenshots.push(name);
}

try {
  await check('Signature inputs and exact types are present without any edges',async()=>{
    await mount(signature,'factory');
    const actual=await page.locator('.relation-tree-content > .relation-tree-member').first().locator(':scope > .relation-tree-list > li > .relation-tree-branch').evaluateAll(branches=>branches.map(branch=>({
      id:branch.dataset.treeNode,
      text:branch.querySelector(':scope > summary').textContent,
      open:branch.open,
    })));
    assert.deepEqual(actual.map(item=>item.id),['factory.context','factory.configuration','factory.httpClient']);
    for(const [index,type] of ['android.content.Context','TmdbSdkConfiguration','HttpClient'].entries()) {
      assert.ok(actual[index].text.includes(type),`Signature input ${index+1} includes its exact declared type`);
      assert.equal(actual[index].open,true,'Immediate signature parameters are initially expanded');
    }
    assert.match(await page.locator('.relation-tree-heading').innerText(),/returns StreamCoreClient/);
    assert.equal(await page.locator('[data-tree-node="factory.local"]').evaluate(node=>node.open),false,'Locals stay lazy on first render');
    await screenshot('tree-signature-1440.png');
  });

  await check('Signature IDs include constructor property parameters without duplicating inputs',async()=>{
    const data={nodes:[
      makeNode('class','callable',{label:'Client',signatureParameterIds:['constructor.limit','constructor.token','constructor.limit']}),
      makeNode('constructor','callable',{label:'Client constructor',ownerId:'class',isConstructor:true}),
      makeNode('constructor.limit','parameter',{label:'limit',ownerId:'constructor',typeName:'Int'}),
      makeNode('constructor.token','property',{label:'token',ownerId:'constructor',typeName:'String?',isParameter:true}),
      makeNode('class.throwable','parameter',{label:'throwable',ownerId:'class',typeName:'Throwable',isParameter:true}),
      makeNode('emptySignature','callable',{label:'No signature inputs',signatureParameterIds:[]}),
      makeNode('emptySignature.catchValue','parameter',{label:'catchValue',ownerId:'emptySignature',typeName:'Throwable'}),
    ],edges:[],files:fixtureFiles};
    await mount(data,'class');
    const inputs=page.locator('.relation-tree-content > .relation-tree-member').first();
    const actual=await inputs.locator(':scope > .relation-tree-list > li > .relation-tree-branch').evaluateAll(items=>items.map(item=>({id:item.dataset.treeNode,text:item.querySelector(':scope > summary').textContent})));
    assert.deepEqual(actual.map(item=>item.id),['constructor.limit','constructor.token']);
    assert.match(actual[0].text,/Int/);
    assert.match(actual[1].text,/String\?/);
    assert.match(await inputs.locator(':scope > h3').innerText(),/Inputs · 2/);
    assert.equal(await inputs.locator(':scope > .relation-tree-list > li > [data-tree-node="class.throwable"]').count(),0,'An owned catch parameter does not override the authoritative signature');
    assert.equal(await page.locator('.relation-tree-content > .relation-tree-member').last().locator(':scope > .relation-tree-list > li > [data-tree-node="class.throwable"]').count(),1,'Non-signature owned parameters remain discoverable as local declarations');
    await page.evaluate(()=>window.treeInstance.render('constructor','android'));
    const constructorInputs=page.locator('.relation-tree-content > .relation-tree-member').first();
    assert.match(await constructorInputs.locator(':scope > h3').innerText(),/Inputs · 2/,'Owned isParameter properties also appear without explicit signature IDs');
    await page.evaluate(()=>window.treeInstance.render('emptySignature','android'));
    const emptyTitles=await page.locator('.relation-tree-content > .relation-tree-member > h3').allTextContents();
    assert.ok(emptyTitles.every(title=>!title.startsWith('Inputs')),'An explicit empty signature does not infer inputs from an owned catch parameter');
  });

  await check('A parameter exposes its creator and consumer in separate directions',async()=>{
    await mount(values,'factory.httpClient');
    const upstream=page.locator('.relation-tree-content > .relation-tree-upstream');
    const downstream=page.locator('.relation-tree-content > .relation-tree-downstream');
    assert.equal(await upstream.locator(':scope > .relation-tree-list > li > [data-tree-node="producer"]').count(),1);
    assert.equal(await downstream.locator(':scope > .relation-tree-list > li > [data-tree-node="consumer"]').count(),1);
    assert.match(await upstream.innerText(),/Value produced by/);
    assert.match(await downstream.innerText(),/Read by/);
    await page.locator('.relation-tree-content > .relation-tree-downstream [data-tree-node="consumer"] > summary [data-tree-select="consumer"]').click();
    assert.deepEqual(await page.evaluate(()=>window.treeCalls.selected),['consumer']);
    assert.equal(await page.locator('.relation-tree-heading h2').innerText(),'httpClient','Selecting a name does not replace the tree root');
  });

  await check('Source evidence supplies the exact usage text and source range callback',async()=>{
    const evidence=page.locator('.relation-tree-content > .relation-tree-upstream .relation-tree-evidence').first();
    assert.equal(await evidence.locator('.relation-tree-usage-line').first().innerText(),fixtureLines.slice(7,10).join('\n'));
    await evidence.locator('.relation-tree-source').first().click();
    assert.deepEqual(await page.evaluate(()=>window.treeCalls.sources),[{file:fixturePath,startLine:8,endLine:10}]);
  });

  await check('One-level expansion opens only the visible frontier',async()=>{
    await mount(values,'factory.httpClient',{initialDepth:0});
    assert.equal(await page.locator('.relation-tree-branch[open]').count(),0);
    await page.locator('[data-tree-expand="level"]').click();
    const openDepths=await page.locator('.relation-tree-branch[open]').evaluateAll(items=>items.map(item=>Number(item.dataset.treeDepth)));
    assert.ok(openDepths.length>=2);
    assert.ok(openDepths.every(depth=>depth===1),'Only the immediate frontier is opened');
  });

  const chainLength=141;
  const chain={
    nodes:Array.from({length:chainLength},(_,index)=>makeNode(`chain.${index}`,'value',{label:`value${index}`,typeName:`example.Value<Depth${index}>`})),
    edges:Array.from({length:chainLength-1},(_,index)=>makeEdge(`chain-edge.${index}`,`chain.${index}`,`chain.${index+1}`)),
    files:fixtureFiles,
  };
  await check('All reachable expansion follows a 140-hop chain without a 36-node or depth cap',async()=>{
    await mount(chain,'chain.0',{initialDepth:0});
    await expandAll();
    const depths=await page.locator('.relation-tree-branch').evaluateAll(items=>items.map(item=>Number(item.dataset.treeDepth)));
    const ids=await page.locator('.relation-tree-branch').evaluateAll(items=>items.map(item=>item.dataset.treeNode));
    assert.equal(new Set(ids).size,chainLength-1);
    assert.ok(Math.max(...depths)>=140,'The final node remains reachable beyond 100 levels');
    assert.equal(await page.locator('[data-tree-node="chain.140"]').evaluate(node=>node.open),true);
  });

  await check('Same-root selection preserves expanded DOM identity, scroll and keyboard focus',async()=>{
    const retained=await page.evaluate(()=>{
      const host=document.querySelector('#tree-test-host');
      const tree=window.treeInstance.element;
      const branch=tree.querySelector('[data-tree-node="chain.140"]');
      const focus=tree.querySelector('[data-tree-expand="level"]');
      focus.focus({preventScroll:true});
      host.scrollTop=650;
      const scroll=host.scrollTop;
      window.treeInstance.render('chain.0','android',{selectedNodeId:'chain.70'});
      return {sameTree:tree===window.treeInstance.element,sameBranch:branch===tree.querySelector('[data-tree-node="chain.140"]'),open:branch.open,scrollBefore:scroll,scrollAfter:host.scrollTop,focus:document.activeElement===focus,
        selected:[...tree.querySelectorAll('[data-tree-select="chain.70"]')].every(button=>button.getAttribute('aria-pressed')==='true')};
    });
    assert.equal(retained.sameTree,true);
    assert.equal(retained.sameBranch,true);
    assert.equal(retained.open,true);
    assert.ok(retained.scrollBefore>0);
    assert.equal(retained.scrollAfter,retained.scrollBefore);
    assert.equal(retained.focus,true);
    assert.equal(retained.selected,true);
  });

  await check('Deep and ordinary trees do not cause horizontal document overflow at 360 or 1440 pixels',async()=>{
    for(const width of [360,1440]) {
      await page.setViewportSize({width,height:900});
      assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,`Deep tree fits ${width}px`);
      assert.equal(await page.evaluate(()=>document.querySelector('#tree-test-host').scrollWidth>document.querySelector('#tree-test-host').clientWidth+1),false,`Deep tree host fits ${width}px`);
    }
    await mount(values,'factory.httpClient');
    for(const width of [360,1440]) {
      await page.setViewportSize({width,height:900});
      assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,`Value tree fits ${width}px`);
      await screenshot(`tree-value-${width}.png`);
    }
  });

  await check('Cycles terminate with back-references and shared nodes expand only once',async()=>{
    const data={nodes:['a','b','c','shared'].map(id=>makeNode(id)),edges:[makeEdge('ab','a','b'),makeEdge('bc','b','c'),makeEdge('ca','c','a'),makeEdge('as','a','shared'),makeEdge('bs','b','shared')],files:fixtureFiles};
    await mount(data,'a',{initialDepth:0});
    await expandAll();
    const ids=await page.locator('.relation-tree-branch').evaluateAll(items=>items.map(item=>item.dataset.treeNode));
    assert.equal(ids.length,new Set(ids).size,'A reachable symbol has only one expanded occurrence');
    assert.deepEqual([...new Set(ids)].sort(),['b','c','shared']);
    assert.ok(await page.locator('[data-tree-reference="a"]').count(),'Cycle points back to the root');
    assert.ok(await page.locator('[data-tree-reference="shared"]').count(),'Diamond paths retain shared references');
    assert.match(await page.locator('.relation-tree-content').innerText(),/Back-reference|Shared reference/);
  });

  await check('One-way property projections do not absorb the parent or sibling values upstream',async()=>{
    const data={nodes:[makeNode('configuration'),makeNode('configuration.backend','property',{ownerId:'configuration',label:'backend',typeName:'String'}),makeNode('configuration.token','property',{ownerId:'configuration',label:'token',typeName:'String'})],
      edges:[makeEdge('backend-projection','configuration','configuration.backend','reads',{projection:true,upstream:false,downstream:true}),makeEdge('token-projection','configuration','configuration.token','reads',{projection:true,upstream:false,downstream:true})],files:fixtureFiles};
    await mount(data,'configuration.backend');
    const upstream=page.locator('.relation-tree-content > .relation-tree-upstream');
    assert.equal(await upstream.locator('[data-tree-node="configuration"]').count(),0);
    assert.equal(await upstream.locator('[data-tree-select="configuration.token"]').count(),0);
    assert.match(await upstream.innerText(),/Where it comes from · 0/);
    await page.evaluate(()=>window.treeInstance.render('configuration','android'));
    const owned=page.locator('.relation-tree-content > .relation-tree-downstream');
    assert.equal(await owned.locator(':scope > .relation-tree-list > li > [data-tree-node="configuration.backend"]').count(),1,'The parent still exposes its downstream field projection');
    assert.equal(await owned.locator(':scope > .relation-tree-list > li > [data-tree-node="configuration.token"]').count(),1);
  });

  await check('Unresolved and indexed boundaries are explicit and never called definitive sources',async()=>{
    const data={nodes:[makeNode('unresolved','external',{label:'unresolvedExpression',resolution:'unresolved',origin:'source-index'}),makeNode('indexed','value',{label:'knownValue',origin:'source-index'})],edges:[],files:fixtureFiles};
    await mount(data,'unresolved');
    assert.equal(await page.locator('.relation-tree-heading .relation-tree-resolution').innerText(),'Unresolved');
    assert.match(await page.locator('.relation-tree-heading .relation-tree-boundary').innerText(),/could not determine/);
    assert.match(await page.locator('.relation-tree-content > .relation-tree-upstream').innerText(),/does not establish the original source/);
    await page.evaluate(()=>window.treeInstance.render('indexed','android'));
    assert.equal(await page.locator('.relation-tree-heading .relation-tree-indexed').innerText(),'Source indexed');
  });

  await check('Collapse and reroot cancel in-flight expansion without stale asynchronous writes',async()=>{
    const size=650;
    const data={nodes:[makeNode('hub'),...Array.from({length:size},(_,index)=>makeNode(`leaf.${index}`))],edges:Array.from({length:size},(_,index)=>makeEdge(`hub.${index}`,'hub',`leaf.${index}`)),files:fixtureFiles};
    await mount(data,'hub',{initialDepth:0});
    const cancelled=await page.evaluate(()=>{
      document.querySelector('[data-tree-expand="all"]').click();
      const wasBusy=document.querySelector('.relation-tree').getAttribute('aria-busy')==='true';
      document.querySelector('[data-tree-collapse="all"]').click();
      return {wasBusy,busy:document.querySelector('.relation-tree').getAttribute('aria-busy'),open:document.querySelectorAll('.relation-tree-branch[open]').length};
    });
    assert.equal(cancelled.wasBusy,true,'Fixture reaches an asynchronous expansion boundary');
    assert.equal(cancelled.busy,'false');
    assert.equal(cancelled.open,0);
    await page.evaluate(()=>new Promise(resolve=>setTimeout(resolve,60)));
    assert.equal(await page.locator('.relation-tree-branch[open]').count(),0);
    assert.match(await page.locator('.relation-tree-status').innerText(),/All branches collapsed/);
    await page.evaluate(()=>{
      document.querySelector('[data-tree-expand="all"]').click();
      window.treeInstance.render('leaf.649','android',{initialDepth:0});
    });
    await page.evaluate(()=>new Promise(resolve=>setTimeout(resolve,60)));
    assert.equal(await page.locator('.relation-tree-heading h2').innerText(),'leaf.649');
    assert.equal(await page.locator('.relation-tree').getAttribute('aria-busy'),'false');
    assert.doesNotMatch(await page.locator('.relation-tree-status').innerText(),/Expanding|no depth limit/);
  });

  if(!process.argv.includes('--skip-real')) {
    await check('Generated explorer defaults to Full tree and can reroot to createAndroid',async()=>{
      const url=pathToFileURL(path.join(root,'docs/sdk/streamcore-sdk-explorer.html')).href;
      await page.goto(url);
      await page.waitForFunction(()=>window.SDK_EXPLORER && window.SDK_RELATION_TREE);
      assert.equal(await page.locator('#tree-view').getAttribute('aria-pressed'),'true');
      assert.equal(await page.locator('#tree-viewport .relation-tree').isVisible(),true);
      const data=JSON.parse(gunzipSync(Buffer.from(await page.locator('#explorer-data').textContent(),'base64')).toString('utf8'));
      const factory=data.nodes.find(node=>node.key==='tmdb.android.factory');
      assert.ok(factory,'The real snapshot includes the Android provider factory');
      await page.evaluate(id=>window.SDK_EXPLORER.selectNode(id),factory.id);
      if(await page.locator('#tree-root-selected').isVisible())await page.locator('#tree-root-selected').click();
      assert.match(await page.locator('#tree-viewport .relation-tree-heading h2').innerText(),/createAndroid/);
      const directGroups=await page.locator('#tree-viewport .relation-tree-content > .relation-tree-member').evaluateAll(groups=>groups.map(group=>({title:group.querySelector(':scope > h3').textContent,text:group.textContent})));
      const inputs=directGroups.find(group=>group.title.startsWith('Inputs'));
      assert.ok(inputs,'The real function has a complete signature Inputs group');
      assert.match(inputs.text,/context/);
      assert.match(inputs.text,/config/);
      await screenshot('tree-real-createAndroid.png');
      report.realSnapshot=data.snapshot;
    });
  } else checks.push({name:'Generated explorer smoke',skipped:true,reason:'--skip-real'});

  assert.deepEqual(errors,[],'No browser JavaScript errors');
  assert.deepEqual(external,[],'Tree verification makes no external requests');
  report.success=true;
  console.log(JSON.stringify({success:true,checks:checks.length,screenshots,realSnapshot:report.realSnapshot || null},null,2));
} catch(error) {
  report.failure={message:error.message,stack:error.stack};
  await page.screenshot({path:path.join(output,'tree-failure.png')}).catch(()=>{});
  screenshots.push('tree-failure.png');
  throw error;
} finally {
  await fs.writeFile(path.join(output,'tree-verification.json'),JSON.stringify(report,null,2));
  await browser.close();
}
