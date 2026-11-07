package dev.belyaevad.texttools.app.ui.editor

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.ed_act_edit
import texttools.app.generated.resources.ed_act_new
import texttools.app.generated.resources.ed_add
import texttools.app.generated.resources.ed_app_desc
import texttools.app.generated.resources.ed_app_edit
import texttools.app.generated.resources.ed_app_hint
import texttools.app.generated.resources.ed_app_new
import texttools.app.generated.resources.ed_app_order
import texttools.app.generated.resources.ed_app_order_hint
import texttools.app.generated.resources.ed_app_proc
import texttools.app.generated.resources.ed_app_proc_req
import texttools.app.generated.resources.ed_app_title
import texttools.app.generated.resources.ed_app_title_hint
import texttools.app.generated.resources.ed_cancel
import texttools.app.generated.resources.ed_col_hint
import texttools.app.generated.resources.ed_col_instr
import texttools.app.generated.resources.ed_col_key
import texttools.app.generated.resources.ed_col_lang
import texttools.app.generated.resources.ed_col_model
import texttools.app.generated.resources.ed_col_name
import texttools.app.generated.resources.ed_col_proc
import texttools.app.generated.resources.ed_col_result
import texttools.app.generated.resources.ed_col_title
import texttools.app.generated.resources.ed_copy_suffix
import texttools.app.generated.resources.ed_del_action
import texttools.app.generated.resources.ed_del_app
import texttools.app.generated.resources.ed_delete
import texttools.app.generated.resources.ed_down
import texttools.app.generated.resources.ed_dup
import texttools.app.generated.resources.ed_edit
import texttools.app.generated.resources.ed_field_key
import texttools.app.generated.resources.ed_field_lang
import texttools.app.generated.resources.ed_field_model
import texttools.app.generated.resources.ed_field_name
import texttools.app.generated.resources.ed_field_prefix
import texttools.app.generated.resources.ed_field_prompt
import texttools.app.generated.resources.ed_field_result
import texttools.app.generated.resources.ed_grid_order
import texttools.app.generated.resources.ed_hint
import texttools.app.generated.resources.ed_icon
import texttools.app.generated.resources.ed_key_none
import texttools.app.generated.resources.ed_key_swap
import texttools.app.generated.resources.ed_key_take
import texttools.app.generated.resources.ed_key_taken
import texttools.app.generated.resources.ed_key_taken_body
import texttools.app.generated.resources.ed_key_taken_swap
import texttools.app.generated.resources.ed_key_taken_take
import texttools.app.generated.resources.ed_lang_auto
import texttools.app.generated.resources.ed_lang_auto_short
import texttools.app.generated.resources.ed_lang_off
import texttools.app.generated.resources.ed_lang_style
import texttools.app.generated.resources.ed_last_action
import texttools.app.generated.resources.ed_model_default
import texttools.app.generated.resources.ed_model_hint
import texttools.app.generated.resources.ed_model_hint_empty
import texttools.app.generated.resources.ed_name_empty
import texttools.app.generated.resources.ed_no
import texttools.app.generated.resources.ed_no_hint
import texttools.app.generated.resources.ed_ok
import texttools.app.generated.resources.ed_prefix_hint
import texttools.app.generated.resources.ed_prompt_empty
import texttools.app.generated.resources.ed_quick_edit
import texttools.app.generated.resources.ed_quick_instr
import texttools.app.generated.resources.ed_quick_name_hint
import texttools.app.generated.resources.ed_quick_new
import texttools.app.generated.resources.ed_quick_required
import texttools.app.generated.resources.ed_reset
import texttools.app.generated.resources.ed_reset_ask
import texttools.app.generated.resources.ed_result_open
import texttools.app.generated.resources.ed_result_replace
import texttools.app.generated.resources.ed_result_window
import texttools.app.generated.resources.ed_row_order
import texttools.app.generated.resources.ed_save
import texttools.app.generated.resources.ed_tab_actions
import texttools.app.generated.resources.ed_tab_apps
import texttools.app.generated.resources.ed_tab_quick
import texttools.app.generated.resources.ed_up
import texttools.app.generated.resources.ed_window_title
import texttools.app.generated.resources.ed_yes

