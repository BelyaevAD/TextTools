// Ищет переменные, чьё имя совпадает с именем функции без учёта регистра.
//
// В AHK v2 имена регистронезависимы, поэтому присваивание `foo := Foo()` делает
// функцию Foo недоступной внутри этой функции: `foo` становится локальной
// переменной, и вызов падает на "This local variable has not been assigned a
// value". Ошибка проявляется только когда до строки доходит выполнение, то есть
// после деплоя и клика пользователя. За сессию поймана трижды:
//   apps := CloneApps(APPS)      - редактор перестал открываться
//   for l in ... + L("ключ")     - локализация погасила саму себя
//   modelRows := ModelRows()     - не открывались настройки
//
// Запуск:  node tools/check-shadowing.mjs
// Код возврата 1, если что-то найдено.

import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1');
const files = [];
for (const dir of ['Lib', '.']) {
  for (const f of readdirSync(join(root, dir))) {
    if (f.endsWith('.ahk')) files.push({ rel: dir === '.' ? f : `${dir}/${f}`, path: join(root, dir, f) });
  }
}
for (const f of files) f.lines = readFileSync(f.path, 'utf8').split(/\r?\n/);

// Код строки: содержимое строковых литералов погашено, комментарий отрезан.
//
// Литералы гасятся ПЕРВЫМИ и одним проходом слева направо. Прежний порядок
// (сначала `replace(/;.*$/, '')`, потом литералы) обрезал строку по ';' ВНУТРИ
// литерала, незакрытый остаток разбирался как код, и строка
//     s := "note(1); хвост"
// давала ложный «вызов необъявленной функции note()». Из того же порядка
// Lib/markdown.ahk заканчивался на глубине вложенности 12 вместо 0 (CSS внутри
// секции продолжения), и половина файла оставалась без областей параметров.
//
// Экранирование кавычки в AHK v2 - удвоение ("" внутри "..."), не обратный слэш.
// ';' начинает комментарий только в начале строки или после пробельного символа.
function codeOf(line) {
  let out = '';
  let i = 0;
  while (i < line.length) {
    const ch = line[i];
    if (ch === '"' || ch === "'") {
      out += ch + ch;                                  // пустой литерал вместо содержимого
      i++;
      while (i < line.length) {
        // Экранирование в AHK v2 - ЛИБО удвоение кавычки (""), ЛИБО обратная
        // кавычка (`"); обратный слэш не экранирует ничего. Вторую форму
        // пропустить нельзя: на строке
        //     Check(JsonEsc("a`"b") = "a\`"b", "...")
        // разбор только по удвоению закрывает литерал на `" и съедает
        // закрывающую скобку вызова - дальше вся строка разбирается как мусор.
        if (line[i] === '`') { i += 2; continue; }
        if (line[i] === ch) {
          if (line[i + 1] === ch) { i += 2; continue; }  // удвоенная кавычка внутри литерала
          i++;
          break;
        }
        i++;
      }
      continue;
    }
    if (ch === ';' && (i === 0 || /\s/.test(line[i - 1]))) break;
    out += ch;
    i++;
  }
  return out;
}

for (const f of files) f.code = f.lines.map(codeOf);

