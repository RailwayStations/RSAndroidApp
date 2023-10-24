package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TimetableTest {
    private var station: Station? = null

    @BeforeEach
    fun setUp() {
        station = Station(
            "de",
            "4711",
            "Some Famous Station",
            0.0,
            0.0,
            "LOL",
            photoId = null,
        )
    }

    @Test
    fun createTimetableIntentWithId() {
        val country = Country("de", "Deutschland", null, "https://example.com/{id}/blah")
        assertThat(
            Timetable().createTimetableIntent(country, station)!!.data.toString()
        ).isEqualTo("https://example.com/4711/blah")
    }

    @Test
    fun createTimetableIntentWithTitle() {
        val country = Country("de", "Deutschland", null, "https://example.com/{title}/blah")
        assertThat(
            Timetable().createTimetableIntent(country, station)!!.data.toString()
        ).isEqualTo("https://example.com/Some Famous Station/blah")
    }

    @Test
    fun createTimetableIntentWithDS100() {
        val country = Country("de", "Deutschland", null, "https://example.com/{DS100}/blah")
        assertThat(
            Timetable().createTimetableIntent(country, station)!!.data.toString()
        ).isEqualTo("https://example.com/LOL/blah")
    }
}