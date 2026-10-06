// Local documentation UI check. Uses the project's existing Playwright install.
// Run: node tools/sdk/check-guide.mjs
import {createRequire} from 'node:module';
import {fileURLToPath, pathToFileURL} from 'node:url';
import path from 'node:path';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const require=createRequire(path.join(root,'webApp/e2e/package.json'));
const {chromium}=require('playwright');
const output=path.join(root,'build/sdk-guide');
await fs.mkdir(output,{recursive:true});
const browser=await chromium.launch({channel:'chrome',headless:true});
const context=await browser.newContext({viewport:{width:1440,height:1000},colorScheme:'light',reducedMotion:'reduce'});
const page=await context.newPage();
const errors=[];
const external=[];
page.on('pageerror',error=>errors.push(error.message));
page.on('request',request=>{if(/^https?:/.test(request.url())) external.push(request.url());});
const url=pathToFileURL(path.join(root,'docs/sdk/streamcore-sdk-guide.html')).href;
try {
  await page.goto(url);
  const data=await page.locator('#guide-data').textContent().then(JSON.parse);
  const overflows=[];
  for(const width of [1440,390,360]) {
    await page.setViewportSize({width,height:900});
    for(const chapter of data.chapters) {
      await page.goto(`${url}#${chapter.id}`);
      await page.locator('main h1').waitFor();
      assert.equal(await page.locator('main h1').innerText(),chapter.title);
      const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth+1);
      if(overflow) overflows.push(`${chapter.id}@${width}`);
      if(width===1440) {
        if(chapter.quiz) {
          await page.locator(`[data-quiz="${chapter.quiz.correct}"]`).click();
          assert.match(await page.locator('#quiz-feedback').textContent(),/^Correct/);
        }
        const references=page.locator('main [data-source]');
        if(await references.count()) {
          await references.first().click();
          assert.equal(await page.locator('#source-dialog').evaluate(node=>node.open),true);
          assert.ok((await page.locator('#code-reader').textContent()).length>0);
          await page.keyboard.press('Escape');
        }
      }
    }
  }
  assert.deepEqual(overflows,[],'No document-width overflow');
  await page.setViewportSize({width:1440,height:1000});
  await page.goto(`${url}#modules`);
  for(const module of data.interactions.modules) {
    await page.locator(`[data-module="${module.id}"]`).click();
    assert.equal(await page.locator('.module-detail h3').textContent(),module.name);
  }
  await page.locator('[data-module="runtime"]').click();
  await page.screenshot({path:path.join(output,'desktop-modules.png')});

  await page.goto(`${url}#workbench`);
  for(const journey of data.interactions.journeys) {
    await page.locator('#journey-select').selectOption(journey.id);
    for(let i=0;i<journey.steps.length;i++) {
      await page.locator(`[data-step="${i}"]`).click();
      assert.equal(await page.locator('.trace-detail h3').textContent(),journey.steps[i].title);
      await page.locator('.trace-detail .source-ref').click();
      assert.ok(await page.locator('.code-line.match').count(),`${journey.id}/${i} source symbol`);
      await page.keyboard.press('Escape');
    }
  }
  await page.locator('#journey-select').selectOption('details');
  await page.locator('[data-step="1"]').click();
  await page.screenshot({path:path.join(output,'desktop-walkthrough.png')});

  await page.locator('[data-tool="session"]').click();
  for(const action of ['restoreSession','A','Adult','start','logout','B','Adult','finish']) await page.locator(`[data-session="${action}"]`).click();
  assert.match(await page.locator('.event-log').textContent(),/StaleSession/);
  for(const action of ['start','Adult','finish']) await page.locator(`[data-session="${action}"]`).click();
  assert.match(await page.locator('.event-log').textContent(),/StaleActivation/);
  await page.locator('[data-session="close"]').click();
  assert.equal(await page.locator('[data-session="A"]').isDisabled(),true);

  await page.locator('[data-tool="progress"]').click();
  await page.locator('#progress-send').click();
  assert.match(await page.locator('#progress-output').textContent(),/30s \/ 600s/);
  await page.locator('#progress-position').fill('35');
  await page.locator('#progress-send').click();
  assert.match(await page.locator('#progress-log').textContent(),/bucket has not advanced/);
  await page.locator('#progress-event').selectOption('Checkpoint');
  await page.locator('#progress-send').click();
  assert.match(await page.locator('#progress-output').textContent(),/35s \/ 600s/);
  await page.locator('#progress-duration').fill('0');
  await page.locator('#progress-send').click();
  assert.match(await page.locator('#progress-output').textContent(),/35s \/ 600s/);
  await page.locator('#progress-event').selectOption('Direct');
  await page.locator('#progress-send').click();
  assert.match(await page.locator('#progress-output').textContent(),/No entry/);
  await page.locator('#progress-event').selectOption('Periodic');
  await page.locator('#progress-duration').fill('600');
  await page.locator('#progress-position').fill('40');
  await page.locator('#progress-failure').check();
  await page.locator('#progress-send').click();
  await page.locator('#progress-failure').uncheck();
  await page.locator('#progress-send').click();
  assert.match(await page.locator('#progress-output').textContent(),/No entry/);
  assert.match(await page.locator('#progress-output').textContent(),/4 \(40s boundary\)/);
  await page.locator('#progress-event').selectOption('Checkpoint');
  await page.locator('#progress-position').fill('570');
  await page.locator('#progress-send').click();
  assert.match(await page.locator('#progress-output').textContent(),/No entry/);

  await page.locator('[data-tool="history"]').click();
  await page.locator('#history-send').click();
  assert.match(await page.locator('#history-list').textContent(),/No history/);
  await page.locator('#history-interaction').selectOption('Submitted');
  await page.locator('#history-send').click();
  assert.equal(await page.locator('#history-list li').first().textContent(),'Star Wars');
  await page.locator('#history-query').fill('STAR WARS');
  await page.locator('#history-send').click();
  assert.equal(await page.locator('#history-list li').count(),1);
  assert.equal(await page.locator('#history-list li').textContent(),'STAR WARS');
  for(const query of ['Dune','Alien','Sintel','Arrival','Severance']) {
    await page.locator('#history-query').fill(query);await page.locator('#history-send').click();
  }
  assert.equal(await page.locator('#history-list li').count(),5);
  await page.locator('#history-storage').selectOption('fail');
  await page.locator('#history-query').fill('Foundation');
  await page.locator('#history-send').click();
  assert.match(await page.locator('#history-outcome').textContent(),/Catalogue results still succeed/);

  await page.goto(`${url}#source`);
  await page.locator('#file-query').fill('RuntimeSession');
  assert.ok((await page.locator('.file-row').count())>=1);
  await page.locator('.file-row strong').filter({hasText:/^RuntimeSession\.kt$/}).click();
  await page.locator('#source-find').fill('withAuthorizedProfile');
  assert.ok(await page.locator('.code-line.match').count());
  await page.screenshot({path:path.join(output,'source-reader.png')});
  await page.keyboard.press('Escape');
  await page.locator('#file-query').fill('definitely-no-such-file');
  assert.match(await page.locator('#file-results').textContent(),/No matching files/);
  await page.locator('main h1').click();
  await page.keyboard.press('/');
  await page.locator('#global-search').fill('AuthProvider');
  assert.ok(await page.locator('.search-result').count());
  await page.keyboard.press('Escape');

  await page.goto(`${url}#session`);
  await page.locator('#mark-read').check();
  await page.reload();
  assert.equal(await page.locator('#mark-read').isChecked(),true);
  await page.locator('#theme').click();
  assert.equal(await page.locator('html').getAttribute('data-theme'),'dark');
  await page.screenshot({path:path.join(output,'desktop-dark.png')});
  await page.setViewportSize({width:390,height:844});
  await page.screenshot({path:path.join(output,'mobile-lesson-dark.png')});
  await page.locator('#theme').click();
  await page.goto(`${url}#overview`);
  await page.screenshot({path:path.join(output,'mobile-overview.png')});
  await page.locator('#menu').click();
  assert.equal(await page.locator('#menu').getAttribute('aria-expanded'),'true');
  await page.locator('#chapters a[href="#playback"]').click();
  await page.waitForFunction(()=>document.getElementById('menu').getAttribute('aria-expanded')==='false');
  assert.equal(await page.locator('#menu').getAttribute('aria-expanded'),'false');
  assert.equal(await page.locator('main h1').textContent(),data.chapters.find(chapter=>chapter.id==='playback').title);
  await page.goto(`${url}#workbench`);
  await page.locator('[data-tool="traces"]').click();
  await page.screenshot({path:path.join(output,'mobile-walkthrough.png')});
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth+1),false);
  assert.deepEqual(errors,[],'No browser JavaScript errors');
  assert.deepEqual(external,[],'Guide works without external network requests');
  const result={chapters:data.chapters.length,journeys:data.interactions.journeys.length,files:data.files.length,viewports:[1440,390,360],errors,externalRequests:external.length,checks:'navigation, quizzes, module map, walkthrough source anchors, session isolation, playback cadence/failure/threshold, history normalization/retention/failure, source search, global search, read progress, themes, mobile navigation'};
  await fs.writeFile(path.join(output,'verification.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify(result,null,2));
} finally {await browser.close();}
