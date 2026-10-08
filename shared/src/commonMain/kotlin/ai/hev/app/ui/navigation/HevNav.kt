package ai.hev.app.ui.navigation

import ai.hev.app.resources.Res
import ai.hev.app.resources.decide
import ai.hev.app.resources.history
import ai.hev.app.resources.settings
import ai.hev.app.ui.components.HevIcons
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The app's screens. Phones keep the single-pane flow; a window at least 600 × 480 dp adds a
 * navigation rail, and one at least 840 × 480 dp shows lists and their details side by side.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HevNavDisplay(navigator: HevNavigator, modifier: Modifier = Modifier) {
    val backStack = navigator.backStack
    val back = { backStack.pop() }
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val sizeClass = adaptiveInfo.windowSizeClass
    val rail = sizeClass.isAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val twoPane = sizeClass.isAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val current = backStack.section

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Section.entries.forEach { section ->
                item(
                    selected = section == current,
                    onClick = { backStack.show(section) },
                    icon = { Icon(section.icon, contentDescription = null) },
                    label = { Text(stringResource(section.label)) },
                )
            }
        },
        modifier = modifier,
        layoutType = if (rail) NavigationSuiteType.NavigationRail else NavigationSuiteType.None,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = back,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            sceneStrategies = listOf(
                rememberListDetailSceneStrategy(
                    backNavigationBehavior = BackNavigationBehavior.PopLatest,
                    directive = paneDirective(adaptiveInfo, if (twoPane) 2 else 1),
                ),
            ),
            transitionSpec = { crossfade() },
            popTransitionSpec = { crossfade() },
            predictivePopTransitionSpec = { crossfade() },
            entryProvider = entryProvider {
                entry<Route.Home>(metadata = ListDetailSceneStrategy.listPane(Section.Decide)) {
                    HomeScreen(
                        newDecisions = navigator.newDecisions,
                        onOpenSettings = { backStack.openAbove(Route.Home, Route.Settings) }.takeUnless { rail },
                        onOpenHistory = { backStack.openAbove(Route.Home, Route.History) }.takeUnless { rail },
                        onResult = { id -> backStack.openAbove(Route.Home, Route.Result(id)) },
                    )
                }
                entry<Route.Result>(metadata = ListDetailSceneStrategy.detailPane(Section.Decide)) { route ->
                    ResultScreen(
                        historyId = route.historyId,
                        onBack = back,
                        onOpenHistory = { backStack.openAbove(Route.Home, Route.History) },
                    )
                }
                entry<Route.Settings>(metadata = ListDetailSceneStrategy.listPane(Section.Settings)) {
                    SettingsScreen(
                        onBack = back.takeUnless { rail },
                        onOpenProviders = { backStack.openAbove(Route.Settings, Route.Providers) },
                    )
                }
                entry<Route.Providers>(metadata = ListDetailSceneStrategy.detailPane(Section.Settings)) {
                    ProvidersScreen(
                        onBack = back,
                        onEditProvider = { id -> backStack.openAbove(Route.Providers, Route.ProviderEdit(id)) },
                        onAddProvider = { backStack.openAbove(Route.Providers, Route.ProviderEdit(null)) },
                    )
                }
                entry<Route.ProviderEdit>(metadata = ListDetailSceneStrategy.extraPane(Section.Settings)) { route ->
                    ProviderEditScreen(providerId = route.providerId, onBack = back)
                }
                entry<Route.History>(metadata = ListDetailSceneStrategy.listPane(Section.History)) {
                    HistoryScreen(
                        onBack = back.takeUnless { rail },
                        onOpen = { id -> backStack.openAbove(Route.History, Route.HistoryDetail(id)) },
                    )
                }
                entry<Route.HistoryDetail>(metadata = ListDetailSceneStrategy.detailPane(Section.History)) { route ->
                    HistoryDetailScreen(historyId = route.historyId, onBack = back)
                }
            },
        )
    }
}

/** The top-level destinations the rail offers; [root] sits directly above the start screen. */
private enum class Section(val root: Route?, val icon: ImageVector, val label: StringResource) {
    Decide(null, HevIcons.Balance, Res.string.decide),
    History(Route.History, HevIcons.History, Res.string.history),
    Settings(Route.Settings, HevIcons.Settings, Res.string.settings),
}

private val NavBackStack<NavKey>.section: Section
    get() = Section.entries.firstOrNull { it.root != null && it.root == getOrNull(1) } ?: Section.Decide

private fun NavBackStack<NavKey>.show(section: Section) {
    popTo(Route.Home)
    section.root?.let(::add)
}

/**
 * Default pane sizes and spacing for the window, limited to [panes] side by side, never stacked,
 * and without moving focus into a pane when it appears.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun paneDirective(adaptiveInfo: WindowAdaptiveInfo, panes: Int): PaneScaffoldDirective {
    val default = calculatePaneScaffoldDirective(adaptiveInfo)
    return PaneScaffoldDirective(
        maxHorizontalPartitions = panes,
        horizontalPartitionSpacerSize = default.horizontalPartitionSpacerSize,
        maxVerticalPartitions = 1,
        verticalPartitionSpacerSize = default.verticalPartitionSpacerSize,
        defaultPanePreferredWidth = default.defaultPanePreferredWidth,
        defaultPanePreferredHeight = default.defaultPanePreferredHeight,
        excludedBounds = default.excludedBounds,
        shouldAutoFocusCurrentDestination = false,
    )
}

/** The cross-fade the app has always used between screens. */
private fun crossfade(): ContentTransform = fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS))

private const val FADE_MILLIS = 700
