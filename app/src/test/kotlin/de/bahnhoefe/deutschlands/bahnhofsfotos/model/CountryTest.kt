package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

class CountryTest {

    @Test
    void getCountryByCode() {
        var country = Country.getCountryByCode(Set.of(
                new Country("de", "Deutschland"),
                new Country("at", "Österreich")
        ), "at");

        assertThat(country).isNotEmpty();
        assertThat(country.get().getCode()).isEqualTo("at");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "'', false",
            ", false",
            "'blah', true"
    })
    void hasTimetableUrlTemplate(String timetableUrlTemplate, boolean expectedValue) {
        var country = new Country("de", "Deutschland", null, timetableUrlTemplate);

        assertThat(country.hasTimetableUrlTemplate()).isEqualTo(expectedValue);
    }

}