package dev.belyaevad.texttools.platform.win

import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Crypt32
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.Win32Exception
import com.sun.jna.platform.win32.WinCrypt
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.MSG
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import dev.belyaevad.texttools.platform.ClipIo
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Robot
import java.awt.Toolkit
import java.awt.Window
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.KeyEvent
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

private const val MOD_ALT = 0x0001
private const val MOD_CONTROL = 0x0002
private const val MOD_SHIFT = 0x0004
private const val MOD_WIN = 0x0008
private const val MOD_NOREPEAT = 0x4000
private const val VK_SPACE = 0x20
private const val WM_HOTKEY = 0x0312
private const val WM_QUIT = 0x0012
private const val WM_SWAP = 0x8000
private const val SW_RESTORE = 9
private const val ACTIVATE_WAIT_MILLIS = 2_000L
private const val ACTIVATE_POLL_MILLIS = 20L
private const val PASTE_SETTLE_MILLIS = 150L
private const val PASTE_AFTER_WRITE_MILLIS = 50L
private const val MODIFIER_WAIT_MILLIS = 1_000L
private const val MODIFIER_POLL_MILLIS = 10L
private const val CLIP_LOCK_TRIES = 5
private const val CLIP_LOCK_PAUSE_MILLIS = 20L
private const val ERROR_ALREADY_EXISTS = 183
private const val TOKEN_ELEVATION = 20
private const val CF_BITMAP = 2
private const val MENU_MARGIN_PX = 6
private const val RESULT_GAP_PX = 12
private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
private const val RUN_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
private const val RUN_VALUE = "TextTools"
private const val MUTEX_NAME = "Local\\TextTools"

internal const val MENU_HOTKEY_ID = 1
internal const val REPEAT_HOTKEY_ID = 2
private const val FACTORY_MENU_HOTKEY = "^!Space"
private const val FACTORY_REPEAT_HOTKEY = "^+Space"

private val shellClasses =
    setOf(
        "Shell_TrayWnd",
        "Shell_SecondaryTrayWnd",
        "Progman",
        "WorkerW",
        "#32768",
        "Windows.UI.Core.CoreWindow",
        "XamlExplorerHostIslandWindow",
        "Xaml_WindowedPopupClass",
    )

/** True for the Windows shell windows the old tracker ignored. */
fun isShellClass(className: String): Boolean = className in shellClasses

/** Which global chord fired. */
enum class GlobalChord {
    Menu,
    Repeat,
}

/** One RegisterHotKey chord. Built from an AutoHotkey hotkey string such as `^!Space`. */
internal data class Chord(
    val modifiers: Int,
    val virtualKey: Int,
)

/**
 * Parses `^` Ctrl, `!` Alt, `+` Shift, `#` Win, then a key name.
 * Returns null when the key is missing or unknown. Does not register anything.
 */
internal fun parseChord(spec: String): Chord? {
    if (spec.isEmpty()) return null
    var modifiers = 0
    var index = 0
    while (index < spec.length) {
        val bit =
            when (spec[index]) {
                '^' -> MOD_CONTROL
                '!' -> MOD_ALT
                '+' -> MOD_SHIFT
                '#' -> MOD_WIN
                else -> null
            } ?: break
        modifiers = modifiers or bit
        index++
    }
    val key = virtualKey(spec.substring(index)) ?: return null
    return Chord(modifiers, key)
}

internal interface HotkeyPort {
    fun register(
        id: Int,
        chord: Chord,
    ): Boolean

    fun unregister(id: Int)
}

/**
 * Swaps the menu and repeat chords.
 * The previous ids are released first. If both new chords are refused, the previous pair is put back
 * when it existed and the OS accepts it again.
 */
internal class HotkeyRegistration(
    private val port: HotkeyPort,
) {
    private var menu: Chord? = null
    private var repeat: Chord? = null

    fun menuReady(): Boolean = menu != null

    fun replace(
        nextMenu: Chord,
        nextRepeat: Chord?,
    ): Boolean {
        val previousMenu = menu
        val previousRepeat = repeat
        if (menu != null) port.unregister(MENU_HOTKEY_ID)
        if (repeat != null) port.unregister(REPEAT_HOTKEY_ID)
        val menuOk = port.register(MENU_HOTKEY_ID, nextMenu)
        val repeatOk = nextRepeat != null && port.register(REPEAT_HOTKEY_ID, nextRepeat)
        if (!menuOk && !repeatOk) {
            menu = restore(MENU_HOTKEY_ID, previousMenu)
            repeat = restore(REPEAT_HOTKEY_ID, previousRepeat)
            return false
        }
        menu = if (menuOk) nextMenu else null
        repeat = if (repeatOk) nextRepeat else null
        return menuOk && (repeatOk || nextRepeat == null)
    }

    fun clear() {
        if (menu != null) port.unregister(MENU_HOTKEY_ID)
        if (repeat != null) port.unregister(REPEAT_HOTKEY_ID)
        menu = null
        repeat = null
    }

    private fun restore(
        id: Int,
        chord: Chord?,
    ): Chord? {
        if (chord == null) return null
        return if (port.register(id, chord)) chord else null
    }
}

/**
 * Registers the two TextTools chords on a Windows message loop.
 * [start] registers the saved pair. The factory pair is only the fallback when that pair is refused.
 * [swap] replaces the live pair. Returns false when the strings are invalid, the loop is down,
 * or Windows did not accept both chords.
 */
