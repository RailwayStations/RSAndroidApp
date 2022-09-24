package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import androidx.annotation.NonNull;

import java.util.Objects;

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighScoreItem that = (HighScoreItem) o;
        return photos == that.photos && position == that.position && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, photos, position);
    }
    
}
