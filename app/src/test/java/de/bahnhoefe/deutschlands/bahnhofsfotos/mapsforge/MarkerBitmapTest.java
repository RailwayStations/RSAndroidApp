package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge;

import android.content.Context;

import org.junit.Test;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Point;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

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
        assertEquals(srcOwnPhoto, markerBitmap.getBitmap(true, true, true, false));
    }
    @Test
    public void getBitmap_OwnPhotoInactive() {
        assertEquals(srcOwnPhotoInactive, markerBitmap.getBitmap(true, true, false, false));
    }

    @Test
    public void getBitmap_PhotoActive() {
        assertEquals(srcWithPhoto, markerBitmap.getBitmap(true, false, true, false));
    }
    @Test
    public void getBitmap_PhotoInactive() {
        assertEquals(srcWithPhotoInactive, markerBitmap.getBitmap(true, false, false, false));
    }

    @Test
    public void getBitmap_WithoutPhotoActive() {
        assertEquals(srcWithoutPhoto, markerBitmap.getBitmap(false, false, true, false));
    }

    @Test
    public void getBitmap_WithoutPhotoInactive() {
        assertEquals(srcWithoutPhotoInactive, markerBitmap.getBitmap(false, false, false, false));
    }

    @Test
    public void getBitmap_PendingUpload() {
        assertEquals(srcPendingUpload, markerBitmap.getBitmap(false, false, false, true));
    }

}