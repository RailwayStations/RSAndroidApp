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
    public static File getLocalFotoDir(Context context) {
        return mkdirs(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
    }

    private static File mkdirs(File dir) {
        try {
            org.apache.commons.io.FileUtils.forceMkdir(dir);
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
    public static File getStoredMediaFile(Context context, String country, String bahnhofId) {
        String countryPrefix = country != null ? country : MISSING_DIR;
        File mediaStorageDir = mkdirs(FileUtils.getLocalFotoDir(context));
        if (mediaStorageDir == null) {
            return null;
        }

        File storeMediaFile = new File(mediaStorageDir, String.format("%s-%s.jpg", countryPrefix, bahnhofId));
        Log.d(TAG, "StoredMediaFile: " + storeMediaFile.toString());

        return storeMediaFile;
    }

    /**
     * Get the temporary file path for the Camera app to store the unprocessed foto to.
     *
     * @return the File
     */
    public static File getCameraMediaFile(Context context, String bahnhofId) {
        return getImageCacheFile(context, bahnhofId);
    }

    public static File getImageCacheFile(Context applicationContext, String bahnhofId) {
        File imagePath = new File(applicationContext.getCacheDir(), "images");
        imagePath.mkdirs();
        return new File(imagePath, bahnhofId + ".jpg");
    }

    public static List<LocalPhoto> getLocalPhotos(Context context) {
        File fotoDir = FileUtils.getLocalFotoDir(context);

        List<LocalPhoto> localPhotos = new ArrayList<>();
        if (fotoDir != null) {
            // add fotos from root (not yet migrated)
            localPhotos.addAll(getLocalPhotos(fotoDir));

            // get files from country dirs
            File[] listFile = fotoDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() && !file.getName().endsWith(TEMP_DIR);
                }
            });
            for (File subDir : listFile) {
                localPhotos.addAll(getLocalPhotos(subDir));
            }
        }
        return localPhotos;
    }

    public static List<LocalPhoto> getLocalPhotos(File fotoDir) {
        File[] listFile = fotoDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".jpg");
            }
        });
        return toLocalPhotos(listFile);
    }

    public static List<LocalPhoto> toLocalPhotos(File[] listFile) {
        List<LocalPhoto> localPhotos = new ArrayList<>(listFile.length);
        for (File file : listFile) {
            localPhotos.add(new LocalPhoto(file));
        }
        return localPhotos;
    }

    public static String getCountryFromFile(File file) {
        if (isOldFile(file) || isMissingStation(file)) {
            return null;
        }
        return file.getParentFile().getName();
    }

    public static boolean isMissingStation(File file) {
        return file.getParentFile().getName().equals(MISSING_DIR);
    }

    public static boolean isOldFile(File file) {
        return file.getParentFile().getName().equals(PHOTO_DIR);
    }

    public static void moveFile(File file, File targetFile) throws IOException {
        org.apache.commons.io.FileUtils.copyFile(file, targetFile);
        org.apache.commons.io.FileUtils.forceDelete(file);
    }

    public static void deleteQuietly(File file) {
        try {
            org.apache.commons.io.FileUtils.forceDelete(file);
        } catch (IOException ignored) {
            Log.w(TAG, "unable to delete file " + file, ignored);
        }
    }
}
