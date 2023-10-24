package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

internal class PhotoStationsTest {
    @Test
    fun photographerUrl() {
        val photoStations = PhotoStations(
            "https://api.railway-stations.org/photos/", listOf(),
            listOf(
                Photographer(
                    "name",
                    URL("http://example.com")
                )
            )
        )
        assertThat(photoStations.getPhotographerUrl("name"))
            .isEqualTo("http://example.com")
        assertThat(photoStations.getPhotographerUrl("non-existing")).isNull()
    }

    @Test
    fun licenseName() {
        val photoStations = PhotoStations(
            "https://api.railway-stations.org/photos/",
            listOf(
                PhotoLicense(
                    "CC0_10",
                    "CC0 1.0 Universell (CC0 1.0)"
                )
            )
        )
        assertThat(photoStations.getLicenseName("CC0_10"))
            .isEqualTo("CC0 1.0 Universell (CC0 1.0)")
        assertThat(photoStations.getLicenseName("CC4")).isNull()
    }

    @Test
    fun licenseUrl() {
        val photoStations = PhotoStations(
            "https://api.railway-stations.org/photos/",
            listOf(
                PhotoLicense(
                    "CC0_10",
                    "CC0 1.0 Universell (CC0 1.0)",
                    URL("https://creativecommons.org/publicdomain/zero/1.0/")
                )
            )
        )
        assertThat(photoStations.getLicenseUrl("CC0_10"))
            .isEqualTo("https://creativecommons.org/publicdomain/zero/1.0/")
        assertThat(photoStations.getLicenseUrl("CC4")).isNull()
    }
}