package ai.hev.app.ui.navigation

import ai.hev.app.ui.common.Shortcuts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.KeyEvent
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Where the user is in the app, hoisted so a desktop window can move around with keyboard shortcuts. */
@Stable
class HevNavigator internal constructor(internal val backStack: NavBackStack<NavKey>) {
    private val newDecisionRequests = Channel<Unit>(Channel.CONFLATED)

    /** Emits each time the user asks for a new decision, which clears the Decide form. */
    internal val newDecisions: Flow<Unit> = newDecisionRequests.receiveAsFlow()

    /** Follows the navigation shortcut [event] stands for, if any, and tells whether it did. */
    fun handleShortcut(event: KeyEvent): Boolean {
        when {
            Shortcuts.NewDecision.matches(event) -> {
                backStack.popTo(Route.Home)
                newDecisionRequests.trySend(Unit)
            }
            Shortcuts.Settings.matches(event) -> backStack.openAbove(Route.Home, Route.Settings)
            else -> return false
        }
        return true
    }
}

@Composable
fun rememberHevNavigator(): HevNavigator {
    val backStack = rememberNavBackStack(RouteSaving, Route.Home)
    return remember(backStack) { HevNavigator(backStack) }
}
