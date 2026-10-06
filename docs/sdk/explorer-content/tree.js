/* A source relationship tree. All text is inserted through DOM text nodes. */
(() => {
  'use strict';

  const kindNames = {callable:'Function',parameter:'Parameter',value:'Variable',property:'Property',contract:'Contract',external:'External'};

  function create({nodes:nodeList, edges:edgeList, files:fileList, onSelectNode, onOpenSource}) {
    const nodes = new Map(nodeList.map(node => [node.id,node]));
    const files = new Map(fileList.map(file => [file.path,file]));
    const element = document.createElement('section');
    element.className = 'relation-tree';
    element.setAttribute('aria-label','Complete source relationship tree');
    const incoming = new Map();
    const outgoing = new Map();
    const owned = new Map();
    for (const edge of edgeList) {
      if (!incoming.has(edge.to)) incoming.set(edge.to,[]);
      if (!outgoing.has(edge.from)) outgoing.set(edge.from,[]);
      incoming.get(edge.to).push(edge);
      outgoing.get(edge.from).push(edge);
    }
    for (const node of nodeList) {
      if (!node.ownerId) continue;
      if (!owned.has(node.ownerId)) owned.set(node.ownerId,[]);
      owned.get(node.ownerId).push(node);
    }

    let rootId = '';
    let contextId = '';
    let selectedId = '';
    let initialDepth = 2;
    let records = [];
    let toolbarStatus = null;
    let version = 0;
    let busy = false;
    let expandedUnique = null;
    const sourceLines = new Map();
    const matches = item => !Array.isArray(item.contextIds) || item.contextIds.includes(contextId);

    function el(tag, className, text) {
      const node = document.createElement(tag);
      if (className) node.className = className;
      if (text !== undefined) node.textContent = text;
      return node;
    }

    function button(label, className, action) {
      const result = el('button',className,label);
      result.type = 'button';
      result.addEventListener('click',event => { event.preventDefault();event.stopPropagation();action(); });
      return result;
    }

    function nodeType(node) {
      const type = node.typeName ?? node.declaredType ?? node.valueType ?? node.type;
      return typeof type === 'string' ? type : type?.text || type?.name || '';
    }

    function kindName(node) {
      return node.isConstructor || node.declarationKind === 'constructor' ? 'Constructor' : kindNames[node.kind] || node.kind || 'Symbol';
    }

    function select(nodeId) {
      selectedId = nodeId;
      updateSelection();
      if (onSelectNode) onSelectNode(nodeId);
    }

    function updateSelection() {
      element.querySelectorAll('[data-tree-select]').forEach(button => {
        const selected = button.dataset.treeSelect === selectedId;
        button.classList.toggle('is-selected',selected);
        button.setAttribute('aria-pressed',String(selected));
      });
    }

    function nodeButton(node, className='relation-tree-node-name') {
      const result = button(node.label || node.symbol || node.id,className,() => select(node.id));
      result.dataset.treeSelect = node.id;
      result.title = node.symbol || node.label || node.id;
      result.classList.toggle('is-selected',node.id===selectedId);
      result.setAttribute('aria-pressed',String(node.id===selectedId));
      return result;
    }

    function sourceButton(evidence, label) {
      if (!evidence?.file || !files.has(evidence.file)) return null;
      const file = files.get(evidence.file);
      const startLine = Math.max(1,Number(evidence.startLine) || 1);
      const endLine = Math.max(startLine,Number(evidence.endLine) || startLine);
      const reference = {file:evidence.file,startLine,endLine};
      const result = button(label || `${file.name || evidence.file.split('/').pop()}:${startLine}${endLine>startLine?`–${endLine}`:''}`,'relation-tree-source',() => { if(onOpenSource)onOpenSource(reference); });
      result.title = `${evidence.file}:${startLine}`;
      return result;
    }

    function relationName(edge, direction) {
      const backwards = direction === 'upstream';
      const verbs = {
        calls:backwards?'Called by':'Calls',
        constructs:backwards?'Created by':'Creates instance',
        produces:backwards?'Value produced by':'Produces value',
        passes:backwards?'Argument supplied by':'Passed into',
        aliases:backwards?'Same value from':'Same value exposed as',
        binds:backwards?'Provided by':'Provides implementation for',
        captures:backwards?'Uses value from':'Used by',
        reads:backwards?'Reads value from':'Read by',
        derives:backwards?'Derived from':'Used to derive',
        returns:backwards?'Returned value from':'Returns to',
        deferred:backwards?'Invoked later by':'Invokes later',
        closes:backwards?'Released by':'Releases',
        owns:backwards?'Owned by':'Owns',
      };
      let label = verbs[edge.kind] || edge.kind || 'Related to';
      if(edge.argumentName)label += ` · ${edge.argumentName}`;
      if(edge.argumentIndex != null)label += ` · argument ${edge.argumentIndex}`;
      return label;
    }

    function evidenceList(edges, direction) {
      if (!edges.length) return null;
      const list = el('ul','relation-tree-evidence');
      for(const edge of edges) {
        const item = el('li');
        item.append(el('span','relation-tree-relation',relationName(edge,direction)));
        const source = sourceButton(edge.evidence);
        if(source)item.append(source);
        if(edge.evidence?.file && files.has(edge.evidence.file)) {
          if(!sourceLines.has(edge.evidence.file))sourceLines.set(edge.evidence.file,files.get(edge.evidence.file).source.split('\n'));
          const lines = sourceLines.get(edge.evidence.file);
          const first = Math.max(1,Number(edge.evidence.startLine) || 1);
          const last = Math.min(lines.length,Math.max(first,Number(edge.evidence.endLine) || first));
          const excerpt = lines.slice(first-1,last).join('\n').trim();
          if(excerpt)item.append(el('code','relation-tree-usage-line',excerpt));
        }
        if(edge.condition)item.append(el('span','relation-tree-condition',edge.condition));
        if(edge.resolution === 'unresolved' || edge.unresolved)item.append(el('span','relation-tree-boundary','Unresolved relationship — inspect the source evidence.'));
        list.append(item);
      }
      return list;
    }

    function metadata(node) {
      const row = el('div','relation-tree-metadata');
      row.append(el('span','relation-tree-kind',kindName(node)));
      if(node.unresolved || node.resolution==='unresolved')row.append(el('span','relation-tree-resolution','Unresolved'));
      else if(node.origin==='source-index')row.append(el('span','relation-tree-indexed','Source indexed'));
      const type = nodeType(node);
      if(type)row.append(el('code','relation-tree-type',type));
      if(node.returnType)row.append(el('code','relation-tree-type',`returns ${node.returnType}`));
      const owner = nodes.get(node.ownerId);
      if(owner) {
        const ownerLabel = el('span','relation-tree-owner');
        ownerLabel.append(document.createTextNode('In '),nodeButton(owner,'relation-tree-owner-name'));
        row.append(ownerLabel);
      }
      const source = sourceButton(node);
      if(source)row.append(source);
      return row;
    }

    function boundary(node) {
      if(node.unresolved || node.resolution === 'unresolved')return 'Unresolved reference — static analysis could not determine its declaration or value source.';
      if(node.external || node.kind === 'external')return 'External dependency boundary — its implementation is outside this source snapshot.';
      return '';
    }

    function connected(nodeId,direction) {
      const candidates = direction === 'upstream' ? incoming.get(nodeId) : outgoing.get(nodeId);
      return (candidates || []).filter(edge => edge.kind !== 'owns' && edge[direction]!==false && matches(edge) && nodes.has(edge.from) && nodes.has(edge.to) && matches(nodes.get(edge.from)) && matches(nodes.get(edge.to)));
    }

    function childGroups(node) {
      const memberMap = new Map((owned.get(node.id) || []).filter(matches).map(member=>[member.id,member]));
      for(const parameterId of node.signatureParameterIds || []) {
        const parameter=nodes.get(parameterId);
        if(parameter && matches(parameter))memberMap.set(parameter.id,parameter);
      }
      for(const edge of outgoing.get(node.id) || []) {
        const member=nodes.get(edge.to);
        if((edge.kind==='owns' || edge.projection) && matches(edge) && member && matches(member))memberMap.set(member.id,member);
      }
      const members = [...memberMap.values()];
      const memberIds = new Set(members.map(member => member.id));
      const upstream = connected(node.id,'upstream');
      const downstream = connected(node.id,'downstream');
      const signatureIds = new Set(node.signatureParameterIds || []);
      const hasSignature = Array.isArray(node.signatureParameterIds);
      const isInput = member => hasSignature ? signatureIds.has(member.id) : member.kind === 'parameter' || member.isParameter === true;
      const inputMembers = members.filter(isInput);
      const valueRoot=['parameter','property','value'].includes(node.kind);
      const downstreamMemberIds=new Set(valueRoot?downstream.map(edge=>edge.to):[]);
      const otherMembers = members.filter(member=>!isInput(member) && !downstreamMemberIds.has(member.id));
      const groups = [];
      function ownedGroup(label,items) {
        if(!items.length)return;
        groups.push({label,direction:'member',entries:items.map(member=>({node:member,edges:upstream.filter(edge=>edge.from===member.id).concat(downstream.filter(edge=>edge.to===member.id))}))});
      }
      ownedGroup('Inputs',inputMembers);
      ownedGroup('Locals, properties & nested declarations',otherMembers);
      for(const [direction,allEdges] of [['upstream',upstream],['downstream',downstream]]) {
        const byPeer = new Map();
        for(const edge of allEdges) {
          const peerId = direction === 'upstream' ? edge.from : edge.to;
          if(memberIds.has(peerId) && !(direction==='downstream' && downstreamMemberIds.has(peerId)))continue;
          if(!byPeer.has(peerId))byPeer.set(peerId,{node:nodes.get(peerId),edges:[]});
          byPeer.get(peerId).edges.push(edge);
        }
        groups.push({label:direction==='upstream'?'Where it comes from':'Where it goes',direction,entries:[...byPeer.values()]});
      }
      return groups;
    }

    function reference(node, edges, direction, shared = false) {
      const row = el('div','relation-tree-reference');
      row.dataset.treeReference = node.id;
      row.append(nodeButton(node),el('span','relation-tree-reference-note',shared?'Shared reference — expanded elsewhere in this tree.':'Back-reference — already present in this branch.'));
      row.append(metadata(node));
      const evidence = evidenceList(edges,direction);
      if(evidence)row.append(evidence);
      return row;
    }

    function entry(node, path, depth, relationEdges=[], direction='member', eager=true) {
      if(path.has(node.id))return reference(node,relationEdges,direction);
      if(expandedUnique?.has(node.id))return reference(node,relationEdges,direction,true);
      const details = el('details','relation-tree-branch');
      details.dataset.treeNode = node.id;
      details.dataset.treeDepth = String(depth);
      details.style.setProperty('--relation-depth',String(depth));
      const summary = el('summary','relation-tree-summary');
      summary.append(nodeButton(node));
      const type = nodeType(node);
      summary.append(el('span','relation-tree-kind',kindName(node)));
      if(type)summary.append(el('code','relation-tree-type',type));
      details.append(summary);
      const nextPath = expandedUnique ? path : new Set(path);
      if(!expandedUnique)nextPath.add(node.id);
      const record = {node,details,path:nextPath,sharedPath:!!expandedUnique,depth,loaded:false,body:null,relationEdges,direction};
      records.push(record);
      details.addEventListener('toggle',()=>{
        if(details.open && !record.loaded)populate(record,!busy);
      });
      if(eager && depth<initialDepth) {
        details.open = true;
        populate(record,true);
      }
      return details;
    }

    function populate(record,eager) {
      if(record.loaded)return;
      record.loaded = true;
      if(record.sharedPath && !expandedUnique) {
        record.path=new Set([rootId]);
        let ancestor=record.details;
        while(ancestor){record.path.add(ancestor.dataset.treeNode);ancestor=ancestor.parentElement?.closest('.relation-tree-branch');}
        record.sharedPath=false;
      }
      const body = el('div','relation-tree-branch-body');
      record.body = body;
      body.append(metadata(record.node));
      if(record.node.symbol && record.node.symbol!==record.node.label)body.append(el('code','relation-tree-symbol',record.node.symbol));
      const evidence = evidenceList(record.relationEdges,record.direction);
      if(evidence)body.append(evidence);
      const boundaryText = boundary(record.node);
      if(boundaryText)body.append(el('p','relation-tree-boundary',boundaryText));
      appendGroups(body,record.node,record.path,record.depth+1,eager);
      record.details.append(body);
    }

    function appendGroups(host,node,path,depth,eager) {
      for(const group of childGroups(node)) {
        const section = el('section',`relation-tree-group relation-tree-${group.direction}`);
        section.append(el('h3','relation-tree-group-title',`${group.label} · ${group.entries.length}`));
        if(!group.entries.length) {
          section.append(el('p','relation-tree-empty',group.direction==='upstream'?'No earlier relationship is indexed. This does not establish the original source.':'No later relationship is indexed. Unresolved or external usage may still exist.'));
        } else {
          const list = el('ul','relation-tree-list');
          for(const item of group.entries) {
            const row = el('li');
            const eagerEntry=eager && (depth!==1 || node.kind!=='callable' || group.label==='Inputs');
            row.append(entry(item.node,path,depth,item.edges,group.direction,eagerEntry));
            list.append(row);
          }
          section.append(list);
        }
        host.append(section);
      }
    }

    function setBusy(value) {
      busy = value;
      element.setAttribute('aria-busy',String(value));
      element.querySelectorAll('[data-tree-expand]').forEach(button=>{button.disabled=value;});
    }

    function status(message) {
      if(toolbarStatus)toolbarStatus.textContent=message;
    }

    function collapseAll() {
      version++;
      expandedUnique=null;
      setBusy(false);
      records.forEach(record=>{record.details.open=false;});
      status('All branches collapsed. Inputs and immediate connections remain listed.');
    }

    function expandLevel() {
      const visibleClosed = records.filter(record=>record.details.isConnected && !record.details.open && !record.details.parentElement?.closest('details:not([open])'));
      for(const record of visibleClosed) {
        record.details.open=true;
        populate(record,false);
      }
      updateSelection();
      status(`Expanded ${visibleClosed.length} visible branches by one level.`);
    }

    async function expandAll() {
      const token = ++version;
      setBusy(true);
      expandedUnique = new Set([rootId]);
      // Rebuild only the branch area in unique-node mode; controls and focus stay put.
      const content = element.querySelector('.relation-tree-content');
      records=[];
      content.replaceChildren();
      appendGroups(content,nodes.get(rootId),new Set([rootId]),1,false);
      let processed=0;
      for(let cursor=0;cursor<records.length;cursor++) {
        if(token!==version)return;
        const record=records[cursor];
        if(!record.details.isConnected) {
          // Earlier shared references may have detached this record.
        } else if(expandedUnique.has(record.node.id)) {
          record.details.replaceWith(reference(record.node,record.relationEdges,record.direction,true));
        } else {
          expandedUnique.add(record.node.id);
          record.details.open=true;
          populate(record,false);
          processed++;
        }
        if((cursor+1)%30===0) {
          status(`Expanding ${expandedUnique.size} reachable symbols…`);
          await new Promise(resolve=>setTimeout(resolve,0));
        }
      }
      if(token!==version)return;
      expandedUnique=null;
      setBusy(false);
      updateSelection();
      status(`Expanded ${processed} reachable symbols. Shared and cyclic links remain as references; no depth limit was applied.`);
    }

    function render(nextRootId,nextContextId,options={}) {
      const root=nodes.get(nextRootId);
      selectedId=options.selectedNodeId || nextRootId;
      if(rootId===nextRootId && contextId===nextContextId && element.childElementCount) {
        updateSelection();
        return element;
      }
      version++;
      rootId=nextRootId;
      contextId=nextContextId;
      records=[];
      expandedUnique=null;
      initialDepth=Number.isInteger(options.initialDepth)?Math.max(0,options.initialDepth):2;
      element.replaceChildren();
      if(!root || !matches(root)) {
        setBusy(false);
        element.append(el('p','relation-tree-empty','The selected symbol is not available in this source context.'));
        return element;
      }
      const heading=el('header','relation-tree-heading');
      heading.append(el('p','eyebrow','Source relationship tree'),el('h2',null,root.label || root.symbol || root.id),metadata(root));
      heading.append(el('p','relation-tree-intro','Expand any symbol to inspect its inputs, ownership and both directions of use. Select a name to inspect it; the tree stays in place.'));
      const boundaryText=boundary(root);
      if(boundaryText)heading.append(el('p','relation-tree-boundary',boundaryText));
      element.append(heading);
      const controls=el('div','relation-tree-controls');
      const level=button('Expand one level',null,expandLevel);
      level.dataset.treeExpand='level';
      const all=button('Expand all reachable',null,()=>{void expandAll();});
      all.dataset.treeExpand='all';
      const collapse=button('Collapse branches',null,collapseAll);
      collapse.dataset.treeCollapse='all';
      controls.append(level,all,collapse);
      toolbarStatus=el('p','relation-tree-status',`${nodes.size} symbols indexed. Branches load as you expand them.`);
      toolbarStatus.setAttribute('role','status');
      element.append(controls,toolbarStatus);
      const content=el('div','relation-tree-content');
      appendGroups(content,root,new Set([root.id]),1,true);
      element.append(content);
      setBusy(false);
      updateSelection();
      return element;
    }

    function destroy() {
      version++;
      expandedUnique=null;
      records=[];
      sourceLines.clear();
      element.replaceChildren();
      element.remove();
    }

    return {element,render,destroy};
  }

  window.SDK_RELATION_TREE = Object.freeze({create});
})();
