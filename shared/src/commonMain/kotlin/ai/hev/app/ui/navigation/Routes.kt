package ai.hev.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val RESULT = "result/{historyId}"
    const val SETTINGS = "settings"
    const val PROVIDERS = "providers"
    const val PROVIDER_EDIT = "provider/{providerId}"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history_detail/{historyId}"

    fun result(historyId: Long) = "result/$historyId"
    fun providerEdit(providerId: String) = "provider/$providerId"
    fun historyDetail(historyId: Long) = "history_detail/$historyId"

    const val NEW_PROVIDER = "new"
}
