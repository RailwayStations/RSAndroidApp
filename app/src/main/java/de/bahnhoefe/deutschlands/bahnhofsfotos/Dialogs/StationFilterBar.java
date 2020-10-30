package de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication;
import de.bahnhoefe.deutschlands.bahnhofsfotos.CountryActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter;

public class StationFilterBar extends LinearLayout {

    private final Button toggleSort;
    private final Button photoFilter;
    private final Button activeFilter;
    private final Button nicknameFilter;
    private OnChangeListener listener;
    private BaseApplication baseApplication;

    public StationFilterBar(final Context context) {
        this(context, null);
    }

    public StationFilterBar(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StationFilterBar(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public StationFilterBar(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.station_filter_bar, this);

        toggleSort = findViewById(R.id.toggleSort);
        toggleSort.setOnClickListener(this::toggleSort);

        photoFilter = findViewById(R.id.photoFilter);
        photoFilter.setOnClickListener(this::togglePhotoFilter);

        activeFilter = findViewById(R.id.activeFilter);
        activeFilter.setOnClickListener(this::toggleActiveFilter);

        nicknameFilter = findViewById(R.id.nicknameFilter);
        nicknameFilter.setOnClickListener(this::selectNicknameFilter);

        final Button countrySelection = findViewById(R.id.countrySelection);
        countrySelection.setOnClickListener(this::selectCountry);
    }

    private void setFilterButton(final Button button, final int iconRes, final int textRes, final int textColorRes) {
        button.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(getContext(), iconRes), null, null);
        button.setTextColor(getResources().getColor(textColorRes));
        button.setText(textRes);
    }

    public void setBaseApplication(final BaseApplication baseApplication) {
        this.baseApplication = baseApplication;
        final StationFilter stationFilter = baseApplication.getStationFilter();

        setFilterButton(photoFilter, stationFilter.getPhotoIcon(), stationFilter.getPhotoText(), stationFilter.getPhotoColor());
        setFilterButton(nicknameFilter, stationFilter.getNicknameIcon(), stationFilter.getNicknameText(), stationFilter.getNicknameColor());
        setFilterButton(activeFilter, stationFilter.getActiveIcon(), stationFilter.getActiveText(), stationFilter.getActiveColor());

        setSortOrder(baseApplication.getSortByDistance());
    }

    public void toggleActiveFilter(final View view) {
        final StationFilter stationFilter = baseApplication.getStationFilter();
        stationFilter.toggleActive();
        setFilterButton(activeFilter, stationFilter.getActiveIcon(), stationFilter.getActiveText(), stationFilter.getActiveColor());
        updateStationFilter(stationFilter);
    }

    public void togglePhotoFilter(final View view) {
        final StationFilter stationFilter = baseApplication.getStationFilter();
        stationFilter.togglePhoto();
        setFilterButton(photoFilter, stationFilter.getPhotoIcon(), stationFilter.getPhotoText(), stationFilter.getPhotoColor());
        updateStationFilter(stationFilter);
    }

    public void selectCountry(final View view) {
        final Intent intent = new Intent(getContext(), CountryActivity.class);
        getContext().startActivity(intent);
    }

    public void toggleSort(final View view) {
        boolean sortByDistance = baseApplication.getSortByDistance();
        sortByDistance = !sortByDistance;
        setSortOrder(sortByDistance);
        baseApplication.setSortByDistance(sortByDistance);
        if (listener != null) {
            listener.sortOrderChanged(sortByDistance);
        }
    }

    public void setSortOrder(final boolean sortByDistance) {
        setFilterButton(toggleSort, sortByDistance ? R.drawable.ic_sort_by_distance_active_24px : R.drawable.ic_sort_by_alpha_active_24px, R.string.sort_order, R.color.filterActive);
    }

    public void selectNicknameFilter(final View view) {
        int selectedNickname = -1;
        final String[] nicknames = baseApplication.getDbAdapter().getPhotographerNicknames();
        final StationFilter stationFilter = baseApplication.getStationFilter();
        if (nicknames.length == 0) {
            Toast.makeText(getContext(), getContext().getString(R.string.no_nicknames_found), Toast.LENGTH_LONG).show();
            return;
        }
        for (int i = 0; i < nicknames.length; i++) {
            if (nicknames[i].equals(stationFilter.getNickname())) {
                selectedNickname = i;
            }
        }

        new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.select_nickname)
                .setSingleChoiceItems(nicknames, selectedNickname, null)
                .setPositiveButton(R.string.button_ok_text, (dialog, whichButton) -> {
                    dialog.dismiss();
                    final int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition >= 0 && nicknames.length > selectedPosition) {
                        stationFilter.setNickname(nicknames[selectedPosition]);
                        setFilterButton(nicknameFilter, stationFilter.getNicknameIcon(), stationFilter.getNicknameText(), stationFilter.getNicknameColor());
                        updateStationFilter(stationFilter);
                    }
                })
                .setNeutralButton(R.string.button_remove_text, (dialog, whichButton) -> {
                    dialog.dismiss();
                    stationFilter.setNickname(null);
                    setFilterButton(nicknameFilter, stationFilter.getNicknameIcon(), stationFilter.getNicknameText(), stationFilter.getNicknameColor());
                    updateStationFilter(stationFilter);
                })
                .setNegativeButton(R.string.button_myself_text, (dialog, whichButton) -> {
                    dialog.dismiss();
                    stationFilter.setNickname(baseApplication.getNickname());
                    setFilterButton(nicknameFilter, stationFilter.getNicknameIcon(), stationFilter.getNicknameText(), stationFilter.getNicknameColor());
                    updateStationFilter(stationFilter);
                })
                .create().show();
    }

    private void updateStationFilter(final StationFilter stationFilter) {
        baseApplication.setStationFilter(stationFilter);
        if (listener != null) {
            listener.stationFilterChanged(stationFilter);
        }
    }

    public void setOnChangeListener(final OnChangeListener listener) {
        this.listener = listener;
    }

    public void setSortOrderEnabled(final boolean enabled) {
        toggleSort.setVisibility(enabled ? VISIBLE : GONE);
    }

    public interface OnChangeListener {
        void stationFilterChanged(final StationFilter stationFilter);
        void sortOrderChanged(final boolean sortByDistance);
    }

}