class WinHotkeys internal constructor(
    private val onChord: (GlobalChord) -> Unit,
    private val port: HotkeyPort,
) {
    constructor(onChord: (GlobalChord) -> Unit) : this(onChord, User32HotkeyPort())

    @Volatile
    private var threadId: Int = 0

    @Volatile
    private var initialMenu: String = FACTORY_MENU_HOTKEY

    @Volatile
    private var initialRepeat: String = FACTORY_REPEAT_HOTKEY

    private val running = AtomicBoolean(false)
    private val both = AtomicBoolean(false)
    private val jobs = LinkedBlockingQueue<SwapJob>()
    private val registration = HotkeyRegistration(port)
    private var thread: Thread? = null

    /**
     * Starts the loop and registers [menu] and [repeat].
     * The factory pair is used only when [menu] cannot be parsed, or when Windows refuses the saved pair.
     * True when the menu chord is registered. The chords are registered on this thread, which is where Windows delivers them.
     */
    fun start(
        menu: String = FACTORY_MENU_HOTKEY,
        repeat: String = FACTORY_REPEAT_HOTKEY,
    ): Boolean {
        if (!windowsHost()) return false
        if (thread != null) return both.get()
        initialMenu = menu
        initialRepeat = repeat
        val ready = CountDownLatch(1)
        val worker =
            Thread(
                {
                    threadMain(ready)
                },
                "texttools-hotkeys",
            )
        worker.isDaemon = true
        thread = worker
        worker.start()
        if (!ready.await(5, TimeUnit.SECONDS)) return false
        return both.get()
    }

    /**
     * Drops the registered pair and installs [menu] and [repeat].
     * A string Windows would not understand leaves the current pair untouched.
     * When both registrations are refused, the previous pair stays if it can be registered again.
     */
    fun swap(
        menu: String,
        repeat: String,
    ): Boolean {
        val menuChord = parseChord(menu) ?: return false
        val repeatChord = if (repeat.isBlank()) null else parseChord(repeat) ?: return false
        if (!running.get()) return false
        val job = SwapJob(menuChord, repeatChord, CompletableFuture())
        jobs.offer(job)
        val posted = User32.INSTANCE.PostThreadMessage(threadId, WM_SWAP, null, null)
        if (posted == 0) {
            jobs.remove(job)
            return false
        }
        return try {
            job.answer.get(2, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        } catch (_: Exception) {
            false
        }
    }

    /** Asks the loop to exit and drop the chords. */
    fun stop() {
        val worker = thread ?: return
        val id = threadId
        if (id != 0) {
            User32.INSTANCE.PostThreadMessage(id, WM_QUIT, null, null)
        }
        worker.join(1_000)
    }

    private fun threadMain(ready: CountDownLatch) {
        try {
            threadId = Kernel32.INSTANCE.GetCurrentThreadId()
            // A chord registered before GetMessage is stored and then never delivered.
            User32.INSTANCE.PeekMessage(MSG(), null, 0, 0, 0)
            running.set(true)
            val menuChord = parseChord(initialMenu) ?: parseChord(FACTORY_MENU_HOTKEY)
            val repeatChord = if (initialRepeat.isBlank()) null else parseChord(initialRepeat) ?: parseChord(FACTORY_REPEAT_HOTKEY)
            if (menuChord != null) {
                jobs.offer(SwapJob(menuChord, repeatChord, CompletableFuture(), initial = true))
                val posted = User32.INSTANCE.PostThreadMessage(threadId, WM_SWAP, null, null)
                if (posted == 0) {
                    jobs.poll()
                    both.set(false)
                    ready.countDown()
                }
            } else {
                both.set(false)
                ready.countDown()
            }
            pumpMessages(ready)
        } finally {
            running.set(false)
            registration.clear()
            ready.countDown()
        }
    }

    private fun pumpMessages(ready: CountDownLatch) {
        val user32 = User32.INSTANCE
        val msg = MSG()
        while (true) {
            val code = user32.GetMessage(msg, null, 0, 0)
            if (code == 0 || code == -1) break
            if (msg.message == WM_HOTKEY) {
                when (msg.wParam.toInt()) {
                    MENU_HOTKEY_ID -> onChord(GlobalChord.Menu)
                    REPEAT_HOTKEY_ID -> onChord(GlobalChord.Repeat)
                }
            } else if (msg.message == WM_SWAP) {
                val job = jobs.poll()
                if (job != null) {
                    job.answer.complete(applySwap(job))
                    if (job.initial) ready.countDown()
                }
            }
            user32.TranslateMessage(msg)
            user32.DispatchMessage(msg)
        }
    }

    private fun applySwap(job: SwapJob): Boolean {
        var accepted = registration.replace(job.menu, job.repeat)
        if (job.initial && !registration.menuReady()) {
            val factoryMenu = parseChord(FACTORY_MENU_HOTKEY)
            val factoryRepeat = parseChord(FACTORY_REPEAT_HOTKEY).takeIf { job.repeat != null }
            if (factoryMenu != null) {
                accepted = registration.replace(factoryMenu, factoryRepeat)
            }
        }
        if (job.initial) both.set(registration.menuReady())
        return accepted
    }
}

private data class SwapJob(
    val menu: Chord,
    val repeat: Chord?,
    val answer: CompletableFuture<Boolean>,
    val initial: Boolean = false,
)

private class User32HotkeyPort : HotkeyPort {
    override fun register(
        id: Int,
        chord: Chord,
    ): Boolean = User32.INSTANCE.RegisterHotKey(null, id, chord.modifiers or MOD_NOREPEAT, chord.virtualKey)

    override fun unregister(id: Int) {
        User32.INSTANCE.UnregisterHotKey(null, id)
    }
}

/**
 * Claims `Local\TextTools`. True when this process is the first; the second caller gets false and does not own the mutex.
 * A failure to create the mutex continues, so this process does not kill anyone else. Nothing here stops another process.
 */
fun claimFirstInstance(): Boolean {
    if (!windowsHost()) return true
    val kernel = Kernel32.INSTANCE
    kernel.SetLastError(0)
    val created = kernel.CreateMutex(null, true, MUTEX_NAME) ?: return true
    if (kernel.GetLastError() == ERROR_ALREADY_EXISTS) {
        kernel.CloseHandle(created)
        return false
    }
    return true
}

/** Screen point in pixels. */
data class ScreenPoint(
    val x: Int,
    val y: Int,
)

/** One monitor work area. Right and bottom are exclusive, as in AHK `MonitorGetWorkArea`. */
data class WorkArea(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun containsX(x: Int): Boolean = x >= left && x < right

    fun containsY(y: Int): Boolean = y >= top && y < bottom
}

/**
 * Screen rectangle of the window the user was typing in.
 * Right and bottom are exclusive, as in [WorkArea].
 */
data class WindowBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun contains(point: ScreenPoint): Boolean = point.x >= left && point.x < right && point.y >= top && point.y < bottom
}

