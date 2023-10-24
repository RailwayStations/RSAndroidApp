package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class ProviderApp constructor(
    val type: String,
    val name: String,
    val url: String,
) {

    val isAndroid: Boolean
        get() = "android" == type
    private val isWeb: Boolean
        get() = "web" == type
    val isCompatible: Boolean
        get() = isAndroid || isWeb

    companion object {
        @JvmStatic
        fun hasCompatibleProviderApps(providerApps: List<ProviderApp>): Boolean {
            return providerApps.any { obj: ProviderApp -> obj.isCompatible }
        }

        @JvmStatic
        fun getCompatibleProviderApps(providerApps: List<ProviderApp>): List<ProviderApp> {
            return providerApps
                .filter { obj: ProviderApp -> obj.isCompatible }
                .toList()
        }
    }
}