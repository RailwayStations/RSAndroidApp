package de.bahnhoefe.deutschlands.bahnhofsfotos;

import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.createChooser;

import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityDetailsBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.StationInfoBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Country;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoStations;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProviderApp;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Station;
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.BitmapCache;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.NavItem;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Timetable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = DetailsActivity.class.getSimpleName();

    // Names of Extras that this class reacts to
    public static final String EXTRA_STATION = "EXTRA_STATION";

    private static final String LINK_FORMAT = "<b><a href=\"%s\">%s</a></b>";

    private BaseApplication baseApplication;
    private RSAPIClient rsapiClient;
    private ActivityDetailsBinding binding;
    private Station station;
    private Set<Country> countries;
    private String nickname;
    private PhotoPagerAdapter photoPagerAdapter;
    private final Map<String, Bitmap> photoBitmaps = new HashMap<>();
    private PhotoPagerAdapter.PageablePhoto selectedPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        baseApplication = (BaseApplication) getApplication();
        rsapiClient = baseApplication.getRsapiClient();
        countries = baseApplication.getDbAdapter().fetchCountriesWithProviderApps(baseApplication.getCountryCodes());

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        photoPagerAdapter = new PhotoPagerAdapter(this);
        binding.details.viewPager.setAdapter(photoPagerAdapter);
        binding.details.viewPager.setCurrentItem(0, false);
        binding.details.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                var pageablePhoto = photoPagerAdapter.getPageablePhotoAtPosition(position);
                onPageablePhotoSelected(pageablePhoto);
            }
        });

        // switch off image and license view until we actually have a foto
        binding.details.licenseTag.setVisibility(View.INVISIBLE);
        binding.details.licenseTag.setMovementMethod(LinkMovementMethod.getInstance());

        readPreferences();
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            station = (Station) intent.getSerializableExtra(EXTRA_STATION);

            if (station == null) {
                Log.w(TAG, "EXTRA_STATION in intent data missing");
                Toast.makeText(this, R.string.station_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            binding.details.marker.setImageDrawable(ContextCompat.getDrawable(this, getMarkerRes()));

            binding.details.tvStationTitle.setText(station.getTitle());
            binding.details.tvStationTitle.setSingleLine(false);

            if (station.hasPhoto()) {
                if (ConnectionUtil.checkInternetConnection(this)) {
                    photoBitmaps.put(station.getPhotoUrl(), null);
                    BitmapCache.getInstance().getPhoto((bitmap) -> {
                        PhotoPagerAdapter.PageablePhoto pageablePhoto = PhotoPagerAdapter.PageablePhoto.builder()
                                .id(station.getPhotoId())
                                .bitmap(bitmap)
                                .url(station.getPhotoUrl())
                                .photographer(station.getPhotographer())
                                .photographerUrl(station.getPhotographerUrl())
                                .license(station.getLicense())
                                .licenseUrl(station.getLicenseUrl())
                                .build();
                        runOnUiThread(() -> {
                            photoPagerAdapter.addPageablePhoto(pageablePhoto);
                            onPageablePhotoSelected(pageablePhoto);
                        });
                    }, station.getPhotoUrl());
                }
            }

            loadAdditionalPhotos(station);
        }

    }

    private void loadAdditionalPhotos(final Station station) {
        rsapiClient.getPhotoStationById(station.getCountry(), station.getId()).enqueue(new Callback<>() {

            @Override
            public void onResponse(@NonNull final Call<PhotoStations> call, @NonNull final Response<PhotoStations> response) {
                if (response.isSuccessful()) {
                    var photoStations = response.body();
                    if (photoStations == null) {
                        return;
                    }
                    photoStations.getStations().stream()
                            .flatMap(photoStation -> photoStation.getPhotos().stream())
                            .forEach(photo -> {
                                var url = photoStations.getPhotoBaseUrl() + photo.getPath();
                                if (!photoBitmaps.containsKey(url)) {
                                    photoBitmaps.put(url, null);
                                    BitmapCache.getInstance().getPhoto((bitmap) -> runOnUiThread(() -> photoPagerAdapter.addPageablePhoto(PhotoPagerAdapter.PageablePhoto.builder()
                                            .id(photo.getId())
                                            .url(url)
                                            .bitmap(bitmap)
                                            .photographer(photo.getPhotographer())
                                            .photographerUrl(photoStations.getPhotographerUrl(photo.getPhotographer()))
                                            .license(photoStations.getLicenseName(photo.getLicense()))
                                            .licenseUrl(photoStations.getLicenseUrl(photo.getLicense()))
                                            .build())), url);
                                }
                            });
                }
            }

            @Override
            public void onFailure(@NonNull final Call<PhotoStations> call, @NonNull final Throwable t) {
                Log.e(TAG, "Failed to load additional photos", t);
            }
        });
    }

    private int getMarkerRes() {
        if (station == null) {
            return R.drawable.marker_missing;
        }
        if (station.hasPhoto()) {
            if (isOwner()) {
                return station.isActive() ? R.drawable.marker_violet : R.drawable.marker_violet_inactive;
            } else {
                return station.isActive() ? R.drawable.marker_green : R.drawable.marker_green_inactive;
            }
        } else {
            return station.isActive() ? R.drawable.marker_red : R.drawable.marker_red_inactive;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        readPreferences();
    }

    private void readPreferences() {
        nickname = baseApplication.getNickname();
    }

    private boolean isOwner() {
        return station != null && TextUtils.equals(nickname, station.getPhotographer());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.details, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_photo) {
            var intent = new Intent(DetailsActivity.this, UploadActivity.class);
            intent.putExtra(UploadActivity.EXTRA_STATION, station);
            startActivity(intent);
        } else if (itemId == R.id.report_problem) {
            var intent = new Intent(DetailsActivity.this, ProblemReportActivity.class);
            intent.putExtra(ProblemReportActivity.EXTRA_STATION, station);
            intent.putExtra(ProblemReportActivity.EXTRA_PHOTO_ID, selectedPhoto != null ? selectedPhoto.getId() : null);
            startActivity(intent);
        } else if (itemId == R.id.nav_to_station) {
            startNavigation(DetailsActivity.this);
        } else if (itemId == R.id.timetable) {
            Country.getCountryByCode(countries, station.getCountry()).map(country -> {
                var timetableIntent = new Timetable().createTimetableIntent(country, station);
                if (timetableIntent != null) {
                    startActivity(timetableIntent);
                }
                return null;
            });
        } else if (itemId == R.id.share_link) {
            var stationUri = Uri.parse(String.format("https://map.railway-stations.org/station.php?countryCode=%s&stationId=%s", station.getCountry(), station.getId()));
            startActivity(new Intent(ACTION_VIEW, stationUri));
        } else if (itemId == R.id.share_photo) {
            Country.getCountryByCode(countries, station.getCountry()).map(country -> {
                var shareIntent = createPhotoSendIntent();
                if (shareIntent == null) {
                    return null;
                }
                shareIntent.putExtra(Intent.EXTRA_TEXT, country.getTwitterTags() + " " + binding.details.tvStationTitle.getText());
                shareIntent.setType("image/jpeg");
                startActivity(createChooser(shareIntent, "send"));
                return null;
            });
        } else if (itemId == R.id.station_info) {
            showStationInfo(null);
        } else if (itemId == R.id.provider_android_app) {
            Country.getCountryByCode(countries, station.getCountry()).map(country -> {
                var providerApps = country.getCompatibleProviderApps();
                if (providerApps.size() == 1) {
                    openAppOrPlayStore(providerApps.get(0), this);
                } else if (providerApps.size() > 1) {
                    var appNames = providerApps.stream()
                            .map(ProviderApp::getName).toArray(CharSequence[]::new);
                    SimpleDialogs.simpleSelect(this, getResources().getString(R.string.choose_provider_app), appNames, (dialog, which) -> {
                        if (which >= 0 && providerApps.size() > which) {
                            openAppOrPlayStore(providerApps.get(which), DetailsActivity.this);
                        }
                    });
                } else {
                    Toast.makeText(this, R.string.provider_app_missing, Toast.LENGTH_LONG).show();
                }
                return null;
            });
        } else if (itemId == android.R.id.home) {
            navigateUp();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    /**
     * Tries to open the provider app if installed. If it is not installed or cannot be opened Google Play Store will be opened instead.
     *
     * @param context activity context
     */
    public void openAppOrPlayStore(ProviderApp providerApp, Context context) {
        // Try to open App
        boolean success = openApp(providerApp, context);
        // Could not open App, open play store instead
        if (!success) {
            var intent = new Intent(ACTION_VIEW);
            intent.setData(Uri.parse(providerApp.getUrl()));
            context.startActivity(intent);
        }
    }

    /**
     * Open another app.
     *
     * @param context activity context
     * @return true if likely successful, false if unsuccessful
     * @see https://stackoverflow.com/a/7596063/714965
     */
    @SuppressWarnings("JavadocReference")
    private boolean openApp(ProviderApp providerApp, Context context) {
        if (!providerApp.isAndroid()) {
            return false;
        }
        var manager = context.getPackageManager();
        try {
            String packageName = Uri.parse(providerApp.getUrl()).getQueryParameter("id");
            assert packageName != null;
            var intent = manager.getLaunchIntentForPackage(packageName);
            if (intent == null) {
                return false;
            }
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        navigateUp();
    }

    public void navigateUp() {
        var callingActivity = getCallingActivity(); // if MapsActivity was calling, then we don't want to rebuild the Backstack
        var upIntent = NavUtils.getParentActivityIntent(this);
        if (callingActivity == null && upIntent != null) {
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot()) {
                Log.v(TAG, "Recreate back stack");
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
            }
        }

        finish();
    }

    public void showStationInfo(View view) {
        var stationInfoBinding = StationInfoBinding.inflate(getLayoutInflater());
        stationInfoBinding.id.setText(station.getId());
        stationInfoBinding.coordinates.setText(String.format(Locale.US, getResources().getString(R.string.coordinates), station.getLat(), station.getLon()));
        stationInfoBinding.active.setText(station != null && station.isActive() ? R.string.active : R.string.inactive);
        stationInfoBinding.owner.setText(station != null && station.getPhotographer() != null ? station.getPhotographer() : "");
        if (station.isOutdated()) {
            stationInfoBinding.outdatedLabel.setVisibility(View.VISIBLE);
        }

        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                .setTitle(binding.details.tvStationTitle.getText())
                .setView(stationInfoBinding.getRoot())
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    private Intent createPhotoSendIntent() {
        if (selectedPhoto != null) {
            var sendIntent = new Intent(Intent.ACTION_SEND);
            var newFile = FileUtils.getImageCacheFile(getApplicationContext(), String.valueOf(System.currentTimeMillis()));
            try {
                Log.i(TAG, "Save photo to: " + newFile);
                selectedPhoto.getBitmap().compress(Bitmap.CompressFormat.JPEG, Constants.STORED_PHOTO_QUALITY, new FileOutputStream(newFile));
                sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(DetailsActivity.this,
                        BuildConfig.APPLICATION_ID + ".fileprovider", newFile));
                return sendIntent;
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error saving cached bitmap", e);
            }
        }
        return null;
    }

    private void startNavigation(Context context) {
        var adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item,
                android.R.id.text1, NavItem.values()) {
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                var item = getItem(position);
                assert item != null;

                var view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);

                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(item.getIconRes(), 0, 0, 0);
                tv.setText(getString(item.getTextRes()));

                //Add margin between image and text (support various screen densities)
                int dp5 = (int) (20 * getResources().getDisplayMetrics().density + 0.5f);
                int dp7 = (int) (20 * getResources().getDisplayMetrics().density);
                tv.setCompoundDrawablePadding(dp5);
                tv.setPadding(dp7, 0, 0, 0);

                return view;
            }
        };

        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.navMethod)
                .setAdapter(adapter, (dialog, position) -> {
                    var item = adapter.getItem(position);
                    assert item != null;
                    var lat = station.getLat();
                    var lon = station.getLon();
                    var intent = item.createIntent(DetailsActivity.this, lat, lon, binding.details.tvStationTitle.getText().toString(), getMarkerRes());
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, R.string.activitynotfound, Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    public void onPageablePhotoSelected(PhotoPagerAdapter.PageablePhoto pageablePhoto) {
        selectedPhoto = pageablePhoto;
        binding.details.licenseTag.setVisibility(View.INVISIBLE);

        if (pageablePhoto == null) {
            return;
        }

        // Lizenzinfo aufbauen und einblenden
        binding.details.licenseTag.setVisibility(View.VISIBLE);
        boolean photographerUrlAvailable = pageablePhoto.getPhotographerUrl() != null && !pageablePhoto.getPhotographerUrl().isEmpty();
        boolean licenseUrlAvailable = pageablePhoto.getLicenseUrl() != null && !pageablePhoto.getLicenseUrl().isEmpty();

        String photographerText;
        if (photographerUrlAvailable) {
            photographerText = String.format(
                    LINK_FORMAT,
                    pageablePhoto.getPhotographerUrl(),
                    pageablePhoto.getPhotographer());
        } else {
            photographerText = pageablePhoto.getPhotographer();
        }

        String licenseText;
        if (licenseUrlAvailable) {
            licenseText = String.format(
                    LINK_FORMAT,
                    pageablePhoto.getLicenseUrl(),
                    pageablePhoto.getLicense());
        } else {
            licenseText = pageablePhoto.getLicense();
        }

        binding.details.licenseTag.setText(
                Html.fromHtml(
                        String.format(
                                getText(R.string.license_tag).toString(),
                                photographerText,
                                licenseText), Html.FROM_HTML_MODE_LEGACY
                )
        );
    }

}