/**
 * Where a popup should open so it stays on [cursor]'s screen and does not cover [target].
 * Right, left, below and above are tried. The side nearest the cursor wins.
 * shortcut: a target that fills the work area is covered, because no side fits.
 */
fun popupBeside(
    target: WindowBounds,
    width: Int,
    height: Int,
    areas: List<WorkArea>,
    cursor: ScreenPoint,
): ScreenPoint {
    val centerX = (target.left + target.right) / 2
    val centerY = (target.top + target.bottom) / 2
    val area =
        areas.firstOrNull { it.containsX(cursor.x) && it.containsY(cursor.y) }
            ?: areas.firstOrNull { it.containsX(centerX) && it.containsY(centerY) }
            ?: nearestWorkArea(cursor.x, cursor.y, areas)
            ?: return ScreenPoint(target.right + RESULT_GAP_PX, target.top)
    val minX = area.left + MENU_MARGIN_PX
    val minY = area.top + MENU_MARGIN_PX
    val maxX = (area.right - width - MENU_MARGIN_PX).coerceAtLeast(minX)
    val maxY = (area.bottom - height - MENU_MARGIN_PX).coerceAtLeast(minY)

    fun along(
        value: Int,
        min: Int,
        max: Int,
    ) = value.coerceIn(min, max)

    val gap = RESULT_GAP_PX
    val candidates =
        listOf(
            ScreenPoint(target.right + gap, along(cursor.y, minY, maxY)),
            ScreenPoint(target.left - width - gap, along(cursor.y, minY, maxY)),
            ScreenPoint(along(cursor.x, minX, maxX), target.bottom + gap),
            ScreenPoint(along(cursor.x, minX, maxX), target.top - height - gap),
        )

    fun inside(point: ScreenPoint) = point.x in minX..maxX && point.y in minY..maxY

    fun overlaps(point: ScreenPoint): Boolean {
        val right = point.x + width
        val bottom = point.y + height
        return point.x < target.right && right > target.left && point.y < target.bottom && bottom > target.top
    }

    fun gapTo(point: ScreenPoint): Long {
        val dx =
            when {
                cursor.x < point.x -> (point.x - cursor.x).toLong()
                cursor.x > point.x + width -> (cursor.x - (point.x + width)).toLong()
                else -> 0L
            }
        val dy =
            when {
                cursor.y < point.y -> (point.y - cursor.y).toLong()
                cursor.y > point.y + height -> (cursor.y - (point.y + height)).toLong()
                else -> 0L
            }
        return dx * dx + dy * dy
    }

    val open = candidates.filter { inside(it) && !overlaps(it) }
    if (open.isNotEmpty()) return open.minBy { gapTo(it) }
    val nearest = candidates.minBy { gapTo(it) }
    return ScreenPoint(nearest.x.coerceIn(minX, maxX), nearest.y.coerceIn(minY, maxY))
}

/**
 * The rectangle the popup must avoid.
 * An input field wins. A caret inside the window is a short line in that field.
 * A page selection uses the cursor.
 */
fun anchorBox(
    field: WindowBounds?,
    caret: ScreenPoint?,
    cursor: ScreenPoint,
    window: WindowBounds?,
): WindowBounds {
    if (field != null && !pageSized(field, window)) return field
    val mark = caret?.takeUnless { it.x == 0 && it.y == 0 }
    if (mark != null && (window == null || window.contains(mark))) {
        return WindowBounds(mark.x, mark.y, mark.x + 2, mark.y + 20)
    }
    return WindowBounds(cursor.x, cursor.y, cursor.x + 1, cursor.y + 1)
}

private fun pageSized(
    box: WindowBounds,
    window: WindowBounds?,
): Boolean {
    if (window == null) return false
    val frameW = window.right - window.left
    val frameH = window.bottom - window.top
    if (frameW <= 0 || frameH <= 0) return false
    val wide = box.right - box.left
    val high = box.bottom - box.top
    return wide * 100 >= frameW * 85 && high * 100 >= frameH * 85
}

