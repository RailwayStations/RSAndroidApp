package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.model.Point
import org.mockito.Mockito.mock

/**
 * Tests for [MarkerBitmap].
 */
class MarkerBitmapTest {
    private val srcWithoutPhoto = mock(
        Bitmap::class.java
    )
    private val srcWithPhoto = mock(
        Bitmap::class.java
    )
    private val srcOwnPhoto = mock(
        Bitmap::class.java
    )
    private val srcWithoutPhotoInactive = mock(
        Bitmap::class.java
    )
    private val srcWithPhotoInactive = mock(
        Bitmap::class.java
    )
    private val srcOwnPhotoInactive = mock(
        Bitmap::class.java
    )
    private val srcPendingUpload = mock(
        Bitmap::class.java
    )
    private val markerBitmap = MarkerBitmap(
        mock(
            Context::class.java
        ),
        srcWithoutPhoto,
        srcWithPhoto,
        srcOwnPhoto,
        srcWithoutPhotoInactive,
        srcWithPhotoInactive,
        srcOwnPhotoInactive,
        srcPendingUpload,
        mock(Point::class.java),
        0f,
        0,
        mock(
            Paint::class.java
        )
    )

    @Test
    fun bitmap_OwnPhotoActive() {
        assertThat(
            markerBitmap.getBitmap(
                hasPhoto = true,
                ownPhoto = true,
                stationActive = true,
                inbox = false
            )
        )
            .isSameAs(srcOwnPhoto)
    }

    @Test
    fun bitmap_OwnPhotoInactive() {
        assertThat(
            markerBitmap.getBitmap(
                hasPhoto = true,
                ownPhoto = true,
                stationActive = false,
                inbox = false
            )
        )
            .isSameAs(srcOwnPhotoInactive)
    }

    @Test
    fun bitmap_PhotoActive() {
        assertThat(
            markerBitmap.getBitmap(
                hasPhoto = true,
                ownPhoto = false,
                stationActive = true,
                inbox = false
            )
        )
            .isSameAs(srcWithPhoto)
    }

    @Test
    fun bitmap_PhotoInactive() {
        assertThat(
            markerBitmap.getBitmap(
                hasPhoto = true,
                ownPhoto = false,
                stationActive = false,
                inbox = false
            )
        )
            .isSameAs(srcWithPhotoInactive)
    }

    @Test
    fun bitmap_WithoutPhotoActive() {
        assertThat(
            markerBitmap.getBitmap(
                hasPhoto = false,
                ownPhoto = false,
                stationActive = true,
                inbox = false
            )
        )
            .isSameAs(srcWithoutPhoto)
    }

    @Test
    fun bitmap_WithoutPhotoInactive() {
        assertThat(
            markerBitmap.getBitmap(
                hasPhoto = false,
                ownPhoto = false,
                stationActive = false,
                inbox = false
            )
        )
            .isSameAs(srcWithoutPhotoInactive)
    }

    @Test
    fun bitmap_PendingUpload() {
        assertThat(
            markerBitmap.getBitmap(
                hasPhoto = false,
                ownPhoto = false,
                stationActive = false,
                inbox = true,
            )
        )
            .isSameAs(srcPendingUpload)
    }
}