package dev.belyaevad.texttools.platform.win

import com.sun.jna.NativeLibrary
import com.sun.jna.Structure
import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Crypt32
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinCrypt
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.MSG
import com.sun.jna.ptr.IntByReference
import dev.belyaevad.texttools.platform.ClipIo
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val MOD_ALT = 0x0001
private const val MOD_CONTROL = 0x0002
private const val MOD_SHIFT = 0x0004
private const val MOD_WIN = 0x0008
private const val VK_SPACE = 0x20
private const val WM_HOTKEY = 0x0312
private const val WM_QUIT = 0x0012
private const val WM_SWAP = 0x8000
private const val SW_RESTORE = 9
private const val ERROR_ALREADY_EXISTS = 183
private const val TOKEN_ELEVATION = 20
private const val CF_BITMAP = 2
private const val CARET_DROP_PX = 24
private const val MENU_MARGIN_PX = 6
private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
private const val MUTEX_NAME = "Local\\TextTools"

internal const val MENU_HOTKEY_ID = 1
internal const val REPEAT_HOTKEY_ID = 2

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

    fun hasAny(): Boolean = menu != null || repeat != null

    fun replace(
        nextMenu: Chord,
        nextRepeat: Chord,
    ): Boolean {
        val previousMenu = menu
        val previousRepeat = repeat
        if (menu != null) port.unregister(MENU_HOTKEY_ID)
        if (repeat != null) port.unregister(REPEAT_HOTKEY_ID)
        val menuOk = port.register(MENU_HOTKEY_ID, nextMenu)
        val repeatOk = port.register(REPEAT_HOTKEY_ID, nextRepeat)
        if (!menuOk && !repeatOk) {
            menu = restore(MENU_HOTKEY_ID, previousMenu)
            repeat = restore(REPEAT_HOTKEY_ID, previousRepeat)
            return false
        }
        menu = if (menuOk) nextMenu else null
        repeat = if (repeatOk) nextRepeat else null
        return menuOk && repeatOk
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
 * [start] uses Ctrl+Alt+Space and Ctrl+Shift+Space.
 * [swap] replaces that pair. Returns false when the strings are invalid, the loop is down,
 * or Windows did not accept both chords.
 */
class WinHotkeys internal constructor(
    private val onChord: (GlobalChord) -> Unit,
    private val port: HotkeyPort,
) {
    constructor(onChord: (GlobalChord) -> Unit) : this(onChord, User32HotkeyPort())

    @Volatile
    private var threadId: Int = 0

    private val running = AtomicBoolean(false)
    private val both = AtomicBoolean(false)
    private val jobs = LinkedBlockingQueue<SwapJob>()
    private val registration = HotkeyRegistration(port)
    private var thread: Thread? = null

    /**
     * Starts the loop. The chords are registered on that same thread, which is where Windows delivers them.
     * True only when both chords were accepted.
     */
    fun start(): Boolean {
        if (thread != null) return both.get()
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
        val repeatChord = parseChord(repeat) ?: return false
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
            User32.INSTANCE.PeekMessage(MSG(), null, 0, 0, 0)
            val menuChord = parseChord("^!Space")
            val repeatChord = parseChord("^+Space")
            val accepted =
                if (menuChord != null && repeatChord != null) {
                    registration.replace(menuChord, repeatChord)
                } else {
                    false
                }
            both.set(accepted)
            running.set(registration.hasAny())
            ready.countDown()
            if (running.get()) pumpMessages()
        } finally {
            running.set(false)
            registration.clear()
            ready.countDown()
        }
    }

    private fun pumpMessages() {
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
                    job.answer.complete(registration.replace(job.menu, job.repeat))
                }
            }
            user32.TranslateMessage(msg)
            user32.DispatchMessage(msg)
        }
    }
}

private data class SwapJob(
    val menu: Chord,
    val repeat: Chord,
    val answer: CompletableFuture<Boolean>,
)

private class User32HotkeyPort : HotkeyPort {
    override fun register(
        id: Int,
        chord: Chord,
    ): Boolean = User32.INSTANCE.RegisterHotKey(null, id, chord.modifiers, chord.virtualKey)

    override fun unregister(id: Int) {
        User32.INSTANCE.UnregisterHotKey(null, id)
    }
}

/** What the second process should do. It never terminates the process that already holds the mutex. */
enum class InstanceAction { Continue, FocusAndExit }

/** Focus-and-exit when another TextTools already owns the session. */
internal fun instanceAction(alreadyRunning: Boolean): InstanceAction =
    if (alreadyRunning) InstanceAction.FocusAndExit else InstanceAction.Continue

/**
 * Runs [focusFirst] and [exitThisProcess] only for [InstanceAction.FocusAndExit].
 * There is no path that stops another process.
 */
fun runInstanceAction(
    action: InstanceAction,
    focusFirst: () -> Unit,
    exitThisProcess: () -> Unit,
) {
    if (action == InstanceAction.FocusAndExit) {
        focusFirst()
        exitThisProcess()
    }
}

private object HeldMutex {
    var handle: WinNT.HANDLE? = null
}

/**
 * Claims `Local\TextTools`. The second caller gets [InstanceAction.FocusAndExit] and does not own the mutex.
 * A failure to create the mutex continues, so this process does not kill anyone else.
 */
fun claimFirstInstance(): InstanceAction {
    val kernel = Kernel32.INSTANCE
    kernel.SetLastError(0)
    val created = kernel.CreateMutex(null, true, MUTEX_NAME) ?: return InstanceAction.Continue
    if (kernel.GetLastError() == ERROR_ALREADY_EXISTS) {
        kernel.CloseHandle(created)
        return InstanceAction.FocusAndExit
    }
    HeldMutex.handle = created
    return InstanceAction.Continue
}

