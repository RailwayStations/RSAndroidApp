package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ProviderAppTest {
    @ParameterizedTest
    @CsvSource("iOS, false", "android, true", "web, true")
    fun hasCompatibleProviderApps(type: String, expectedHasCompatibleProviderApps: Boolean) {
        val providerApps = listOf(ProviderApp(type, "", ""))
        assertThat(hasCompatibleProviderApps(providerApps))
            .isEqualTo(expectedHasCompatibleProviderApps)
    }

    @Test
    fun compatibleProviderApps() {
        val webProviderApp = ProviderApp("web", "", "")
        val androidProviderApp = ProviderApp("android", "", "")
        val providerApps = listOf(webProviderApp, ProviderApp("iOS", "", ""), androidProviderApp)
        assertThat(getCompatibleProviderApps(providerApps))
            .containsExactlyInAnyOrder(webProviderApp, androidProviderApp)
    }
}