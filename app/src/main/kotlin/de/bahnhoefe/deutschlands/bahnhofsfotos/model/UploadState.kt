package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import java.lang.reflect.Type

@JsonAdapter(UploadState.Serializer::class)
enum class UploadState(val textId: Int, val colorId: Int, val isPending: Boolean) {
    NOT_YET_SENT(
        R.string.upload_not_yet_sent,
        R.color.gridItem,
        true
    ),

    UNKNOWN(R.string.upload_state_unknown, R.color.gridItem, true),

    REVIEW(
        R.string.upload_state_in_review, R.color.gridItemReview, true
    ),

    CONFLICT(R.string.upload_state_conflict, R.color.gridItemError, true),

    ACCEPTED(
        R.string.upload_state_accepted, R.color.gridItemGood, false
    ),
    
    REJECTED(R.string.upload_state_rejected, R.color.gridItemError, false);

    internal class Serializer : JsonDeserializer<UploadState> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): UploadState {
            return try {
                valueOf(json.asString)
            } catch (e: Exception) {
                UNKNOWN
            }
        }
    }
}