internal enum class Ed(
    val res: StringResource,
) {
    ACT_EDIT(Res.string.ed_act_edit),
    ACT_NEW(Res.string.ed_act_new),
    ADD(Res.string.ed_add),
    APP_DESC(Res.string.ed_app_desc),
    APP_EDIT(Res.string.ed_app_edit),
    APP_HINT(Res.string.ed_app_hint),
    APP_NEW(Res.string.ed_app_new),
    APP_ORDER(Res.string.ed_app_order),
    APP_ORDER_HINT(Res.string.ed_app_order_hint),
    APP_PROC(Res.string.ed_app_proc),
    APP_PROC_REQ(Res.string.ed_app_proc_req),
    APP_TITLE(Res.string.ed_app_title),
    APP_TITLE_HINT(Res.string.ed_app_title_hint),
    CANCEL(Res.string.ed_cancel),
    COL_HINT(Res.string.ed_col_hint),
    COL_INSTR(Res.string.ed_col_instr),
    COL_KEY(Res.string.ed_col_key),
    COL_LANG(Res.string.ed_col_lang),
    COL_MODEL(Res.string.ed_col_model),
    COL_NAME(Res.string.ed_col_name),
    COL_PROC(Res.string.ed_col_proc),
    COL_RESULT(Res.string.ed_col_result),
    COL_TITLE(Res.string.ed_col_title),
    COPY_SUFFIX(Res.string.ed_copy_suffix),
    DEL_ACTION(Res.string.ed_del_action),
    DEL_APP(Res.string.ed_del_app),
    DELETE(Res.string.ed_delete),
    DOWN(Res.string.ed_down),
    DUP(Res.string.ed_dup),
    EDIT(Res.string.ed_edit),
    FIELD_KEY(Res.string.ed_field_key),
    FIELD_LANG(Res.string.ed_field_lang),
    FIELD_MODEL(Res.string.ed_field_model),
    FIELD_NAME(Res.string.ed_field_name),
    FIELD_PREFIX(Res.string.ed_field_prefix),
    FIELD_PROMPT(Res.string.ed_field_prompt),
    FIELD_RESULT(Res.string.ed_field_result),
    GRID_ORDER(Res.string.ed_grid_order),
    HINT(Res.string.ed_hint),
    ICON(Res.string.ed_icon),
    KEY_NONE(Res.string.ed_key_none),
    KEY_SWAP(Res.string.ed_key_swap),
    KEY_TAKE(Res.string.ed_key_take),
    KEY_TAKEN(Res.string.ed_key_taken),
    KEY_TAKEN_BODY(Res.string.ed_key_taken_body),
    KEY_TAKEN_SWAP(Res.string.ed_key_taken_swap),
    KEY_TAKEN_TAKE(Res.string.ed_key_taken_take),
    LANG_AUTO(Res.string.ed_lang_auto),
    LANG_AUTO_SHORT(Res.string.ed_lang_auto_short),
    LANG_OFF(Res.string.ed_lang_off),
    LANG_STYLE(Res.string.ed_lang_style),
    LAST_ACTION(Res.string.ed_last_action),
    MODEL_DEFAULT(Res.string.ed_model_default),
    MODEL_HINT(Res.string.ed_model_hint),
    MODEL_HINT_EMPTY(Res.string.ed_model_hint_empty),
    NAME_EMPTY(Res.string.ed_name_empty),
    NO(Res.string.ed_no),
    NO_HINT(Res.string.ed_no_hint),
    OK(Res.string.ed_ok),
    PREFIX_HINT(Res.string.ed_prefix_hint),
    PROMPT_EMPTY(Res.string.ed_prompt_empty),
    QUICK_EDIT(Res.string.ed_quick_edit),
    QUICK_INSTR(Res.string.ed_quick_instr),
    QUICK_NAME_HINT(Res.string.ed_quick_name_hint),
    QUICK_NEW(Res.string.ed_quick_new),
    QUICK_REQUIRED(Res.string.ed_quick_required),
    RESET(Res.string.ed_reset),
    RESET_ASK(Res.string.ed_reset_ask),
    RESULT_OPEN(Res.string.ed_result_open),
    RESULT_REPLACE(Res.string.ed_result_replace),
    RESULT_WINDOW(Res.string.ed_result_window),
    ROW_ORDER(Res.string.ed_row_order),
    SAVE(Res.string.ed_save),
    TAB_ACTIONS(Res.string.ed_tab_actions),
    TAB_APPS(Res.string.ed_tab_apps),
    TAB_QUICK(Res.string.ed_tab_quick),
    UP(Res.string.ed_up),
    WINDOW_TITLE(Res.string.ed_window_title),
    YES(Res.string.ed_yes),
}

@Composable
internal fun tr(key: Ed): String = stringResource(key.res)

@Composable
internal fun tr(
    key: Ed,
    arg: Any,
): String = stringResource(key.res, arg)

@Composable
internal fun tr(
    key: Ed,
    first: Any,
    second: Any,
): String = stringResource(key.res, first, second)