/** The box the action menu and the result window must sit beside. Read before focus moves. */
fun selectionAnchor(
    watch: ForegroundWatch,
    caret: ScreenPoint?,
    cursor: ScreenPoint,
    window: WindowBounds?,
): WindowBounds {
    val hwnd = watch.sourceNow()
    val field = if (hwnd == null) null else focusedInputBounds(hwnd) ?: controlBounds(hwnd)
    return anchorBox(field, caret, cursor, window)
}

internal fun controlBounds(hwnd: HWND): WindowBounds? {
    if (!windowsHost()) return null
    return try {
        val threadId = User32.INSTANCE.GetWindowThreadProcessId(hwnd, null)
        if (threadId == 0) return null
        val info = GuiThreadInfo()
        info.cbSize = info.size()
        if (!CaretUser32.INSTANCE.GetGUIThreadInfo(threadId, info)) return null
        val control = info.hwndCaret ?: info.hwndFocus ?: return null
        val rect = RECT()
        if (!User32.INSTANCE.GetWindowRect(control, rect)) return null
        if (rect.right <= rect.left || rect.bottom <= rect.top) return null
        userSpace(WindowBounds(rect.left, rect.top, rect.right, rect.bottom))
    } catch (_: Throwable) {
        null
    }
}

/**
 * Caret of [hwnd], in AWT user space like [java.awt.MouseInfo], read before the clipboard capture moves focus.
 * An empty caret rectangle means the control did not publish one.
 */
fun caretScreenPoint(hwnd: HWND): ScreenPoint? {
    if (!windowsHost()) return null
    return try {
        val threadId = User32.INSTANCE.GetWindowThreadProcessId(hwnd, null)
        if (threadId == 0) return null
        val info = GuiThreadInfo()
        info.cbSize = info.size()
        if (!CaretUser32.INSTANCE.GetGUIThreadInfo(threadId, info)) return null
        val caret = info.hwndCaret ?: return null
        val peer = caret.pointer ?: return null
        if (Pointer.nativeValue(peer) == 0L) return null
        val rect = info.rcCaret
        if (rect.right <= rect.left || rect.bottom <= rect.top) return null
        val point = POINT()
        point.x = rect.left
        point.y = rect.top
        if (!CaretUser32.INSTANCE.ClientToScreen(caret, point)) return null
        userSpace(ScreenPoint(point.x, point.y))
    } catch (_: Throwable) {
        null
    }
}

/** Work areas of the attached monitors, in the same pixels as [java.awt.MouseInfo]. */
fun currentWorkAreas(): List<WorkArea> {
    val toolkit = Toolkit.getDefaultToolkit()
    return GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { device ->
        val bounds = device.defaultConfiguration.bounds
        val insets = toolkit.getScreenInsets(device.defaultConfiguration)
        WorkArea(
            left = bounds.x + insets.left,
            top = bounds.y + insets.top,
            right = bounds.x + bounds.width - insets.right,
            bottom = bounds.y + bounds.height - insets.bottom,
        )
    }
}

internal fun nearestWorkArea(
    x: Int,
    y: Int,
    areas: List<WorkArea>,
): WorkArea? =
    areas.minByOrNull { area ->
        val dx =
            when {
                x < area.left -> area.left - x
                x >= area.right -> x - (area.right - 1)
                else -> 0
            }
        val dy =
            when {
                y < area.top -> area.top - y
                y >= area.bottom -> y - (area.bottom - 1)
                else -> 0
            }
        dx.toLong() + dy.toLong()
    }

/** HKCU Run command. Building it does not write the registry. */
data class RunCommand(
    val key: String,
    val valueName: String,
    val command: String,
)

/**
 * The autostart command for an installed TextTools exe.
 * The path is quoted. A script path is rejected. Nothing is written to this machine.
 */
fun installedAutostart(exe: Path): RunCommand {
    val absolute = exe.toAbsolutePath().normalize()
    val fileName = absolute.fileName.toString()
    require(fileName.endsWith(".exe", ignoreCase = true)) {
        "Autostart is only built for an installed exe"
    }
    val text = absolute.toString()
    require(!(text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0)) {
        "The installed exe path cannot be written into the autostart command"
    }
    return RunCommand(RUN_KEY, "TextTools", "\"$text\"")
}

/**
 * The running program, when it is an installed exe.
 * `java.exe` and `javaw.exe` are the development launch, not an install.
 */
fun currentInstalledExe(): Path? {
    val command =
        ProcessHandle
            .current()
            .info()
            .command()
            .orElse("")
    if (command.isBlank()) return null
    return installedExeOrNull(Path.of(command))
}

