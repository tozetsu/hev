package ai.hev.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import ai.hev.app.ui.history.HistoryDetailScreen
import ai.hev.app.ui.history.HistoryScreen
import ai.hev.app.ui.home.HomeScreen
import ai.hev.app.ui.result.ResultScreen
import ai.hev.app.ui.settings.ProviderEditScreen
import ai.hev.app.ui.settings.ProvidersScreen
import ai.hev.app.ui.settings.SettingsScreen

@Composable
fun HevNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onResult = { id -> navController.navigate(Routes.result(id)) },
            )
        }
        composable(
            Routes.RESULT,
            arguments = listOf(navArgument("historyId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.read { getLong("historyId") } ?: 0L
            ResultScreen(
                historyId = id,
                onBack = { navController.popBackStack() },
                onOpenHistory = {
                    navController.navigate(Routes.HISTORY) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenProviders = { navController.navigate(Routes.PROVIDERS) },
            )
        }
        composable(Routes.PROVIDERS) {
            ProvidersScreen(
                onBack = { navController.popBackStack() },
                onEditProvider = { id -> navController.navigate(Routes.providerEdit(id)) },
                onAddProvider = { navController.navigate(Routes.providerEdit(Routes.NEW_PROVIDER)) },
            )
        }
        composable(
            Routes.PROVIDER_EDIT,
            arguments = listOf(navArgument("providerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.read { getString("providerId") } ?: Routes.NEW_PROVIDER
            ProviderEditScreen(
                providerId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Routes.historyDetail(id)) },
            )
        }
        composable(
            Routes.HISTORY_DETAIL,
            arguments = listOf(navArgument("historyId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.read { getLong("historyId") } ?: 0L
            HistoryDetailScreen(
                historyId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
