(() => {
  'use strict';
  const DATA = JSON.parse(document.getElementById('guide-data').textContent);
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const escape = value => String(value).replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
  const plain = html => { const node = document.createElement('div'); node.innerHTML = html; return node.textContent; };
  const storageKey = 'streamcore-maintainer-guide-v1';
  let preferences = {};
  try { preferences = JSON.parse(localStorage.getItem(storageKey) || '{}') || {}; } catch (_) { /* Reading works with storage disabled. */ }
  let read = new Set(Array.isArray(preferences.read) ? preferences.read : []);
  const quizAnswers = {};
  const textByChapter = DATA.chapters.map(chapter => ({...chapter, text:plain(chapter.body)}));
  let currentRoute = '';
  let currentSource = null;
  let sourceMatch = -1;
  let workbenchTab = 'traces';
  let journeyId = 'construction';
  let journeyStep = 0;
  let explorerLimit = 60;
  let toastTimeout;
  const persist = () => { try { localStorage.setItem(storageKey, JSON.stringify({...preferences,read:[...read]})); } catch (_) { /* Optional local preferences. */ } };
  const notify = message => { $('#toast').textContent = message; $('#toast').hidden = false; clearTimeout(toastTimeout); toastTimeout = setTimeout(() => { $('#toast').hidden = true; }, 2800); };
  const sourceRef = (file, label = file, symbol = '') => `<button type="button" class="source-ref" data-source="${escape(file)}" data-symbol="${escape(symbol)}">${escape(label)}</button>`;
  const resolveFile = name => DATA.files.find(file => file.path === name) || DATA.files.find(file => file.name === name);
  const theme = () => preferences.theme || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  const applyTheme = () => { document.documentElement.dataset.theme = theme(); $('#theme').textContent = theme() === 'dark' ? 'Light' : 'Dark'; $('#theme').setAttribute('aria-label', `Switch to ${theme() === 'dark' ? 'light' : 'dark'} theme`); };
  applyTheme();
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => { if (!preferences.theme) applyTheme(); });
  $('#theme').addEventListener('click', () => { preferences.theme = theme() === 'dark' ? 'light' : 'dark'; persist(); applyTheme(); });
  $('#menu').addEventListener('click', () => { const opened = $('#sidebar').classList.toggle('open'); $('#menu').setAttribute('aria-expanded', String(opened)); });

  function updateNavigation() {
    $('#chapters').innerHTML = DATA.chapters.map((chapter,index) => `<a href="#${chapter.id}" ${currentRoute === chapter.id ? 'aria-current="page"' : ''}><span class="chapter-index">${read.has(chapter.id) ? '✓' : String(index+1).padStart(2,'0')}</span><span>${escape(chapter.title)}</span></a>`).join('');
    const count = DATA.chapters.filter(chapter => read.has(chapter.id)).length;
    $('#read-progress').textContent = `${count} of ${DATA.chapters.length} lessons marked read`;
    $$('.sidebar-tools a').forEach(link => { if (link.hash === `#${currentRoute}`) link.setAttribute('aria-current','page'); else link.removeAttribute('aria-current'); });
  }
  function header(kicker,title,summary,metadata='') {
    return `<header class="article-header"><p class="eyebrow">${escape(kicker)}</p><h1>${escape(title)}</h1><p class="lead">${escape(summary)}</p>${metadata ? `<div class="meta">${metadata}</div>` : ''}</header>`;
  }
  function renderChapter(chapter) {
    const index = DATA.chapters.indexOf(chapter);
    const minutes = Math.max(3, Math.ceil(plain(chapter.body).split(/\s+/).length / 180));
    const q = chapter.quiz;
    const quiz = q ? `<section class="quiz" aria-labelledby="quiz-title"><p class="eyebrow">Check your understanding</p><h3 id="quiz-title">${escape(q.question)}</h3><div class="quiz-options">${q.options.map((option,i) => `<button type="button" data-quiz="${i}" aria-pressed="${quizAnswers[chapter.id] === i}">${escape(option)}</button>`).join('')}</div><p class="quiz-feedback" id="quiz-feedback" aria-live="polite"></p></section>` : '';
    $('#main').innerHTML = `<article class="article">${header(chapter.kicker,chapter.title,chapter.summary,`<span>${minutes} min reading + exercises</span><span>Working-tree snapshot: ${escape(DATA.snapshot.date)}</span>`)}<div class="lesson-content">${chapter.body}</div>${quiz}<footer class="chapter-footer"><label class="read-check"><input type="checkbox" id="mark-read" ${read.has(chapter.id) ? 'checked' : ''}> I've read this lesson</label><div class="footer-nav">${index ? `<a href="#${DATA.chapters[index-1].id}">← ${escape(DATA.chapters[index-1].title)}</a>` : '<span></span>'}${index < DATA.chapters.length-1 ? `<a href="#${DATA.chapters[index+1].id}">${escape(DATA.chapters[index+1].title)} →</a>` : '<a href="#workbench">Practice in the workbench →</a>'}</div></footer></article>`;
    $('#mark-read').addEventListener('change', event => { event.target.checked ? read.add(chapter.id) : read.delete(chapter.id); persist(); updateNavigation(); });
    const exercise = {session:['session','Experiment with account and profile changes'],playback:['progress','Try the playback event simulator'],catalog:['history','Try the search-history simulator'],lifetime:['traces','Walk through client construction']}[chapter.id];
    if(exercise) $('.lesson-content').insertAdjacentHTML('beforeend',`<aside class="exercise"><strong>Put it into practice.</strong><p><a href="#workbench/${exercise[0]}">${exercise[1]} →</a></p></aside>`);
    $$('[data-quiz]').forEach(button => button.addEventListener('click', () => {
      const selected = Number(button.dataset.quiz);
      quizAnswers[chapter.id] = selected;
      $$('[data-quiz]').forEach(option => option.setAttribute('aria-pressed', String(option === button)));
      $('#quiz-feedback').textContent = `${selected === q.correct ? 'Correct.' : 'Try this reasoning:'} ${q.explanation}`;
    }));
    if ($('[data-widget="module-map"]')) renderModuleMap();
  }
  function renderModuleMap(selected='runtime') {
    const host = $('[data-widget="module-map"]');
    if (!host) return;
    const modules = DATA.interactions.modules;
    const chosen = modules.find(module => module.id === selected);
    const button = module => `<button type="button" data-module="${module.id}" aria-pressed="${module.id === selected}">${escape(module.name)}<small>${escape(module.role)}</small></button>`;
    host.innerHTML = `<div class="module-map"><p class="small">Select a module. The first row follows the main compile-dependency direction, left to right.</p><div class="module-chain">${modules.slice(0,4).map(button).join('')}</div><div class="module-extra">${modules.slice(4).map(button).join('')}</div><div class="module-detail" aria-live="polite"><h3>${escape(chosen.name)}</h3><p>${escape(chosen.owns)}</p><p><strong>Boundary:</strong> ${escape(chosen.boundary)}</p><p>${sourceRef(chosen.file,'Open its source')} <a href="#${chosen.chapter}">Read the related lesson</a></p></div></div>`;
    $$('[data-module]',host).forEach(item => item.addEventListener('click', () => { renderModuleMap(item.dataset.module); $(`[data-module="${item.dataset.module}"]`,host).focus(); }));
  }
  function renderExplorer() {
    const sdkFiles = DATA.files.filter(file => file.path.startsWith('sdk/'));
    const modules = [...new Set(DATA.files.map(file => file.module))];
    $('#main').innerHTML = `<article class="article">${header('Reference desk','Read the actual source','Search the SDK by file, type, package or responsibility. Source buttons throughout the course open this same embedded snapshot.')}<p>${sdkFiles.length} SDK files are inventoried, with text source embedded and binary artwork listed by path. Relevant application, sample and build files are included for cross-boundary reading. Generated build outputs and private local configuration are excluded.</p><div class="file-filters"><label>Filename, type or package<input id="file-query" type="search" placeholder="RuntimeSession, profile, error, DTO…"></label><label>Module<select id="file-module"><option value="">All modules</option>${modules.map(module => `<option>${escape(module)}</option>`).join('')}</select></label><label>File kind<select id="file-kind"><option value="">All files</option><option value="production">Production source</option><option value="tests">Tests</option><option value="resources">Resources / artwork</option></select></label></div><p id="file-count" class="small" role="status"></p><div id="file-results" class="file-results"></div><button id="file-more" type="button">Show more files</button><details><summary>About this snapshot</summary><p>Built ${escape(DATA.snapshot.date)} from commit <code>${escape(DATA.snapshot.commit)}</code>${DATA.snapshot.dirty ? ' plus working-tree SDK changes' : ''}. Content fingerprint: <code>${escape(DATA.snapshot.fingerprint)}</code>. A source button shows the embedded version, not a live read from disk. Refresh the source and review lessons with <code>python tools/sdk/build-guide.py</code>.</p></details></article>`;
    ['#file-query','#file-module','#file-kind'].forEach(selector => $(selector).addEventListener(selector === '#file-query' ? 'input' : 'change', () => { explorerLimit=60; renderFiles(); }));
    $('#file-more').addEventListener('click', () => { explorerLimit += 60; renderFiles(); });
    renderFiles();
  }
  function renderFiles() {
    const query = $('#file-query').value.toLowerCase().trim();
    const module = $('#file-module').value;
    const kind = $('#file-kind').value;
    const matches = DATA.files.filter(file => (!module || file.module === module) && (!query || `${file.path} ${file.declarations.join(' ')} ${file.summary}`.toLowerCase().includes(query)) && (!kind || (kind === 'tests' ? file.test : kind === 'resources' ? file.binary || file.path.includes('Resources') || file.path.includes('/res/') : !file.test && !file.binary && /\.kts?$/.test(file.name))));
    $('#file-count').textContent = `${matches.length} matches. Showing ${Math.min(explorerLimit,matches.length)}.`;
    $('#file-results').innerHTML = matches.length ? matches.slice(0,explorerLimit).map(file => `<button type="button" class="file-row" data-source="${escape(file.path)}"><strong>${escape(file.name)}</strong><small>${escape(file.module)} / ${escape(file.sourceSet || 'configuration or resource')} ${file.test ? '/ test' : ''}</small><small>${escape(file.path)}</small>${file.summary ? `<p>${escape(file.summary.length > 230 ? file.summary.slice(0,227)+'…' : file.summary)}</p>` : file.declarations.length ? `<p>${escape(file.declarations.slice(0,5).join(', '))}</p>` : ''}</button>`).join('') : '<p class="empty">No matching files. Try a shorter name or choose all modules.</p>';
    $('#file-more').hidden = matches.length <= explorerLimit;
  }
  function highlightLine(line) {
    const pattern = /(\/\/.*$|"(?:[^"\\]|\\.)*"|\b(?:package|import|class|interface|object|fun|val|var|private|internal|public|override|suspend|return|when|if|else|is|in|try|catch|finally|throw|data|sealed|enum|expect|actual|true|false|null)\b)/g;
    let last=0, output='';
    for (const match of line.matchAll(pattern)) { output += escape(line.slice(last,match.index)); const value=match[0]; output += value.startsWith('//') ? `<span class="token-comment">${escape(value)}</span>` : value.startsWith('"') ? escape(value) : `<span class="token-keyword">${escape(value)}</span>`; last=match.index+value.length; }
    return output + escape(line.slice(last));
  }
  function openSource(name,symbol='') {
    const file = resolveFile(name);
    if (!file) { notify(`No snapshot found for ${name}`); return; }
    if ($('#search-dialog').open) $('#search-dialog').close();
    currentSource = file; sourceMatch=-1;
    $('#source-title').textContent=file.name;
    $('#source-meta').textContent=`${file.module} / ${file.sourceSet || 'configuration or resource'}`;
    $('#source-path').textContent=file.path;
    $('#source-description').textContent=file.summary || (file.test ? 'A behavioral test. Read its inputs, action and assertions to understand what is established.' : file.binary ? 'Binary resource inventory. Open this repository path in your IDE to inspect the artwork.' : 'Exact repository text captured when the guide was built.');
    $('#source-find').value=symbol;
    $('#source-status').textContent=`${file.lines} lines`;
    $('#code-reader').innerHTML=file.binary ? '<p style="padding:20px">Artwork is listed but not duplicated inside this guide.</p>' : file.source.split('\n').map((line,i) => `<div class="code-line" data-line="${i+1}"><span class="line-no">${i+1}</span><span class="line-text">${highlightLine(line)}</span></div>`).join('');
    if (!$('#source-dialog').open) $('#source-dialog').showModal();
    $('#code-reader').scrollTop=0;
    if (symbol) findSource();
  }
  function findSource() {
    const term = $('#source-find').value.toLowerCase();
    $$('.code-line.match').forEach(line => line.classList.remove('match'));
    if (!term || !currentSource) return;
    const lines = currentSource.source.split('\n');
    const matches = lines.map((line,i) => line.toLowerCase().includes(term) ? i : -1).filter(i => i>=0);
    if (!matches.length) { $('#source-status').textContent='No matches'; return; }
    sourceMatch = matches.find(index => index > sourceMatch) ?? matches[0];
    const element = $(`[data-line="${sourceMatch+1}"]`);
    element.classList.add('match');
    $('#code-reader').scrollTop = element.offsetTop - $('#code-reader').offsetTop - 80;
    $('#source-status').textContent=`Match ${matches.indexOf(sourceMatch)+1} of ${matches.length}, line ${sourceMatch+1}`;
  }
  $('#source-next').addEventListener('click',findSource);
  $('#source-find').addEventListener('input', () => { sourceMatch=-1; findSource(); });
  $('#source-find').addEventListener('keydown',event => { if(event.key==='Enter') findSource(); });
  $('#copy-path').addEventListener('click',async () => { try { await navigator.clipboard.writeText(currentSource.path); notify('Repository path copied'); } catch (_) { $('#source-status').textContent='Select the path above to copy it.'; } });
  $$('[data-close]').forEach(button => button.addEventListener('click',() => document.getElementById(button.dataset.close).close()));
  document.addEventListener('click',event => { const reference=event.target.closest('[data-source]'); if(reference) openSource(reference.dataset.source,reference.dataset.symbol || ''); });

  function openSearch() { $('#search-dialog').showModal(); $('#global-search').focus(); search(); }
  function search() {
    const query=$('#global-search').value.trim().toLowerCase();
    if(!query) { $('#global-results').innerHTML='<p class="small">Search explanations, concepts, symbols and paths. Try “activation”, “history” or “AuthProvider”.</p>'; return; }
    const chapters=textByChapter.filter(chapter => `${chapter.title} ${chapter.text}`.toLowerCase().includes(query));
    const files=DATA.files.filter(file => `${file.path} ${file.declarations.join(' ')}`.toLowerCase().includes(query)).slice(0,18);
    $('#global-results').innerHTML=chapters.map(chapter => `<button type="button" class="search-result" data-search-chapter="${chapter.id}"><strong>${escape(chapter.title)}</strong><small>Lesson / ${escape(chapter.summary)}</small></button>`).join('')+files.map(file => `<button type="button" class="search-result" data-source="${escape(file.path)}"><strong>${escape(file.name)}</strong><small>${escape(file.path)}</small></button>`).join('') || '<p>No matches. Try a shorter term.</p>';
    $$('[data-search-chapter]').forEach(button => button.addEventListener('click',() => { $('#search-dialog').close(); location.hash=button.dataset.searchChapter; }));
  }
  $('#search-open').addEventListener('click',openSearch);
  $('#global-search').addEventListener('input',search);
  document.addEventListener('keydown',event => {
    if(event.key==='Escape' && document.querySelector('dialog[open]')) { event.preventDefault(); document.querySelector('dialog[open]').close(); return; }
    if(event.key==='/' && !['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName) && !document.querySelector('dialog[open]')) { event.preventDefault(); openSearch(); }
    if(event.key==='Escape' && $('#sidebar').classList.contains('open')) { $('#sidebar').classList.remove('open'); $('#menu').setAttribute('aria-expanded','false'); $('#menu').focus(); }
  });

  function renderWorkbench() {
    $('#main').innerHTML=`<article class="article">${header('Practice with the rules','Follow a request','Use the real call paths, then experiment with a few carefully scoped teaching models. No backend requests or SDK writes occur here.')}<div class="tools-tabs" role="group" aria-label="Workbench tools">${[['traces','Request walkthroughs'],['session','Account & profile'],['progress','Playback progress'],['history','Search history']].map(([id,label]) => `<button type="button" data-tool="${id}" aria-pressed="${workbenchTab===id}">${label}</button>`).join('')}</div><div id="tool-content"></div></article>`;
    $$('[data-tool]').forEach(button => button.addEventListener('click',() => { workbenchTab=button.dataset.tool; renderWorkbench(); $(`[data-tool="${workbenchTab}"]`).focus(); }));
    if(workbenchTab==='traces') renderTrace();
    if(workbenchTab==='session') renderSessionLab();
    if(workbenchTab==='progress') renderProgressLab();
    if(workbenchTab==='history') renderHistoryLab();
  }
  function renderTrace() {
    const journey=DATA.interactions.journeys.find(item => item.id===journeyId);
    const step=journey.steps[journeyStep];
    $('#tool-content').innerHTML=`<label>Operation<select id="journey-select">${DATA.interactions.journeys.map(item => `<option value="${item.id}" ${item.id===journeyId?'selected':''}>${escape(item.title)}</option>`).join('')}</select></label><p style="margin-top:20px">${escape(journey.intro)}</p><div class="lab"><div class="trace-layout"><div class="trace-steps" aria-label="Request stages">${journey.steps.map((item,i) => `<button type="button" data-step="${i}" aria-pressed="${i===journeyStep}">${escape(item.title)}</button>`).join('')}</div><div class="trace-detail" aria-live="polite"><p class="trace-number">${journeyStep+1} of ${journey.steps.length}</p><p class="trace-owner">${escape(step.owner)}</p><h3>${escape(step.title)}</h3><p>${escape(step.body)}</p><div class="lab-result"><strong>What changes?</strong><p>${escape(step.effect)}</p></div>${sourceRef(step.file,'Read the exact implementation',step.symbol)}<div class="trace-nav"><button id="trace-prev" type="button" ${journeyStep===0?'disabled':''}>Previous</button><button id="trace-next" type="button" ${journeyStep===journey.steps.length-1?'disabled':''}>Next</button></div></div></div></div><p class="small">These walkthroughs describe current code. They do not execute the SDK. Open a source reference to inspect branches omitted from the main path.</p>`;
    $('#journey-select').addEventListener('change',event => { journeyId=event.target.value;journeyStep=0;renderTrace();$('#journey-select').focus(); });
    $$('[data-step]').forEach(button => button.addEventListener('click',() => { journeyStep=Number(button.dataset.step);renderTrace();$(`[data-step="${journeyStep}"]`).focus(); }));
    $('#trace-prev').addEventListener('click',() => { journeyStep--;renderTrace();$('[data-step][aria-pressed=true]').focus(); });
    $('#trace-next').addEventListener('click',() => { journeyStep++;renderTrace();$('[data-step][aria-pressed=true]').focus(); });
  }
  let session;
  function resetSession() { session={isAuthInitialized:false,closed:false,account:null,profile:null,accountVersion:0,activation:0,pending:null,log:['New SDK instance. No account or authorized profile.']}; }
  resetSession();
  function renderSessionLab() {
    $('#tool-content').innerHTML=`<div class="lab"><h2>One SDK, changing context</h2><p>Try: restore session → log in A → enter Adult → start request → logout → log in B → enter Adult → finish request. Both accounts use the same example profile ID, <code>adult</code>.</p><p class="small">Teaching model: unprotected profiles, successful backend/storage operations, and one pending read. Direct login is valid before restoration; a later restore returns the initialized context. PIN, migration, mutex timing and failure reconciliation are covered in the lessons.</p><div class="state-grid" aria-live="polite"><div><span>SDK</span><strong>${session.closed?'Closed':session.isAuthInitialized?'Auth initialized':'Auth not initialized'}</strong></div><div><span>Account</span><strong>${session.account||'None'}</strong></div><div><span>Authorized profile</span><strong>${session.profile||'None'}</strong></div><div><span>Activation generation</span><strong>${session.profile?session.activation:'No active grant'}</strong></div></div><div class="lab-controls"><button data-session="restoreSession" ${session.closed?'disabled':''}>Restore session</button><button data-session="A" ${session.closed||session.account?'disabled':''}>Log in A</button><button data-session="B" ${session.closed||session.account?'disabled':''}>Log in B</button><button data-session="Adult" ${!session.account||session.closed?'disabled':''}>Enter Adult</button><button data-session="Kids" ${!session.account||session.closed?'disabled':''}>Enter Kids</button></div><div class="lab-controls"><button data-session="start" ${!session.profile||session.closed?'disabled':''}>Start request</button><button data-session="finish" ${!session.pending?'disabled':''}>Finish request</button><button data-session="logout" ${!session.account||session.closed?'disabled':''}>Logout</button><button data-session="close" ${session.closed?'disabled':''}>Close SDK</button><button data-session="reset">Reset example</button></div><div class="lab-result"><strong>Captured by the pending request</strong><p>${session.pending?escape(`${session.pending.account} / ${session.pending.profile}, account generation ${session.pending.accountVersion}, activation ${session.pending.activation}`):'No pending request'}</p></div><div class="event-log" role="log" aria-label="Session events">${session.log.slice(-8).map(line=>`<p>${escape(line)}</p>`).join('')}</div><p class="small" style="margin-top:20px">The real implementation compares object identities, not these display counters. ${sourceRef('RuntimeSession.kt','Read the shared guards','checkProfileResult')} ${sourceRef('AuthorizedProfile.kt','Read AuthorizedProfile')}</p></div>`;
    $$('[data-session]').forEach(button=>button.addEventListener('click',()=>{
      const action=button.dataset.session;
      if(action==='reset') resetSession();
      else if(action==='restoreSession') {session.log.push(session.isAuthInitialized?'Authentication already initialized: return the current context.':'Session restoration completed. This example has no persisted credentials.');session.isAuthInitialized=true;}
      else if(action==='A'||action==='B') {session.account=action;session.accountVersion++;session.isAuthInitialized=true;session.profile=null;session.log.push(`Account ${action} authenticated. No profile authorization yet.`);}
      else if(action==='Adult'||action==='Kids') {session.profile=action;session.activation++;session.log.push(`Entered ${action}; new activation ${session.activation}.`);}
      else if(action==='start') {session.pending={account:session.account,accountVersion:session.accountVersion,profile:session.profile,activation:session.activation};session.log.push('Started request with captured account/profile authorization.');}
      else if(action==='finish') {const p=session.pending;session.log.push(session.closed?'In-flight SDK work is cancelled on close; new operations fail Closed.':p.account!==session.account||p.accountVersion!==session.accountVersion?'Rejected: StaleSession. This result belongs to an earlier account session.':p.activation!==session.activation||p.profile!==session.profile?'Rejected: StaleActivation. Same account does not imply the same profile grant.':'Accepted: account and activation still match.');session.pending=null;}
      else if(action==='logout') {session.log.push(`Logout succeeded for ${session.account}. Saved data retained; active account/profile cleared.`);session.account=null;session.profile=null;}
      else if(action==='close') {session.closed=true;session.account=null;session.profile=null;session.log.push('SDK disposed. Owned work cancelled; no remote logout or saved-data erasure.');}
      renderSessionLab();
    }));
  }
  let progress={bucket:0,saved:null,log:[]};
  function renderProgressLab() {
    $('#tool-content').innerHTML=`<div class="lab"><h2>When does progress get saved?</h2><p>Assume a current authorized profile, allowed content and progress capability. This simulation isolates cadence and resume policy. Position and duration below are seconds; Kotlin stores milliseconds.</p><div class="lab-controls"><label>Starting position<input id="initial-position" type="number" min="0" max="1000000" value="0"></label><button id="progress-reset">Reset example at this position</button></div><div class="lab-controls"><label>Position (seconds)<input id="progress-position" type="number" min="0" max="1000000" value="30"></label><label>Duration (seconds; 0 = unknown)<input id="progress-duration" type="number" min="0" max="1000000" value="600"></label><label>Event<select id="progress-event"><option>Periodic</option><option>Checkpoint</option><option>Completed</option><option value="Direct">Direct updateProgress</option></select></label></div><label class="read-check"><input type="checkbox" id="progress-failure"> Simulate a failed storage operation</label><div class="lab-controls" style="margin-top:18px"><button class="primary" id="progress-send">Report event</button></div><div id="progress-output" aria-live="polite"></div><div id="progress-log" class="event-log" role="log" aria-label="Playback event outcomes"></div><p class="small" style="margin-top:20px">No automatic retries or timer. A failed periodic attempt consumes its bucket. Reset clears only this teaching example. ${sourceRef('RuntimePlaybackService.kt','Read event handling','override suspend fun reportEvent')} ${sourceRef('PlaybackProgressPolicy.kt','Read thresholds')}</p></div>`;
    $('#progress-reset').addEventListener('click',()=>{const value=Number($('#initial-position').value);if(!$('#initial-position').checkValidity()||!Number.isFinite(value)) return;progress={bucket:Math.floor(value/10),saved:null,log:[`New recorder at ${value}s; last attempted bucket ${Math.floor(value/10)}. Example saved entry cleared.`]};updateProgressOutput();});
    $('#progress-send').addEventListener('click',()=>{
      if(!$('#progress-position').reportValidity()||!$('#progress-duration').reportValidity()) return;
      const position=Number($('#progress-position').value),duration=Number($('#progress-duration').value),event=$('#progress-event').value,fail=$('#progress-failure').checked;
      let result;
      if(event!=='Completed'&&event!=='Direct'&&duration<=0) result='Success with no write: unknown duration preserves the saved entry and cadence bucket.';
      else if(event==='Periodic'&&Math.floor(position/10)<=progress.bucket) result='Success with no write: position bucket has not advanced.';
      else {
        if(event==='Periodic') progress.bucket=Math.floor(position/10);
        const save=event!=='Completed'&&duration>0&&position>=30&&position/duration<.95;
        if(fail) result=`Failure: simulated storage ${save?'upsert':'removal'} failed. Saved entry unchanged${event==='Periodic'?'; periodic bucket remains advanced':''}.`;
        else if(save) {progress.saved={position,duration};result=`Success: upsert ${position}s / ${duration}s.`;}
        else {progress.saved=null;result='Success: remove saved progress (completed or outside resume range).';}
      }
      progress.log.push(`${event} ${position}s/${duration}s → ${result}`);updateProgressOutput();
    });
    updateProgressOutput();
  }
  function updateProgressOutput() {
    $('#progress-output').innerHTML=`<div class="state-grid"><div><span>Last attempted periodic bucket</span><strong>${progress.bucket} (${progress.bucket*10}s boundary)</strong></div><div><span>Saved progress</span><strong>${progress.saved?`${progress.saved.position}s / ${progress.saved.duration}s`:'No entry'}</strong></div></div><p class="small">Resume eligibility: duration &gt; 0; position ≥ 30 seconds; fraction &lt; 95%. Exactly 95% removes the entry.</p>`;
    $('#progress-log').innerHTML=progress.log.length?progress.log.slice(-8).map(line=>`<p>${escape(line)}</p>`).join(''):'<p>Try Periodic at 30s, then 35s, then Checkpoint at 35s.</p>';
  }
  let history=[];
  function renderHistoryLab() {
    $('#tool-content').innerHTML=`<div class="lab"><h2>What enters search history?</h2><p>Assume a valid profile, search/history support and a successful catalogue response. Try the same query with different spacing and capitalization.</p><div class="lab-controls"><label>Query<input id="history-query" type="text" value="  Star   Wars  "></label><label>Interaction<select id="history-interaction"><option>Typing</option><option>Submitted</option><option>RecentSelected</option><option value="ResultSelected">Result selected</option></select></label></div><div class="lab-controls"><label>Allowed results returned<select id="history-results"><option value="nonempty">One or more</option><option value="empty">None</option></select></label><label>History write<select id="history-storage"><option value="ok">Succeeds</option><option value="fail">Fails</option></select></label><button class="primary" id="history-send">Apply interaction</button><button id="history-reset">Reset example</button></div><div class="lab-result" id="history-outcome" aria-live="polite">Typing alone does not add history.</div><h3>Recent queries, newest first</h3><ol id="history-list"></ol><p class="small">Result selected represents the separate <code>resultSelected</code> call; it does not repeat a catalogue query. No profile refresh, cancellation or network is simulated here. ${sourceRef('RuntimeSearchService.kt','Read interaction handling')} ${sourceRef('SearchQueryNormalizer.kt','Read normalization')}</p></div>`;
    const update=()=>{$('#history-list').innerHTML=history.length?history.map(item=>`<li>${escape(item)}</li>`).join(''):'<li>No history yet.</li>';};
    update();
    $('#history-send').addEventListener('click',()=>{
      const query=$('#history-query').value.trim().replace(/\s+/g,' '),interaction=$('#history-interaction').value,selected=interaction==='ResultSelected';
      const records=query.length>=3&&(selected||(interaction!=='Typing'&&$('#history-results').value==='nonempty'));
      let explanation;
      if(query.length<3) explanation='Too short after normalization. No history write; catalogue search would return empty after its context checks.';
      else if(!records) explanation=interaction==='Typing'?'Typing does not record history.':'No allowed results, so this search does not record history.';
      else if($('#history-storage').value==='fail') explanation=selected?'The explicit resultSelected operation reports the history failure. History is unchanged.':'Catalogue results still succeed even though the history write failed. History is unchanged.';
      else {history=[query,...history.filter(item=>item.toLowerCase()!==query.toLowerCase())].slice(0,5);explanation='Recorded at the front; case-insensitive duplicate removed; latest spelling retained; maximum five.';}
      $('#history-outcome').textContent=`Normalized: “${query}”. ${explanation}`;update();
    });
    $('#history-reset').addEventListener('click',()=>{history=[];$('#history-outcome').textContent='Example history cleared.';update();});
  }

  function route() {
    const [hash,tool]=decodeURIComponent(location.hash.slice(1) || 'overview').split('/');
    if(hash==='workbench'&&['traces','session','progress','history'].includes(tool)) workbenchTab=tool;
    currentRoute=DATA.chapters.some(chapter=>chapter.id===hash)||['workbench','source'].includes(hash)?hash:'overview';
    $('#sidebar').classList.remove('open');$('#menu').setAttribute('aria-expanded','false');
    updateNavigation();
    if(currentRoute==='source') renderExplorer(); else if(currentRoute==='workbench') renderWorkbench(); else renderChapter(DATA.chapters.find(chapter=>chapter.id===currentRoute));
    document.title=`${currentRoute==='source'?'Source explorer':currentRoute==='workbench'?'Request workbench':DATA.chapters.find(chapter=>chapter.id===currentRoute).title} | Inside StreamCore`;
    window.scrollTo(0,0);
    if(location.hash) $('#main').focus({preventScroll:true});
  }
  window.addEventListener('hashchange',route);
  $('#print').addEventListener('click',()=>{
    let print=document.querySelector('.print-course');if(print) print.remove();
    print=document.createElement('div');print.className='print-course';
    print.innerHTML=DATA.chapters.map(chapter=>`<article class="print-chapter"><p>${escape(chapter.kicker)}</p><h1>${escape(chapter.title)}</h1><p>${escape(chapter.summary)}</p>${chapter.body}${chapter.quiz?`<h3>Check your understanding</h3><p>${escape(chapter.quiz.question)}</p><p>${escape(chapter.quiz.explanation)}</p>`:''}</article>`).join('');
    $$('details',print).forEach(detail=>detail.open=true);document.body.appendChild(print);window.print();
  });
  route();
})();
