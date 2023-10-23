package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import de.bahnhoefe.deutschlands.bahnhofsfotos.MapsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public enum NavItem {

    OEPNV(R.string.nav_oepnv, R.drawable.ic_directions_bus_gray_24px, "google.navigation:ll=%s,%s&mode=Transit"),
    CAR(R.string.nav_car, R.drawable.ic_directions_car_gray_24px, "google.navigation:ll=%s,%s&mode=d"),
    BIKE(R.string.nav_bike, R.drawable.ic_directions_bike_gray_24px, "google.navigation:ll=%s,%s&mode=b"),
    WALK(R.string.nav_walk, R.drawable.ic_directions_walk_gray_24px, "google.navigation:ll=%s,%s&mode=w"),
    SHOW(R.string.nav_show, R.drawable.ic_info_gray_24px, "geo:0,0?q=%s,%s(%s)"),
    SHOW_ON_MAP(R.string.nav_show_on_map, R.drawable.ic_map_gray_24px, null) {
        @Override
        public Intent createIntent(Context packageContext, double lat, double lon, String text, int markerRes) {
            var intent = new Intent(packageContext, MapsActivity.class);
            intent.putExtra(MapsActivity.EXTRAS_LATITUDE, lat);
            intent.putExtra(MapsActivity.EXTRAS_LONGITUDE, lon);
            intent.putExtra(MapsActivity.EXTRAS_MARKER, markerRes);
            return intent;
        }
    };

    private final int textRes;
    private final int iconRes;
    private final String uriTemplate;

    NavItem(int textRes, int iconRes, String uriTemplate) {
        this.textRes = textRes;
        this.iconRes = iconRes;
        this.uriTemplate = uriTemplate;
    }

    public int getTextRes() {
        return textRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    public Intent createIntent(Context packageContext, double lat, double lon, String text, int markerRes) {
        var uriString = String.format(uriTemplate, lat, lon, text);
        return new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
    }

}
