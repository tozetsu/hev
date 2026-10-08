package ai.hev.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed

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

/** Routes are saved by their serializers, which non-Android targets cannot find by reflection. */
@OptIn(ExperimentalSerializationApi::class)
internal val RouteSaving = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { subclassesOfSealed<Route>() }
    }
}
