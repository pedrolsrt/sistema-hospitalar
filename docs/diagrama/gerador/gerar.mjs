// Gera o diagrama de classes completo em SVG e PNG a partir da fonte em Mermaid.
// Uso: npm run gerar, ou node gerar.mjs [fonte.mmd] [saida.svg] [saida.png]
import fs from 'node:fs';
import path from 'node:path';
import { aqui, abrirNavegador, abrirPagina, padrao } from './navegador.mjs';

const [fonte = padrao.fonte, saidaSvg = padrao.svg, saidaPng = padrao.png] = process.argv.slice(2);
const texto = fs.readFileSync(fonte, 'utf-8');
const navegador = await abrirNavegador().catch(e => { console.error(e.message); process.exit(1); });
try {
  const { p, fechar } = await abrirPagina(navegador);
  try {
    await p.evaluate(t => window.desenhar(t), texto);
    await p.evaluate(t => { window.__mmd = t; }, texto);
    const resultado = await p.evaluate(fs.readFileSync(path.join(aqui, 'acabamento.js'), 'utf-8'));
    if (resultado.problemas.length) {
      console.error('O diagrama tem problemas de leitura:\n  ' + resultado.problemas.join('\n  '));
      process.exitCode = 1;
    }
    const svg = await p.$eval('#alvo svg', s => s.outerHTML);
    fs.writeFileSync(saidaSvg, '<?xml version="1.0" encoding="UTF-8"?>\n' + svg + '\n');
    // PNG em escala 2, para o texto continuar legível ao ampliar.
    await p.setViewport({ width: Math.ceil(resultado.largura), height: Math.ceil(resultado.altura), deviceScaleFactor: 2 });
    await (await p.$('#alvo svg')).screenshot({ path: saidaPng });
    console.log(`${resultado.classes} classes, ${resultado.ligacoes} ligações e ${resultado.rotulos} rótulos.`);
    console.log(`Gerados ${path.basename(saidaSvg)} e ${path.basename(saidaPng)}.`);
  } finally {
    fechar();
  }
} finally {
  await navegador.close();
}
