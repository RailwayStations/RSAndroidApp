package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.content.Context;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class StationFilter {

    private Boolean photo;

    private Boolean active;

    private String nickname;

    public StationFilter(Boolean photo, Boolean active, String nickname) {
        this.photo = photo;
        this.active = active;
        this.nickname = nickname;
    }

    public Boolean hasPhoto() {
        return photo;
    }

    public void setPhoto(Boolean photo) {
        this.photo = photo;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getPhotoIcon() {
        if (photo == null) {
            return R.drawable.ic_photo_inactive_24px;
        } else if (photo) {
            return R.drawable.ic_photo_active_24px;
        }
        return R.drawable.ic_photo_missing_active_24px;
    }

    public int getNicknameIcon() {
        return nickname == null ? R.drawable.ic_person_inactive_24px : R.drawable.ic_person_active_24px;
    }

    public int getActiveIcon() {
        if (active == null) {
            return R.drawable.ic_station_active_inactive_24px;
        } else if (active) {
            return R.drawable.ic_station_active_active_24px;
        }
        return R.drawable.ic_station_inactive_active_24px;
    }

    public int getActiveText() {
        return active == null ? R.string.no_text : active ? R.string.filter_active : R.string.filter_inactive;
    }

    public boolean isPhotoFilterActive() {
        return photo != null;
    }

    public boolean isActiveFilterActive() {
        return active != null;
    }

    public boolean isNicknameFilterActive() {
        return nickname != null;
    }

    public String getNicknameText(Context context) {
        return isNicknameFilterActive() ? nickname : context.getString(R.string.no_text);
    }

}
