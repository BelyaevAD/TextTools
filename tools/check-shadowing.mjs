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

// имена функций верхнего уровня: строка начинается с имени и открывающей скобки
const funcs = new Map();                       // нижний регистр -> как объявлено
for (const f of files) {
  for (const line of readFileSync(f.path, 'utf8').split(/\r?\n/)) {
    const m = /^([A-Za-z_][A-Za-z0-9_]*)\s*\(/.exec(line);
    if (m) funcs.set(m[1].toLowerCase(), m[1]);
  }
}

const hits = [];
for (const f of files) {
  const lines = readFileSync(f.path, 'utf8').split(/\r?\n/);
  lines.forEach((line, i) => {
    const code = line.replace(/;.*$/, '');
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

// --- вызовы несуществующих функций ---
//
// AHK не считает это ошибкой загрузки: имя без объявления он трактует как
// переменную, и падение "This global variable has not been assigned a value"
// случается только когда до строки доедет выполнение. /validate такое пропускает.
// Ловилось дважды: Loc() до объявления и ParseModelIds(), испорченный слепой
// заменой подстроки.
const builtins = new Set(('abs asc atan blockinput break caretgetpos ceil chr click clipwait comcall comobject '
  + 'comobjactive comobjarray comobjconnect comobjflags comobjfromptr comobjget comobjquery comobjtype comobjvalue '
  + 'controladditem controlchoosestring controlclick controlfocus controlgetchecked controlgetchoice '
  + 'controlgetclassnn controlgetenabled controlgetexstyle controlgetfocus controlgethwnd controlgetindex '
  + 'controlgetitems controlgetpos controlgetstyle controlgettext controlgetvisible controlhide controlmove '
  + 'controlsend controlsendtext controlsetchecked controlsetenabled controlsetexstyle controlsetstyle '
  + 'controlsettext controlshow cos dateadd datediff detecthiddentext detecthiddenwindows dirconvert dircopy '
  + 'dircreate dirdelete direxist dirmove dirselect dllcall download drivedeject driveeject drivegetcapacity '
  + 'drivegetfilesystem drivegetlabel drivegetlist drivegetserial drivegetspacefree drivegetstatus '
  + 'drivegetstatuscd drivegettype drivelock driveretract drivesetlabel driveunlock edit editgetcurrentcol '
  + 'editgetcurrentline editgetline editgetlinecount editgetselectedtext editpaste envget envset exception exp '
  + 'exitapp fileappend filecopy filecreateshortcut filedelete fileencoding fileexist filegetattrib '
  + 'filegetshortcut filegetsize filegettime filegetversion fileinstall filemove fileopen fileread filerecycle '
  + 'filerecycleempty fileselect filesetattrib filesettime float floor format formattime func getkeyname '
  + 'getkeysc getkeystate getkeyvk getmethod groupactivate groupadd groupclose groupdeactivate gui guictrl '
  + 'guictrlfromhwnd guifromhwnd hasbase hasmethod hasprop hotif hotifwinactive hotifwinexist hotifwinnotactive '
  + 'hotifwinnotexist hotkey hotstring il_add il_create il_destroy imagesearch inidelete iniread iniwrite '
  + 'inputbox inputhook installkeybdhook installmousehook instr integer isalnum isalpha isdigit isfloat isinteger '
  + 'islabel islower isnumber isobject isset issetref isspace istime isupper isxdigit keyhistory keywait '
  + 'listhotkeys listlines listvars listviewgetcontent ln log ltrim map max mem menu menubar menufromhandle '
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
  + 'soundsetvolume splitpath sqrt statusbargettext statusbarwait strcompare strget strlen strlower strput '
  + 'strreplace strsplit strtitle strupper subst substr suspend sysget sysgetipaddresses tan thread tooltip '
  + 'trayseticon traytip trim type validatenumber varsetstrcapacity verifyclass vercompare wingetalwaysontop '
  + 'winactivate winactivatebottom winactive winclose winexist wingetclass wingetclientpos wingetcontrols '
  + 'wingetcontrolshwnd wingetcount wingetexstyle wingetid wingetidlast wingetlist wingetminmax wingetpid '
  + 'wingetpos wingetprocessname wingetprocesspath wingetstyle wingettext wingettitle wingettranspaint '
  + 'wingettransparent winhide winkill winmaximize winminimize winminimizeall winminimizeallundo winmove '
  + 'winmovebottom winmovetop winredraw winrestore winsetalwaysontop winsetenabled winsetexstyle winsetregion '
  + 'winsetstyle winsettitle winsettransparent winsettranscolor winshow winwait winwaitactive winwaitclose '
  + 'winwaitnotactive array buffer class error file gui inputhook menu object regexmatchinfo string '
  + 'valueerror typeerror memberror methoderror propertyerror indexerror keyerror oserror targeterror '
  + 'timeouterror unseterror zerodivisionerror').split(' ').filter(Boolean));

// Параметр функции тоже вызываем: в AHK он может держать func-объект и
// вызываться как less(a, b) — см. SortBy(arr, less) в Lib/http.ahk. Область
// учитываем по телу функции, а не глобально: иначе опечатка, совпавшая с
// именем чужого параметра, прошла бы незамеченной.
const paramScopes = new Map();            // rel -> [{from, to, names:Set}]
for (const f of files) {
  const lines = readFileSync(f.path, 'utf8').split(/\r?\n/);
  const scopes = [];
  let depth = 0, cur = null;
  lines.forEach((line, i) => {
    const code = line.replace(/;.*$/, '');
    if (depth === 0) {
      const d = /^\s*[A-Za-z_][A-Za-z0-9_]*\s*\(([^()]*)\)\s*\{\s*$/.exec(code);
      if (d) {
        const names = new Set();
        for (const part of d[1].split(',')) {
          const pm = /^\s*&?\s*([A-Za-z_][A-Za-z0-9_]*)/.exec(part);
          if (pm) names.add(pm[1].toLowerCase());
        }
        cur = { from: i + 1, to: i + 1, names };
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

// имена функций и методов, объявленных где угодно, включая вложенные
const defined = new Set(funcs.keys());
for (const f of files) {
  for (const line of readFileSync(f.path, 'utf8').split(/\r?\n/)) {
    const m = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\([^()]*\)\s*\{?\s*$/.exec(line.replace(/;.*$/, ''));
    if (m) defined.add(m[1].toLowerCase());
    const fat = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\([^()]*\)\s*=>/.exec(line);
    if (fat) defined.add(fat[1].toLowerCase());
  }
}

const undef = [];
for (const f of files) {
  const lines = readFileSync(f.path, 'utf8').split(/\r?\n/);
  let inCont = false;                       // секция продолжения "( ... )" - это текст промпта
  lines.forEach((line, i) => {
    if (inCont) {
      if (/^\s*\)/.test(line)) inCont = false;
      return;
    }
    if (/^\s*\(\s*$/.test(line)) { inCont = true; return; }
    const code = line.replace(/;.*$/, '').replace(/"(?:[^"\\]|\\.)*"/g, '""').replace(/'(?:[^'\\]|\\.)*'/g, "''");
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
    console.log('НАЙДЕНО ЗАТЕНЕНИЕ ФУНКЦИЙ ПЕРЕМЕННЫМИ:');
    for (const h of hits) console.log('  ' + h);
  }
  if (undef.length) {
    console.log('ВЫЗОВЫ НЕОБЪЯВЛЕННЫХ ФУНКЦИЙ:');
    for (const h of undef) console.log('  ' + h);
  }
  process.exit(1);
}
console.log(`Чисто: файлов ${files.length}, функций ${funcs.size} — затенения нет, необъявленных вызовов нет`);
