package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import java.util.stream.Collectors

data class ProviderApp @JvmOverloads constructor(
    var type: String? = null,
    var name: String? = null,
    var url: String? = null
) {

    val isAndroid: Boolean
        get() = "android" == type
    val isWeb: Boolean
        get() = "web" == type
    val isCompatible: Boolean
        get() = isAndroid || isWeb

    companion object {
        @JvmStatic
        fun hasCompatibleProviderApps(providerApps: List<ProviderApp>): Boolean {
            return providerApps.stream().anyMatch { obj: ProviderApp -> obj.isCompatible }
        }

        @JvmStatic
        fun getCompatibleProviderApps(providerApps: List<ProviderApp>): List<ProviderApp> {
            return providerApps.stream()
                .filter { obj: ProviderApp -> obj.isCompatible }
                .collect(Collectors.toList())
        }
    }
}