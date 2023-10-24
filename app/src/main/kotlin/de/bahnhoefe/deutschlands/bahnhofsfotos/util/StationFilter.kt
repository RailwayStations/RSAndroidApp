package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.content.Context
import de.bahnhoefe.deutschlands.bahnhofsfotos.R

class StationFilter(private var photo: Boolean?, var isActive: Boolean?, var nickname: String?) {

    fun hasPhoto(): Boolean? {
        return photo
    }

    fun setPhoto(photo: Boolean?) {
        this.photo = photo
    }

    val photoIcon: Int
        get() {
            if (photo == null) {
                return R.drawable.ic_photo_inactive_24px
            } else if (photo as Boolean) {
                return R.drawable.ic_photo_active_24px
            }
            return R.drawable.ic_photo_missing_active_24px
        }
    val nicknameIcon: Int
        get() = if (nickname == null) R.drawable.ic_person_inactive_24px else R.drawable.ic_person_active_24px
    val activeIcon: Int
        get() {
            if (isActive == null) {
                return R.drawable.ic_station_active_inactive_24px
            } else if (isActive!!) {
                return R.drawable.ic_station_active_active_24px
            }
            return R.drawable.ic_station_inactive_active_24px
        }
    val activeText: Int
        get() = if (isActive == null) R.string.no_text else if (isActive!!) R.string.filter_active else R.string.filter_inactive
    val isPhotoFilterActive: Boolean
        get() = photo != null
    val isActiveFilterActive: Boolean
        get() = isActive != null
    val isNicknameFilterActive: Boolean
        get() = nickname != null

    fun getNicknameText(context: Context?): String? {
        return if (isNicknameFilterActive) nickname else context!!.getString(R.string.no_text)
    }
}