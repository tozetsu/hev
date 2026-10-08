package ai.hev.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Every screen and the arguments it opens with. */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data class Result(val historyId: Long) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Providers : Route

    /** A null [providerId] adds a new provider. */
    @Serializable
    data class ProviderEdit(val providerId: String?) : Route

    @Serializable
    data object History : Route

    @Serializable
    data class HistoryDetail(val historyId: Long) : Route
}
