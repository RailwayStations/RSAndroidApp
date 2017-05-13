package de.bahnhoefe.deutschlands.bahnhofsfotos;

enum NotificationState {

    OFF(false, false, R.drawable.ic_notifications_off_white_24px),
    ALL(true, false, R.drawable.ic_notifications_active_white_24px),
    ONLY_WITHOUT_PHOTO(true, true, R.drawable.ic_notifications_active_white_photo_24px);

    private final boolean active;
    private final boolean onlyWithoutPhoto;
    private final int iconResourceId;

    NotificationState(final boolean active, final boolean onlyWithoutPhoto, final int iconResourceId) {
        this.active = active;
        this.onlyWithoutPhoto = onlyWithoutPhoto;
        this.iconResourceId = iconResourceId;
    }

    public boolean onlyWithoutPhoto() {
        return onlyWithoutPhoto;
    }

    public boolean isActive() {
        return active;
    }


    public int getIconResourceId() {
        return iconResourceId;
    }
}