/** An installed exe path, or null for a script, Java, or a path that cannot be quoted. */
fun installedExeOrNull(path: Path): Path? {
    val name = path.fileName?.toString().orEmpty()
    if (!name.endsWith(".exe", ignoreCase = true)) return null
    if (name.equals("java.exe", ignoreCase = true) || name.equals("javaw.exe", ignoreCase = true)) return null
    return try {
        installedAutostart(path)
        path.toAbsolutePath().normalize()
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * The saved checkbox wins after the user has stored it.
 * Before that, the installer registry value is the choice. A missing value is off.
 */
fun autostartEnabled(
    stored: Boolean?,
    registryMatches: Boolean,
): Boolean = stored ?: registryMatches

/**
 * Writes or removes the HKCU Run value for an installed exe.
 * A development launch has no exe and this call does nothing.
 * A refused registry call is ignored, as the old `reg.exe` exit codes were: it must not stop the start or Settings.
 */
class WindowsRunKey {
    fun read(): String? {
        if (!windowsHost()) return null
        return try {
            if (!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_PATH, RUN_VALUE)) return null
            Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, RUN_PATH, RUN_VALUE)
        } catch (_: RuntimeException) {
            null // Win32Exception, or a value that is not a string: both read as "no TextTools command"
        }
    }

    fun apply(
        enabled: Boolean,
        exe: Path,
    ) {
        if (!windowsHost()) return
        val desired = if (enabled) installedAutostart(exe).command else null
        if (read() == desired) return
        try {
            if (desired == null) {
                Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_PATH, RUN_VALUE)
            } else {
                Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, RUN_PATH) // reg add created a missing Run key too
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, RUN_PATH, RUN_VALUE, desired)
            }
        } catch (_: Win32Exception) {
            // A refused write leaves the value as it was; the next start or save tries again.
        }
    }
}

/** API key and proxy URL. Both live in the current-user secret store, never in SQLite. */
enum class SecretSlot { ApiKey, ProxyUrl }

/** The OS secret store is missing or refused the call. The message never contains the secret. */
class SecretStoreException(
    message: String,
) : RuntimeException(message)

internal interface SecretProtector {
    fun protect(plain: ByteArray): ByteArray

    fun unprotect(blob: ByteArray): ByteArray
}

internal interface SecretBytes {
    fun read(slot: SecretSlot): ByteArray?

    fun write(
        slot: SecretSlot,
        blob: ByteArray,
    )
}

/**
 * Current-user secrets. [get] returns null when the slot is empty.
 * A missing directory or a protector that would store the plaintext is an error, not a password file.
 * Values are not logged.
 */
class UserSecrets internal constructor(
    private val bytes: SecretBytes,
    private val protector: SecretProtector,
) {
    fun get(slot: SecretSlot): String? {
        val blob = bytes.read(slot) ?: return null
        val plain = protector.unprotect(blob)
        return try {
            plain.toString(Charsets.UTF_8)
        } finally {
            plain.fill(0)
        }
    }

    fun put(
        slot: SecretSlot,
        value: String,
    ) {
        val plain = value.encodeToByteArray()
        try {
            val blob = protector.protect(plain)
            if (blob.contentEquals(plain)) {
                throw SecretStoreException("The Windows secret store is unavailable")
            }
            bytes.write(slot, blob)
        } finally {
            plain.fill(0)
        }
    }
}

internal fun userSecrets(
    root: Path,
    protector: SecretProtector,
): UserSecrets = UserSecrets(DirectorySecretBytes(root), protector)

fun userDataRoot(): Path {
    val roaming = System.getenv("APPDATA")
    if (roaming.isNullOrBlank()) return Path.of(System.getProperty("user.home"), "AppData", "Roaming", "TextTools")
    return Path.of(roaming, "TextTools")
}

/**
 * DPAPI store under `%APPDATA%\TextTools\secrets`.
 * Ciphertext only. If the directory cannot be created, this throws and writes nothing.
 */
fun openUserSecretStore(): UserSecrets {
    val root = userDataRoot().resolve("secrets")
    try {
        Files.createDirectories(root)
    } catch (_: IOException) {
        throw SecretStoreException("The Windows secret store is unavailable")
    }
    return userSecrets(root, DpapiProtector)
}

/** Remembers the last user window, skipping this process and the shell. */
class ForegroundWatch {
    @Volatile
    var source: HWND? = null
        private set

    /** Polls the foreground window. Same 400 ms interval as the old tracker. */
    fun start() {
        if (!windowsHost()) return
        Thread(
            {
                val user32 = User32.INSTANCE
                while (true) {
                    val hwnd = user32.GetForegroundWindow()
                    if (hwnd != null && !isOwnProcess(hwnd) && !isShellWindow(hwnd)) {
                        source = hwnd
                    }
                    Thread.sleep(400)
                }
            },
            "texttools-foreground",
        ).apply { isDaemon = true }.start()
    }

    /** The window that should receive Ctrl+C: the live one, or the last remembered. */
    fun sourceNow(): HWND? {
        val live = User32.INSTANCE.GetForegroundWindow()
        if (live != null && !isOwnProcess(live) && !isShellWindow(live)) {
            return live
        }
        return source
    }

    /** Caret of the source window, read before the clipboard capture changes focus. */
    fun caretNow(): ScreenPoint? {
        val hwnd = sourceNow() ?: return null
        return caretScreenPoint(hwnd)
    }
}

/** Clipboard bound to the remembered source window. The window handle stays inside this module. */
fun windowsClipboard(watch: ForegroundWatch): ClipIo = AwtClip { watch.sourceNow() }

