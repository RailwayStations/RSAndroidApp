package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class StationFilter {

    private Boolean photo;

    private Boolean active;

    private String nickname;

    public StationFilter(final Boolean photo, final Boolean active, final String nickname) {
        this.photo = photo;
        this.active = active;
        this.nickname = nickname;
    }

    public Boolean hasPhoto() {
        return photo;
    }

    public void setPhoto(final Boolean photo) {
        this.photo = photo;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(final String nickname) {
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

    public void togglePhoto() {
        if (photo == null) {
            photo = Boolean.TRUE;
        } else if (photo) {
            photo = Boolean.FALSE;
        } else {
            photo = null;
        }
    }

    public void toggleActive() {
        if (active == null) {
            active = Boolean.TRUE;
        } else if (active) {
            active = Boolean.FALSE;
        } else {
            active = null;
        }
    }

    public int getActiveColor() {
        return active == null ? R.color.filterInactive : R.color.filterActive;
    }

    public int getPhotoColor() {
        return photo == null ? R.color.filterInactive : R.color.filterActive;
    }

    public int getNicknameColor() {
        return nickname == null ? R.color.filterInactive : R.color.filterActive;
    }

    public int getPhotoText() {
        return photo == null || photo ? R.string.filter_photo : R.string.filter_no_photo;
    }

    public int getNicknameText() {
        return R.string.filter_nickname;
    }

    public int getActiveText() {
        return active == null || active ? R.string.filter_active : R.string.filter_inactive;
    }
}
