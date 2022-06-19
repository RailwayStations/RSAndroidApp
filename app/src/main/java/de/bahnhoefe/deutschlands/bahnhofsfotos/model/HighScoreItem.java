package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import androidx.annotation.NonNull;

public class HighScoreItem {
    private final String name;
    private final int photos;
    private final int position;

    public HighScoreItem(String name, int photos, int position) {
        this.name = name;
        this.photos = photos;
        this.position = position;
    }

    public int getPhotos() {
        return photos;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    @Override
    @NonNull
    public String toString() {
        return name;
    }

}
