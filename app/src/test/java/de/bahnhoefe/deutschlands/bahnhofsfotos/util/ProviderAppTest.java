package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ProviderApp}.
 */
public class ProviderAppTest {

    /**
     * Tests if the providers app package name "de.bahnhoefe" is correctly parsed.
     */
    @Test
    public void openAppOrPlayStore_validPackageName() {

        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        final Intent intent = mock(Intent.class);
        when(packageManager.getLaunchIntentForPackage(eq("de.bahnhoefe"))).thenReturn(intent);

        ProviderApp.openAppOrPlayStore(context, "https://play.google.com/store/apps/details?id=de.bahnhoefe");

        verify(context).startActivity(argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Intent argument) {
                return argument == intent;
            }
        }));
    }
}