package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

class PhotoStationsTest {

    @Test
    void getPhotographerUrl() throws MalformedURLException {
        var photoStations = new PhotoStations(
                "https://api.railway-stations.org/photos/",
                List.of(),
                List.of(new Photographer(
                        "name",
                        new URL("http://example.com"))));

        assertThat(photoStations.getPhotographerUrl("name")).isEqualTo("http://example.com");
        assertThat(photoStations.getPhotographerUrl("non-existing")).isNull();
    }

    @Test
    void getLicenseName() {
        var photoStations = new PhotoStations(
                "https://api.railway-stations.org/photos/",
                List.of(new PhotoLicense(
                        "CC0_10",
                        "CC0 1.0 Universell (CC0 1.0)")));

        assertThat(photoStations.getLicenseName("CC0_10")).isEqualTo("CC0 1.0 Universell (CC0 1.0)");
        assertThat(photoStations.getLicenseName("CC4")).isNull();
    }

    @Test
    void getLicenseUrl() throws MalformedURLException {
        var photoStations = new PhotoStations(
                "https://api.railway-stations.org/photos/",
                List.of(new PhotoLicense(
                        "CC0_10",
                        "CC0 1.0 Universell (CC0 1.0)",
                        new URL("https://creativecommons.org/publicdomain/zero/1.0/"))));

        assertThat(photoStations.getLicenseUrl("CC0_10")).isEqualTo("https://creativecommons.org/publicdomain/zero/1.0/");
        assertThat(photoStations.getLicenseUrl("CC4")).isNull();
    }

}