// Линт: кириллица в UI-вызове без Loc().
// Русский текст вторым аргументом Loc("ключ", "русский") — канон (ADR 032 п. 7).
// Строки лога (TT_Log) и throw Error не проверяем.
//
// Запуск: node tools/check-cyrillic.mjs
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1');
const skipFile = /^(tests-|capture-screenshots)/;
const uiCall = /\b(MsgBox|TrayTip|Flash|InputBox|BusyShow|BusyUpdate|BusyDetail|BusyDone|A_TrayMenu\.Add)\s*\(|\.Add\(\s*"(Text|Button|Radio|CheckBox|GroupBox)"|Gui\s*\(/;
const cyr = /[А-Яа-яЁё]/;
const hits = [];

function walk(dir, rel) {
  for (const f of readdirSync(dir)) {
    if (f.endsWith('.ahk') && !skipFile.test(f)) {
      const path = join(dir, f);
      const lines = readFileSync(path, 'utf8').split(/\r?\n/);
      lines.forEach((line, i) => {
        const code = line.replace(/;.*$/, '');
        if (!cyr.test(code)) return;
        if (!uiCall.test(code)) return;
        if (/\bLoc\s*\(/.test(code)) return;
        if (/\bTT_Log\s*\(/.test(code) || /\bthrow\s+Error\s*\(/.test(code)) return;
        hits.push(`${rel}${f}:${i + 1}  кириллица в UI без Loc(): ${code.trim().slice(0, 120)}`);
      });
    }
  }
}

walk(join(root, 'Lib'), 'Lib/');
walk(root, '');

if (hits.length) {
  console.log('КИРИЛЛИЦА В UI БЕЗ Loc():');
  for (const h of hits) console.log('  ' + h);
  process.exit(1);
}
console.log('Чисто: кириллица в UI только внутри Loc()');
