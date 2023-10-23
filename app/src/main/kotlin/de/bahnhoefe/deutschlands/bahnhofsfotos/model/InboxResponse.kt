package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import java.lang.reflect.Type

data class InboxResponse @JvmOverloads constructor(
    @SerializedName("state") var inState: InboxResponseState? = InboxResponseState.ERROR,
    var message: String? = null,
    var id: Long? = null,
    var filename: String? = null,
    var inboxUrl: String? = null,
    var crc32: Long? = null
) {

    var state: InboxResponseState
        get() = inState ?: InboxResponseState.ERROR
        set(value) {
            inState = value
        }

    @JsonAdapter(InboxResponseState.Serializer::class)
    enum class InboxResponseState(val messageId: Int, val uploadState: UploadState) {
        REVIEW(
            R.string.upload_completed,
            UploadState.REVIEW
        ),
        LAT_LON_OUT_OF_RANGE(
            R.string.upload_lat_lon_out_of_range,
            UploadState.UNKNOWN
        ),
        NOT_ENOUGH_DATA(
            R.string.upload_not_enough_data, UploadState.UNKNOWN
        ),
        UNSUPPORTED_CONTENT_TYPE(
            R.string.upload_unsupported_content_type,
            UploadState.UNKNOWN
        ),
        PHOTO_TOO_LARGE(
            R.string.upload_too_big, UploadState.UNKNOWN
        ),
        CONFLICT(R.string.upload_conflict, UploadState.CONFLICT), UNAUTHORIZED(
            R.string.authorization_failed, UploadState.UNKNOWN
        ),
        ERROR(R.string.upload_failed, UploadState.UNKNOWN);

        internal class Serializer : JsonDeserializer<InboxResponseState> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): InboxResponseState {
                return try {
                    valueOf(json.asString)
                } catch (e: Exception) {
                    ERROR
                }
            }
        }
    }

}