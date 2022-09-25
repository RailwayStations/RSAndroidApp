package de.bahnhoefe.deutschlands.bahnhofsfotos;

import static org.assertj.core.api.Assertions.assertThat;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.jupiter.api.Test;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleInstrumentationTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        var appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertThat(appContext.getPackageName()).isEqualTo("de.bahnhoefe.deutschlands.bahnhofsfotos.debug");
    }
}