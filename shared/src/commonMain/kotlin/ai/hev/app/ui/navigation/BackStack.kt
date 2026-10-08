package ai.hev.app.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Leaves the start screen in place, so a stray second back tap cannot empty the stack. */
internal fun NavBackStack<NavKey>.pop() {
    if (size > 1) removeAt(lastIndex)
}

/** Pops everything above [route], or down to the start screen if [route] is not on the stack. */
internal fun NavBackStack<NavKey>.popTo(route: Route) {
    while (size > 1 && last() != route) removeAt(lastIndex)
}

/**
 * Opens [route] directly above [parent]. On a phone [parent] is already on top; beside a list
 * pane this replaces the detail that was showing instead of stacking another.
 */
internal fun NavBackStack<NavKey>.openAbove(parent: Route, route: Route) {
    popTo(parent)
    add(route)
}
