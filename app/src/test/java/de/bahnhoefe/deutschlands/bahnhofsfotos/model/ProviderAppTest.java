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
        var providerApps = List.of(ProviderApp.builder().type(type).build());

        assertThat(ProviderApp.hasCompatibleProviderApps(providerApps)).isEqualTo(expectedHasCompatibleProviderApps);
    }

    @Test
    void getCompatibleProviderApps() {
        var webProviderApp = ProviderApp.builder().type("web").build();
        var androidProviderApp = ProviderApp.builder().type("android").build();
        var providerApps = List.of(webProviderApp, ProviderApp.builder().type("iOS").build(), androidProviderApp);

        assertThat(ProviderApp.getCompatibleProviderApps(providerApps)).containsExactlyInAnyOrder(webProviderApp, androidProviderApp);
    }

}