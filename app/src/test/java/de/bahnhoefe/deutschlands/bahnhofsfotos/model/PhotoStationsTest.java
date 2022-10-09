package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

class PhotoStationsTest {

    @Test
    void getPhotographerUrl() throws MalformedURLException {
        var photoStations = PhotoStations.builder()
                .photoBaseUrl("https://api.railway-stations.org/photos/")
                .photographers(List.of(Photographer.builder()
                        .name("name")
                        .url(new URL("http://example.com"))
                        .build()))
                .build();

        assertThat(photoStations.getPhotographerUrl("name")).isEqualTo("http://example.com");
        assertThat(photoStations.getPhotographerUrl("non-existing")).isNull();
    }

    @Test
    void getLicenseName() {
        var photoStations = PhotoStations.builder()
                .photoBaseUrl("https://api.railway-stations.org/photos/")
                .licenses(List.of(PhotoLicense.builder()
                        .id("CC0_10")
                        .name("CC0 1.0 Universell (CC0 1.0)")
                        .build()))
                .build();

        assertThat(photoStations.getLicenseName("CC0_10")).isEqualTo("CC0 1.0 Universell (CC0 1.0)");
        assertThat(photoStations.getLicenseName("CC4")).isNull();
    }

    @Test
    void getLicenseUrl() throws MalformedURLException {
        var photoStations = PhotoStations.builder()
                .photoBaseUrl("https://api.railway-stations.org/photos/")
                .licenses(List.of(PhotoLicense.builder()
                        .id("CC0_10")
                        .url(new URL("https://creativecommons.org/publicdomain/zero/1.0/"))
                        .build()))
                .build();

        assertThat(photoStations.getLicenseUrl("CC0_10")).isEqualTo("https://creativecommons.org/publicdomain/zero/1.0/");
        assertThat(photoStations.getLicenseUrl("CC4")).isNull();
    }

}