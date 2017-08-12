package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class HighScoreItem {
    private final String name;
    private final int photos;
    private final int position;

    public HighScoreItem(final String name, final int photos, final int position) {
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
    public String toString() {
        return name;
    }

}
