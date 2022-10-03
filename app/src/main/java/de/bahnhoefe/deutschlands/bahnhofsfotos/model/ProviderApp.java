package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import static java.util.stream.Collectors.toList;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderApp {

    String countryCode;
    String type;
    String name;
    String url;

    public boolean isAndroid() {
        return "android".equals(type);
    }

    public boolean isWeb() {
        return "web".equals(type);
    }

    public boolean isCompatible() {
        return isAndroid() || isWeb();
    }

    public static boolean hasCompatibleProviderApps(List<ProviderApp> providerApps) {
        return providerApps.stream().anyMatch(ProviderApp::isCompatible);
    }

    public static List<ProviderApp> getCompatibleProviderApps(List<ProviderApp> providerApps) {
        return providerApps.stream()
                .filter(ProviderApp::isCompatible)
                .collect(toList());
    }

}
