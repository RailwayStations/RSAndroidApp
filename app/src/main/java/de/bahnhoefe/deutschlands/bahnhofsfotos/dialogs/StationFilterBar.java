package de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.chip.Chip;

import java.util.stream.IntStream;

import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication;
import de.bahnhoefe.deutschlands.bahnhofsfotos.CountryActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter;

public class StationFilterBar extends LinearLayout {

    private static final String TAG = StationFilterBar.class.getSimpleName();

    private final Chip toggleSort;
    private final Chip photoFilter;
    private final Chip activeFilter;
    private final Chip nicknameFilter;
    private final Chip countrySelection;
    private OnChangeListener listener;
    private BaseApplication baseApplication;
    private Activity activity;

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
        toggleSort.setOnClickListener(this::showSortMenu);

        photoFilter = findViewById(R.id.photoFilter);
        photoFilter.setOnClickListener(this::showPhotoFilter);

        activeFilter = findViewById(R.id.activeFilter);
        activeFilter.setOnClickListener(this::showActiveFilter);

        nicknameFilter = findViewById(R.id.nicknameFilter);
        nicknameFilter.setOnClickListener(this::selectNicknameFilter);

        countrySelection = findViewById(R.id.countrySelection);
        countrySelection.setOnClickListener(this::selectCountry);
    }

    private void setCloseIcon(final Chip chip, final int icon) {
        chip.setCloseIcon(AppCompatResources.getDrawable(this.baseApplication, icon));
    }

    private void setChipStatus(Chip chip, int iconRes, boolean active, int textRes) {
        setChipStatus(chip, iconRes, active, baseApplication.getString(textRes));
    }

    private void setChipStatus(Chip chip, int iconRes, boolean active, String text) {
        if (iconRes != 0) {
            chip.setChipIcon(getTintedDrawable(this.baseApplication, iconRes, getChipForegroundColor(active)));
        } else {
            chip.setChipIcon(null);
        }
        chip.setChipBackgroundColorResource(active ? R.color.colorPrimary : R.color.fullTransparent);
        chip.setTextColor(getChipForegroundColor(active));
        chip.setCloseIconTintResource(getChipForegroundColorRes(active));
        chip.setChipStrokeColorResource(active ? R.color.colorPrimary : R.color.chipForeground);
        chip.setText(text);
        chip.setTextEndPadding(0);
        if (TextUtils.isEmpty(text)) {
            chip.setTextStartPadding(0);
        } else {
            chip.setTextStartPadding(activity.getResources().getDimension(R.dimen.chip_textStartPadding));
        }
    }

    private Drawable getTintedDrawable(Context context, int imageId, int color) {
        if (imageId > 0) {
            var unwrappedDrawable = ContextCompat.getDrawable(context, imageId);
            return getTintedDrawable(unwrappedDrawable, color);
        }
        return null;
    }

    @Nullable
    private static Drawable getTintedDrawable(Drawable unwrappedDrawable, int color) {
        if (unwrappedDrawable != null) {
            var wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
            DrawableCompat.setTint(wrappedDrawable, color);
            return wrappedDrawable;
        }
        return null;
    }

    private int getChipForegroundColor(final boolean active) {
        return this.baseApplication.getColor(getChipForegroundColorRes(active));
    }

    private int getChipForegroundColorRes(final boolean active) {
        return active ? R.color.colorOnPrimary : R.color.chipForeground;
    }

    public void init(BaseApplication baseApplication, Activity activity) {
        this.baseApplication = baseApplication;
        this.activity = activity;
        if (activity instanceof OnChangeListener onChangeListener) {
            listener = onChangeListener;
        }
        var stationFilter = baseApplication.getStationFilter();

        setChipStatus(photoFilter, stationFilter.getPhotoIcon(), stationFilter.isPhotoFilterActive(), R.string.no_text);
        setChipStatus(nicknameFilter, stationFilter.getNicknameIcon(), stationFilter.isNicknameFilterActive(), stationFilter.getNicknameText(this.baseApplication));
        setChipStatus(activeFilter, stationFilter.getActiveIcon(), stationFilter.isActiveFilterActive(), stationFilter.getActiveText());
        setChipStatus(countrySelection, R.drawable.ic_countries_active_24px, true, getCountryText(baseApplication));

        setSortOrder(baseApplication.getSortByDistance());
    }

    private static String getCountryText(final BaseApplication baseApplication) {
        return String.join(",", baseApplication.getCountryCodes());
    }

    private void showActiveFilter(View v) {
        var popup = new PopupMenu(activity, v);
        popup.getMenuInflater().inflate(R.menu.active_filter, popup.getMenu());

        popup.setOnMenuItemClickListener(menuItem -> {
            var stationFilter = baseApplication.getStationFilter();
            if (menuItem.getItemId() == R.id.active_filter_active) {
                stationFilter.setActive(Boolean.TRUE);
            } else if (menuItem.getItemId() == R.id.active_filter_inactive) {
                stationFilter.setActive(Boolean.FALSE);
            } else {
                stationFilter.setActive(null);
            }
            setChipStatus(activeFilter, stationFilter.getActiveIcon(), stationFilter.isActiveFilterActive(), R.string.no_text);
            updateStationFilter(stationFilter);
            return false;
        });

        setPopupMenuIcons(popup);
        popup.setOnDismissListener(menu -> setCloseIcon(activeFilter, R.drawable.ic_baseline_arrow_drop_up_24));
        popup.show();
        setCloseIcon(activeFilter, R.drawable.ic_baseline_arrow_drop_down_24);
    }

    private void showPhotoFilter(View v) {
        var popup = new PopupMenu(activity, v);
        popup.getMenuInflater().inflate(R.menu.photo_filter, popup.getMenu());

        popup.setOnMenuItemClickListener(menuItem -> {
            var stationFilter = baseApplication.getStationFilter();
            if (menuItem.getItemId() == R.id.photo_filter_has_photo) {
                stationFilter.setPhoto(Boolean.TRUE);
            } else if (menuItem.getItemId() == R.id.photo_filter_without_photo) {
                stationFilter.setPhoto(Boolean.FALSE);
            } else {
                stationFilter.setPhoto(null);
            }
            setChipStatus(photoFilter, stationFilter.getPhotoIcon(), stationFilter.isPhotoFilterActive(), R.string.no_text);
            updateStationFilter(stationFilter);
            return false;
        });

        setPopupMenuIcons(popup);
        popup.setOnDismissListener(menu -> setCloseIcon(photoFilter, R.drawable.ic_baseline_arrow_drop_up_24));
        popup.show();
        setCloseIcon(photoFilter, R.drawable.ic_baseline_arrow_drop_down_24);
    }

    public void selectCountry(View view) {
        getContext().startActivity(new Intent(getContext(), CountryActivity.class));
    }

    private void showSortMenu(View v) {
        var popup = new PopupMenu(activity, v);
        popup.getMenuInflater().inflate(R.menu.sort_order, popup.getMenu());

        popup.setOnMenuItemClickListener(menuItem -> {
            boolean sortByDistance = menuItem.getItemId() == R.id.sort_order_by_distance;
            setSortOrder(sortByDistance);
            baseApplication.setSortByDistance(sortByDistance);
            if (listener != null) {
                listener.sortOrderChanged(sortByDistance);
            }
            return false;
        });

        setPopupMenuIcons(popup);
        popup.setOnDismissListener(menu -> setCloseIcon(toggleSort, R.drawable.ic_baseline_arrow_drop_up_24));
        popup.show();
        setCloseIcon(toggleSort, R.drawable.ic_baseline_arrow_drop_down_24);
    }

    @SuppressLint("RestrictedApi")
    private void setPopupMenuIcons(final PopupMenu popup) {
        try {
            if (popup.getMenu() instanceof MenuBuilder menuBuilder) {
                menuBuilder.setOptionalIconsVisible(true);
                for (var item : menuBuilder.getVisibleItems()) {
                    var iconMarginPx =
                            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0, getResources().getDisplayMetrics());
                    if (item.getIcon() != null) {
                        InsetDrawable icon;
                        icon = new InsetDrawable(item.getIcon(), iconMarginPx, 0, iconMarginPx, 0);
                        icon.setTint(getResources().getColor(R.color.colorSurface, null));
                        item.setIcon(icon);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error setting popupMenuIcons: ", e);
        }
    }

    public void setSortOrder(boolean sortByDistance) {
        setChipStatus(toggleSort, sortByDistance ? R.drawable.ic_sort_by_distance_active_24px : R.drawable.ic_sort_by_alpha_active_24px, true, R.string.no_text);
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
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition >= 0 && nicknames.length > selectedPosition) {
                        stationFilter.setNickname(nicknames[selectedPosition]);
                        setChipStatus(nicknameFilter, stationFilter.getNicknameIcon(), stationFilter.isNicknameFilterActive(), stationFilter.getNicknameText(baseApplication));
                        updateStationFilter(stationFilter);
                    }
                })
                .setNeutralButton(R.string.button_remove_text, (dialog, whichButton) -> {
                    dialog.dismiss();
                    stationFilter.setNickname(null);
                    setChipStatus(nicknameFilter, stationFilter.getNicknameIcon(), stationFilter.isNicknameFilterActive(), stationFilter.getNicknameText(baseApplication));
                    updateStationFilter(stationFilter);
                })
                .setNegativeButton(R.string.button_myself_text, (dialog, whichButton) -> {
                    dialog.dismiss();
                    stationFilter.setNickname(baseApplication.getNickname());
                    setChipStatus(nicknameFilter, stationFilter.getNicknameIcon(), stationFilter.isNicknameFilterActive(), stationFilter.getNicknameText(baseApplication));
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

    public void setSortOrderEnabled(boolean enabled) {
        toggleSort.setVisibility(enabled ? VISIBLE : GONE);
    }

    public interface OnChangeListener {
        void stationFilterChanged(StationFilter stationFilter);

        void sortOrderChanged(boolean sortByDistance);
    }

}
