// Acabamento do SVG gerado pelo Mermaid, executado na página depois do desenho:
// visual do diagrama da Sprint 1, notação UML das setas e rótulos ao lado das linhas.
(() => {
  const NS = 'http://www.w3.org/2000/svg';
  const svg = document.querySelector('#alvo svg');
  const problemas = [];

  // Classes: cabeçalho bege, bordas escuras, membros no formato UML e nome em itálico nas abstratas.
  const reescrever = (texto, ehMetodo) => {
    const m = texto.match(/^([-#+~])(.*)$/);
    if (!m) return texto;
    const [, vis, resto] = m;
    if (ehMetodo) {
      const mm = resto.match(/^(.*\))\s*:\s*(.+)$/);
      return mm ? `${vis} ${mm[1]}: ${mm[2]}` : `${vis} ${resto}`;
    }
    const ma = resto.match(/^(\S+)\s+(\S+)$/);
    return ma ? `${vis} ${ma[2]}: ${ma[1]}` : `${vis} ${resto}`;
  };
  const trocarTexto = (textEl, novo) => {
    const internos = [...textEl.querySelectorAll('tspan.text-inner-tspan')];
    if (!internos.length) { (textEl.querySelector('tspan.text-outer-tspan') || textEl).textContent = novo; return; }
    internos[0].textContent = novo;
    internos.slice(1).forEach(t => t.remove());
  };
  for (const no of svg.querySelectorAll('g.node')) {
    const caixa = no.querySelector('.label-container path');
    const nums = caixa.getAttribute('d').match(/-?[\d.]+/g).map(Number);
    const meiaL = Math.abs(nums[0]), meiaA = Math.abs(nums[1]);
    const divisor = no.querySelector('g.divider path');
    const yDiv = divisor ? Number(divisor.getAttribute('d').match(/-?[\d.]+/g)[1]) : meiaA;
    const cab = document.createElementNS(NS, 'rect');
    cab.setAttribute('x', -meiaL); cab.setAttribute('y', -meiaA);
    cab.setAttribute('width', 2 * meiaL); cab.setAttribute('height', yDiv + meiaA);
    cab.setAttribute('style', 'fill: #ebe6da !important; stroke: none !important');
    caixa.after(cab);
    no.querySelectorAll('.label-container path, g.divider path').forEach(p => {
      if (p.getAttribute('stroke') !== 'none') { p.setAttribute('stroke', '#222222'); p.setAttribute('stroke-width', '1.6'); }
    });
    for (const [grupo, ehMetodo] of [['.members-group', false], ['.methods-group', true]]) {
      for (const t of no.querySelectorAll(grupo + ' text')) trocarTexto(t, reescrever(t.textContent.trim(), ehMetodo));
    }
    const anot = no.querySelector('.annotation-group text');
    if (anot && anot.textContent.includes('abstract')) no.querySelector('.label-group text').setAttribute('font-style', 'italic');
    const c = no.querySelector('.label-container').getBoundingClientRect();
    for (const t of no.querySelectorAll('text')) {
      const r = t.getBoundingClientRect();
      if (r.width && (r.left < c.left - 0.5 || r.right > c.right + 0.5 || r.top < c.top || r.bottom > c.bottom))
        problemas.push('texto fora da caixa: ' + no.id + ': ' + t.textContent);
    }
  }

  // Associação (navegação) e dependência usam seta aberta; o Mermaid desenha uma ponta preenchida.
  const defs = document.createElementNS(NS, 'defs');
  defs.innerHTML = `
    <marker id="seta-aberta-fim" markerUnits="userSpaceOnUse" markerWidth="16" markerHeight="16" refX="13" refY="7" orient="auto">
      <path d="M 1,1 L 13,7 L 1,13" fill="none" stroke="#222222" stroke-width="1.5"/></marker>
    <marker id="seta-aberta-inicio" markerUnits="userSpaceOnUse" markerWidth="16" markerHeight="16" refX="1" refY="7" orient="auto">
      <path d="M 13,1 L 1,7 L 13,13" fill="none" stroke="#222222" stroke-width="1.5"/></marker>`;
  svg.insertBefore(defs, svg.firstChild);
  for (const p of svg.querySelectorAll('path.relation')) {
    for (const lado of ['start', 'end']) {
      const m = p.getAttribute('marker-' + lado) || '';
      if (m.includes('dependency')) p.setAttribute('marker-' + lado, `url(#seta-aberta-${lado === 'end' ? 'fim' : 'inicio'})`);
    }
  }

  // Geometria em coordenadas do SVG: caixas das classes e pontos de cada linha.
  const ctmSvg = svg.getScreenCTM().inverse();
  const paraSvg = (x, y) => { const p = svg.createSVGPoint(); p.x = x; p.y = y; return p.matrixTransform(ctmSvg); };
  const caixaSvg = el => {
    const r = el.getBoundingClientRect(); const a = paraSvg(r.left, r.top), b = paraSvg(r.right, r.bottom);
    return { x1: a.x, y1: a.y, x2: b.x, y2: b.y };
  };
  const nos = {};
  for (const no of svg.querySelectorAll('g.node')) {
    const nome = no.id.replace(/^classId-/, '').replace(/-\d+$/, '');
    nos[nome] = caixaSvg(no.querySelector('.label-container'));
  }
  const titulo = svg.querySelector('.classDiagramTitleText');
  titulo.setAttribute('style', 'font-size: 30px; font-weight: bold; fill: #222222');

  // Papéis e multiplicidades vêm da fonte: a aresta de id "id_A_B_k_0" é a k-ésima relação do .mmd.
  const relacoes = [];
  const padrao = /^\s*(\w+)(?: "([^"]+)")? (<\|--|--\|>|<\|\.\.|\.\.\|>|\*--|--\*|o--|--o|-->|<--|\.\.>|<\.\.) (?:"([^"]+)" )?(\w+)(?: : (.*))?$/;
  for (const linha of (window.__mmd || '').split('\n')) {
    const m = linha.match(padrao);
    if (m) relacoes.push({ a: m[1], ma: m[2], seta: m[3], mb: m[4], b: m[5], papel: m[6] });
  }
  const arestas = [];
  for (const p of svg.querySelectorAll('path.relation')) {
    const k = Number(p.id.match(/_(\d+)_\d+$/)[1]);
    const rel = relacoes[k - 1];
    if (!rel || !p.id.startsWith(`id_${rel.a}_${rel.b}_`)) { problemas.push('aresta sem relação: ' + p.id); continue; }
    const n = p.getTotalLength(), m = p.getScreenCTM(), pts = [];
    for (let d = 0; d <= n; d += 2) {
      const q = p.getPointAtLength(d), s = svg.createSVGPoint(); s.x = q.x; s.y = q.y;
      const t = s.matrixTransform(m).matrixTransform(ctmSvg); pts.push([t.x, t.y]);
    }
    arestas.push({ el: p, rel, pts });
  }
  if (arestas.length !== relacoes.length) problemas.push(`arestas ${arestas.length} x relações ${relacoes.length}`);
  for (const a of arestas) {
    for (const [nome, c] of Object.entries(nos)) {
      if (nome === a.rel.a || nome === a.rel.b) continue;
      if (a.pts.some(([x, y]) => x > c.x1 + 1 && x < c.x2 - 1 && y > c.y1 + 1 && y < c.y2 - 1))
        problemas.push(`linha ${a.rel.a}-${a.rel.b} atravessa ${nome}`);
    }
  }

  // Rótulos ao lado da linha, como no diagrama da Sprint 1. Cada um fica na primeira posição que não encosta
  // em título, classe, ponta de seta, outro rótulo ou linha, e que está mais perto da sua linha que das outras.
  svg.querySelectorAll('.edgeLabels, .edgeTerminals').forEach(g => g.remove());
  const camada = document.createElementNS(NS, 'g');
  camada.setAttribute('class', 'rotulos');
  svg.appendChild(camada);
  const ocupados = [];
  const tituloCaixa = caixaSvg(titulo);
  const intersecta = (r, s, folga = 0) => r.x1 < s.x2 + folga && s.x1 < r.x2 + folga && r.y1 < s.y2 + folga && s.y1 < r.y2 + folga;
  const marcadores = [];
  for (const a of arestas) {
    for (const [lado, pts] of [['start', a.pts], ['end', [...a.pts].reverse()]]) {
      if (!a.el.getAttribute('marker-' + lado)) continue;
      const [x, y] = pts[Math.min(pts.length - 1, 5)];
      marcadores.push({ x1: x - 12, y1: y - 12, x2: x + 12, y2: y + 12 });
    }
  }
  const livre = r => {
    if (intersecta(r, tituloCaixa, 4)) return false;
    if (marcadores.some(m => intersecta(r, m, 2))) return false;
    if (Object.values(nos).some(c => intersecta(r, c, 3))) return false;
    if (ocupados.some(o => intersecta(r, o, 12))) return false;
    for (const a of arestas) if (a.pts.some(([x, y]) => x > r.x1 - 3 && x < r.x2 + 3 && y > r.y1 - 3 && y < r.y2 + 3)) return false;
    return true;
  };
  const criarTexto = (texto, tamanho) => {
    const t = document.createElementNS(NS, 'text');
    t.setAttribute('style', `font-size: ${tamanho}px; fill: #222222; font-family: Arial, Helvetica, sans-serif`);
    t.setAttribute('text-anchor', 'middle'); t.setAttribute('dominant-baseline', 'central');
    t.textContent = texto; camada.appendChild(t);
    const b = t.getBBox(); return { t, w: b.width, h: b.height };
  };
  const pontoEm = (pts, d) => {
    const i = Math.max(1, Math.min(pts.length - 1, Math.round(d / 2)));
    const [x0, y0] = pts[i - 1], [x1, y1] = pts[i];
    const n = Math.hypot(x1 - x0, y1 - y0) || 1;
    return { x: x1, y: y1, dx: (x1 - x0) / n, dy: (y1 - y0) / n };
  };
  const distancia = (r, pts) => {
    let m = Infinity;
    for (const [x, y] of pts) {
      const dx = Math.max(r.x1 - x, 0, x - r.x2), dy = Math.max(r.y1 - y, 0, y - r.y2);
      m = Math.min(m, Math.hypot(dx, dy));
    }
    return m;
  };
  const posicionar = (rotulo, candidatos, descricao, propria) => {
    for (const { x, y, dx, dy } of candidatos) {
      for (const lado of [1, -1]) {
        const nx = -dy * lado, ny = dx * lado;
        const afast = Math.abs(nx) * rotulo.w / 2 + Math.abs(ny) * rotulo.h / 2 + 8;
        const cx = x + nx * afast, cy = y + ny * afast;
        const r = { x1: cx - rotulo.w / 2, y1: cy - rotulo.h / 2, x2: cx + rotulo.w / 2, y2: cy + rotulo.h / 2 };
        const dPropria = distancia(r, propria.pts);
        if (livre(r) && arestas.every(o => o === propria || distancia(r, o.pts) > dPropria + 6)) {
          rotulo.t.setAttribute('x', cx); rotulo.t.setAttribute('y', cy);
          ocupados.push(r); return true;
        }
      }
    }
    problemas.push('sem posição livre para ' + descricao);
    rotulo.t.remove();
    return false;
  };
  // Multiplicidades primeiro, junto das pontas; depois os papéis, ao longo da linha.
  const distancias = [14, 20, 28, 38, 50, 64, 80, 98, 120, 145, 175, 210];
  for (const a of arestas) {
    if (a.rel.ma) posicionar(criarTexto(a.rel.ma, 15), distancias.map(d => pontoEm(a.pts, d)),
      `${a.rel.ma} em ${a.rel.a} (${a.rel.a}-${a.rel.b})`, a);
    if (a.rel.mb) posicionar(criarTexto(a.rel.mb, 15), distancias.map(d => pontoEm([...a.pts].reverse(), d)),
      `${a.rel.mb} em ${a.rel.b} (${a.rel.a}-${a.rel.b})`, a);
  }
  for (const a of arestas) {
    if (!a.rel.papel) continue;
    const total = (a.pts.length - 1) * 2;
    const fracoes = [0.5, 0.45, 0.55, 0.4, 0.6, 0.35, 0.65, 0.3, 0.7, 0.25, 0.75, 0.2, 0.8, 0.15, 0.85];
    posicionar(criarTexto(a.rel.papel, 15), fracoes.map(f => pontoEm(a.pts, f * total)), `papel ${a.rel.papel} (${a.rel.a}-${a.rel.b})`, a);
  }

  // Cores das linhas e dos marcadores, fundo branco e margem em volta do desenho.
  const estilo = document.createElementNS(NS, 'style');
  estilo.textContent = `
    #diagrama .node text, #diagrama .node tspan { fill: #222222 !important; font-family: Arial, Helvetica, sans-serif; }
    #diagrama .relation { stroke: #222222 !important; stroke-width: 1.4px !important; }
    #diagrama .extension, #diagrama .aggregation, #diagrama .extension path, #diagrama .aggregation path { fill: #ffffff !important; stroke: #222222 !important; }
    #diagrama .composition, #diagrama .composition path { fill: #222222 !important; stroke: #222222 !important; }`;
  svg.appendChild(estilo);

  const [x, y, w, h] = svg.getAttribute('viewBox').split(' ').map(Number);
  const mg = 40, vb = [x - mg, y - mg, w + 2 * mg, h + 2 * mg];
  svg.setAttribute('viewBox', vb.join(' '));
  const fundo = document.createElementNS(NS, 'rect');
  fundo.setAttribute('x', vb[0]); fundo.setAttribute('y', vb[1]);
  fundo.setAttribute('width', vb[2]); fundo.setAttribute('height', vb[3]); fundo.setAttribute('fill', '#ffffff');
  svg.insertBefore(fundo, svg.firstChild);
  svg.setAttribute('width', vb[2]); svg.setAttribute('height', vb[3]);
  svg.style.maxWidth = 'none';
  svg.style.backgroundColor = '#ffffff';
  for (const t of camada.querySelectorAll('text')) {
    const b = t.getBBox();
    if (b.x < vb[0] || b.y < vb[1] || b.x + b.width > vb[0] + vb[2] || b.y + b.height > vb[1] + vb[3])
      problemas.push('rótulo fora da imagem: ' + t.textContent);
  }
  return { largura: vb[2], altura: vb[3], classes: Object.keys(nos).length, ligacoes: arestas.length,
    rotulos: camada.childElementCount, problemas };
})()
