// Partes comuns aos scripts: caminhos padrão, Chrome e a página que desenha o diagrama com o Mermaid.
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import puppeteer from 'puppeteer-core';

export const aqui = path.dirname(fileURLToPath(import.meta.url));
const docs = path.join(aqui, '..', '..');
export const padrao = {
  fonte: path.join(docs, 'diagrama-de-classes-completo.mmd'),
  svg: path.join(docs, 'diagrama-de-classes-sprint2.svg'),
  png: path.join(docs, 'diagrama-de-classes-sprint2.png'),
};

const CHROME_WINDOWS = 'C:/Program Files/Google/Chrome/Application/chrome.exe';

export async function abrirNavegador() {
  const chrome = process.env.CHROME_PATH || CHROME_WINDOWS;
  if (!fs.existsSync(chrome)) {
    throw new Error(`Chrome não encontrado em ${chrome}. Informe o caminho na variável CHROME_PATH.`);
  }
  // Sem suavização de texto por subpixel, que deixaria franjas coloridas no PNG.
  return puppeteer.launch({ executablePath: chrome,
    args: ['--no-first-run', '--disable-extensions', '--disable-lcd-text', '--force-color-profile=srgb'] });
}

// Layout ELK em camadas, com as cores e a fonte do diagrama da Sprint 1. A semente fixa deixa o traçado das
// bordas igual a cada geração.
const pagina = `<!doctype html><html><head><meta charset="utf-8">
<style>body{margin:0;background:#ffffff}</style></head><body><div id="alvo"></div>
<script type="module">
import mermaid from '/node_modules/mermaid/dist/mermaid.esm.min.mjs';
import elk from '/node_modules/@mermaid-js/layout-elk/dist/mermaid-layout-elk.esm.min.mjs';
mermaid.registerLayoutLoaders(elk);
window.desenhar = async (texto) => {
  mermaid.initialize({
    startOnLoad: false, layout: 'elk', htmlLabels: false, securityLevel: 'strict', handDrawnSeed: 1,
    theme: 'base', fontFamily: 'Arial, Helvetica, sans-serif', fontSize: 15,
    themeVariables: {
      fontFamily: 'Arial, Helvetica, sans-serif', fontSize: '15px', background: '#ffffff',
      primaryColor: '#faf8f3', primaryBorderColor: '#222222', primaryTextColor: '#222222',
      lineColor: '#222222', secondaryColor: '#faf8f3', tertiaryColor: '#ffffff', titleColor: '#222222'
    },
    class: { padding: 10 },
    elk: { mergeEdges: false, nodePlacementStrategy: 'NETWORK_SIMPLEX', cycleBreakingStrategy: 'GREEDY' }
  });
  const { svg } = await mermaid.render('diagrama', texto);
  document.getElementById('alvo').innerHTML = svg;
};
window.pronto = true;
</script></body></html>`;

// Servidor local só para a página carregar os módulos do Mermaid de node_modules.
export async function abrirPagina(navegador) {
  const tipos = { '.mjs': 'text/javascript', '.js': 'text/javascript' };
  const servidor = http.createServer((req, res) => {
    if (req.url === '/') { res.writeHead(200, { 'content-type': 'text/html' }); return res.end(pagina); }
    const arquivo = path.join(aqui, decodeURIComponent(req.url.split('?')[0]));
    if (!arquivo.startsWith(aqui) || !fs.existsSync(arquivo)) { res.writeHead(404); return res.end(); }
    res.writeHead(200, { 'content-type': tipos[path.extname(arquivo)] || 'application/octet-stream' });
    fs.createReadStream(arquivo).pipe(res);
  }).listen(0, '127.0.0.1');
  await new Promise(r => servidor.once('listening', r));
  const p = await navegador.newPage();
  p.on('pageerror', e => console.error('Erro na página:', e.message));
  await p.goto(`http://127.0.0.1:${servidor.address().port}/`);
  await p.waitForFunction('window.pronto === true');
  return { p, fechar: () => servidor.close() };
}
