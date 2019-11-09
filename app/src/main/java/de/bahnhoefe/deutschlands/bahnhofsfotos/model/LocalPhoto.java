package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import android.util.Log;

import java.io.File;

import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;

public class LocalPhoto extends UploadStateQuery {

    private static final String TAG = LocalPhoto.class.getSimpleName();

    private transient File file;

    private transient boolean oldFile;

    public LocalPhoto(File file) {
        super();
        this.file = file;

        String fileName = file.getName().substring(0, file.getName().length() - 4);
        oldFile = FileUtils.isOldFile(file);

        if (oldFile) {
            String[] nameParts = fileName.split("[_]");
            if (nameParts.length >= 2) {
                if (nameParts.length == 2) {
                    id = nameParts[1];
                } else {
                    try {
                        lat = Double.parseDouble(nameParts[1]);
                        lon = Double.parseDouble(nameParts[2]);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Error extracting coordinates from filename: " + fileName);
                    }
                }
            } else {
                nameParts = fileName.split("[-]"); // fallback to old naming convention
                if (nameParts.length == 2) {
                    id = nameParts[1];
                }
            }
        } else {
            String[] nameParts = fileName.split("[_]");
            if (nameParts.length == 2) {
                try {
                    lat = Double.parseDouble(nameParts[0]);
                    lon = Double.parseDouble(nameParts[1]);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Error extracting coordinates from filename: " + fileName);
                }
            } else {
                id = fileName;
            }
        }

        countryCode = FileUtils.getCountryFromFile(file);
    }

    public static String getIdByLatLon(Double latitude, Double longitude) {
        return latitude + "_" + longitude;
    }

    public File getFile() {
        return file;
    }

    public boolean isOldFile() {
        return oldFile;
    }

    public boolean hasCoords() {
        return lat != null && lon != null;
    }

    public String getDisplayName() {
        return (countryCode != null ? countryCode + " - " : "") + file.getName();
    }

}