/** Moves the foreground to an existing TextTools window. Does nothing when that window is absent. */
fun focusFirstTextToolsWindow() {
    val hwnd = User32.INSTANCE.FindWindow(null, "TextTools") ?: return
    User32.INSTANCE.ShowWindow(hwnd, SW_RESTORE)
    User32.INSTANCE.SetForegroundWindow(hwnd)
}

/** Second launch focuses the first window and exits through [exitThisProcess]. */
fun enterSingleInstance(exitThisProcess: () -> Unit) {
    runInstanceAction(claimFirstInstance(), ::focusFirstTextToolsWindow, exitThisProcess)
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
 * Where the action menu should open. A usable caret wins and is moved down 24px; otherwise the cursor is used.
 * The point is clamped by 6px inside the work area that contains it.
 * Returns coordinates only. Does not open a window.
 */
fun menuPosition(
    caret: ScreenPoint?,
    cursor: ScreenPoint,
    menuWidth: Int,
    menuHeight: Int,
    workAreas: List<WorkArea>,
): ScreenPoint {
    val anchor = caret?.takeUnless { it.x <= 0 && it.y <= 0 }
    var x = anchor?.x ?: cursor.x
    var y = if (anchor != null) anchor.y + CARET_DROP_PX else cursor.y
    for (area in workAreas) {
        if (!area.containsX(x) || !area.containsY(y)) continue
        val minX = area.left + MENU_MARGIN_PX
        val minY = area.top + MENU_MARGIN_PX
        val maxX = (area.right - menuWidth - MENU_MARGIN_PX).coerceAtLeast(minX)
        val maxY = (area.bottom - menuHeight - MENU_MARGIN_PX).coerceAtLeast(minY)
        x = x.coerceIn(minX, maxX)
        y = y.coerceIn(minY, maxY)
        break
    }
    return ScreenPoint(x, y)
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
    if (!fileName.endsWith(".exe", ignoreCase = true)) {
        throw IllegalArgumentException("Автозапуск собирается только для установленного exe")
    }
    val text = absolute.toString()
    if (text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
        throw IllegalArgumentException("Путь установленного exe нельзя записать в команду автозапуска")
    }
    return RunCommand(RUN_KEY, "TextTools", "\"$text\"")
}

/** True when [command] is exactly the command [installedAutostart] would build for [exe]. */
fun autostartMatchesInstall(
    command: RunCommand,
    exe: Path,
): Boolean = command == installedAutostart(exe)

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
                throw SecretStoreException("Секретное хранилище Windows недоступно")
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

/**
 * DPAPI store under `%LOCALAPPDATA%\TextTools\secrets`.
 * Ciphertext only. If the directory cannot be created, this throws and writes nothing.
 */
fun openUserSecretStore(): UserSecrets {
    val local = System.getenv("LOCALAPPDATA")
    if (local.isNullOrBlank()) {
        throw SecretStoreException("Секретное хранилище Windows недоступно")
    }
    val root = Path.of(local, "TextTools", "secrets")
    try {
        Files.createDirectories(root)
    } catch (_: IOException) {
        throw SecretStoreException("Секретное хранилище Windows недоступно")
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
}

/** Clipboard bound to the remembered source window. The window handle stays inside this module. */
fun windowsClipboard(watch: ForegroundWatch): ClipIo = AwtClip { watch.sourceNow() }

/** Clipboard and synthetic Ctrl+C / Ctrl+V for the focused window. */
internal class AwtClip(
    private val focus: () -> HWND?,
) : ClipIo {
    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private val robot = Robot()

    override fun readText(): String? =
        try {
            clipboard.getData(DataFlavor.stringFlavor) as? String
        } catch (_: Exception) {
            null
        }

    override fun writeText(text: String): Boolean =
        try {
            clipboard.setContents(StringSelection(text), null)
            true
        } catch (_: Exception) {
            false
        }

    override fun sendCopy() = chord(KeyEvent.VK_C)

    override fun sendPaste() = chord(KeyEvent.VK_V)

    override fun activateSource(): Boolean {
        val hwnd = focus() ?: return false
        val user32 = User32.INSTANCE
        user32.ShowWindow(hwnd, SW_RESTORE)
        return user32.SetForegroundWindow(hwnd)
    }

    override fun hasImageOrFiles(): Boolean = clipboardHasImageOrFiles()

    override fun elevatedForeign(): Boolean {
        val hwnd = focus() ?: return false
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

private fun isShellWindow(hwnd: HWND): Boolean {
    val buffer = CharArray(256)
    User32.INSTANCE.GetClassName(hwnd, buffer, buffer.size)
    val name = String(buffer).trimEnd('\u0000')
    return isShellClass(name)
}

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
        if (!Files.isRegularFile(path)) return null
        return try {
            Files.readAllBytes(path)
        } catch (_: IOException) {
            throw SecretStoreException("Секретное хранилище Windows недоступно")
        }
    }

    override fun write(
        slot: SecretSlot,
        blob: ByteArray,
    ) {
        if (!Files.isDirectory(root)) {
            throw SecretStoreException("Секретное хранилище Windows недоступно")
        }
        try {
            Files.write(file(slot), blob)
        } catch (_: IOException) {
            throw SecretStoreException("Секретное хранилище Windows недоступно")
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
            throw SecretStoreException("Секретное хранилище Windows недоступно ($code)")
        }
        return try {
            output.getData() ?: ByteArray(0)
        } finally {
            Kernel32.INSTANCE.LocalFree(output.pbData)
        }
    }
}

private fun elevationOf(pid: Int): Boolean? {
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

private class TokenElevationInfo : Structure() {
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
