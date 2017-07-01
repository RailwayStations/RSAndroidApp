package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class HighScoreItem {
    private final String name;
    private final int photos;

    public HighScoreItem(final String name, final int photos) {
        this.name = name;
        this.photos = photos;
    }

    public int getPhotos() {
        return photos;
    }

    public String getName() {
        return name;
    }
}