/** Clipboard and synthetic Ctrl+C / Ctrl+V for the focused window. */
internal class AwtClip(
    private val focus: () -> HWND?,
) : ClipIo {
    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private val robot = Robot()

    @Volatile
    private var target: HWND? = null

    override fun readText(): String? =
        whenUnlocked {
            try {
                clipboard.getData(DataFlavor.stringFlavor) as? String ?: ""
            } catch (_: UnsupportedFlavorException) {
                "" // no text on the clipboard is not a lock
            } catch (_: IOException) {
                "" // advertised text that cannot be fetched is not a lock either
            }
        }

    override fun writeText(text: String): Boolean =
        whenUnlocked {
            clipboard.setContents(StringSelection(text), null)
            true
        } ?: false

    /** AWT throws IllegalStateException while another process holds the clipboard. */
    private fun <T : Any> whenUnlocked(block: () -> T): T? {
        // No retry pause if a caller ever reads the clipboard from the UI thread; capture and paste run on IO.
        val tries = if (EventQueue.isDispatchThread()) 1 else CLIP_LOCK_TRIES
        repeat(tries) { attempt ->
            try {
                return block()
            } catch (_: IllegalStateException) {
                if (attempt < tries - 1) Thread.sleep(CLIP_LOCK_PAUSE_MILLIS)
            } catch (_: IOException) {
                return null
            }
        }
        return null
    }

    override fun sendCopy() {
        if (!windowsHost()) return
        waitForModifiersUp()
        target = focus()
        chord(KeyEvent.VK_C)
    }

    // VK_MENU (Alt), VK_SHIFT, VK_LWIN, VK_RWIN. A held Ctrl is fine: Ctrl+C is what we send.
    private val heldModifiers = intArrayOf(0x12, 0x10, 0x5B, 0x5C)

    /** The hotkey's Alt, Shift or Win may still be down; the source would then see Ctrl+Alt+C or Ctrl+Shift+C. */
    private fun waitForModifiersUp() {
        val deadline = System.currentTimeMillis() + MODIFIER_WAIT_MILLIS
        while (System.currentTimeMillis() < deadline &&
            heldModifiers.any { User32.INSTANCE.GetAsyncKeyState(it).toInt() < 0 }
        ) {
            Thread.sleep(MODIFIER_POLL_MILLIS)
        }
    }

    override fun sendPaste() {
        if (!windowsHost()) return
        Thread.sleep(PASTE_AFTER_WRITE_MILLIS) // let clipboard watchers release the update before Ctrl+V
        chord(KeyEvent.VK_V)
    }

    override fun forgetSource() {
        target = null
    }

    override fun hasCapturedSource(): Boolean = target != null

    override fun activateSource(): Boolean {
        if (!windowsHost()) return false
        val hwnd = target ?: focus() ?: return false
        val user32 = User32.INSTANCE
        if (shouldRestoreWindow(isIconic(hwnd))) user32.ShowWindow(hwnd, SW_RESTORE)
        if (!user32.SetForegroundWindow(hwnd)) return false
        val deadline = System.currentTimeMillis() + ACTIVATE_WAIT_MILLIS
        while (!sameWindow(user32.GetForegroundWindow(), hwnd)) {
            if (System.currentTimeMillis() >= deadline) return false
            Thread.sleep(ACTIVATE_POLL_MILLIS)
        }
        Thread.sleep(PASTE_SETTLE_MILLIS)
        return true
    }

    override fun canAcceptText(): Boolean {
        val hwnd = target ?: focus() ?: return false
        if (caretScreenPoint(hwnd) != null) return true
        return focusedInputAcceptsText(hwnd)
    }

    override fun hasImageOrFiles(): Boolean = clipboardHasImageOrFiles()

    override fun elevatedForeign(): Boolean {
        if (!windowsHost()) return false
        val hwnd = target ?: focus() ?: return false
        val pid = IntByReference()
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid)
        val own = Kernel32.INSTANCE.GetCurrentProcessId()
        if (pid.value == 0 || pid.value == own) return false
        return blocksPaste(sameProcess = false, elevated = elevationOf(pid.value))
    }

    private fun chord(key: Int) {
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(key)
        robot.keyRelease(key)
        robot.keyRelease(KeyEvent.VK_CONTROL)
    }
}

/** A maximized or normal window is only focused. Restoring it would shrink a maximized one. */
internal fun shouldRestoreWindow(iconic: Boolean): Boolean = iconic

private fun isIconic(hwnd: HWND): Boolean {
    val iconic = NativeLibrary.getInstance("user32").getFunction("IsIconic")
    return iconic.invokeInt(arrayOf<Any>(hwnd)) != 0
}

private fun sameWindow(
    live: HWND?,
    expected: HWND,
): Boolean = live != null && live == expected

/**
 * A foreign window blocks paste when it is elevated or its token cannot be read.
 * The same process is never treated as foreign.
 */
internal fun blocksPaste(
    sameProcess: Boolean,
    elevated: Boolean?,
): Boolean {
    if (sameProcess) return false
    return elevated != false
}

private fun isOwnProcess(hwnd: HWND): Boolean {
    val pid = IntByReference()
    User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid)
    return pid.value != 0 && pid.value == Kernel32.INSTANCE.GetCurrentProcessId()
}

/**
 * Pixels of the invisible resize border that Windows adds around a decorated window.
 * [GetWindowRect] includes them and the visible frame from DWM does not.
 * A window that is not on screen yet, and every non-Windows host, returns zero.
 */
fun decoratedFrameSlack(window: Window): Pair<Int, Int> {
    if (!windowsHost() || !window.isDisplayable || !window.isShowing) return 0 to 0
    return try {
        val pointer = Native.getComponentPointer(window) ?: return 0 to 0
        val hwnd = HWND(pointer)
        val outer = RECT()
        if (!User32.INSTANCE.GetWindowRect(hwnd, outer)) return 0 to 0
        val visible = RECT()
        val status =
            DwmapiLib.INSTANCE.DwmGetWindowAttribute(
                hwnd,
                DwmapiLib.EXTENDED_FRAME_BOUNDS,
                visible,
                visible.size(),
            )
        if (status != 0) return 0 to 0
        val transform = window.graphicsConfiguration.defaultTransform
        frameSlack(
            outer.right - outer.left,
            outer.bottom - outer.top,
            visible.right - visible.left,
            visible.bottom - visible.top,
            transform.scaleX,
            transform.scaleY,
        )
    } catch (_: UnsatisfiedLinkError) {
        0 to 0
    } catch (_: Exception) {
        0 to 0
    }
}

