package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;


import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentationTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        var appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("de.bahnhoefe.deutschlands.bahnhofsfotos.debug", appContext.getPackageName());
    }
}