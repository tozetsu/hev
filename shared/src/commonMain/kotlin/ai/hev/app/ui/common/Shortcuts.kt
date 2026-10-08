package ai.hev.app.ui.common

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** A key, with or without Ctrl, and how tooltips spell it. */
class Shortcut internal constructor(private val keys: Set<Key>, private val ctrl: Boolean, val label: String) {
    fun matches(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyDown && event.key in keys && event.isCtrlPressed == ctrl &&
            !event.isAltPressed && !event.isShiftPressed && !event.isMetaPressed
}

/** The desktop app's keyboard shortcuts. */
object Shortcuts {
    val Submit = Shortcut(setOf(Key.Enter, Key.NumPadEnter), ctrl = true, "Ctrl+Enter")
    val NewDecision = Shortcut(setOf(Key.N), ctrl = true, "Ctrl+N")
    val Settings = Shortcut(setOf(Key.Comma), ctrl = true, "Ctrl+,")

    /** Compose Desktop already turns Escape into back navigation; this only names it. */
    val Back = Shortcut(setOf(Key.Escape), ctrl = false, "Esc")
    val Quit = Shortcut(setOf(Key.Q), ctrl = true, "Ctrl+Q")
}
