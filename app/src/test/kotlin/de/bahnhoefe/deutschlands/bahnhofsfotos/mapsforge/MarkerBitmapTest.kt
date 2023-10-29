package de.bahnhoefe.deutschlands.bahnhofsfotos.mapsforge

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mapsforge.core.graphics.Bitmap

/**
 * Tests for [MarkerBitmap].
 */
class MarkerBitmapTest {
    private val srcWithoutPhoto = mockk<Bitmap>()
    private val srcWithPhoto = mockk<Bitmap>()
    private val srcOwnPhoto = mockk<Bitmap>()
    private val srcWithoutPhotoInactive = mockk<Bitmap>()
    private val srcWithPhotoInactive = mockk<Bitmap>()
    private val srcOwnPhotoInactive = mockk<Bitmap>()
    private val srcPendingUpload = mockk<Bitmap>()
    private val markerBitmap = MarkerBitmap(
        mockk(relaxed = true),
        srcWithoutPhoto,
        srcWithPhoto,
        srcOwnPhoto,
        srcWithoutPhotoInactive,
        srcWithPhotoInactive,
        srcOwnPhotoInactive,
        srcPendingUpload,
        mockk(relaxed = true),
        0f,
        0,
        mockk(relaxed = true)
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