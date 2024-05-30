package de.bahnhoefe.deutschlands.bahnhofsfotos.model

data class ProviderApp(
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

}

fun hasCompatibleProviderApps(providerApps: List<ProviderApp>) =
    providerApps.any { it.isCompatible }

fun getCompatibleProviderApps(providerApps: List<ProviderApp>) =
    providerApps
        .filter { it.isCompatible }
        .toList()
