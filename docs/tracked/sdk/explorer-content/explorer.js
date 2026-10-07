(async () => {
  'use strict';
  const dataElement=document.getElementById('explorer-data');
  const packed=Uint8Array.from(atob(dataElement.textContent.trim()),character=>character.charCodeAt(0));
  const stream=new Blob([packed]).stream().pipeThrough(new DecompressionStream('gzip'));
  const DATA=JSON.parse(await new Response(stream).text());
  const $ = selector => document.querySelector(selector);
  const $$ = selector => [...document.querySelectorAll(selector)];
  const esc = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
  const nodes = new Map(DATA.nodes.map(node => [node.id, node]));
  const files = new Map(DATA.files.map(file => [file.path, file]));
  const edges = new Map(DATA.edges.map(edge => [edge.id, edge]));
  const valueKinds = new Set(['parameter', 'value', 'property']);
  const kindLabels = new Map();
  const contextMatches = (item, context) => !Array.isArray(item.contextIds) || item.contextIds.includes(context);
  const journeyMatches = (node, journey) => node.journeys?.includes(journey);
  const defaultContext = DATA.contexts.find(context => context.id === 'tmdb-android') || DATA.contexts[0];
  const defaultJourney = DATA.journeys.find(journey => journey.id === 'construction') || DATA.journeys[0];
  const defaultNode = context => DATA.nodes.find(node => node.key === 'tmdb.api.httpClient' && contextMatches(node, context)) || DATA.nodes.find(node => node.label === 'KtorTmdbApi.httpClient' && contextMatches(node, context)) || DATA.nodes.find(node => journeyMatches(node, defaultJourney.id) && contextMatches(node, context)) || DATA.nodes.find(node => contextMatches(node, context));
  let state = {context:defaultContext.id, journey:defaultJourney.id, node:defaultNode(defaultContext.id)?.id || '', mode:'auto', direction:'both', values:true, pinnedFunction:'', view:'tree', treeRoot:''};
  let relationTree;
  let extraNodes = new Set();
  let revealedNodes = new Set();
  let selectedFile = null;
  let evidenceRange = null;
  let sourceMatch = -1;
  let searchLimit = 24;
  let toastTimer;
  let currentGraph = {visible:[], visibleEdges:[], upstream:new Set(), downstream:new Set(), upstreamEdges:new Set(), downstreamEdges:new Set()};
  let positions = new Map();
  let positionKey = '';
  let view = {x:0, y:0, scale:0.85};
  let historyIndex = 0;
  let historyLength = 1;
  let drag = null;
  let moved = false;
  let activePane = null;
  let paneReturnFocus = null;
  let keyboardNavigation = false;
  let restoringGraphFocus = false;
  const NODE_WIDTH = 232;
  const NODE_HEIGHT = 86;
  const COLUMN_GAP = 56;
  const ROW_GAP = 39;
  const themeKey = 'streamcore-sdk-explorer-theme';
  let theme = 'system';
  try { theme = localStorage.getItem(themeKey) || 'system'; } catch (_) { /* Optional preference. */ }
  if (!['system','light','dark'].includes(theme)) theme = 'system';
  $('#theme').value = theme;
  function applyTheme() {
    document.documentElement.dataset.theme = theme === 'system' ? (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light') : theme;
  }
  applyTheme();
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => { if (theme === 'system') applyTheme(); });
  $('#theme').addEventListener('change', event => { theme = event.target.value; applyTheme(); try { localStorage.setItem(themeKey, theme); } catch (_) { /* Optional preference. */ } });
  const notify = message => { clearTimeout(toastTimer); $('#toast').textContent = message; $('#toast').hidden = false; toastTimer = setTimeout(() => { $('#toast').hidden = true; }, 3000); };
  function getContext() { return DATA.contexts.find(context => context.id === state.context); }
  function getJourney() { return DATA.journeys.find(journey => journey.id === state.journey); }
  function validState(input) {
    const context = DATA.contexts.some(item => item.id === input.context) ? input.context : defaultContext.id;
    const journey = DATA.journeys.some(item => item.id === input.journey) ? input.journey : defaultJourney.id;
    const requested = nodes.get(input.node);
    const journeyDefault = journey === defaultJourney.id ? defaultNode(context) : DATA.nodes.find(node => contextMatches(node,context) && journeyMatches(node,journey) && !valueKinds.has(node.kind)) || DATA.nodes.find(node => contextMatches(node,context) && journeyMatches(node,journey));
    const selected = requested && contextMatches(requested, context) ? requested : journeyDefault || defaultNode(context);
    const pinned = nodes.get(input.pinnedFunction);
    return {context, journey, node:selected?.id || '', mode:['auto','call','value'].includes(input.mode) ? input.mode : 'auto', direction:['both','upstream','downstream'].includes(input.direction) ? input.direction : 'both', values:input.values !== false, view:input.view==='graph'?'graph':'tree', treeRoot:nodes.has(input.treeRoot) && contextMatches(nodes.get(input.treeRoot),context) ? input.treeRoot : selected?.id || '', pinnedFunction:pinned?.kind==='callable' && contextMatches(pinned,context) ? pinned.id : ''};
  }
  function parseHash() {
    const params = new URLSearchParams(location.hash.slice(1));
    return validState({context:params.get('context'), journey:params.get('journey'), node:params.get('node'), mode:params.get('mode'), direction:params.get('direction'), values:params.get('values') !== '0', pinnedFunction:params.get('from'),view:params.get('view'),treeRoot:params.get('root')});
  }
  function stateHash() {
    const params = new URLSearchParams({context:state.context, journey:state.journey, node:state.node, mode:state.mode, direction:state.direction, values:state.values ? '1' : '0',view:state.view,root:state.treeRoot || state.node});
    if(state.pinnedFunction)params.set('from',state.pinnedFunction);
    return `#${params}`;
  }
  function saveLocation(replace = false) {
    if (!replace && location.hash === stateHash()) return;
    if (!replace) { historyIndex++; historyLength = historyIndex + 1; }
    try { history[replace ? 'replaceState' : 'pushState']({explorerIndex:historyIndex}, '', stateHash()); }
    catch (_) { if (replace) location.replace(stateHash()); else location.hash = stateHash(); }
    updateHistory();
  }
  function updateHistory() {
    $('#history-back').disabled = historyIndex <= 0;
    $('#history-forward').disabled = historyIndex >= historyLength - 1;
  }
  function navigate(changes, {focus=false, inspect=false, keepTreeRoot=false} = {}) {
    const previous = state;
    const requested={...state,...changes};
    if(changes.node && !keepTreeRoot && !changes.treeRoot && requested.view==='tree')requested.treeRoot=changes.node;
    state = validState(requested);
    const changedMap = previous.context !== state.context || previous.journey !== state.journey;
    if (changedMap) { extraNodes.clear();revealedNodes.clear(); }
    saveLocation();
    render();
    if (state.view==='graph' && (focus || changedMap)) focusSelected();
    if (previous.node !== state.node) $('#inspector').scrollTop = 0;
    if (inspect && matchMedia('(max-width:1050px)').matches) setPane('inspector', true);
  }
  function selectNode(id, {focus=false, inspect=false,keepTreeRoot=false} = {}) {
    const node = nodes.get(id);
    if (!node || !contextMatches(node, state.context)) return;
    navigate({node:id}, {focus, inspect,keepTreeRoot});
  }
  function switchContext(provider, platform) {
    const next = DATA.contexts.find(context => context.provider === provider && context.platform === platform);
    if (!next) { notify('No source context is available for this combination.'); renderControls(); return; }
    const old = nodes.get(state.node);
    const replacement = old && contextMatches(old, next.id) ? old : DATA.nodes.find(node => node.key === old?.key && contextMatches(node, next.id)) || DATA.nodes.find(node => journeyMatches(node, state.journey) && contextMatches(node, next.id)) || defaultNode(next.id);
    navigate({context:next.id, node:replacement?.id || ''}, {focus:true});
  }
  function calculateGraph() {
    const contextNodes = DATA.nodes.filter(node => contextMatches(node, state.context));
    const contextIds = new Set(contextNodes.map(node => node.id));
    const contextualEdges = DATA.edges.filter(edge => contextMatches(edge, state.context) && contextIds.has(edge.from) && contextIds.has(edge.to));
    const selected = nodes.get(state.node);
    const effectiveMode = state.mode !== 'auto' ? state.mode : valueKinds.has(selected?.kind) ? 'value' : selected?.kind==='external' && contextualEdges.some(edge=>(edge.from===state.node || edge.to===state.node) && edge.traces.includes('value')) ? 'value' : 'call';
    const tracingEdges = contextualEdges.filter(edge => edge.traces?.includes(effectiveMode));
    const pair = state.pinnedFunction && valueKinds.has(selected?.kind) ? findConnectionPath(state.pinnedFunction,state.node,contextualEdges) : null;
    function walk(direction) {
      const reached = new Set();
      const traversed = new Set();
      const visited = new Set([state.node]);
      const queue = [state.node];
      const adjacency = new Map();
      tracingEdges.forEach(edge => {
        const key = direction === 'upstream' ? edge.to : edge.from;
        if (!adjacency.has(key)) adjacency.set(key, []);
        adjacency.get(key).push(edge);
      });
      for (let cursor = 0; cursor < queue.length; cursor++) {
        for (const edge of adjacency.get(queue[cursor]) || []) {
          if(edge[direction]===false)continue;
          traversed.add(edge.id);
          const id = direction === 'upstream' ? edge.from : edge.to;
          if (id !== state.node) reached.add(id);
          if (!visited.has(id)) { visited.add(id); queue.push(id); }
        }
      }
      return {reached, traversed};
    }
    const upstream = state.direction === 'downstream' ? {reached:new Set(), traversed:new Set()} : walk('upstream');
    const downstream = state.direction === 'upstream' ? {reached:new Set(), traversed:new Set()} : walk('downstream');
    const included = new Set(contextNodes.filter(node => node.origin!=='source-index' && journeyMatches(node, state.journey)).map(node => node.id));
    const adjacent=contextualEdges.filter(edge=>edge.from===state.node || edge.to===state.node).flatMap(edge=>[edge.from,edge.to]);
    const reviewedReachable=[...upstream.reached,...downstream.reached].filter(id=>nodes.get(id)?.origin!=='source-index');
    [state.node, state.pinnedFunction, ...(pair?.nodeIds || []), ...adjacent, ...reviewedReachable, ...extraNodes].filter(Boolean).forEach(id => revealedNodes.add(id));
    revealedNodes.forEach(id => included.add(id));
    const pathNodes = new Set(pair?.nodeIds || []),pathEdges = new Set(pair?.edgeIds || []);
    const visible = contextNodes.filter(node => included.has(node.id) && (state.values || !valueKinds.has(node.kind) || node.id === state.node || extraNodes.has(node.id) || upstream.reached.has(node.id) || downstream.reached.has(node.id) || pathNodes.has(node.id)));
    const visibleIds = new Set(visible.map(node => node.id));
    const visibleEdges = contextualEdges.filter(edge => visibleIds.has(edge.from) && visibleIds.has(edge.to) && (edge.traces.includes(effectiveMode) || pathEdges.has(edge.id)));
    return {contextNodes, contextualEdges, tracingEdges, visible, visibleEdges, effectiveMode, pair, pathNodes, pathEdges, upstream:upstream.reached, downstream:downstream.reached, upstreamEdges:upstream.traversed, downstreamEdges:downstream.traversed};
  }
  function findConnectionPath(from, to, contextualEdges) {
    const adjacency = new Map();
    contextualEdges.filter(edge=>edge.traces.length && !['owns','closes'].includes(edge.kind)).forEach(edge=>{
      if(!adjacency.has(edge.from))adjacency.set(edge.from,[]);
      adjacency.get(edge.from).push(edge);
    });
    function directed(start,end) {
      const previous=new Map([[start,null]]),queue=[start];
      for(let i=0;i<queue.length && !previous.has(end);i++) {
        for(const edge of adjacency.get(queue[i]) || []) {
          if(previous.has(edge.to))continue;
          previous.set(edge.to,edge);queue.push(edge.to);
        }
      }
      if(!previous.has(end))return null;
      const nodeIds=[end],edgeIds=[];
      for(let id=end;id!==start;){const edge=previous.get(id);edgeIds.unshift(edge.id);id=edge.from;nodeIds.unshift(id);}
      return {nodeIds,edgeIds};
    }
    const forward=directed(from,to);
    if(forward)return {...forward,reverse:false};
    const reverse=directed(to,from);
    return reverse ? {...reverse,reverse:true} : {nodeIds:[],edgeIds:[],reverse:false};
  }
  function renderControls() {
    const context = getContext();
    const journey = getJourney();
    $('#provider').value = context.provider;
    $('#platform').value = context.platform;
    $('#trace-mode').value = state.mode;
    $('#direction').value = state.direction;
    $('#show-values').checked = state.values;
    const isTree=state.view==='tree';
    $('#main').dataset.surface=state.view;
    $('#tree-view').setAttribute('aria-pressed',String(isTree));
    $('#graph-view').setAttribute('aria-pressed',String(!isTree));
    $('#graph-viewport').hidden=isTree;
    $('#tree-viewport').hidden=!isTree;
    $('.graph-footer').hidden=isTree;
    $$('.graph-only').forEach(control=>control.hidden=isTree);
    $('#tree-root-selected').hidden=!isTree || state.node===state.treeRoot;
    $('#graph-guidance').hidden=isTree;
    const modeText = currentGraph.effectiveMode === 'value' ? 'Value flow: where this value is created, passed, and used.' : 'Function calls: the callers and operations connected to this function.';
    $('#trace-explanation').textContent = isTree ? 'Full tree: expand inputs, locals, origins and downstream uses. Every source-indexed signature is included; unresolved calls are labeled.' : `${state.mode==='auto'?'Automatic · ':''}${modeText}${state.pinnedFunction ? ' The pinned path can include both calls and value passing.' : ''} This is an overview; Full tree expands every indexed branch.`;
    const pinned = nodes.get(state.pinnedFunction);
    $('#pinned-summary').hidden=!pinned || isTree;
    $('#pinned-function-name').textContent=pinned ? `${displayName(pinned)} → ${valueKinds.has(nodes.get(state.node)?.kind) ? displayName(nodes.get(state.node)) : 'select a value'}` : '';
    $('#context-label').textContent = context.label;
    $('#context-note').textContent = context.currentAppWiring === false ? 'SDK entrypoint available. This combination is not wired by the current app.' : 'Follows the current application wiring.';
    $('#journey-title').textContent = journey.label;
    $('#journey-description').textContent = journey.description || 'Select a function or value to trace its connections through the SDK.';
    const applicableJourneys = DATA.journeys.filter(item => DATA.nodes.some(node => contextMatches(node, state.context) && journeyMatches(node, item.id)));
    $('#journey-count').textContent = String(applicableJourneys.length);
    $('#journeys').innerHTML = applicableJourneys.map((item, index) => `<button class="journey-button" data-journey="${esc(item.id)}" ${state.journey === item.id ? 'aria-current="page"' : ''}><span class="journey-index">${String(index + 1).padStart(2,'0')}</span><span>${esc(item.label)}</span></button>`).join('');
    updateHistory();
  }
  function assignPositions() {
    const key = `${state.context}:${state.journey}`;
    if (positionKey === key) {
      // Keep the user's spatial reference. Newly discovered cross-journey nodes
      // get unused slots; existing nodes never move when selection changes.
      for (const node of currentGraph.visible) {
        if (positions.has(node.id)) continue;
        const occupied = [...positions.values()];
        const neighbor = occupied.reduce((best, spot) => !best || Math.abs(spot.column-node.column)<Math.abs(best.column-node.column) ? spot : best, null);
        const x = neighbor?.x ?? 50;
        const lastY = Math.max(-55,...occupied.filter(spot=>spot.x===x).map(spot=>spot.y));
        positions.set(node.id,{x,y:lastY+NODE_HEIGHT+ROW_GAP,column:node.column});
      }
      return;
    }
    positions = new Map();
    positionKey = key;
    const active = new Set([state.node,...currentGraph.upstream,...currentGraph.downstream,...currentGraph.pathNodes]);
    const hasTrace = active.size > 1;
    const distance = new Map([[state.node,0]]);
    const queue=[state.node];
    for(let cursor=0;cursor<queue.length;cursor++){
      const id=queue[cursor];
      for(const edge of currentGraph.visibleEdges){
        const next=edge.from===id?edge.to:edge.to===id?edge.from:null;
        if(next && active.has(next) && !distance.has(next)){distance.set(next,distance.get(id)+1);queue.push(next);}
      }
    }
    const activeColumns=[...new Set(currentGraph.visible.filter(node=>!hasTrace || active.has(node.id)).map(node=>node.column || 0))].sort((a,b)=>a-b);
    const columns=new Map();
    currentGraph.visible.forEach(node=>{
      const modelColumn=node.column || 0;
      const nearest=activeColumns.reduce((best,column)=>Math.abs(column-modelColumn)<Math.abs(best-modelColumn)?column:best,activeColumns[0] || 0);
      const columnIndex=activeColumns.indexOf(nearest);
      if(!columns.has(columnIndex))columns.set(columnIndex,[]);
      columns.get(columnIndex).push(node);
    });
    const maxActiveRows=Math.max(1,...[...columns.values()].map(column=>column.filter(node=>active.has(node.id)).length));
    const unrelatedTop=70+maxActiveRows*(NODE_HEIGHT+ROW_GAP)+90;
    const priority=node=>node.id===state.node?0:currentGraph.upstream.has(node.id)?1:2;
    [...columns].sort((a,b)=>a[0]-b[0]).forEach(([columnIndex,candidates])=>{
      const traced=candidates.filter(node=>active.has(node.id)).sort((a,b)=>priority(a)-priority(b)||(distance.get(a.id)||0)-(distance.get(b.id)||0));
      const unrelated=candidates.filter(node=>!active.has(node.id));
      traced.forEach((node,row)=>positions.set(node.id,{x:columnIndex*(NODE_WIDTH+COLUMN_GAP)+50,y:70+row*(NODE_HEIGHT+ROW_GAP),column:node.column}));
      unrelated.forEach((node,row)=>positions.set(node.id,{x:columnIndex*(NODE_WIDTH+COLUMN_GAP)+50,y:(hasTrace?unrelatedTop:70+traced.length*(NODE_HEIGHT+ROW_GAP))+row*(NODE_HEIGHT+ROW_GAP),column:node.column}));
    });
  }
  function splitLabel(label, limit = 26) {
    if (label.length <= limit) return [label];
    let index = Math.max(label.lastIndexOf('.', limit), label.lastIndexOf(' ', limit));
    if (index < 8) index = limit;
    else if (label[index] === '.') index++;
    const first = label.slice(0, index);
    const rest = label.slice(index).trim();
    return [first, rest.length > limit ? `${rest.slice(0, limit - 1)}…` : rest];
  }
  function kindLabel(node) {
    if(node.isConstructor || node.declarationKind==='constructor')return 'Constructor';
    if(kindLabels.has(node.id))return kindLabels.get(node.id);
    if (node.kind === 'callable' && node.file) {
      const declaration = files.get(node.file)?.source.split('\n').slice(node.startLine-1,node.endLine).join('\n') || '';
      if (/\b(?:class|object)\s+\w+/.test(declaration)){kindLabels.set(node.id,'Constructor');return 'Constructor';}
    }
    const label=({callable:'Function',parameter:'Parameter',value:'Variable',property:'Property',contract:'Contract',external:'External'})[node.kind] || node.kind;
    kindLabels.set(node.id,label);return label;
  }
  function displayName(node) {
    return node.kind === 'callable' && !node.label.includes('(') ? `${node.label}()` : node.label;
  }
  function relationshipVerb(edge) {
    return ({calls:'calls',constructs:'creates',produces:'returns',reads:'read by',derives:'used to derive',returns:'returned into',passes:'passes into',aliases:'same value',binds:'resolves to',captures:'used by',deferred:'runs later',owns:'owns',closes:'closes'})[edge.kind] || edge.kind;
  }
  function relationshipSentence(edge) {
    const source = nodes.get(edge.from), target = nodes.get(edge.to);
    const from = displayName(source);
    const to = displayName(target);
    const owner = nodes.get(target.ownerId);
    if (edge.kind === 'passes') {
      if (target.kind === 'parameter' && owner) return `${from} is passed as the ${target.label} parameter to ${displayName(owner)}.`;
      if (target.kind === 'property') return `${from} is stored in ${target.symbol || to}.`;
      return `${from} is passed into ${to}.`;
    }
    if (edge.kind === 'aliases') return `${from} and ${to} refer to the same value.`;
    if (edge.kind === 'captures') return `${to} uses ${from}.`;
    if (edge.kind === 'deferred') return `${from} invokes ${to} later, when this path runs.`;
    if (edge.kind === 'produces' && valueKinds.has(nodes.get(edge.from).kind)) return `${from} supplies ${to}.`;
    return `${from} ${relationshipVerb(edge)} ${to}.`;
  }
  function edgeGeometry(edge, index) {
    const from = positions.get(edge.from);
    const to = positions.get(edge.to);
    const fromNode=nodes.get(edge.from),toNode=nodes.get(edge.to);
    const fromShape=SDK_NODE_SHAPES.shapeKey(fromNode.kind,kindLabel(fromNode)==='Constructor');
    const toShape=SDK_NODE_SHAPES.shapeKey(toNode.kind,kindLabel(toNode)==='Constructor');
    const outgoing=currentGraph.visibleEdges.filter(candidate=>candidate.from===edge.from && positions.get(candidate.to).x>from.x);
    const outgoingIndex=Math.max(0,outgoing.findIndex(candidate=>candidate.id===edge.id));
    const y1 = from.y + (outgoing.length>2 ? 24+outgoingIndex/(outgoing.length-1)*43 : NODE_HEIGHT/2);
    const x1 = from.x + SDK_NODE_SHAPES.connectionX(fromShape,'right',y1-from.y,NODE_WIDTH,NODE_HEIGHT);
    const x2 = to.x + SDK_NODE_SHAPES.connectionX(toShape,'left',NODE_HEIGHT/2,NODE_WIDTH,NODE_HEIGHT);
    const y2 = to.y + NODE_HEIGHT / 2;
    if (x2 > x1) {
      const separation=outgoing.length>1?Math.min(15,outgoingIndex*2):0;
      const bend = Math.max(21, (x2-x1) / 2 + separation);
      return {d:`M${x1},${y1} C${x1+bend},${y1} ${x2-bend},${y2} ${x2},${y2}`, lx:(x1+x2)/2, ly:(y1+y2)/2 - 7};
    }
    const lane = 26 + index % 5 * 10;
    const top = Math.min(from.y, to.y) - lane;
    const targetX = to.x + NODE_WIDTH / 2;
    return {d:`M${x1},${y1} C${x1+lane},${y1} ${x1+lane},${top} ${x1},${top} L${targetX},${top} Q${targetX-8},${top} ${targetX-8},${top+8} L${targetX-8},${to.y}`, lx:(x1+targetX)/2, ly:top-5};
  }
  function renderGraph() {
    assignPositions();
    const visibleIds = new Set(currentGraph.visible.map(node => node.id));
    const hasTrace = currentGraph.upstream.size + currentGraph.downstream.size > 0;
    const classesForNode = id => [id === state.node ? 'is-selected' : currentGraph.pathNodes.has(id) ? 'is-path' : currentGraph.upstream.has(id) ? 'is-origin' : currentGraph.downstream.has(id) ? 'is-consumer' : hasTrace ? 'is-dim' : '',id===state.pinnedFunction?'is-pinned':''].filter(Boolean).join(' ');
    const classesForEdge = id => currentGraph.pathEdges.has(id) ? 'is-path' : currentGraph.upstreamEdges.has(id) ? 'is-origin' : currentGraph.downstreamEdges.has(id) ? 'is-consumer' : hasTrace ? 'is-dim' : '';
    $('#graph defs').innerHTML = ['default','origin','consumer'].map(kind => `<marker id="arrow-${kind}" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="var(--${kind === 'default' ? 'line' : kind})"/></marker>`).join('');
    const shownEdgeLabels = new Set();
    const edgeMarkup = currentGraph.visibleEdges.map((edge, index) => {
      const geometry = edgeGeometry(edge, index);
      const cls = classesForEdge(edge.id);
      const marker = cls === 'is-path' ? 'origin' : cls === 'is-origin' ? 'origin' : cls === 'is-consumer' ? 'consumer' : 'default';
      const labelKey = `${edge.from === state.node ? 'outgoing' : 'incoming'}:${edge.kind}:${edge.argumentName || ''}`;
      const showLabel = (edge.from === state.node || edge.to === state.node) && !shownEdgeLabels.has(labelKey);
      if(showLabel)shownEdgeLabels.add(labelKey);
      return `<g class="graph-edge ${cls}" data-edge-id="${esc(edge.id)}" data-kind="${esc(edge.kind)}" aria-hidden="true"><title>${esc(relationshipSentence(edge))}</title><path class="edge-line" d="${geometry.d}" marker-end="url(#arrow-${marker})"/>${showLabel?`<text class="edge-label" x="${geometry.lx}" y="${geometry.ly}" text-anchor="middle">${esc(relationshipVerb(edge))}</text>`:''}</g>`;
    }).join('');
    const nodeMarkup = currentGraph.visible.map(node => {
      const {x,y} = positions.get(node.id);
      const shape=SDK_NODE_SHAPES.shapeKey(node.kind,kindLabel(node)==='Constructor');
      const lines = splitLabel(displayName(node));
      const description = node.kind === 'external' || node.external ? 'External boundary' : node.file?.split('/').pop() || node.symbol || '';
      const metadata = description.length > 34 ? `${description.slice(0,31)}…` : description;
      return `<g class="graph-node ${classesForNode(node.id)}" data-node-id="${esc(node.id)}" data-key="${esc(node.key)}" data-kind="${esc(node.kind)}" data-shape="${shape}" role="button" tabindex="0" aria-label="${esc(`${node.label}, ${kindLabel(node)}${node.id === state.node ? ', selected' : ''}`)}" aria-pressed="${node.id === state.node}" transform="translate(${x},${y})"><title>${esc(displayName(node))}${node.description ? `\n${esc(node.description)}` : ''}</title>${SDK_NODE_SHAPES.markup(shape,NODE_WIDTH,NODE_HEIGHT)}<circle class="node-port" cx="${SDK_NODE_SHAPES.connectionX(shape,'left',43,NODE_WIDTH,NODE_HEIGHT)}" cy="43" r="3"/><circle class="node-port" cx="${SDK_NODE_SHAPES.connectionX(shape,'right',43,NODE_WIDTH,NODE_HEIGHT)}" cy="43" r="3"/><text class="node-kind" x="13" y="18">${esc(kindLabel(node))}${node.id === state.node ? ' · selected' : ''}</text><text class="node-title" x="13" y="${lines.length === 1 ? 43 : 37}">${lines.map((line,i) => `<tspan x="13" dy="${i ? 15 : 0}">${esc(line)}</tspan>`).join('')}</text><text class="node-meta" x="13" y="72">${esc(metadata)}</text></g>`;
    }).join('');
    $('#graph-world').innerHTML = edgeMarkup + nodeMarkup;
    $('#graph-empty').hidden = currentGraph.visible.length > 0;
    $('#graph-status').textContent = `${visibleIds.size} symbols · ${currentGraph.upstream.size} origins · ${currentGraph.downstream.size} consumers`;
    applyView();
  }
  function applyView() {
    $('#graph-world').setAttribute('transform', `translate(${view.x} ${view.y}) scale(${view.scale})`);
    $('#zoom-level').textContent = `${Math.round(view.scale * 100)}%`;
  }
  function zoom(factor, clientX, clientY) {
    const bounds = $('#graph').getBoundingClientRect();
    const x = clientX == null ? bounds.width / 2 : clientX - bounds.left;
    const y = clientY == null ? bounds.height / 2 : clientY - bounds.top;
    const nextScale = Math.min(2, Math.max(.025, view.scale * factor));
    const ratio = nextScale / view.scale;
    view = {x:x-(x-view.x)*ratio, y:y-(y-view.y)*ratio, scale:nextScale};
    applyView();
  }
  function fitGraph() {
    if (!currentGraph.visible.length) return;
    const rect = $('#graph').getBoundingClientRect();
    const spots = currentGraph.visible.map(node => positions.get(node.id));
    const minX = Math.min(...spots.map(spot => spot.x)) - 40;
    const minY = Math.min(...spots.map(spot => spot.y)) - 70;
    const maxX = Math.max(...spots.map(spot => spot.x)) + NODE_WIDTH + 40;
    const maxY = Math.max(...spots.map(spot => spot.y)) + NODE_HEIGHT + 60;
    const scale = Math.max(.025, Math.min(.95, rect.width/(maxX-minX), (rect.height-35)/(maxY-minY)));
    view = {x:(rect.width-(maxX-minX)*scale)/2-minX*scale, y:(rect.height-35-(maxY-minY)*scale)/2-minY*scale, scale};
    applyView();
  }
  function focusSelected() {
    const spot = positions.get(state.node);
    if (!spot) return;
    const rect = $('#graph').getBoundingClientRect();
    const scale = rect.width < 500 ? .94 : 1;
    view = {x:rect.width*(rect.width<500?.54:.68)-(spot.x+NODE_WIDTH/2)*scale, y:Math.min(rect.height*.26,145)-(spot.y+NODE_HEIGHT/2)*scale, scale};
    applyView();
  }
  function syntax(line) {
    const pattern = /(\/\/.*$|"(?:[^"\\]|\\.)*"|\b(?:package|import|class|interface|object|fun|val|var|private|internal|public|override|suspend|return|when|if|else|is|in|try|catch|finally|throw|data|sealed|enum|expect|actual|true|false|null|constructor|by)\b)/g;
    let cursor = 0;
    let result = '';
    for (const match of line.matchAll(pattern)) {
      result += esc(line.slice(cursor, match.index));
      result += match[0].startsWith('//') ? `<span class="code-comment">${esc(match[0])}</span>` : match[0].startsWith('"') ? esc(match[0]) : `<span class="code-keyword">${esc(match[0])}</span>`;
      cursor = match.index + match[0].length;
    }
    return result + esc(line.slice(cursor));
  }
  function codeLines(file, start, end, selectedStart, selectedEnd) {
    return file.source.split('\n').slice(start-1,end).map((line,index) => {
      const number = start + index;
      return `<div class="code-line${number >= selectedStart && number <= selectedEnd ? ' is-evidence' : ''}" data-line="${number}"><span class="line-number">${number}</span><span class="line-text">${syntax(line)}</span></div>`;
    }).join('');
  }
  function evidenceButton(edge) {
    if (!edge.evidence?.file || !files.has(edge.evidence.file)) return '';
    return `<button data-evidence="${esc(edge.id)}" aria-label="Read evidence for ${esc(edge.kind)} relationship">Source :${esc(edge.evidence.startLine)}</button>`;
  }
  function connectionList(list, direction) {
    if (!list.length) return `<p class="empty-hint">No mapped ${direction === 'incoming' ? 'incoming' : 'outgoing'} ${currentGraph.effectiveMode === 'value' ? 'value' : 'call'} connection in this context.</p>`;
    return `<ul class="connection-list">${list.map(edge => {
      const node = nodes.get(direction === 'incoming' ? edge.from : edge.to);
      if (!node) return '';
      return `<li><p class="connection-sentence" data-relationship="${esc(edge.id)}">${esc(relationshipSentence(edge))}</p><button class="connection-select" data-select-node="${esc(node.id)}">Select ${esc(displayName(node))}</button><div class="connection-meta"><span>${esc(kindLabel(node))}</span>${edge.kind === 'deferred' || edge.phase === 'deferred' ? '<span class="deferred-tag">deferred</span>' : ''}${evidenceButton(edge)}</div>${edge.condition ? `<p class="connection-description">${esc(edge.condition)}</p>` : ''}${edge.description ? `<details class="relationship-notes"><summary>Why this connection?</summary><p class="connection-description">${esc(edge.description)}</p></details>`:''}</li>`;
    }).join('')}</ul>`;
  }
  function originName(node) {
    let owner = nodes.get(node.ownerId);
    if (owner?.kind === 'parameter' && owner.ownerId) owner = nodes.get(owner.ownerId) || owner;
    return `${displayName(node)}${owner ? ` · ${displayName(owner)}` : ''}`;
  }
  function originTree(node) {
    if (currentGraph.effectiveMode !== 'value' || !valueKinds.has(node.kind)) return '';
    const incoming = new Map();
    currentGraph.contextualEdges.filter(edge=>edge.traces.includes('value') && edge.upstream!==false).forEach(edge=>{
      if(!incoming.has(edge.to))incoming.set(edge.to,[]);
      incoming.get(edge.to).push(edge);
    });
    if (!incoming.has(node.id)) return '';
    const seen = new Set([node.id]);
    let count = 0, truncated = false;
    function branches(id, depth) {
      const sourceEdges = incoming.get(id) || [];
      if (!sourceEdges.length) return '';
      if(depth > 12 || count >= 36){truncated=true;return '';}
      return `<ul>${sourceEdges.map(edge=>{
        if(count>=36){truncated=true;return '';}
        count++;
        const source=nodes.get(edge.from),repeated=seen.has(source.id);
        seen.add(source.id);
        const leaf=!incoming.has(source.id);
        return `<li data-origin-step="${esc(source.key)}"><p class="origin-relation">${esc(relationshipVerb(edge))}${edge.argumentName ? ` · ${esc(edge.argumentName)}` : ''}</p><button data-select-node="${esc(source.id)}">${esc(originName(source))}</button><div class="connection-meta"><span>${esc(kindLabel(source))}${leaf ? ' · mapped origin' : ''}</span>${evidenceButton(edge)}${source.file ? `<button data-source="${esc(source.file)}" data-start="${source.startLine}" data-end="${source.endLine}">${esc(source.file.split('/').pop())}:${source.startLine}</button>` : ''}</div>${repeated ? '<p class="empty-hint">Same source shown above.</p>' : branches(source.id,depth+1)}</li>`;
      }).join('')}</ul>`;
    }
    const tree=branches(node.id,0);
    return `<section class="inspector-section origin-section"><h3>Origin chain</h3><p class="empty-hint">Read from this value back to where it was supplied.</p><div class="origin-tree"><p class="origin-current">${esc(originName(node))}</p>${tree}</div>${truncated ? '<p class="empty-hint">Open the full tree to expand every available branch in either direction.</p>' : ''}</section>`;
  }
  function inputOrigins(node, inputNodes) {
    if (!inputNodes.length) return '';
    return `<section class="inspector-section input-origins" id="input-origins"><h3>${kindLabel(node) === 'Constructor' ? 'Constructor' : 'Function'} inputs</h3><p class="empty-hint">Choose an input to follow the value supplied by its caller.</p><ul>${inputNodes.map(input=>{
      const supplied=currentGraph.contextualEdges.filter(edge=>edge.to===input.id && edge.traces.includes('value'));
      return `<li data-input-key="${esc(input.key)}"><strong>${esc(input.label)}${input.typeName ? `: ${esc(input.typeName)}` : ''}</strong>${supplied.length ? supplied.map(edge=>`<p class="argument-binding">${edge.argumentExpression ? `<code>${esc(input.label)} = ${esc(edge.argumentExpression)}</code>` : `Supplied by ${esc(displayName(nodes.get(edge.from)))}`}</p><div class="connection-meta">${evidenceButton(edge)}</div>`).join('') : '<p class="empty-hint">No caller value is mapped in this context.</p>'}<button class="trace-input" data-trace-origin="${esc(input.id)}">Trace origin</button><button class="trace-input" data-open-tree="${esc(input.id)}">Full tree</button></li>`;
    }).join('')}</ul></section>`;
  }
  function pinPanel(node) {
    const pinned=nodes.get(state.pinnedFunction);
    return `<div class="pin-controls">${node.kind==='callable' ? `<button id="pin-function" ${pinned?.id===node.id?'disabled':''}>${pinned?.id===node.id?'Function pinned':'Pin function as start'}</button>` : ''}${!pinned && node.kind==='callable' ? '<p class="usage-note">Pin this function, then select a nested value to see their connection as a list.</p>' : ''}</div>`;
  }
  function pairPanel(node) {
    const pinned=nodes.get(state.pinnedFunction);
    if(!pinned)return '';
    let body;
    if(!valueKinds.has(node.kind))body='<p class="usage-note">Now select a variable, parameter or property in the graph, search, or inputs list.</p>';
    else if(!currentGraph.pair?.nodeIds.length)body='<p class="usage-note">No directed connection is mapped between this function and value in the selected provider and platform. The creator and consumer lists below remain available.</p>';
    else {
      const pair=currentGraph.pair;
      body=`<p class="usage-note">${pair.reverse?'This value leads to the pinned function.':'One shortest mapped path from the pinned function to this value.'} Each step follows a source-backed connection.</p><ol>${pair.nodeIds.map((id,index)=>{
        const step=nodes.get(id),edge=edges.get(pair.edgeIds[index]);
        return `<li class="path-step" data-path-node="${esc(id)}"><button class="usage-method" data-select-node="${esc(id)}">${esc(displayName(step))}</button><p class="usage-note">${esc(step.ownerName || step.file?.split('/').pop() || 'External boundary')}</p>${edge ? `<div class="path-relation">${esc(relationshipSentence(edge))}${edge.condition ? ` ${esc(edge.condition)}`:''}${edge.phase ? ` · ${esc(edge.phase)}`:''}<div class="connection-meta">${evidenceButton(edge)}</div></div>`:''}</li>`;
      }).join('')}</ol>`;
    }
    return `<section class="inspector-section path-panel"><h3>Pinned function → selected value</h3><p class="usage-note">Start: ${esc(displayName(pinned))}</p><div id="connection-path">${body}</div></section>`;
  }
  function valueOwnership(node) {
    if(!valueKinds.has(node.kind))return '';
    const valueEdges=currentGraph.contextualEdges.filter(edge=>edge.traces.includes('value'));
    const valueIn=new Map(),valueOut=new Map();
    valueEdges.forEach(edge=>{if(!valueIn.has(edge.to))valueIn.set(edge.to,[]);valueIn.get(edge.to).push(edge);if(!valueOut.has(edge.from))valueOut.set(edge.from,[]);valueOut.get(edge.from).push(edge);});
    const creators=new Map(),consumers=new Map();
    const identity=new Set(['aliases','passes','binds']);
    const visited=new Set(),queue=[node.id];
    // Aliases/parameters carry the object. Stop at its producing callable;
    // constructor inputs and arbitrary captures are dependencies, not creators.
    for(let i=0;i<queue.length;i++) {
      const id=queue[i];if(visited.has(id))continue;visited.add(id);
      const producing=(valueIn.get(id)||[]).filter(edge=>edge.kind==='produces' && edge.upstream!==false);
      if(producing.length){producing.forEach(edge=>creators.set(edge.from,{node:nodes.get(edge.from),edge,role:'Creates / returns this value'}));continue;}
      const previous=(valueIn.get(id)||[]).filter(edge=>edge.upstream!==false && identity.has(edge.kind));
      if(!previous.length){const origin=nodes.get(id);creators.set(id,{node:origin,role:origin.external?'Supplied outside this snapshot':'Value supplied here'});}
      else previous.forEach(edge=>queue.push(edge.from));
    }
    const used=new Set(),forward=[node.id];
    for(let i=0;i<forward.length;i++) {
      const id=forward[i];if(used.has(id))continue;used.add(id);
      for(const edge of (valueOut.get(id)||[]).filter(item=>item.downstream!==false)) {
        const target=nodes.get(edge.to);
        if(['captures','reads','derives','returns'].includes(edge.kind))consumers.set(target.id,{node:target,edge,role:edge.kind==='derives'?'Derived using this value':edge.kind==='returns'?'Receives this returned value':'Uses this value'});
        else if(identity.has(edge.kind)) {
          consumers.set(target.id,{node:target,edge,role:target.kind==='parameter'?'Receives this value as an argument':'Receives / retains this value'});
          if(valueKinds.has(target.kind))forward.push(target.id);
        }
      }
    }
    function ownerFor(record,isCreator) {
      const item=record.node;
      if(item.kind==='callable' && kindLabel(item)==='Constructor')return {name:item.symbol,kind:'class'};
      if(isCreator && item.kind==='callable' && item.ownerKind==='file')return {name:item.symbol,kind:'function'};
      return {name:item.ownerName || (item.external?'External boundary':item.file?.split('/').pop() || item.symbol),kind:item.ownerKind || (item.external?'external':'file')};
    }
    function groupsMarkup(records,isCreator) {
      if(!records.size)return `<p class="usage-note">No ${isCreator?'creator':'consumer'} is mapped for this value in this context.</p>`;
      const groups=new Map();
      records.forEach(record=>{const owner=ownerFor(record,isCreator),key=`${owner.kind}:${owner.name}`;if(!groups.has(key))groups.set(key,{owner,records:[]});groups.get(key).records.push(record);});
      return [...groups.values()].map(group=>`<section class="usage-owner" data-owner="${esc(group.owner.name)}"><details ${isCreator?'open':''}><summary>${esc(group.owner.name)} <span>${esc(group.owner.kind)} · ${group.records.length} ${isCreator?'origins':'uses'}</span></summary><ul class="usage-items">${group.records.map(record=>`<li data-usage-node="${esc(record.node.id)}"><button class="usage-method" data-select-node="${esc(record.node.id)}">${esc(record.node.symbol || record.node.label)}</button><p class="usage-note">${esc(record.role)}</p><div class="connection-meta">${record.edge?evidenceButton(record.edge):''}${record.node.file?`<button data-source="${esc(record.node.file)}" data-start="${record.node.startLine}" data-end="${record.node.endLine}">Source :${record.node.startLine}</button>`:''}</div></li>`).join('')}</ul></details></section>`).join('');
    }
    return `<section class="inspector-section usage-summary"><h3>Created by / supplied by</h3><div id="value-creators">${groupsMarkup(creators,true)}</div><h3>Used by / received by</h3><p class="usage-note">Grouped by the owning class, function or file. Includes uses through forwarded aliases; other constructor inputs are excluded.</p><div id="value-consumers">${groupsMarkup(consumers,false)}</div></section>`;
  }
  function renderInspector() {
    const node = nodes.get(state.node);
    if (!node) { $('#node-detail').innerHTML = '<p class="empty-hint">Select a mapped symbol to inspect its source and connections.</p>'; return; }
    const file = files.get(node.file);
    const incoming = currentGraph.tracingEdges.filter(edge => edge.to === node.id && edge.upstream!==false);
    const outgoing = currentGraph.tracingEdges.filter(edge => edge.from === node.id && edge.downstream!==false);
    const other = currentGraph.contextualEdges.filter(edge => (edge.from === node.id || edge.to === node.id) && !edge.traces?.includes(currentGraph.effectiveMode));
    const children = currentGraph.contextNodes.filter(child => child.ownerId === node.id);
    const inputNodes = Array.isArray(node.signatureParameterIds) ? node.signatureParameterIds.map(id=>nodes.get(id)).filter(child=>child && contextMatches(child,state.context)) : node.kind === 'callable' ? children.filter(child=>child.kind==='parameter' || child.kind==='property' && currentGraph.contextualEdges.some(edge=>edge.to===child.id && edge.kind==='passes')) : [];
    const inputIds = new Set(inputNodes.map(input=>input.id));
    const remainingChildren = children.filter(child=>!inputIds.has(child.id));
    const owner = nodes.get(node.ownerId);
    const alternateMode = currentGraph.effectiveMode === 'value' ? 'call' : 'value';
    const alternateAvailable = other.some(edge => edge.traces.includes(alternateMode));
    const modePrompt = !incoming.length && !outgoing.length && alternateAvailable ? `<p class="mode-prompt">This ${kindLabel(node).toLowerCase()} has ${alternateMode === 'call' ? 'function-call' : 'value-flow'} connections. <button data-trace-mode="${alternateMode}">Show ${alternateMode === 'call' ? 'function calls' : 'value flow'}</button></p>` : '';
    const roleDescription = kindLabel(node) === 'Constructor' ? 'A constructor: creates an object. Value flow follows the values supplied to it and the instance it creates.' : ({callable:'A function: follow its calls, or follow its inputs and result in Value flow.',parameter:`An input${owner ? ` to ${displayName(owner)}` : ' supplied by a caller'}. Follow its origin to see the value passed here.`,value:'A variable: a local name for a value. Follow where the value is created and used.',property:'A property: a value stored on an object and used by its methods.',contract:'A contract: a named interface resolved to an implementation.',external:'An external boundary: implementation outside this source snapshot.'})[node.kind];
    const start = Math.max(1, node.startLine || 1);
    const end = Math.min(file?.lines || Infinity, node.endLine || start);
    const previewStart = Math.max(1, start - 1);
    const previewEnd = Math.min(file?.lines || 1, start + 7);
    const source = file ? `<section class="inspector-section"><h3>Source</h3><button class="file-link" data-source="${esc(file.path)}" data-start="${start}" data-end="${end}">${esc(file.name)}:${start}</button><p class="file-module">${esc(file.module)} · ${esc(file.sourceSet)}</p><p class="source-reference" data-source-reference>${esc(file.path)}:${start}</p><div class="source-preview" tabindex="0" aria-label="Selected source excerpt">${codeLines(file,previewStart,previewEnd,start,end)}</div><div class="source-actions"><button class="text-button" data-source="${esc(file.path)}" data-start="${start}" data-end="${end}">Open full source ↗</button><button class="text-button" data-copy-path="${esc(file.path)}:${start}">Copy path:line</button></div></section>` : `<p class="boundary-note">${node.external || node.kind === 'external' ? 'External dependency boundary. Its implementation is outside this repository snapshot.' : 'This node represents an explicitly mapped contract or relationship.'}</p>`;
    const originMarkup = originTree(node);
    const members = remainingChildren.length ? `<section class="inspector-section"><h3>Members &amp; locals <span class="count">${remainingChildren.length}</span></h3><ul class="connection-list">${remainingChildren.map(child=>`<li><button class="connection-select" data-select-node="${esc(child.id)}">Select ${esc(child.label)}</button><div class="connection-meta"><span>${esc(kindLabel(child))}</span>${child.file ? `<button data-source="${esc(child.file)}" data-start="${child.startLine || 1}" data-end="${child.endLine || child.startLine || 1}">Source :${child.startLine || 1}</button>`:''}</div></li>`).join('')}</ul></section>` : '';
    $('#node-detail').innerHTML = `<span class="node-type">${esc(kindLabel(node))}</span><h2 id="selected-node-title">${esc(displayName(node))}</h2><p id="selected-node-symbol" class="symbol-identity">${esc(node.symbol || node.label)}</p>${owner ? `<button class="owner-link" data-select-node="${esc(owner.id)}">Declared in ${esc(displayName(owner))}</button>`:''}<p class="node-role">${esc(roleDescription)}</p><p class="node-description">${esc(node.description || '')}</p><div class="symbol-actions"><button data-open-tree="${esc(node.id)}">Open full tree</button><button class="text-button" data-center-selection>Center on graph</button></div><p class="trace-summary">${currentGraph.effectiveMode === 'value' ? 'Value flow' : 'Function calls'} · ${currentGraph.upstream.size} origins · ${currentGraph.downstream.size} consumers</p>${inputOrigins(node,inputNodes)}${pinPanel(node)}${valueOwnership(node)}${pairPanel(node)}${modePrompt}${originMarkup ? `<details class="inspector-section" ${state.mode==='value' && state.direction==='upstream'?'open':''}><summary>Origin summary</summary>${originMarkup}</details>` : ''}<section class="inspector-section"><h3>${currentGraph.effectiveMode === 'value' ? 'Where this comes from' : 'Called by'} <span class="count">${incoming.length}</span></h3>${connectionList(incoming,'incoming')}</section><section class="inspector-section"><h3>${currentGraph.effectiveMode === 'value' ? 'Where this goes' : 'Calls / creates'} <span class="count">${outgoing.length}</span></h3>${connectionList(outgoing,'outgoing')}</section>${members}${source}${other.length ? `<details class="inspector-section"><summary>Other connections (${other.length})</summary><p class="empty-hint">These connections are outside the current trace mode.</p><ul class="connection-list">${other.map(edge => { const peer = nodes.get(edge.from === node.id ? edge.to : edge.from); return `<li><p class="connection-sentence" data-relationship="${esc(edge.id)}">${esc(relationshipSentence(edge))}</p><button class="connection-select" data-select-node="${esc(peer.id)}">Select ${esc(displayName(peer))}</button><div class="connection-meta">${evidenceButton(edge)}</div></li>`; }).join('')}</ul></details>` : ''}`;
  }
  function renderSearch() {
    const query = $('#search').value.trim().toLowerCase();
    $('#search-results').toggleAttribute('hidden',!query);
    $('#journey-navigation').toggleAttribute('hidden',!!query);
    if (!query) { $('#symbol-results').replaceChildren();$('#file-results').replaceChildren();$('#search-count').textContent='';return; }
    const matchingNodes = DATA.nodes.filter(node => contextMatches(node,state.context) && `${node.label} ${node.symbol || ''} ${node.key || ''} ${node.file || ''}`.toLowerCase().includes(query));
    const matchingFiles = DATA.files.filter(file => file.path.startsWith('sdk/') && !file.test && !file.binary && (`${file.path}\n${file.source}`).toLowerCase().includes(query));
    $('#search-count').textContent = `${matchingNodes.length} symbols · ${matchingFiles.length} source files`;
    $('#symbol-results').innerHTML = matchingNodes.length ? `<h3>Mapped symbols · this context</h3>${matchingNodes.slice(0,searchLimit).map(node => `<button class="search-result" data-select-node="${esc(node.id)}"><strong>${esc(node.label)}</strong><small>${esc(node.kind)} · ${esc(node.file?.split('/').pop() || 'external')}</small></button>`).join('')}` : '<p class="empty-hint">No mapped symbols match. Source files below may contain unmapped code.</p>';
    $('#file-results').innerHTML = (matchingFiles.length ? `<h3>SDK source · all platforms</h3>${matchingFiles.slice(0,searchLimit).map(file => `<button class="search-result" data-source="${esc(file.path)}" data-find="${esc(query)}"><strong>${esc(file.name)}</strong><small>${esc(file.module)} · ${esc(file.sourceSet)}</small></button>`).join('')}` : '<p class="empty-hint" style="margin-top:15px">No matching SDK source files.</p>') + (Math.max(matchingNodes.length,matchingFiles.length)>searchLimit ? '<button id="search-more" class="search-more">Show more results</button>' : '');
  }
  function render() {
    currentGraph = calculateGraph();
    renderControls();
    if(state.view==='graph')renderGraph();
    else if(relationTree)relationTree.render(state.treeRoot || state.node,state.context,{selectedNodeId:state.node,initialDepth:1});
    renderInspector();
    renderSearch();
  }
  async function copyPath(path) {
    try { await navigator.clipboard.writeText(path); notify('Repository path and line copied.'); }
    catch (_) {
      const displayed = [...$$('dialog[open] [data-source-reference]'),...$$('[data-source-reference]')].find(element => element.textContent === path && element.getClientRects().length);
      if (displayed) { const range=document.createRange();range.selectNodeContents(displayed);const selection=window.getSelection();selection.removeAllRanges();selection.addRange(range);notify('Clipboard unavailable. Path and line selected; press Ctrl+C to copy.'); }
      else notify(`Copy this reference: ${path}`);
    }
  }
  function openSource(path, start = 1, end = start, find = '') {
    const file = files.get(path);
    if (!file) { notify('This source is outside the embedded snapshot.'); return; }
    selectedFile = file;
    sourceMatch = -1;
    evidenceRange = {start:Math.max(1,Number(start)||1), end:Math.max(Number(start)||1,Number(end)||Number(start)||1)};
    $('#source-title').textContent = file.name;
    $('#source-path').textContent = `${file.path}:${evidenceRange.start}`;
    $('#source-find').value = find;
    $('#source-status').textContent = `${file.lines} lines · ${file.module} · ${file.sourceSet}`;
    $('#source-code').innerHTML = file.binary ? '<p>This resource is listed but its binary content is not embedded.</p>' : codeLines(file,1,file.source.split('\n').length,evidenceRange.start,evidenceRange.end);
    if (!$('#source-dialog').open) $('#source-dialog').showModal();
    if (find) findSource();
    else requestAnimationFrame(() => scrollToSourceLine(evidenceRange.start));
  }
  function scrollToSourceLine(line) {
    const target = $(`#source-code [data-line="${line}"]`);
    if (target) $('#source-code').scrollTop = target.offsetTop - $('#source-code').offsetTop - 72;
  }
  function findSource() {
    const query = $('#source-find').value.toLowerCase();
    $$('#source-code .is-match').forEach(line => line.classList.remove('is-match'));
    if (!selectedFile) return;
    $('#source-path').textContent = `${selectedFile.path}:${evidenceRange.start}`;
    if (!query) { sourceMatch=-1;$('#source-status').textContent=`${selectedFile.lines} lines · ${selectedFile.module} · ${selectedFile.sourceSet}`;return; }
    const matches = selectedFile.source.split('\n').flatMap((line,index) => line.toLowerCase().includes(query) ? [index + 1] : []);
    if (!matches.length) { $('#source-status').textContent = 'No matches in this file.'; return; }
    sourceMatch = matches.find(line => line > sourceMatch) ?? matches[0];
    $('#source-path').textContent = `${selectedFile.path}:${sourceMatch}`;
    $(`#source-code [data-line="${sourceMatch}"]`)?.classList.add('is-match');
    scrollToSourceLine(sourceMatch);
    $('#source-status').textContent = `Match ${matches.indexOf(sourceMatch)+1} of ${matches.length} · line ${sourceMatch}`;
  }
  function setPane(pane, open) {
    const previousPane = activePane;
    const overlayAvailable = pane === 'navigator' ? matchMedia('(max-width:700px)').matches : matchMedia('(max-width:1050px)').matches;
    const nextPane = open && overlayAvailable ? pane : null;
    if (nextPane && !activePane) paneReturnFocus = document.activeElement;
    activePane = nextPane;
    ['navigator','inspector'].forEach(id => {
      const active = id === nextPane;
      $(`#${id}`).classList.toggle('is-open',active);
      $(`#open-${id}`).setAttribute('aria-expanded',String(active));
      $(`#${id}`).inert = !!nextPane && !active;
      if (active) { $(`#${id}`).setAttribute('role','dialog');$(`#${id}`).setAttribute('aria-modal','true'); }
      else { $(`#${id}`).removeAttribute('role');$(`#${id}`).removeAttribute('aria-modal'); }
    });
    $('.topbar').inert = !!nextPane;
    $('#main').inert = !!nextPane;
    if (nextPane) $(`#${nextPane} .mobile-close`)?.focus();
    else if (previousPane) {
      const target=paneReturnFocus?.isConnected && paneReturnFocus!==document.body && paneReturnFocus!==document.documentElement && paneReturnFocus.getClientRects().length && !paneReturnFocus.closest('[inert]') ? paneReturnFocus : $$('.graph-node').find(element=>element.dataset.nodeId===state.node) || $(`#open-${previousPane}`);
      target?.focus();paneReturnFocus=null;
    }
  }
  $('#provider').addEventListener('change', event => switchContext(event.target.value,$('#platform').value));
  $('#platform').addEventListener('change', event => switchContext($('#provider').value,event.target.value));
  $('#trace-mode').addEventListener('change', event => navigate({mode:event.target.value}));
  $('#direction').addEventListener('change', event => navigate({direction:event.target.value}));
  $('#show-values').addEventListener('change', event => { extraNodes.clear();navigate({values:event.target.checked}); });
  $('#search').addEventListener('input', () => { searchLimit=24; renderSearch(); });
  $('#history-back').addEventListener('click', () => history.back());
  $('#history-forward').addEventListener('click', () => history.forward());
  $('#zoom-in').addEventListener('click', () => zoom(1.2));
  $('#zoom-out').addEventListener('click', () => zoom(1/1.2));
  $('#fit-graph').addEventListener('click', fitGraph);
  $('#focus-node').addEventListener('click', focusSelected);
  $('#tree-view').addEventListener('click',()=>navigate({view:'tree',treeRoot:state.node}));
  $('#graph-view').addEventListener('click',()=>navigate({view:'graph'},{focus:true}));
  $('#tree-root-selected').addEventListener('click',()=>navigate({treeRoot:state.node}));
  $('#clear-function').addEventListener('click',()=>navigate({pinnedFunction:''}));
  $('#arrange-trace').addEventListener('click', () => {
    positionKey = '';revealedNodes.clear();
    render();focusSelected();
    notify('Path arranged around the selection. Further clicks keep this layout.');
  });
  $('#expand-node').addEventListener('click', () => {
    const related = currentGraph.contextualEdges.filter(edge => edge.from === state.node || edge.to === state.node);
    related.forEach(edge => { extraNodes.add(edge.from); extraNodes.add(edge.to); });
    currentGraph.contextNodes.filter(node => node.ownerId === state.node).forEach(node => extraNodes.add(node.id));
    render();
    notify(`Included ${new Set(related.flatMap(edge => [edge.from,edge.to]).filter(id => id !== state.node)).size} direct connections.`);
  });
  $('#source-next').addEventListener('click',findSource);
  $('#source-find').addEventListener('input', () => { sourceMatch=-1; findSource(); });
  $('#source-find').addEventListener('keydown', event => { if (event.key === 'Enter') findSource(); });
  $('#source-copy').addEventListener('click', () => { if (selectedFile) copyPath(`${selectedFile.path}:${sourceMatch > 0 ? sourceMatch : evidenceRange.start}`); });
  $('#close-source').addEventListener('click', () => $('#source-dialog').close());
  $('#close-coverage').addEventListener('click', () => $('#coverage-dialog').close());
  $('#show-coverage').addEventListener('click', () => {
    const groups=new Map();
    (DATA.coverage || []).forEach(item=>{const service=item.file.split('/').pop().replace(/\.kt$/,'');if(!groups.has(service))groups.set(service,[]);groups.get(service).push(item);});
    $('#coverage-content').innerHTML = `<p class="coverage-summary">${DATA.nodes.length} symbols · ${DATA.edges.length} relationships · ${DATA.files.length} source files</p>${DATA.index?`<p class="coverage-summary">Source index: ${DATA.index.callableCount ?? DATA.index.parsedCallables} callables; ${DATA.index.representedParameters}/${DATA.index.formalParameters} formal parameters; ${DATA.index.sourceFileCount ?? DATA.index.sourceFiles} Kotlin files. ${DATA.index.unresolvedCount ?? DATA.index.unresolvedCalls} calls remain explicit unresolved/external boundaries.</p>`:''}${[...groups].map(([service,items])=>`<section class="coverage-group"><h3>${esc(service)} <span class="count">${items.length} operations</span></h3><ul>${items.map(item=>{const node=nodes.get(item.nodeId);const journey=DATA.journeys.find(candidate=>candidate.id===item.journey);return `<li>${node ? `<button class="coverage-operation" data-coverage-node="${esc(node.id)}" data-coverage-journey="${esc(item.journey)}">${esc(item.operation)} →</button><span class="coverage-journey">${esc(journey?.label || '')}</span>` : `<strong>${esc(item.operation)}</strong><p class="coverage-exclusion">${esc(item.exclusion || 'Source is available; this operation has no mapped path.')}</p>`}</li>`;}).join('')}</ul></section>`).join('')}`;
    $('#coverage-dialog').showModal();
  });
  ['navigator','inspector'].forEach(id => $(`#open-${id}`).addEventListener('click', () => setPane(id,!$(`#${id}`).classList.contains('is-open'))));
  document.addEventListener('click', event => {
    const target = event.target;
    const journeyButton = target.closest('[data-journey]');
    if (journeyButton) {
      const journey = journeyButton.dataset.journey;
      const chosen = DATA.nodes.find(node => journeyMatches(node,journey) && contextMatches(node,state.context) && !valueKinds.has(node.kind)) || DATA.nodes.find(node => journeyMatches(node,journey) && contextMatches(node,state.context));
      navigate({journey,node:chosen?.id || state.node,mode:'auto'},{focus:true});
      setPane('navigator',false);
      if(matchMedia('(min-width:701px)').matches) $$('#journeys [data-journey]').find(button=>button.dataset.journey===journey)?.focus();
    }
    const selection = target.closest('[data-select-node]');
    if (selection) { selectNode(selection.dataset.selectNode,{inspect:true}); }
    if (target.closest('[data-center-selection]')) { setPane('inspector',false);navigate({view:'graph'},{focus:true}); }
    if (target.closest('#pin-function')) navigate({pinnedFunction:state.node});
    const openTree=target.closest('[data-open-tree]');
    if(openTree){setPane('inspector',false);navigate({view:'tree',treeRoot:openTree.dataset.openTree,node:openTree.dataset.openTree});}
    const traceInput = target.closest('[data-trace-origin]');
    if (traceInput) navigate({node:traceInput.dataset.traceOrigin,mode:'value',direction:'upstream'},{inspect:true});
    const traceChoice = target.closest('[data-trace-mode]');
    if (traceChoice) navigate({mode:traceChoice.dataset.traceMode});
    const coverage = target.closest('[data-coverage-node]');
    if (coverage) {
      const node=nodes.get(coverage.dataset.coverageNode);
      const context=contextMatches(node,state.context)?state.context:node.contextIds[0];
      $('#coverage-dialog').close();
      navigate({context,journey:coverage.dataset.coverageJourney,node:node.id,mode:'auto'},{focus:true,inspect:true});
    }
    const source = target.closest('[data-source]');
    if (source) openSource(source.dataset.source,source.dataset.start,source.dataset.end,source.dataset.find || '');
    const evidence = target.closest('[data-evidence]');
    if (evidence) { const edge = edges.get(evidence.dataset.evidence); if (edge?.evidence) openSource(edge.evidence.file,edge.evidence.startLine,edge.evidence.endLine); }
    const copy = target.closest('[data-copy-path]');
    if (copy) copyPath(copy.dataset.copyPath);
    const close = target.closest('[data-close-pane]');
    if (close) setPane(close.dataset.closePane,false);
    if (target.closest('#search-more')) { searchLimit += 24; renderSearch(); }
  });
  $('#graph').addEventListener('click', event => {
    if (moved) { moved=false; return; }
    const node = event.target.closest('[data-node-id]');
    if (node) selectNode(node.dataset.nodeId);
  });
  $('#graph').addEventListener('wheel', event => { event.preventDefault(); zoom(Math.exp(-event.deltaY*.0015),event.clientX,event.clientY); },{passive:false});
  $('#graph').addEventListener('pointerdown', event => {
    if (event.button !== 0) return;
    drag={x:event.clientX,y:event.clientY,viewX:view.x,viewY:view.y,pointer:event.pointerId};
    moved=false;
  });
  $('#graph').addEventListener('pointermove', event => {
    if (!drag || event.pointerId !== drag.pointer) return;
    const dx=event.clientX-drag.x,dy=event.clientY-drag.y;
    if (Math.abs(dx)+Math.abs(dy)>5) { moved=true;$('#graph').setPointerCapture(event.pointerId); }
    if (moved) { view.x=drag.viewX+dx;view.y=drag.viewY+dy;$('#graph').classList.add('panning');applyView(); }
  });
  function stopDrag() { drag=null;$('#graph').classList.remove('panning'); }
  $('#graph').addEventListener('pointerup',stopDrag);
  $('#graph').addEventListener('pointercancel',stopDrag);
  $('#graph').addEventListener('keydown', event => {
    const node=event.target.closest('[data-node-id]');
    if (node && (event.key==='Enter' || event.key===' ')) {
      event.preventDefault();selectNode(node.dataset.nodeId);
      requestAnimationFrame(() => {
        restoringGraphFocus=true;
        $$('.graph-node').find(item => item.dataset.nodeId===state.node)?.focus({preventScroll:true});
        restoringGraphFocus=false;
      });
      return;
    }
    if (event.key==='+' || event.key==='=') { event.preventDefault();zoom(1.2); }
    else if (event.key==='-') { event.preventDefault();zoom(1/1.2); }
    else if (event.key.toLowerCase()==='f') { event.preventDefault();fitGraph(); }
    else if (['ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(event.key)) { event.preventDefault();view.x += event.key==='ArrowLeft' ? 70 : event.key==='ArrowRight' ? -70 : 0;view.y += event.key==='ArrowUp' ? 70 : event.key==='ArrowDown' ? -70 : 0;applyView(); }
  });
  $('#graph').addEventListener('focusin', event => {
    if (!keyboardNavigation || restoringGraphFocus) return;
    const node=event.target.closest('[data-node-id]');
    if (!node) return;
    const point=positions.get(node.dataset.nodeId),rect=$('#graph').getBoundingClientRect();
    const x=point.x*view.scale+view.x,y=point.y*view.scale+view.y;
    if (x<0 || x+NODE_WIDTH*view.scale>rect.width || y<0 || y+NODE_HEIGHT*view.scale>rect.height-60) { view.x=rect.width/2-(point.x+NODE_WIDTH/2)*view.scale;view.y=rect.height/2-(point.y+NODE_HEIGHT/2)*view.scale;applyView(); }
  });
  document.addEventListener('pointerdown', () => {keyboardNavigation=false;},{capture:true});
  document.addEventListener('keydown', event => {keyboardNavigation=event.key==='Tab';},{capture:true});
  document.addEventListener('keydown', event => {
    if(event.key==='Escape'){
      const dialogs=$$('dialog[open]');
      if(dialogs.length){event.preventDefault();dialogs[dialogs.length-1].close();return;}
    }
    if (activePane && event.key==='Tab' && !document.querySelector('dialog[open]')) {
      const focusable=[...$(`#${activePane}`).querySelectorAll('button:not([disabled]),input:not([disabled]),select:not([disabled]),a[href],summary,[tabindex="0"]')].filter(element=>element.getClientRects().length);
      const first=focusable[0],last=focusable[focusable.length-1];
      if(event.shiftKey && document.activeElement===first){event.preventDefault();last?.focus();}
      else if(!event.shiftKey && document.activeElement===last){event.preventDefault();first?.focus();}
    }
    if (event.key==='/' && !['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName) && !document.querySelector('dialog[open]')) { event.preventDefault();if(matchMedia('(max-width:700px)').matches)setPane('navigator',true);$('#search').focus(); }
    if (event.key==='Escape' && activePane && !document.querySelector('dialog[open]')) { event.preventDefault();setPane(activePane,false); }
  });
  window.addEventListener('popstate', event => {
    const next=parseHash(),changedMap=next.context!==state.context || next.journey!==state.journey;
    state=next;
    if(changedMap){extraNodes.clear();revealedNodes.clear();}
    historyIndex=Number.isInteger(event.state?.explorerIndex)?event.state.explorerIndex:0;render();
    if(changedMap && state.view==='graph')focusSelected();
  });
  window.addEventListener('hashchange', () => {
    const next=parseHash();
    if(JSON.stringify(next)!==JSON.stringify(state)){
      const changedMap=next.context!==state.context || next.journey!==state.journey;
      state=next;
      if(changedMap){extraNodes.clear();revealedNodes.clear();}
      render();if(changedMap && state.view==='graph')focusSelected();
    }
  });
  window.addEventListener('resize', () => { if(activePane && (matchMedia('(min-width:1051px)').matches || activePane==='navigator' && matchMedia('(min-width:701px)').matches))setPane(activePane,false); });
  relationTree=SDK_RELATION_TREE.create({nodes:DATA.nodes,edges:DATA.edges,files:DATA.files,onSelectNode:id=>selectNode(id,{keepTreeRoot:true}),onOpenSource:reference=>openSource(reference.file,reference.startLine,reference.endLine)});
  $('#tree-viewport').replaceChildren(relationTree.element);
  $('#shape-legend').innerHTML=SDK_NODE_SHAPES.legendMarkup();
  $('#snapshot-commit').textContent=DATA.snapshot.commit;
  $('#snapshot-detail').textContent=`Commit ${DATA.snapshot.commit}${DATA.snapshot.dirty?' + working-tree changes':''}. Fingerprint ${DATA.snapshot.fingerprint}. Regenerate with python tools/sdk/build-explorer.py.`;
  state=location.hash?parseHash():validState(state);
  if(Number.isInteger(history.state?.explorerIndex) && history.state.explorerIndex>=0){historyIndex=history.state.explorerIndex;historyLength=historyIndex+1;}
  saveLocation(true);
  render();
  if(state.view==='graph')requestAnimationFrame(focusSelected);
  // Read-only state is exposed for local verification; the graph remains usable without tools.
  window.SDK_EXPLORER={
    readState:()=>({...state,surface:state.view,effectiveMode:currentGraph.effectiveMode,pathNodes:currentGraph.pair?.nodeIds || [],pathEdges:currentGraph.pair?.edgeIds || [],visibleNodes:currentGraph.visible.map(node=>node.id),visibleEdges:currentGraph.visibleEdges.map(edge=>edge.id),upstream:[...currentGraph.upstream],downstream:[...currentGraph.downstream],upstreamEdges:[...currentGraph.upstreamEdges],downstreamEdges:[...currentGraph.downstreamEdges],view:{...view}}),
    selectNode:(id)=>selectNode(id),
    fit:fitGraph,
  };
})().catch(error=>{document.getElementById('tree-viewport').textContent='Unable to load the source index. Open this file in a current Chrome or Edge browser. '+error.message;console.error(error);});