// --- что считать объявлением функции ---
//
// Требуется открывающая скобка блока или стрелка. Раньше скобка была
// необязательной (`\{?`), и ОДИНОЧНЫЙ ВЫЗОВ в виде отдельной строки подходил под
// шаблон объявления: `ParseModelIdz("x")` регистрировался как объявленная
// функция и опечатка в нём не находилась никогда. Именно этот класс ошибки файл
// и должен ловить (см. ParseModelIds в заголовке). Стиль Allman (скобка на
// следующей строке) поддержан отдельной веткой.
const DECL_BRACE = /^(\s*)([A-Za-z_][A-Za-z0-9_]*)\s*\(([^()]*)\)\s*\{\s*$/;
const DECL_NOBRACE = /^(\s*)([A-Za-z_][A-Za-z0-9_]*)\s*\(([^()]*)\)\s*$/;
const DECL_ARROW = /^(\s*)([A-Za-z_][A-Za-z0-9_]*)\s*\(([^()]*)\)\s*=>/;

// Объявление на строке i (0-based) или null
function declAt(code, i) {
  for (const re of [DECL_BRACE, DECL_ARROW]) {
    const m = re.exec(code[i]);
    if (m) return { indent: m[1], name: m[2], params: m[3] };
  }
  const m = DECL_NOBRACE.exec(code[i]);
  if (m && /^\s*\{\s*$/.test(code[i + 1] || '')) return { indent: m[1], name: m[2], params: m[3] };
  return null;
}

function paramNames(params) {
  const out = [];
  for (const part of params.split(',')) {
    const pm = /^\s*&?\s*([A-Za-z_][A-Za-z0-9_]*)/.exec(part);
    if (pm) out.push(pm[1]);
  }
  return out;
}

// имена функций верхнего уровня: объявление без отступа
const funcs = new Map();                       // нижний регистр -> как объявлено
for (const f of files) {
  f.code.forEach((_, i) => {
    const d = declAt(f.code, i);
    if (d && d.indent === '') funcs.set(d.name.toLowerCase(), d.name);
  });
}

// --- глобальные переменные ---
//
// Затеняется не только функция. Первый пример в заголовке - `apps := CloneApps(APPS)`
// - это затенение ГЛОБАЛЬНОЙ ПЕРЕМЕННОЙ APPS: присваивание делает имя локальным,
// и дальше в этой функции APPS - уже клон, а не общий список. Редактор от этого
// перестал открываться, а проверка функций такой случай не видит.
//
// Берём только имена по принятым в проекте соглашениям (ВЕРХНИЙ_РЕГИСТР, gИмя,
// cfg): локальная переменная в другом регистре - это ровно тот баг, а шумного
// пересечения с обычными локальными именами тут нет.
const isGlobalName = n => /^[A-Z][A-Z0-9_]*$/.test(n) || /^g[A-Z]/.test(n) || n === 'cfg';
const appFiles = files.filter(f => f.rel.startsWith('Lib/') || f.rel === 'TextTools.ahk');
const globals = new Map();                     // нижний регистр -> как объявлено
for (const f of appFiles) {
  for (const code of f.code) {
    const m = /^([A-Za-z_][A-Za-z0-9_]*)\s*:=/.exec(code);
    if (m && isGlobalName(m[1])) globals.set(m[1].toLowerCase(), m[1]);
    const g = /^\s*global\s+(.+)$/i.exec(code);
    if (!g) continue;
    for (const part of g[1].split(',')) {
      const n = part.trim().match(/^[A-Za-z_][A-Za-z0-9_]*/);
      if (n && isGlobalName(n[0])) globals.set(n[0].toLowerCase(), n[0]);
    }
  }
}

const hits = [];
for (const f of files) {
  f.code.forEach((code, i) => {
    // присваивание переменной и переменная цикла for
    const patterns = [
      /(?:^|[\s(,])([A-Za-z_][A-Za-z0-9_]*)\s*:=/g,
      /\bfor\s+([A-Za-z_][A-Za-z0-9_]*)(?:\s*,\s*([A-Za-z_][A-Za-z0-9_]*))?\s+in\b/g,
    ];
    for (const re of patterns) {
      let m;
      while ((m = re.exec(code))) {
        for (const name of [m[1], m[2]]) {
          if (!name) continue;
          const decl = funcs.get(name.toLowerCase());
          // совпадение буква в букву - это сама функция, а не затенение
          if (decl && decl !== name) {
            hits.push(`${f.rel}:${i + 1}  переменная ${name} затеняет функцию ${decl}()`);
          }
        }
      }
    }
  });
}

// Параметр с именем функции - то же затенение, только незаметнее: внутри тела
// имя держит значение параметра, и глобальная функция становится невызываемой.
// Проверка по именам, а не по вызовам: вызов параметра ниже намеренно разрешён
// (параметр может держать func-объект), поэтому сам вызов ничего не покажет.
for (const f of files) {
  f.code.forEach((_, i) => {
    const d = declAt(f.code, i);
    if (!d) return;
    for (const p of paramNames(d.params)) {
      const decl = funcs.get(p.toLowerCase());
      if (decl) hits.push(`${f.rel}:${i + 1}  параметр ${p} функции ${d.name}() затеняет функцию ${decl}()`);
    }
  });
}

// --- вызовы несуществующих функций ---
//
// AHK не считает это ошибкой загрузки: имя без объявления он трактует как
// переменную, и падение "This global variable has not been assigned a value"
// случается только когда до строки доедет выполнение. /validate такое пропускает.
// Ловилось дважды: Loc() до объявления и ParseModelIds(), испорченный слепой
// заменой подстроки.
//
// Список сверен с указателем функций AHK v2.0. Лишнее имя в списке - пропущенная
// опечатка, недостающее - ложное срабатывание на штатном вызове: так в списке не
// хватало CoordMode (он есть в TextTools.ahk, но в бесскобочной форме) и StrPtr.
const builtins = new Set(('abs acos asin atan blockinput callbackcreate callbackfree caretgetpos ceil chr '
  + 'click clipwait comcall comobject '
  + 'comobjactive comobjarray comobjconnect comobjflags comobjfromptr comobjget comobjquery comobjtype comobjvalue '
  + 'comvalue controladditem controlchooseindex controlchoosestring controlclick controldeleteitem controlfinditem '
  + 'controlfocus controlgetchecked controlgetchoice '
  + 'controlgetclassnn controlgetenabled controlgetexstyle controlgetfocus controlgethwnd controlgetindex '
  + 'controlgetitems controlgetpos controlgetstyle controlgettext controlgetvisible controlhide controlmove '
  + 'controlsend controlsendtext controlsetchecked controlsetenabled controlsetexstyle controlsetstyle '
  + 'controlsettext controlshow coordmode cos critical dateadd datediff detecthiddentext detecthiddenwindows dircopy '
  + 'dircreate dirdelete direxist dirmove dirselect dllcall download driveeject drivegetcapacity '
  + 'drivegetfilesystem drivegetlabel drivegetlist drivegetserial drivegetspacefree drivegetstatus '
  + 'drivegetstatuscd drivegettype drivelock driveretract drivesetlabel driveunlock edit editgetcurrentcol '
  + 'editgetcurrentline editgetline editgetlinecount editgetselectedtext editpaste envget envset exit exp '
  + 'exitapp fileappend filecopy filecreateshortcut filedelete fileencoding fileexist filegetattrib '
  + 'filegetshortcut filegetsize filegettime filegetversion fileinstall filemove fileopen fileread filerecycle '
  + 'filerecycleempty fileselect filesetattrib filesettime float floor format formattime func getkeyname '
  + 'getkeysc getkeystate getkeyvk getmethod groupactivate groupadd groupclose groupdeactivate gui '
  + 'guictrlfromhwnd guifromhwnd hasbase hasmethod hasprop hotif hotifwinactive hotifwinexist hotifwinnotactive '
  + 'hotifwinnotexist hotkey hotstring il_add il_create il_destroy imagesearch inidelete iniread iniwrite '
  + 'inputbox inputhook installkeybdhook installmousehook instr integer isalnum isalpha isdigit isfloat isinteger '
  + 'islabel islower isnumber isobject isset issetref isspace istime isupper isxdigit keyhistory keywait '
  + 'listhotkeys listlines listvars listviewgetcontent ln loadpicture log ltrim map max menu menubar menufromhandle '
  + 'menuselect min mod monitorget monitorgetcount monitorgetname monitorgetprimary monitorgetworkarea '
  + 'mouseclick mouseclickdrag mousegetpos mousemove msgbox number numget numput objaddref objbindmethod '
  + 'objfromptr objfromptraddref objgetbase objgetcapacity objgetdatasize objhasownprop objownpropcount '
  + 'objownprops objptr objptraddref objrelease objsetbase objsetcapacity onclipboardchange onerror onexit '
  + 'onmessage ord outputdebug pause persistent pixelgetcolor pixelsearch postmessage processclose processexist '
  + 'processgetname processgetparent processgetpath processsetpriority processwait processwaitclose random '
  + 'regcreatekey regdelete regdeletekey regexmatch regexreplace regread regwrite reload round rtrim run runas '
  + 'runwait send sendevent sendinput sendlevel sendmessage sendmode sendplay sendtext setcapslockstate '
  + 'setcontroldelay setdefaultmousespeed setkeydelay setmousedelay setnumlockstate setregview '
  + 'setscrolllockstate setstorecapslockmode settimer settitlematchmode setwindelay setworkingdir shutdown sin '
  + 'sleep sort soundbeep soundgetinterface soundgetmute soundgetname soundgetvolume soundplay soundsetmute '
  + 'soundsetvolume splitpath sqrt statusbargettext statusbarwait strcompare strget strlen strlower strptr strput '
  + 'strreplace strsplit strtitle strupper substr suspend sysget sysgetipaddresses tan thread tooltip '
  + 'trayseticon traytip trim type varsetstrcapacity vercompare wingetalwaysontop '
  + 'winactivate winactivatebottom winactive winclose winexist wingetclass wingetclientpos wingetcontrols '
  + 'wingetcontrolshwnd wingetcount wingetexstyle wingetid wingetidlast wingetlist wingetminmax wingetpid '
  + 'wingetpos wingetprocessname wingetprocesspath wingetstyle wingettext wingettitle wingettranscolor '
  + 'wingettransparent winhide winkill winmaximize winminimize winminimizeall winminimizeallundo winmove '
  + 'winmovebottom winmovetop winredraw winrestore winsetalwaysontop winsetenabled winsetexstyle winsetregion '
  + 'winsetstyle winsettitle winsettransparent winsettranscolor winshow winwait winwaitactive winwaitclose '
  + 'winwaitnotactive array buffer class clipboardall error file object regexmatchinfo string '
  + 'valueerror typeerror memoryerror memberror methoderror propertyerror indexerror keyerror oserror targeterror '
  + 'timeouterror unseterror unsetitemerror unsetproperror zerodivisionerror').split(' ').filter(Boolean));

// Параметр функции тоже вызываем: в AHK он может держать func-объект и
// вызываться как less(a, b) — см. SortBy(arr, less) в Lib/http.ahk. Область
// учитываем по телу функции, а не глобально: иначе опечатка, совпавшая с
// именем чужого параметра, прошла бы незамеченной.
const paramScopes = new Map();            // rel -> [{from, to, names:Set, globals:Set}]
for (const f of files) {
  const scopes = [];
  let depth = 0, cur = null;
  f.code.forEach((code, i) => {
    if (depth === 0) {
      const d = declAt(f.code, i);
      if (d) {
        const names = new Set();
        for (const p of paramNames(d.params)) names.add(p.toLowerCase());
        cur = { from: i + 1, to: i + 1, names, globals: new Set() };
      }
    }
    if (cur) {
      const g = /^\s*global\s+(.+)$/i.exec(code);
      if (g) {
        for (const part of g[1].split(',')) {
          const n = part.trim().match(/^[A-Za-z_][A-Za-z0-9_]*/);
          if (n) cur.globals.add(n[0].toLowerCase());
        }
      }
    }
    for (const ch of code) {
      if (ch === '{') depth++;
      else if (ch === '}') depth--;
    }
    if (cur && depth === 0) {
      cur.to = i + 1;
      scopes.push(cur);
      cur = null;
    }
  });
  paramScopes.set(f.rel, scopes);
}

function isParamAt(rel, lineNo, name) {
  for (const sc of paramScopes.get(rel) || [])
    if (lineNo >= sc.from && lineNo <= sc.to && sc.names.has(name))
      return true;
  return false;
}

// `global APPS` в теле функции - заявленное намерение писать в общий список,
// регистр при этом AHK не различает, так что затенения нет
function declaresGlobal(rel, lineNo, name) {
  for (const sc of paramScopes.get(rel) || [])
    if (lineNo >= sc.from && lineNo <= sc.to && sc.globals.has(name))
      return true;
  return false;
}

for (const f of files) {
  f.code.forEach((code, i) => {
    if (/^\S/.test(code)) return;                   // строка верхнего уровня - это и есть глобал
    const re = /(?:^|[\s(,])([A-Za-z_][A-Za-z0-9_]*)\s*:=/g;
    let m;
    while ((m = re.exec(code))) {
      const decl = globals.get(m[1].toLowerCase());
      if (!decl || decl === m[1]) continue;
      if (declaresGlobal(f.rel, i + 1, m[1].toLowerCase())) continue;
      hits.push(`${f.rel}:${i + 1}  локальная переменная ${m[1]} затеняет глобальную ${decl}`);
    }
  });
}

// имена функций и методов, объявленных где угодно, включая вложенные
const defined = new Set(funcs.keys());
for (const f of files) {
  f.code.forEach((_, i) => {
    const d = declAt(f.code, i);
    if (d) defined.add(d.name.toLowerCase());
  });
}

const undef = [];
for (const f of files) {
  let inCont = false;                       // секция продолжения "( ... )" - это текст промпта
  f.lines.forEach((line, i) => {
    if (inCont) {
      if (/^\s*\)/.test(line)) inCont = false;
      return;
    }
    if (/^\s*\(\s*$/.test(line)) { inCont = true; return; }
    const code = f.code[i];
    let m;
    // без пробела перед скобкой: `Foo(` - вызов, а `key (a.b ? x : y)` -
    // конкатенация строк, которой в AHK не нужен оператор
    const re = /(?:^|[^A-Za-z0-9_.%])([A-Za-z_][A-Za-z0-9_]{2,})\(/g;
    while ((m = re.exec(code))) {
      const name = m[1].toLowerCase();
      if (defined.has(name) || builtins.has(name)) continue;
      if (isParamAt(f.rel, i + 1, name)) continue;
      // ключевые слова со скобками
      if (['if', 'while', 'for', 'return', 'and', 'or', 'not', 'switch', 'case', 'until', 'catch', 'throw',
           'new', 'loop', 'else', 'try', 'in', 'is', 'contains'].includes(name)) continue;
      undef.push(`${f.rel}:${i + 1}  вызов необъявленной функции ${m[1]}()`);
    }
  });
}

if (hits.length || undef.length) {
  if (hits.length) {
    console.log('НАЙДЕНО ЗАТЕНЕНИЕ ФУНКЦИЙ:');
    for (const h of hits) console.log('  ' + h);
  }
  if (undef.length) {
    console.log('ВЫЗОВЫ НЕОБЪЯВЛЕННЫХ ФУНКЦИЙ:');
    for (const h of undef) console.log('  ' + h);
  }
  process.exit(1);
}
const scopeTotal = [...paramScopes.values()].reduce((n, s) => n + s.length, 0);
console.log(`Чисто: файлов ${files.length}, функций верхнего уровня ${funcs.size}, объявлений всего ${defined.size},`
  + ` областей параметров ${scopeTotal} — затенения нет, необъявленных вызовов нет`);
