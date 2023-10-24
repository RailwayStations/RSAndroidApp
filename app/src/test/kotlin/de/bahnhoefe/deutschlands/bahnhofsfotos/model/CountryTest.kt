package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country.Companion.getCountryByCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class CountryTest {
    @Test
    fun countryByCode() {
        val country = getCountryByCode(
            setOf(
                Country("de", "Deutschland"),
                Country("at", "Österreich")
            ), "at"
        )
        assertThat(country).isNotNull
        assertThat(country?.code).isEqualTo("at")
    }

    @ParameterizedTest
    @CsvSource(value = ["'', false", ", false", "'blah', true"])
    fun hasTimetableUrlTemplate(timetableUrlTemplate: String?, expectedValue: Boolean) {
        val country = Country("de", "Deutschland", null, timetableUrlTemplate)
        assertThat(country.hasTimetableUrlTemplate()).isEqualTo(expectedValue)
    }
}