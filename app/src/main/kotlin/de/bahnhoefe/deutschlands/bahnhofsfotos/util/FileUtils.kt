package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;

public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    /**
     * Get the base directory for storing fotos
     *
     * @return the File denoting the base directory or null, if cannot write to it
     */
    @Nullable
    public static File getLocalFotoDir(Context context) {
        return mkdirs(Objects.requireNonNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)));
    }

    private static File mkdirs(File dir) {
        try {
            Files.createDirectories(dir.toPath());
            return dir;
        } catch (IOException e) {
            Log.e(TAG, "Cannot create directory structure " + dir.getAbsolutePath(), e);
        }
        return null;
    }

    /**
     * Get the file path for storing this stations foto
     *
     * @return the File
     */
    @Nullable
    public static File getStoredMediaFile(Context context, Long uploadId) {
        var mediaStorageDir = FileUtils.getLocalFotoDir(context);
        if (mediaStorageDir == null) {
            return null;
        }

        var storeMediaFile = new File(mediaStorageDir, String.format(Locale.ENGLISH, "%d.jpg", uploadId));
        Log.d(TAG, "StoredMediaFile: " + storeMediaFile);

        return storeMediaFile;
    }

    public static File getImageCacheFile(Context applicationContext, String imageId) {
        File imagePath = new File(applicationContext.getCacheDir(), "images");
        mkdirs(imagePath);
        return new File(imagePath, imageId + ".jpg");
    }

    public static void deleteQuietly(File file) {
        try {
            Files.delete(file.toPath());
        } catch (IOException exception) {
            Log.w(TAG, "unable to delete file " + file, exception);
        }
    }
}
