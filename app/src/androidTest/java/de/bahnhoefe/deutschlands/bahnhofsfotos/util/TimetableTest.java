package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import androidx.test.filters.MediumTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;

import static org.junit.Assert.*;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class TimetableTest {

    private Station station;

    @Before
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
        assertEquals("https://example.com/4711/blah", new Timetable().createTimetableIntent( country, station).getData().toString());
    }

    @Test
    public void createTimetableIntentWithTitle() {
        var country = new Country();
        country.setTimetableUrlTemplate("https://example.com/{title}/blah");
        assertEquals("https://example.com/Some Famous Station/blah", new Timetable().createTimetableIntent( country, station).getData().toString());
    }

    @Test
    public void createTimetableIntentWithDS100() {
        var country = new Country();
        country.setTimetableUrlTemplate("https://example.com/{DS100}/blah");
        assertEquals("https://example.com/LOL/blah", new Timetable().createTimetableIntent( country, station).getData().toString());
    }

}