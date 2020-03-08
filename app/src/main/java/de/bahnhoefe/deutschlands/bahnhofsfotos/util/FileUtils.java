package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;
import android.os.Environment;
import androidx.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.bahnhoefe.deutschlands.bahnhofsfotos.model.LocalPhoto;

public class FileUtils {

    private static final String PHOTO_DIR = "Bahnhofsfotos";
    private static final String TAG = FileUtils.class.getSimpleName();
    private static final String TEMP_DIR = ".temp";
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

    public static List<LocalPhoto> getLocalPhotos(final Context context) {
        final File fotoDir = FileUtils.getLocalFotoDir(context);

        final List<LocalPhoto> localPhotos = new ArrayList<>();
        if (fotoDir != null) {
            // add fotos from root (not yet migrated)
            localPhotos.addAll(getLocalPhotos(fotoDir));

            // get files from country dirs
            final File[] listFile = fotoDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(final File file) {
                    return file.isDirectory() && !file.getName().endsWith(TEMP_DIR);
                }
            });
            for (final File subDir : listFile) {
                localPhotos.addAll(getLocalPhotos(subDir));
            }
        }
        return localPhotos;
    }

    public static List<LocalPhoto> getLocalPhotos(final File fotoDir) {
        final File[] listFile = fotoDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return file.isFile() && file.getName().endsWith(".jpg");
            }
        });
        return toLocalPhotos(listFile);
    }

    public static List<LocalPhoto> toLocalPhotos(final File[] listFile) {
        final List<LocalPhoto> localPhotos = new ArrayList<>(listFile.length);
        for (final File file : listFile) {
            localPhotos.add(new LocalPhoto(file));
        }
        return localPhotos;
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
