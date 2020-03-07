package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public enum ProblemType {
    WRONG_LOCATION(R.string.problem_wrong_location),
    STATION_INACTIVE(R.string.problem_station_inactive),
    STATION_NONEXISTENT(R.string.problem_station_nonexistent),
    WRONG_PHOTO(R.string.problem_wrong_photo),
    OTHER(R.string.problem_other);

    private final int messageId;

    ProblemType(int messageId) {
        this.messageId = messageId;
    }

    public int getMessageId() {
        return messageId;
    }

}
