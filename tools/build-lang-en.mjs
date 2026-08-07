// Собирает ключи Loc("ключ", "русский") из Lib/*.ahk и пишет lang/en.ini.
// Английские значения заданы таблицей ниже; ключ без перевода останется русским
// (так и задумано: Loc() падает обратно на текст из кода).
//
// Запуск:  node tools/build-lang-en.mjs
//
// ВАЖНО: разбор построчный, поэтому Loc("ключ", "текст") должен умещаться в одну
// строку - склейку через "." сборщик не увидит и ключ молча выпадет из пакета.
import fs from 'node:fs';
import path from 'node:path';
const root = new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1');

const keys = new Map();
for (const f of fs.readdirSync(path.join(root, 'Lib'))) {
  const s = fs.readFileSync(path.join(root, 'Lib', f), 'utf8');
  const re = new RegExp('Loc\\("([^"]+)",\\s*"((?:[^"\\\\]|\\\\.)*)"\\)', 'g');
  let m;
  while ((m = re.exec(s))) if (!keys.has(m[1])) keys.set(m[1], m[2]);
}

const en = {
  'tray.history': 'History…',
  'tray.last': 'Show last result',
  'tray.stats': 'Usage statistics',
  'tray.editor': 'Action editor…',
  'tray.settings': 'Settings',
  'tray.log': 'Open log',
  'tray.folder': 'Open script folder',
  'tray.restart': 'Restart',
  'tray.exit': 'Exit',
  'menu.title': 'TextTools — choose an action',
  'menu.hint': 'The key on a button runs the action. Shift — show the result in a window instead of replacing the selection.',
  'menu.instruction': 'Instruction:',
  'menu.editor': '⚙  Action editor',
  'msg.busy': 'A request is already running',
  'msg.no_selection': 'No text selected',
  'msg.no_source_win': 'No window with text found',
  'msg.nothing_repeat': 'Nothing to repeat — no actions yet',
  'msg.request_failed': 'Request failed:',
  'msg.copied': 'Copied',
  'msg.no_results': 'No results yet',
  'run.preparing': 'preparing the request…',
  'run.done': 'done',
  'btn.save': 'Save',
  'btn.cancel': 'Cancel',
  'btn.close': 'Close',
  'btn.delete': 'Delete',
  'btn.copy': 'Copy',
  'res.quick': 'Quick:',
  'res.refine': 'Refine:',
  'res.send': 'Send',
  'res.paste': 'Paste into window',
  'hist.title': 'history',
  'hist.col.time': 'Time',
  'hist.col.action': 'Action',
  'hist.col.tokens': 'Tokens',
  'hist.col.cost': 'Cost',
  'hist.col.source': 'Source',
  'hist.col.result': 'Result',
  'hist.open_result': 'Open result',
  'hist.open_source': 'Open source',
  'hist.copy': 'Copy result',
  'set.title': 'settings',
  'set.tab.api': 'API',
  'set.tab.langs': 'Output languages',
  'set.tab.ui': 'Interface',
  'set.baseurl': 'Base URL (OpenAI-compatible API):',
  'set.model': 'Default model:',
  'set.models': 'Models available to actions (checked ones appear in the action card):',
  'set.col.model': 'Model',
  'set.col.ctx': 'Context',
  'set.col.in': '$/1M in',
  'set.col.out': '$/1M out',
  'set.models.hint': 'Context and prices — OpenRouter catalogue. Double-clicking a row also makes the model default.',
  'set.models_loaded': 'Provider models: {1}. New: {2}, removed as gone: {3}.',
  'set.models_check': 'Check the ones your actions need.',
  'set.load': 'Load from provider',
  'set.recommend': 'Recommended models',
  'set.rec_unknown': 'The provider was not recognised from the address, nothing to recommend.',
  'set.rec_done': 'Models checked for {1}: {2}.',
  'set.rec_missing': 'These were not in the list — added, but not verified with the provider:',
  'rec.role.mech': 'mechanical edits',
  'rec.role.main': 'main model',
  'rec.role.deep': 'analysis and review',
  'rec.role.alt': 'alternative to compare',
  'set.make_default': 'Set as default model',
  'set.pick_row': 'Select a model in the list first',
  'set.default_set': 'Default model: ',
  'set.test': 'Test API',
  'set.langs.hint': 'Checked languages appear in the "Output language" list of an action.',
  'set.col.lang': 'Language',
  'set.col.code': 'Code',
  'set.col.prompt': 'Name used in the prompt',
  'set.add_own': 'Add your own…',
  'set.ui_lang': 'Interface language:',
  'set.ui_lang.hint': 'Packs live in the lang folder next to the script. To add a language: copy en.ini, translate the values, restart.',
  'set.hotkey': 'Menu hotkey (^ = Ctrl, ! = Alt, + = Shift, # = Win):',
  'set.hotkey_repeat': 'Repeat-last-action hotkey:',
  'set.markdown': 'Render Markdown in the result window (uncheck if windows crash the script)',
  'set.loading_models': 'requesting the model list…',
  'set.models_failed': 'Could not get the model list:',
  'set.lang_dup': 'A language with this code already exists.',
  'set.lang_last': 'Cannot delete the last language.',
  'set.hotkey_empty': 'The menu hotkey cannot be empty',
  'set.hotkey_same': 'The menu hotkey and the repeat hotkey must differ',
  'set.hotkey_bad': 'Invalid hotkey:',
  'act.lang': 'Output language:',
  'act.lang.auto': 'same as the source text',
  'act.lang.auto.short': 'as in text',
  'act.lang.off': '(not in the list)',
  'act.langstyle': 'Default language style',
  'act.rustyle': 'Default language style',
  'action.proofread': 'Proofread',
  'action.rewrite': 'Rewrite',
  'action.concise': 'Shorten',
  'action.business': 'Business tone',
  'action.human': 'Sound human',
  'action.prompt': 'Prompt',
  'action.to_en': '→ EN',
  'action.to_ru': '→ RU',
  'action.summary': 'Summarize',
  'action.keypoints': 'Key points',
  'action.table': 'Table',
  'action.spec': 'Spec',
  'action.code': 'Explain code',
  'action.questions': 'Questions',
  'action.reply': 'Reply',
  'action.reqreview': 'Requirements review',
};

// названия заводских действий добавляем всегда: они не встречаются в L() дословно
for (const k of Object.keys(en)) if (k.startsWith('action.')) keys.set(k, '');

const lines = [
  '; TextTools - English interface pack.',
  '; Copy this file to lang\\<code>.ini and translate the values to add a language.',
  '; A missing key falls back to the Russian text built into the code, so a',
  '; partial translation is fine - nothing will be blank.',
  '',
  '[meta]',
  'name = English',
  '',
  '[ui]',
];
const missing = [];
for (const k of [...keys.keys()].sort()) {
  if (en[k] === undefined) { missing.push(k); continue; }
  lines.push(k + ' = ' + en[k]);
}
fs.mkdirSync(path.join(root, 'lang'), { recursive: true });
fs.writeFileSync(path.join(root, 'lang', 'en.ini'), lines.join('\r\n') + '\r\n', 'utf8');
console.log('ключей в коде:', keys.size, '| записано:', lines.length - 9);
if (missing.length) console.log('БЕЗ ПЕРЕВОДА:', missing.join(', '));
