; TextTools - Menu actions (ACTIONS), quick refine buttons (QUICK_REFINE) and accelerators.
; Factory defaults: the live set lives in actions.ini and is edited from the tray.
; This file is the reset target. Included from TextTools.ahk; do not run alone.
;
; role — стабильный ключ mech/main/deep; в actions.ini пишется уже развёрнутый слаг
; (ADR 032: модель на уровне действия хранится именем, «по умолчанию» — пустой строкой).

MENU_KEYS := ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "q", "w", "e", "r", "t", "y"]

DEFAULT_ACTIONS := [
    {id: "proofread", name: "Вычитать",       icon: "✏",  prefix: "Proofread the following text:`n`n",                 task: TASK_PROOFREAD, lang: "auto", ru: true,  win: false, role: "mech"},
    {id: "rewrite", name: "Переписать",     icon: "🔄", prefix: "Clarify the following text:`n`n",                   task: TASK_REWRITE,   lang: "auto", ru: true,  win: false},
    {id: "concise", name: "Короче",         icon: "✂",  prefix: "Shorten the following text:`n`n",                   task: TASK_CONCISE,   lang: "auto", ru: true,  win: false, role: "mech"},
    {id: "business", name: "Деловой тон",    icon: "💼", prefix: "Rewrite the following as business correspondence:`n`n", task: TASK_BUSINESS,  lang: "auto", ru: true,  win: false},
    {id: "human", name: "По-человечески", icon: "🙂", prefix: "Make the following sound like a natural work message:`n`n", task: TASK_HUMAN, lang: "auto", ru: true,  win: false},
    {id: "prompt", name: "Промпт",         icon: "🤖", prefix: "Turn the following into a structured LLM prompt:`n`n", task: TASK_PROMPT,    lang: "auto", ru: true,  win: false},
    {id: "to_en", name: "→ EN",           icon: "🌐", prefix: "Produce English for the following text:`n`n",        task: TASK_EN,        lang: "en",   ru: false, win: false},
    {id: "to_ru", name: "→ RU",           icon: "🌐", prefix: "Produce Russian for the following text:`n`n",        task: TASK_RU,        lang: "ru",   ru: true,  win: false},
    {id: "summary", name: "Суммаризация",   icon: "📝", prefix: "Summarize the following for action:`n`n",            task: TASK_SUMMARY,   lang: "auto", ru: true,  win: true},
    {id: "keypoints", name: "Тезисы",         icon: "📋", prefix: "Extract working bullets from the following:`n`n",   task: TASK_KEYPOINTS, lang: "auto", ru: true,  win: true, role: "deep"},
    {id: "table", name: "Таблица",        icon: "▦",  prefix: "Turn the following into a Markdown table:`n`n",     task: TASK_TABLE,     lang: "auto", ru: true,  win: true, role: "deep"},
    {id: "spec", name: "Задача",         icon: "📄", prefix: "Turn the following into a development specification:`n`n", task: TASK_SPEC,  lang: "auto", ru: true,  win: true, role: "deep"},
    {id: "code", name: "Разбор кода",    icon: "🔍", prefix: "Explain the following code:`n`n",                    task: TASK_CODE,      lang: "auto", ru: true,  win: true, role: "deep"},
    {id: "questions", name: "Вопросы",        icon: "❓", prefix: "List clarifying questions for the following material:`n`n", task: TASK_QUESTIONS, lang: "auto", ru: true,  win: true, role: "deep"},
    {id: "reply", name: "Ответ",          icon: "✉",  prefix: "Draft a reply to the following incoming message:`n`n", task: TASK_REPLY,   lang: "auto", ru: true,  win: true, role: "deep"},
    {id: "reqreview", name: "Ревью требований", icon: "🧐", prefix: "Review the following specification for defects:`n`n", task: TASK_REQREVIEW, lang: "auto", ru: true,  win: true, role: "deep"},
]

for i, a in DEFAULT_ACTIONS
    a.key := (i <= MENU_KEYS.Length) ? MENU_KEYS[i] : ""

CUSTOM_ACTION := {name: "Custom", icon: "", prefix: "Apply the user's instruction to the following text:`n`n", task: TASK_CUSTOM, lang: "auto", ru: true, win: false}

DEFAULT_QUICK := [
    {label: "Короче",     instr: "Cut length noticeably. Drop filler and repeats, but keep every requirement, condition, number, name and open question."},
    {label: "Подробнее",  instr: "Expand with the steps, conditions and consequences a reader needs. Add no facts missing from the source; if something is unknown, say so."},
    {label: "Точнее",     instr: "Tighten wording: replace vague words with exact terms, name the actor and condition in each claim, remove ambiguity. Invent no new facts."},
    {label: "Проще",      instr: "Simplify for a reader outside the team: shorter sentences, plain words where jargon is optional. Keep facts, numbers and meaningful technical terms."},
    {label: "Формальнее", instr: "Raise the register to formal business correspondence without changing the content."},
    {label: "Списком",    instr: "Reshape the same content as a compact list, one idea per item. Do not add or drop anything."},
    {label: "Другой вариант", instr: "Give another version of the same result: different structure and wording, same facts and same task. Avoid repeating the previous phrasing."},
]
