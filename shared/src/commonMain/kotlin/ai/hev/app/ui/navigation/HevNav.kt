package ai.hev.app.ui.navigation

import ai.hev.app.ui.history.HistoryDetailScreen
import ai.hev.app.ui.history.HistoryScreen
import ai.hev.app.ui.home.HomeScreen
import ai.hev.app.ui.result.ResultScreen
import ai.hev.app.ui.settings.ProviderEditScreen
import ai.hev.app.ui.settings.ProvidersScreen
import ai.hev.app.ui.settings.SettingsScreen
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed

@Composable
fun HevNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(RouteSaving, Route.Home)
    val back = { backStack.pop() }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = back,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { crossfade() },
        popTransitionSpec = { crossfade() },
        predictivePopTransitionSpec = { crossfade() },
        entryProvider = entryProvider {
            entry<Route.Home> {
                HomeScreen(
                    onOpenSettings = { backStack.add(Route.Settings) },
                    onOpenHistory = { backStack.add(Route.History) },
                    onResult = { id -> backStack.add(Route.Result(id)) },
                )
            }
            entry<Route.Result> { route ->
                ResultScreen(
                    historyId = route.historyId,
                    onBack = back,
                    onOpenHistory = { backStack.openFromHome(Route.History) },
                )
            }
            entry<Route.Settings> {
                SettingsScreen(
                    onBack = back,
                    onOpenProviders = { backStack.add(Route.Providers) },
                )
            }
            entry<Route.Providers> {
                ProvidersScreen(
                    onBack = back,
                    onEditProvider = { id -> backStack.add(Route.ProviderEdit(id)) },
                    onAddProvider = { backStack.add(Route.ProviderEdit(null)) },
                )
            }
            entry<Route.ProviderEdit> { route ->
                ProviderEditScreen(providerId = route.providerId, onBack = back)
            }
            entry<Route.History> {
                HistoryScreen(
                    onBack = back,
                    onOpen = { id -> backStack.add(Route.HistoryDetail(id)) },
                )
            }
            entry<Route.HistoryDetail> { route ->
                HistoryDetailScreen(historyId = route.historyId, onBack = back)
            }
        },
    )
}

/** Routes are saved by their serializers, which non-Android targets cannot find by reflection. */
@OptIn(ExperimentalSerializationApi::class)
private val RouteSaving = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { subclassesOfSealed<Route>() }
    }
}

/** Leaves the start screen in place, so a stray second back tap cannot empty the stack. */
private fun NavBackStack<NavKey>.pop() {
    if (size > 1) removeAt(lastIndex)
}

/** Returns to the start screen, then opens [route] on top of it. */
private fun NavBackStack<NavKey>.openFromHome(route: Route) {
    while (size > 1) removeAt(lastIndex)
    add(route)
}

/** The cross-fade the app has always used between screens. */
private fun crossfade(): ContentTransform = fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS))

private const val FADE_MILLIS = 700
