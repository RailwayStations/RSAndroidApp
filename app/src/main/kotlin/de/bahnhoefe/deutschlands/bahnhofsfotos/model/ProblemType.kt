package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import de.bahnhoefe.deutschlands.bahnhofsfotos.R

enum class ProblemType(val messageId: Int) {
    WRONG_LOCATION(R.string.problem_wrong_location),

    WRONG_NAME(R.string.problem_wrong_name),

    STATION_INACTIVE(
        R.string.problem_station_inactive
    ),

    STATION_ACTIVE(R.string.problem_station_active),

    STATION_NONEXISTENT(
        R.string.problem_station_nonexistent
    ),
    WRONG_PHOTO(R.string.problem_wrong_photo),

    PHOTO_OUTDATED(
        R.string.problem_photo_outdated
    ),
    
    OTHER(R.string.problem_other);

}