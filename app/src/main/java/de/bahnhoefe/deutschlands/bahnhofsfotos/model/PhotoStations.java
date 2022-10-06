package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class PhotoStations {

    @NonNull
    String photoBaseUrl;

    @NonNull
    @Builder.Default
    List<PhotoLicense> licenses = new ArrayList<>();

    @NonNull
    @Builder.Default
    List<Photographer> photographers = new ArrayList<>();

    @NonNull
    @Builder.Default
    List<PhotoStation> stations = new ArrayList<>();

    public String getPhotographerUrl(final String photographer) {
        return photographers.stream()
                .filter(p -> p.getName().equals(photographer))
                .findAny()
                .map(p -> p.getUrl().toString())
                .orElse(null);
    }

    public String getLicenseName(final String licenseId) {
        return licenses.stream()
                .filter(license -> license.getId().equals(licenseId))
                .findAny()
                .map(PhotoLicense::getName)
                .orElse(null);
    }

    public String getLicenseUrl(final String licenseId) {
        return licenses.stream()
                .filter(license -> license.getId().equals(licenseId))
                .findAny()
                .map(l -> l.getUrl().toString())
                .orElse(null);
    }

}
