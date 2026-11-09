// Confere o SVG gerado: linhas atravessando classes, rótulos encostando em algo, texto fora das caixas,
// notação de cada ligação e se cada rótulo está junto de uma linha que de fato tem aquele papel ou multiplicidade.
// Uso: npm run inspecionar, ou node inspecionar.mjs [diagrama.svg] [fonte.mmd]
import fs from 'node:fs';
import { pathToFileURL } from 'node:url';
import { abrirNavegador, padrao } from './navegador.mjs';

const [arquivoSvg = padrao.svg, fonte = padrao.fonte] = process.argv.slice(2);
const navegador = await abrirNavegador().catch(e => { console.error(e.message); process.exit(1); });
try {
  const p = await navegador.newPage();
  await p.setViewport({ width: 4000, height: 4000 });
  await p.goto(pathToFileURL(arquivoSvg).href);
  const r = await p.evaluate((mmd) => {
    const svg = document.querySelector('svg');
    const problemas = [];
    const ctm = svg.getScreenCTM().inverse();
    const caixa = el => {
      const r = el.getBoundingClientRect();
      const a = new DOMPoint(r.left, r.top).matrixTransform(ctm), b = new DOMPoint(r.right, r.bottom).matrixTransform(ctm);
      return { x1: a.x, y1: a.y, x2: b.x, y2: b.y };
    };
    const toca = (r, s, f = 0) => r.x1 < s.x2 + f && s.x1 < r.x2 + f && r.y1 < s.y2 + f && s.y1 < r.y2 + f;
    const nos = {};
    for (const n of svg.querySelectorAll('g.node')) {
      nos[n.id.replace(/^classId-/, '').replace(/-\d+$/, '')] = caixa(n.querySelector('.label-container'));
      const c = n.querySelector('.label-container').getBoundingClientRect();
      for (const t of n.querySelectorAll('text')) {
        const r = t.getBoundingClientRect();
        if (r.width && (r.left < c.left - 0.5 || r.right > c.right + 0.5 || r.top < c.top - 0.5 || r.bottom > c.bottom + 0.5))
          problemas.push('texto fora da caixa: ' + n.id + ': ' + t.textContent);
      }
    }
    const relacoes = [];
    const pad = /^\s*(\w+)(?: "([^"]+)")? (<\|--|--\|>|<\|\.\.|\.\.\|>|\*--|--\*|o--|--o|-->|<--|\.\.>|<\.\.) (?:"([^"]+)" )?(\w+)(?: : (.*))?$/;
    for (const l of mmd.split('\n')) {
      const m = l.match(pad);
      if (m) relacoes.push({ a: m[1], ma: m[2], seta: m[3], mb: m[4], b: m[5], papel: m[6] });
    }
    const arestas = [];
    for (const el of svg.querySelectorAll('path.relation')) {
      const rel = relacoes[Number(el.id.match(/_(\d+)_\d+$/)[1]) - 1];
      const m = el.getScreenCTM(), pts = [];
      for (let d = 0, n = el.getTotalLength(); d <= n; d += 2) {
        const q = el.getPointAtLength(d), t = new DOMPoint(q.x, q.y).matrixTransform(m).matrixTransform(ctm);
        pts.push([t.x, t.y]);
      }
      arestas.push({ el, rel, pts });
    }
    if (arestas.length !== relacoes.length) problemas.push(`ligações ${arestas.length} x relações ${relacoes.length}`);
    for (const a of arestas) for (const [nome, c] of Object.entries(nos)) {
      if (nome === a.rel.a || nome === a.rel.b) continue;
      if (a.pts.some(([x, y]) => x > c.x1 + 1 && x < c.x2 - 1 && y > c.y1 + 1 && y < c.y2 - 1))
        problemas.push(`linha ${a.rel.a}-${a.rel.b} atravessa ${nome}`);
    }
    // Marcador e traço esperados para cada tipo de ligação.
    const esperado = { '<|--': 'extension', '--|>': 'extension', '<|..': 'extension', '..|>': 'extension',
      '*--': 'composition', '--*': 'composition', 'o--': 'aggregation', '--o': 'aggregation',
      '-->': 'seta-aberta', '<--': 'seta-aberta', '..>': 'seta-aberta', '<..': 'seta-aberta' };
    for (const a of arestas) {
      const marc = (a.el.getAttribute('marker-start') || '') + (a.el.getAttribute('marker-end') || '');
      if (!marc.includes(esperado[a.rel.seta])) problemas.push(`marcador errado em ${a.rel.a}-${a.rel.b}`);
      if (a.el.classList.contains('edge-pattern-dashed') !== a.rel.seta.includes('..')) problemas.push(`traço errado em ${a.rel.a}-${a.rel.b}`);
    }
    const marcas = [];
    for (const a of arestas) for (const [lado, pts] of [['start', a.pts], ['end', [...a.pts].reverse()]]) {
      if (!a.el.getAttribute('marker-' + lado)) continue;
      const [x, y] = pts[Math.min(pts.length - 1, 5)];
      marcas.push({ x1: x - 12, y1: y - 12, x2: x + 12, y2: y + 12 });
    }
    const rotulos = [...svg.querySelectorAll('g.rotulos text')].map(t => ({ t: t.textContent, r: caixa(t) }));
    const titulo = caixa(svg.querySelector('.classDiagramTitleText'));
    const dist = (r, pts) => Math.min(...pts.map(([x, y]) => Math.hypot(Math.max(r.x1 - x, 0, x - r.x2), Math.max(r.y1 - y, 0, y - r.y2))));
    const esperados = arestas.flatMap(a => [a.rel.ma, a.rel.mb, a.rel.papel].filter(Boolean));
    if (rotulos.length !== esperados.length) problemas.push(`rótulos ${rotulos.length} x esperados ${esperados.length}`);
    const vb = svg.viewBox.baseVal;
    rotulos.forEach((l, i) => {
      if (toca(l.r, titulo, 2)) problemas.push('rótulo encosta no título: ' + l.t);
      for (const [nome, c] of Object.entries(nos)) if (toca(l.r, c, 1)) problemas.push(`rótulo ${l.t} encosta em ${nome}`);
      if (marcas.some(m => toca(l.r, m))) problemas.push(`rótulo ${l.t} encosta em ponta de seta`);
      rotulos.forEach((o, j) => { if (j > i && toca(l.r, o.r, 2)) problemas.push(`rótulos ${l.t} e ${o.t} se encostam`); });
      const [maisPerto] = arestas.map(a => ({ a, d: dist(l.r, a.pts) })).sort((x, y) => x.d - y.d);
      if (maisPerto.d < 2) problemas.push(`rótulo ${l.t} encosta numa linha`);
      const dono = maisPerto.a.rel;
      if (![dono.ma, dono.mb, dono.papel].includes(l.t)) problemas.push(`rótulo ${l.t} está mais perto da linha ${dono.a}-${dono.b}, que não o tem`);
      if (l.r.x1 < vb.x || l.r.y1 < vb.y || l.r.x2 > vb.x + vb.width || l.r.y2 > vb.y + vb.height) problemas.push('rótulo cortado: ' + l.t);
    });
    return { classes: Object.keys(nos).length, ligacoes: arestas.length, rotulos: rotulos.length, problemas };
  }, fs.readFileSync(fonte, 'utf-8'));
  console.log(`${r.classes} classes, ${r.ligacoes} ligações e ${r.rotulos} rótulos conferidos.`);
  if (r.problemas.length) {
    console.error('Problemas:\n  ' + r.problemas.join('\n  '));
    process.exitCode = 1;
  } else {
    console.log('Nenhum problema encontrado.');
  }
} finally {
  await navegador.close();
}
