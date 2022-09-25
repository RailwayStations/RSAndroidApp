package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.content.Context;

import org.junit.jupiter.api.Test;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Point;

/**
 * Tests for {@link MarkerBitmap}.
 */
public class MarkerBitmapTest {

    private final Bitmap srcWithoutPhoto = mock(Bitmap.class);
    private final Bitmap srcWithPhoto = mock(Bitmap.class);
    private final Bitmap srcOwnPhoto = mock(Bitmap.class);
    private final Bitmap srcWithoutPhotoInactive = mock(Bitmap.class);
    private final Bitmap srcWithPhotoInactive = mock(Bitmap.class);
    private final Bitmap srcOwnPhotoInactive = mock(Bitmap.class);
    private final Bitmap srcPendingUpload = mock(Bitmap.class);
    private final MarkerBitmap markerBitmap = new MarkerBitmap(mock(Context.class),
            srcWithoutPhoto, srcWithPhoto, srcOwnPhoto, srcWithoutPhotoInactive, srcWithPhotoInactive, srcOwnPhotoInactive, srcPendingUpload,
            mock(Point.class), 0, 0, mock(Paint.class));

    @Test
    public void getBitmap_OwnPhotoActive() {
        assertThat(markerBitmap.getBitmap(true, true, true, false)).isSameAs(srcOwnPhoto);
    }

    @Test
    public void getBitmap_OwnPhotoInactive() {
        assertThat(markerBitmap.getBitmap(true, true, false, false)).isSameAs(srcOwnPhotoInactive);
    }

    @Test
    public void getBitmap_PhotoActive() {
        assertThat(markerBitmap.getBitmap(true, false, true, false)).isSameAs(srcWithPhoto);
    }

    @Test
    public void getBitmap_PhotoInactive() {
        assertThat(markerBitmap.getBitmap(true, false, false, false)).isSameAs(srcWithPhotoInactive);
    }

    @Test
    public void getBitmap_WithoutPhotoActive() {
        assertThat(markerBitmap.getBitmap(false, false, true, false)).isSameAs(srcWithoutPhoto);
    }

    @Test
    public void getBitmap_WithoutPhotoInactive() {
        assertThat(markerBitmap.getBitmap(false, false, false, false)).isSameAs(srcWithoutPhotoInactive);
    }

    @Test
    public void getBitmap_PendingUpload() {
        assertThat(markerBitmap.getBitmap(false, false, false, true)).isSameAs(srcPendingUpload);
    }

}