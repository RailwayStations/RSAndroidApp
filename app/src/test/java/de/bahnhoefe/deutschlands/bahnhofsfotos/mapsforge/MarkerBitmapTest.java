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

    private Bitmap srcWithoutPhoto = mock(Bitmap.class);
    private Bitmap srcWithPhoto = mock(Bitmap.class);
    private Bitmap srcOwnPhoto = mock(Bitmap.class);
    private Bitmap srcWithoutPhotoInactive = mock(Bitmap.class);
    private Bitmap srcWithPhotoInactive = mock(Bitmap.class);
    private Bitmap srcOwnPhotoInactive = mock(Bitmap.class);
    private Bitmap srcPendingUpload = mock(Bitmap.class);
    private MarkerBitmap markerBitmap = new MarkerBitmap(mock(Context.class),
            srcWithoutPhoto, srcWithPhoto, srcOwnPhoto, srcWithoutPhotoInactive, srcWithPhotoInactive, srcOwnPhotoInactive, srcPendingUpload,
            mock(Point.class), 0, 0, mock(Paint.class));

    @Test
    public void getBitmap_OwnPhotoActive() {
        final Bitmap bitmap = markerBitmap.getBitmap(true, true, true, false);
        assertEquals(srcOwnPhoto, bitmap);
    }
    @Test
    public void getBitmap_OwnPhotoInactive() {
        final Bitmap bitmap = markerBitmap.getBitmap(true, true, false, false);
        assertEquals(srcOwnPhotoInactive, bitmap);
    }

    @Test
    public void getBitmap_PhotoActive() {
        final Bitmap bitmap = markerBitmap.getBitmap(true, false, true, false);
        assertEquals(srcWithPhoto, bitmap);
    }
    @Test
    public void getBitmap_PhotoInactive() {
        final Bitmap bitmap = markerBitmap.getBitmap(true, false, false, false);
        assertEquals(srcWithPhotoInactive, bitmap);
    }

    @Test
    public void getBitmap_WithoutPhotoActive() {
        final Bitmap bitmap = markerBitmap.getBitmap(false, false, true, false);
        assertEquals(srcWithoutPhoto, bitmap);
    }

    @Test
    public void getBitmap_WithoutPhotoInactive() {
        final Bitmap bitmap = markerBitmap.getBitmap(false, false, false, false);
        assertEquals(srcWithoutPhotoInactive, bitmap);
    }

    @Test
    public void getBitmap_PendingUpload() {
        final Bitmap bitmap = markerBitmap.getBitmap(false, false, false, true);
        assertEquals(srcPendingUpload, bitmap);
    }

}