/**
 * Non-negative gap from an outer rectangle down to the visible one, both in device pixels,
 * returned in AWT user space (divided by the window's scale, rounded up so the frame is never short).
 */
internal fun frameSlack(
    outerWidth: Int,
    outerHeight: Int,
    visibleWidth: Int,
    visibleHeight: Int,
    scaleX: Double,
    scaleY: Double,
): Pair<Int, Int> =
    ceil((outerWidth - visibleWidth).coerceAtLeast(0) / scaleX).toInt() to
        ceil((outerHeight - visibleHeight).coerceAtLeast(0) / scaleY).toInt()

private interface DwmapiLib : StdCallLibrary {
    @Suppress("ktlint:standard:function-naming")
    fun DwmGetWindowAttribute(
        hwnd: HWND,
        attribute: Int,
        rect: RECT,
        size: Int,
    ): Int

    companion object {
        val INSTANCE: DwmapiLib = Native.load("dwmapi", DwmapiLib::class.java)
        const val EXTENDED_FRAME_BOUNDS = 9
    }
}

/** True on a Windows JVM. Hotkeys and key injection stay here. */
internal fun windowsHost(): Boolean = windowsHost(System.getProperty("os.name").orEmpty())

internal fun windowsHost(osName: String): Boolean = osName.lowercase().contains("win")

/** File name at the end of a Windows or POSIX path. */
fun exeFileName(path: String): String {
    val trimmed = path.trim().trimEnd('\\', '/')
    val cut = maxOf(trimmed.lastIndexOf('\\'), trimmed.lastIndexOf('/'))
    return if (cut < 0) trimmed else trimmed.substring(cut + 1)
}

/** The tracker has a user window that can receive Ctrl+C. */
fun hasUserSource(watch: ForegroundWatch): Boolean = watch.sourceNow() != null

/**
 * Process, title, window class and screen rectangle captured before focus moves.
 * Empty strings and a null rectangle mean the source window was not available.
 */
data class SourceSnapshot(
    val proc: String,
    val title: String,
    val windowClass: String,
    val bounds: WindowBounds?,
)

/** The foreground user window, read before the clipboard capture and before this process opens a window. */
fun sourceSnapshot(watch: ForegroundWatch): SourceSnapshot {
    if (!windowsHost()) return SourceSnapshot("", "", "", null)
    val hwnd = watch.sourceNow() ?: return SourceSnapshot("", "", "", null)
    return SourceSnapshot(processImageName(hwnd), windowTitle(hwnd), windowClassName(hwnd), windowBounds(hwnd))
}

private fun windowBounds(hwnd: HWND): WindowBounds? {
    val rect = RECT()
    if (!User32.INSTANCE.GetWindowRect(hwnd, rect)) return null
    if (rect.right <= rect.left || rect.bottom <= rect.top) return null
    return userSpace(WindowBounds(rect.left, rect.top, rect.right, rect.bottom))
}

private fun windowClassName(hwnd: HWND): String {
    val buffer = CharArray(256)
    User32.INSTANCE.GetClassName(hwnd, buffer, buffer.size)
    return String(buffer).trimEnd('\u0000')
}

private fun windowTitle(hwnd: HWND): String {
    val buffer = CharArray(512)
    User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.size)
    return String(buffer).trimEnd('\u0000')
}

private fun processImageName(hwnd: HWND): String {
    val pid = IntByReference()
    User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid)
    if (pid.value == 0) return ""
    val process =
        Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid.value)
            ?: return ""
    try {
        val buffer = CharArray(1024)
        val size = IntByReference(buffer.size)
        if (!Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, buffer, size)) return ""
        return exeFileName(String(buffer, 0, size.value.coerceAtMost(buffer.size)))
    } finally {
        Kernel32.INSTANCE.CloseHandle(process)
    }
}

private fun isShellWindow(hwnd: HWND): Boolean = isShellClass(windowClassName(hwnd))

private fun virtualKey(name: String): Int? {
    if (name.isEmpty()) return null
    return when (name.lowercase()) {
        "space" -> VK_SPACE
        "enter" -> KeyEvent.VK_ENTER
        "tab" -> KeyEvent.VK_TAB
        "esc", "escape" -> KeyEvent.VK_ESCAPE
        else ->
            when {
                name.length == 1 && name[0].isLetter() -> KeyEvent.VK_A + (name[0].uppercaseChar() - 'A')
                name.length == 1 && name[0].isDigit() -> KeyEvent.VK_0 + (name[0] - '0')
                name.length in 2..3 && (name[0] == 'f' || name[0] == 'F') -> functionKey(name.drop(1))
                else -> null
            }
    }
}

private fun functionKey(number: String): Int? {
    val value = number.toIntOrNull() ?: return null
    if (value !in 1..12) return null
    return KeyEvent.VK_F1 + (value - 1)
}

