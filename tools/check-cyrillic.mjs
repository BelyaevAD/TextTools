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

// Строковый литерал AHK v2: кавычка внутри ЛИБО удваивается (""), ЛИБО
// экранируется обратной кавычкой (`"); обратный слэш не экранирует ничего.
// Обе формы обязательны: учтя только удвоение, разбор закрывает литерал на `"
// и дальше читает остаток строки как код.
const DQ = '"[^"`]*(?:(?:`.|"")[^"`]*)*"';
const SQ = "'[^'`]*(?:(?:`.|'')[^'`]*)*'";
const anyString = new RegExp(`${DQ}|${SQ}`, 'g');
// Loc("ключ", "русский текст") целиком, вместе с русским аргументом
const locCall = new RegExp(`\\bLoc\\s*\\(\\s*${DQ}\\s*,\\s*${DQ}\\s*\\)`, 'g');

// Комментарий отрезаем ПОСЛЕ разбора литералов: ';' внутри строки не комментарий.
// Прежний `line.replace(/;.*$/, '')` обрезал строку по такому ';' и, например,
//     MsgBox("Ошибка; подробности: " Loc("k", "см. лог"))
// терял Loc() вместе с хвостом и давал ложное срабатывание. Он же мог срезать
// кириллицу и пропустить настоящее нарушение.
function splitLine(line) {
  let code = '';                 // строка без содержимого литералов и без комментария
  let strings = '';              // содержимое литералов, склеенное
  let i = 0;
  while (i < line.length) {
    const ch = line[i];
    if (ch === '"' || ch === "'") {
      const m = new RegExp(`^(?:${ch === '"' ? DQ : SQ})`).exec(line.slice(i));
      if (m) {
        code += ch + ch;
        strings += m[0] + '\n';
        i += m[0].length;
        continue;
      }
      // незакрытый литерал - до конца строки, как его увидит AHK
      code += ch + ch;
      strings += line.slice(i) + '\n';
      break;
    }
    if (ch === ';' && (i === 0 || /\s/.test(line[i - 1]))) break;
    code += ch;
    i++;
  }
  return { code, strings };
}

function parenBalance(code) {
  let n = 0;
  for (const ch of code) {
    if (ch === '(') n++;
    else if (ch === ')') n--;
  }
  return n;
}

function check(rel, lines) {
  const parts = lines.map(splitLine);
  for (let i = 0; i < lines.length; i++) {
    if (!uiCall.test(parts[i].code + parts[i].strings)) continue;
    // Логическая строка: вызов может продолжаться на следующих строках, а
    // проверка была построчной - текст на строке-продолжении не проверялся
    // совсем (`MsgBox(` / новая строка / `"Русский текст")`).
    let depth = parenBalance(parts[i].code);
    let text = lines[i];
    let strings = parts[i].strings;
    for (let j = i + 1; j < lines.length && j <= i + 10; j++) {
      const cont = depth > 0 || /^\s*[.,)]/.test(parts[j].code);
      if (!cont) break;
      text += '\n' + lines[j];
      strings += parts[j].strings;
      depth += parenBalance(parts[j].code);
      if (depth <= 0 && !/[,.(]\s*$/.test(parts[j].code.trimEnd())) break;
    }
    if (!cyr.test(strings)) continue;
    if (/\bTT_Log\s*\(/.test(text) || /\bthrow\s+Error\s*\(/.test(text)) continue;
    // Освобождение по Loc() - для КАЖДОГО литерала отдельно, а не для всей
    // строки: раньше хватало одного Loc() где угодно в строке, и сырой русский
    // рядом с ним проходил молча.
    const left = text.replace(locCall, 'LOC');
    let bad = '';
    for (const lit of left.match(anyString) || [])
      if (cyr.test(lit) && !bad) bad = lit;
    if (!bad) continue;
    hits.push(`${rel}:${i + 1}  кириллица в UI без Loc(): ${bad.replace(/\s+/g, ' ').slice(0, 120)}`);
  }
}

function walk(dir, rel) {
  for (const f of readdirSync(dir)) {
    if (f.endsWith('.ahk') && !skipFile.test(f))
      check(rel + f, readFileSync(join(dir, f), 'utf8').split(/\r?\n/));
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
