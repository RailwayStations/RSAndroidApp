package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Intent;
import androidx.test.filters.MediumTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;

import static org.junit.Assert.*;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class TimetableTest {

    private Bahnhof station;

    @Before
    public void setUp() {
        station = new Bahnhof();
        station.setId("4711");
        station.setTitle("Some Famous Station");
        station.setDs100("LOL");
    }

    @Test
    public void createTimetableIntentWithId() {
        Country country = new Country();
        country.setTimetableUrlTemplate("https://example.com/{id}/blah");
        Intent intent = new Timetable().createTimetableIntent( country, station);
        assertEquals("https://example.com/4711/blah", intent.getData().toString());
    }

    @Test
    public void createTimetableIntentWithTitle() {
        Country country = new Country();
        country.setTimetableUrlTemplate("https://example.com/{title}/blah");
        Intent intent = new Timetable().createTimetableIntent( country, station);
        assertEquals("https://example.com/Some Famous Station/blah", intent.getData().toString());
    }

    @Test
    public void createTimetableIntentWithDS100() {
        Country country = new Country();
        country.setTimetableUrlTemplate("https://example.com/{DS100}/blah");
        Intent intent = new Timetable().createTimetableIntent( country, station);
        assertEquals("https://example.com/LOL/blah", intent.getData().toString());
    }

}