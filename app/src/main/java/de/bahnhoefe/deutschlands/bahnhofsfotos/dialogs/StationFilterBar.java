package de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs;

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

import java.util.stream.IntStream;

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

    public StationFilterBar(Context context) {
        this(context, null);
    }

    public StationFilterBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StationFilterBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public StationFilterBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.station_filter_bar, this);

        toggleSort = findViewById(R.id.toggleSort);
        toggleSort.setOnClickListener(this::toggleSort);

        photoFilter = findViewById(R.id.photoFilter);
        photoFilter.setOnClickListener(this::togglePhotoFilter);

        activeFilter = findViewById(R.id.activeFilter);
        activeFilter.setOnClickListener(this::toggleActiveFilter);

        nicknameFilter = findViewById(R.id.nicknameFilter);
        nicknameFilter.setOnClickListener(this::selectNicknameFilter);

        this.<Button>findViewById(R.id.countrySelection).setOnClickListener(this::selectCountry);
    }

    private void setFilterButton(Button button, int iconRes, int textRes, int textColorRes) {
        button.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(getContext(), iconRes), null, null);
        button.setTextColor(getResources().getColor(textColorRes, null));
        button.setText(textRes);
    }

    public void setBaseApplication(BaseApplication baseApplication) {
        this.baseApplication = baseApplication;
        var stationFilter = baseApplication.getStationFilter();

        setFilterButton(photoFilter, stationFilter.getPhotoIcon(), stationFilter.getPhotoText(), stationFilter.getPhotoColor());
        setFilterButton(nicknameFilter, stationFilter.getNicknameIcon(), R.string.filter_nickname, stationFilter.getNicknameColor());
        setFilterButton(activeFilter, stationFilter.getActiveIcon(), stationFilter.getActiveText(), stationFilter.getActiveColor());

        setSortOrder(baseApplication.getSortByDistance());
    }

    public void toggleActiveFilter(View view) {
        var stationFilter = baseApplication.getStationFilter();
        stationFilter.toggleActive();
        setFilterButton(activeFilter, stationFilter.getActiveIcon(), stationFilter.getActiveText(), stationFilter.getActiveColor());
        updateStationFilter(stationFilter);
    }

    public void togglePhotoFilter(View view) {
        var stationFilter = baseApplication.getStationFilter();
        stationFilter.togglePhoto();
        setFilterButton(photoFilter, stationFilter.getPhotoIcon(), stationFilter.getPhotoText(), stationFilter.getPhotoColor());
        updateStationFilter(stationFilter);
    }

    public void selectCountry(View view) {
        getContext().startActivity(new Intent(getContext(), CountryActivity.class));
    }

    public void toggleSort(View view) {
        boolean sortByDistance = baseApplication.getSortByDistance();
        sortByDistance = !sortByDistance;
        setSortOrder(sortByDistance);
        baseApplication.setSortByDistance(sortByDistance);
        if (listener != null) {
            listener.sortOrderChanged(sortByDistance);
        }
    }

    public void setSortOrder(boolean sortByDistance) {
        setFilterButton(toggleSort, sortByDistance ? R.drawable.ic_sort_by_distance_active_24px : R.drawable.ic_sort_by_alpha_active_24px, R.string.sort_order, R.color.filterActive);
    }

    public void selectNicknameFilter(View view) {
        var nicknames = baseApplication.getDbAdapter().getPhotographerNicknames();
        var stationFilter = baseApplication.getStationFilter();
        if (nicknames.length == 0) {
            Toast.makeText(getContext(), getContext().getString(R.string.no_nicknames_found), Toast.LENGTH_LONG).show();
            return;
        }
        int selectedNickname = IntStream.range(0, nicknames.length)
                .filter(i -> nicknames[i].equals(stationFilter.getNickname()))
                .findFirst().orElse(-1);

        new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.select_nickname)
                .setSingleChoiceItems(nicknames, selectedNickname, null)
                .setPositiveButton(R.string.button_ok_text, (dialog, whichButton) -> {
                    dialog.dismiss();
                    int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition >= 0 && nicknames.length > selectedPosition) {
                        stationFilter.setNickname(nicknames[selectedPosition]);
                        setFilterButton(nicknameFilter, stationFilter.getNicknameIcon(), R.string.filter_nickname, stationFilter.getNicknameColor());
                        updateStationFilter(stationFilter);
                    }
                })
                .setNeutralButton(R.string.button_remove_text, (dialog, whichButton) -> {
                    dialog.dismiss();
                    stationFilter.setNickname(null);
                    setFilterButton(nicknameFilter, stationFilter.getNicknameIcon(), R.string.filter_nickname, stationFilter.getNicknameColor());
                    updateStationFilter(stationFilter);
                })
                .setNegativeButton(R.string.button_myself_text, (dialog, whichButton) -> {
                    dialog.dismiss();
                    stationFilter.setNickname(baseApplication.getNickname());
                    setFilterButton(nicknameFilter, stationFilter.getNicknameIcon(), R.string.filter_nickname, stationFilter.getNicknameColor());
                    updateStationFilter(stationFilter);
                })
                .create().show();
    }

    private void updateStationFilter(StationFilter stationFilter) {
        baseApplication.setStationFilter(stationFilter);
        if (listener != null) {
            listener.stationFilterChanged(stationFilter);
        }
    }

    public void setOnChangeListener(OnChangeListener listener) {
        this.listener = listener;
    }

    public void setSortOrderEnabled(boolean enabled) {
        toggleSort.setVisibility(enabled ? VISIBLE : GONE);
    }

    public interface OnChangeListener {
        void stationFilterChanged(StationFilter stationFilter);
        void sortOrderChanged(boolean sortByDistance);
    }

}
