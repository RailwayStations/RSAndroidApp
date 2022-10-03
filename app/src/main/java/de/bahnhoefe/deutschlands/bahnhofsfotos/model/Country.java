package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Country implements Serializable {

    @EqualsAndHashCode.Include
    String code;

    String name;

    String email;

    String twitterTags;

    String timetableUrlTemplate;

    String overrideLicense;

    @Builder.Default
    List<ProviderApp> providerApps = new ArrayList<>();

    public static Optional<Country> getCountryByCode(Set<Country> countries, String countryCode) {
        return countries.stream()
                .filter(country -> country.getCode().equals(countryCode))
                .findAny(); // get first country as fallback
    }

    public boolean hasTimetableUrlTemplate() {
        return timetableUrlTemplate != null && !timetableUrlTemplate.isEmpty();
    }

    public boolean hasCompatibleProviderApps() {
        return ProviderApp.hasCompatibleProviderApps(providerApps);
    }

    public List<ProviderApp> getCompatibleProviderApps() {
        return ProviderApp.getCompatibleProviderApps(providerApps);
    }

}