private class DirectorySecretBytes(
    private val root: Path,
) : SecretBytes {
    override fun read(slot: SecretSlot): ByteArray? {
        val path = file(slot)
        // Only a file known to be absent is "no secret"; one that cannot be inspected is an error.
        if (Files.notExists(path)) return null
        return try {
            Files.readAllBytes(path)
        } catch (_: IOException) {
            throw SecretStoreException("The Windows secret store is unavailable")
        }
    }

    override fun write(
        slot: SecretSlot,
        blob: ByteArray,
    ) {
        if (!Files.isDirectory(root)) {
            throw SecretStoreException("The Windows secret store is unavailable")
        }
        try {
            Files.write(file(slot), blob)
        } catch (_: IOException) {
            throw SecretStoreException("The Windows secret store is unavailable")
        }
    }

    private fun file(slot: SecretSlot): Path =
        root.resolve(
            when (slot) {
                SecretSlot.ApiKey -> "api-key.dpapi"
                SecretSlot.ProxyUrl -> "proxy-url.dpapi"
            },
        )
}

internal object DpapiProtector : SecretProtector {
    override fun protect(plain: ByteArray): ByteArray = transform(plain, protect = true)

    override fun unprotect(blob: ByteArray): ByteArray = transform(blob, protect = false)

    private fun transform(
        input: ByteArray,
        protect: Boolean,
    ): ByteArray {
        if (!windowsHost()) throw SecretStoreException("The Windows secret store is unavailable")
        val output = WinCrypt.DATA_BLOB()
        val ok =
            if (protect) {
                Crypt32.INSTANCE.CryptProtectData(
                    WinCrypt.DATA_BLOB(input),
                    null,
                    null,
                    null,
                    null,
                    WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
                    output,
                )
            } else {
                Crypt32.INSTANCE.CryptUnprotectData(
                    WinCrypt.DATA_BLOB(input),
                    null,
                    null,
                    null,
                    null,
                    WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
                    output,
                )
            }
        if (!ok || output.pbData == null) {
            val code = Kernel32.INSTANCE.GetLastError()
            throw SecretStoreException("The Windows secret store is unavailable ($code)")
        }
        return try {
            output.getData() ?: ByteArray(0)
        } finally {
            Kernel32.INSTANCE.LocalFree(output.pbData)
        }
    }
}

private fun elevationOf(pid: Int): Boolean? =
    try {
        readElevation(pid)
    } catch (_: Throwable) {
        // JNA wraps a refused Structure field in Error. An unreadable token blocks paste; it must not exit the tray.
        null
    }

private fun readElevation(pid: Int): Boolean? {
    val process =
        Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid)
            ?: return null
    try {
        val token = WinNT.HANDLEByReference()
        if (!Advapi32.INSTANCE.OpenProcessToken(process, WinNT.TOKEN_QUERY, token)) return null
        try {
            val info = TokenElevationInfo()
            val returned = IntByReference()
            val ok =
                Advapi32.INSTANCE.GetTokenInformation(
                    token.value,
                    TOKEN_ELEVATION,
                    info,
                    info.size(),
                    returned,
                )
            if (!ok) return null
            info.read()
            return info.tokenIsElevated != 0
        } finally {
            Kernel32.INSTANCE.CloseHandle(token.value)
        }
    } finally {
        Kernel32.INSTANCE.CloseHandle(process)
    }
}

private interface CaretUser32 : StdCallLibrary {
    @Suppress("ktlint:standard:function-naming")
    fun GetGUIThreadInfo(
        idThread: Int,
        info: GuiThreadInfo,
    ): Boolean

    @Suppress("ktlint:standard:function-naming")
    fun ClientToScreen(
        hwnd: HWND,
        point: POINT,
    ): Boolean

    companion object {
        val INSTANCE: CaretUser32 = Native.load("user32", CaretUser32::class.java)
    }
}

/** Public on the JVM: JNA reads these fields from another package. A file-private class makes that read throw Error. */
internal class GuiThreadInfo : Structure() {
    @JvmField var cbSize: Int = 0

    @JvmField var flags: Int = 0

    @JvmField var hwndActive: HWND? = null

    @JvmField var hwndFocus: HWND? = null

    @JvmField var hwndCapture: HWND? = null

    @JvmField var hwndMenuOwner: HWND? = null

    @JvmField var hwndMoveSize: HWND? = null

    @JvmField var hwndCaret: HWND? = null

    @JvmField var rcCaret: RECT = RECT()

    override fun getFieldOrder(): List<String> =
        listOf(
            "cbSize",
            "flags",
            "hwndActive",
            "hwndFocus",
            "hwndCapture",
            "hwndMenuOwner",
            "hwndMoveSize",
            "hwndCaret",
            "rcCaret",
        )
}

internal class TokenElevationInfo : Structure() {
    @JvmField
    var tokenIsElevated: Int = 0

    override fun getFieldOrder(): List<String> = listOf("tokenIsElevated")
}

private fun clipboardHasImageOrFiles(): Boolean {
    val formats =
        intArrayOf(
            CF_BITMAP,
            WinUser.CF_DIB,
            WinUser.CF_DIBV5,
            WinUser.CF_TIFF,
            WinUser.CF_ENHMETAFILE,
            WinUser.CF_HDROP,
            pngFormat(),
        )
    return formats.any { format -> format != 0 && clipboardFormatAvailable(format) }
}

private fun pngFormat(): Int = User32.INSTANCE.RegisterClipboardFormat("PNG")

private fun clipboardFormatAvailable(format: Int): Boolean {
    val available = NativeLibrary.getInstance("user32").getFunction("IsClipboardFormatAvailable")
    return available.invokeInt(arrayOf<Any>(format)) != 0
}
