package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class FileUtils {

    private static final String PHOTO_DIR = "Bahnhofsfotos";
    private static final String TAG = FileUtils.class.getSimpleName();
    public static final String MISSING_DIR = "missing";

    /**
     * Get the base directory for storing fotos
     *
     * @return the File denoting the base directory or null, if cannot write to it
     */
    @Nullable
    public static File getLocalFotoDir(final Context context) {
        return mkdirs(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
    }

    private static File mkdirs(final File dir) {
        try {
            org.apache.commons.io.FileUtils.forceMkdir(dir);
            return dir;
        } catch (final IOException e) {
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
    public static File getStoredMediaFile(final Context context, final Long uploadId) {
        final File mediaStorageDir = mkdirs(FileUtils.getLocalFotoDir(context));
        if (mediaStorageDir == null) {
            return null;
        }

        final File storeMediaFile = new File(mediaStorageDir, String.format(Locale.ENGLISH, "%d.jpg", uploadId));
        Log.d(TAG, "StoredMediaFile: " + storeMediaFile.toString());

        return storeMediaFile;
    }

    public static File getImageCacheFile(final Context applicationContext, final String imageId) {
        final File imagePath = new File(applicationContext.getCacheDir(), "images");
        imagePath.mkdirs();
        return new File(imagePath, imageId + ".jpg");
    }

    public static String getCountryFromFile(final File file) {
        if (isOldFile(file) || isMissingStation(file)) {
            return null;
        }
        return file.getParentFile().getName();
    }

    public static boolean isMissingStation(final File file) {
        return file.getParentFile().getName().equals(MISSING_DIR);
    }

    public static boolean isOldFile(final File file) {
        return file.getParentFile().getName().equals(PHOTO_DIR);
    }

    public static void moveFile(final File file, final File targetFile) throws IOException {
        org.apache.commons.io.FileUtils.copyFile(file, targetFile);
        org.apache.commons.io.FileUtils.forceDelete(file);
    }

    public static void deleteQuietly(final File file) {
        try {
            org.apache.commons.io.FileUtils.forceDelete(file);
        } catch (final IOException ignored) {
            Log.w(TAG, "unable to delete file " + file, ignored);
        }
    }
}
