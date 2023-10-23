package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp.Companion.getCompatibleProviderApps
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp.Companion.hasCompatibleProviderApps
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.List

internal class ProviderAppTest {
    @ParameterizedTest
    @CsvSource("iOS, false", "android, true", "web, true")
    fun hasCompatibleProviderApps(type: String?, expectedHasCompatibleProviderApps: Boolean) {
        val providerApps = List.of(ProviderApp(type))
        assertThat(hasCompatibleProviderApps(providerApps))
            .isEqualTo(expectedHasCompatibleProviderApps)
    }

    @Test
    fun compatibleProviderApps() {
        val webProviderApp = ProviderApp("web")
        val androidProviderApp = ProviderApp("android")
        val providerApps = List.of(webProviderApp, ProviderApp("iOS"), androidProviderApp)
        assertThat(getCompatibleProviderApps(providerApps))
            .containsExactlyInAnyOrder(webProviderApp, androidProviderApp)
    }
}