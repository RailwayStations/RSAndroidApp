package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public enum PhotoFilter {

    ALL_STATIONS(R.drawable.ic_station_filter_all_24px, "STATIONS_WITH_PHOTO"),
    STATIONS_WITH_PHOTO(R.drawable.ic_station_filter_with_photo_24px, "STATIONS_WITHOUT_PHOTO"),
    STATIONS_WITHOUT_PHOTO(R.drawable.ic_station_filter_without_photo_24px, "NICKNAME"),
    NICKNAME(R.drawable.ic_station_filter_nickname_24px, "ALL_STATIONS");

    private final int icon;
    private final String nextFilterName;

    PhotoFilter(int icon, String nextFilterName) {
        this.icon = icon;
        this.nextFilterName = nextFilterName;
    }

    public int getIcon() {
        return icon;
    }

    public PhotoFilter getNextFilter() {
        return PhotoFilter.valueOf(nextFilterName);
    }

}
