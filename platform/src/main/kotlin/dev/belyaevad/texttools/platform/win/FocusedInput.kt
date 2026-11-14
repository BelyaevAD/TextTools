package dev.belyaevad.texttools.platform.win

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.OaIdl
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.OleAuto
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.Variant
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

private const val UIA_EDIT = 50004
private const val UIA_COMBO = 50003
private const val UIA_DOCUMENT = 50030
private const val UIA_PROCESS = 30002
private const val UIA_CONTROL_TYPE = 30003
private const val UIA_ENABLED = 30010
private const val UIA_FOCUSABLE = 30009
private const val UIA_VALUE_READ_ONLY = 30046
private const val UIA_BOUNDS = 30001
private const val VT_I4 = 3
private const val VT_BOOL = 11
private const val VT_ARRAY = 0x2000
private const val GET_FOCUSED = 8
private const val GET_PROPERTY = 10

internal data class FocusedField(
    val processId: Int,
    val controlType: Int,
    val enabled: Boolean,
    val focusable: Boolean?,
    val readOnly: Boolean?,
)

/**
 * An Edit or ComboBox can take text. A Document can too when it says it is writable.
 * A read-only value, or a Document that does not say, is a page selection.
 */
internal fun acceptsTypedText(
    controlType: Int?,
    enabled: Boolean,
    focusable: Boolean?,
    readOnly: Boolean?,
): Boolean {
    if (!enabled || focusable == false) return false
    if (readOnly == true || controlType == null) return false
    if (controlType == UIA_EDIT || controlType == UIA_COMBO) return true
    return controlType == UIA_DOCUMENT && readOnly == false
}

/** The focused control of [hwnd] when it belongs to that window and can take typed text. */
internal fun focusedInputAcceptsText(hwnd: HWND): Boolean {
    if (!windowsHost()) return false
    val field = readFocusedField() ?: return false
    val pid = IntByReference()
    User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid)
    if (pid.value == 0 || field.processId != pid.value) return false
    return acceptsTypedText(field.controlType, field.enabled, field.focusable, field.readOnly)
}

/** Screen rectangle of the focused editable control in [hwnd]'s process. Null for a page selection. */
internal fun focusedInputBounds(hwnd: HWND): WindowBounds? {
    if (!windowsHost()) return null
    val read =
        try {
            queryFocusedRead()
        } catch (_: Throwable) {
            null
        } ?: return null
    val pid = IntByReference()
    User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid)
    if (pid.value == 0 || read.field.processId != pid.value) return null
    if (!acceptsTypedText(read.field.controlType, read.field.enabled, read.field.focusable, read.field.readOnly)) {
        return null
    }
    return read.bounds?.let(::userSpace)
}

private data class FocusedRead(
    val field: FocusedField,
    val bounds: WindowBounds?,
)

/** Focused UI Automation element. Null when Windows does not return one. */
internal fun readFocusedField(): FocusedField? {
    if (!windowsHost()) return null
    return try {
        queryFocusedRead()?.field
    } catch (_: Throwable) {
        null
    }
}

private fun queryFocusedRead(): FocusedRead? {
    val started = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, 0).toInt()
    val ownInit = started == 0
    try {
        val created = PointerByReference()
        val createdHr =
            Ole32.INSTANCE.CoCreateInstance(
                Guid.GUID("ff48dba4-60ef-4201-aa87-54103eef594e"),
                null,
                1,
                Guid.GUID("30cbe57d-d9d0-452a-ab13-7ac5ac4825ee"),
                created,
            )
        val client = created.value
        if (!COMUtils.SUCCEEDED(createdHr) || client == null) return null
        val automation = UiaCall(client)
        try {
            val focused = PointerByReference()
            if (!COMUtils.SUCCEEDED(automation.call(GET_FOCUSED, focused))) return null
            val elementPointer = focused.value ?: return null
            val element = UiaCall(elementPointer)
            try {
                val processId = element.intProperty(UIA_PROCESS) ?: return null
                val controlType = element.intProperty(UIA_CONTROL_TYPE) ?: return null
                val enabled = element.boolProperty(UIA_ENABLED) ?: return null
                val field =
                    FocusedField(
                        processId = processId,
                        controlType = controlType,
                        enabled = enabled,
                        focusable = element.boolProperty(UIA_FOCUSABLE),
                        readOnly = element.boolProperty(UIA_VALUE_READ_ONLY),
                    )
                val bounds =
                    if (acceptsTypedText(controlType, enabled, field.focusable, field.readOnly)) {
                        element.bounds()
                    } else {
                        null
                    }
                return FocusedRead(field, bounds)
            } finally {
                element.Release()
            }
        } finally {
            automation.Release()
        }
    } finally {
        if (ownInit) Ole32.INSTANCE.CoUninitialize()
    }
}

private class UiaCall(
    pointer: Pointer,
) : Unknown(pointer) {
    fun call(
        slot: Int,
        out: PointerByReference,
    ): WinNT.HRESULT = _invokeNativeObject(slot, arrayOf(pointer, out), WinNT.HRESULT::class.java) as WinNT.HRESULT

    fun intProperty(id: Int): Int? = property(id) { it.intValue() }

    fun boolProperty(id: Int): Boolean? =
        property(id) { variant ->
            when (variant.getVarType().toInt()) {
                VT_BOOL -> variant.booleanValue()
                VT_I4 -> variant.intValue() != 0
                else -> null
            }
        }

    fun bounds(): WindowBounds? {
        val variant = Variant.VARIANT()
        val hr =
            _invokeNativeObject(
                GET_PROPERTY,
                arrayOf(pointer, UIA_BOUNDS, variant),
                WinNT.HRESULT::class.java,
            ) as WinNT.HRESULT
        if (!COMUtils.SUCCEEDED(hr)) return null
        return try {
            variant.read()
            boundsOf(variant)
        } catch (_: Throwable) {
            null
        } finally {
            // VariantClear frees the array; JNA's auto-read afterwards would follow the stale parray pointer.
            variant.autoRead = false
            OleAuto.INSTANCE.VariantClear(variant)
        }
    }

    private fun <T> property(
        id: Int,
        read: (Variant.VARIANT) -> T?,
    ): T? {
        val variant = Variant.VARIANT()
        val hr =
            _invokeNativeObject(
                GET_PROPERTY,
                arrayOf(pointer, id, variant),
                WinNT.HRESULT::class.java,
            ) as WinNT.HRESULT
        if (!COMUtils.SUCCEEDED(hr)) return null
        return try {
            variant.read()
            if (variant.getVarType().toInt() != VT_I4 && variant.getVarType().toInt() != VT_BOOL) {
                null
            } else {
                read(variant)
            }
        } finally {
            OleAuto.INSTANCE.VariantClear(variant)
        }
    }
}

/** Reads left, top, width, height from a UIA bounds VARIANT. The VARIANT keeps ownership of the array. */
internal fun boundsOf(variant: Variant.VARIANT): WindowBounds? {
    if (variant.getVarType().toInt() and VT_ARRAY == 0) return null
    val array = variant.value as? OaIdl.SAFEARRAY ?: return null
    val first = array.getLBound(0)
    if (array.getUBound(0) - first + 1 < 4) return null
    val values = (0..3).map { (array.getElement(first + it) as Number).toDouble() }
    if (!values.all { it.isFinite() }) return null
    if (values[2] < 2.0 || values[3] < 2.0) return null
    return WindowBounds(values[0].toInt(), values[1].toInt(), (values[0] + values[2]).toInt(), (values[1] + values[3]).toInt())
}
