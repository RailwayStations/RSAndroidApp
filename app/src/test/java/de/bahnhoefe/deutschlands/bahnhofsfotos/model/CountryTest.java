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
                Country.builder().code("de").build(),
                Country.builder().code("at").build()
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
        var country = Country.builder().timetableUrlTemplate(timetableUrlTemplate).build();

        assertThat(country.hasTimetableUrlTemplate()).isEqualTo(expectedValue);
    }

}