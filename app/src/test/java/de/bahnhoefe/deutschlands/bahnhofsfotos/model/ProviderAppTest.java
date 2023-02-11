package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

class ProviderAppTest {

    @ParameterizedTest
    @CsvSource({
            "iOS, false",
            "android, true",
            "web, true"
    })
    void hasCompatibleProviderApps(String type, boolean expectedHasCompatibleProviderApps) {
        var providerApps = List.of(new ProviderApp(type));

        assertThat(ProviderApp.hasCompatibleProviderApps(providerApps)).isEqualTo(expectedHasCompatibleProviderApps);
    }

    @Test
    void getCompatibleProviderApps() {
        var webProviderApp = new ProviderApp("web");
        var androidProviderApp = new ProviderApp("android");
        var providerApps = List.of(webProviderApp, new ProviderApp("iOS"), androidProviderApp);

        assertThat(ProviderApp.getCompatibleProviderApps(providerApps)).containsExactlyInAnyOrder(webProviderApp, androidProviderApp);
    }

}