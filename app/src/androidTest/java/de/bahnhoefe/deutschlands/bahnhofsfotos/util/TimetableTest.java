package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;

public class TimetableTest {

    private Station station;

    @BeforeEach
    public void setUp() {
        station = new Station();
        station.setId("4711");
        station.setTitle("Some Famous Station");
        station.setDs100("LOL");
    }

    @Test
    public void createTimetableIntentWithId() {
        var country = new Country();
        country.setTimetableUrlTemplate("https://example.com/{id}/blah");
        assertThat(new Timetable().createTimetableIntent(country, station).getData().toString()).isEqualTo("https://example.com/4711/blah");
    }

    @Test
    public void createTimetableIntentWithTitle() {
        var country = new Country();
        country.setTimetableUrlTemplate("https://example.com/{title}/blah");
        assertThat(new Timetable().createTimetableIntent(country, station).getData().toString()).isEqualTo("https://example.com/Some Famous Station/blah");
    }

    @Test
    public void createTimetableIntentWithDS100() {
        var country = new Country();
        country.setTimetableUrlTemplate("https://example.com/{DS100}/blah");
        assertThat(new Timetable().createTimetableIntent(country, station).getData().toString()).isEqualTo("https://example.com/LOL/blah");
    }

}