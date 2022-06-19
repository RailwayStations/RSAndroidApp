package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class InboxResponse {

    private InboxResponseState state;
    private String message;
    private Long id;
    private String filename;
    private String inboxUrl;
    private Long crc32;

    public InboxResponseState getState() {
        return state;
    }

    public void setState(InboxResponseState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setInboxUrl(String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    public Long getCrc32() {
        return crc32;
    }

    public void setCrc32(Long crc32) {
        this.crc32 = crc32;
    }

    @JsonAdapter(InboxResponseState.Serializer.class)
    public enum InboxResponseState {
        REVIEW(R.string.upload_completed, UploadState.REVIEW),
        LAT_LON_OUT_OF_RANGE(R.string.upload_lat_lon_out_of_range, UploadState.UNKNOWN),
        NOT_ENOUGH_DATA(R.string.upload_not_enough_data, UploadState.UNKNOWN),
        UNSUPPORTED_CONTENT_TYPE(R.string.upload_unsupported_content_type, UploadState.UNKNOWN),
        PHOTO_TOO_LARGE(R.string.upload_too_big, UploadState.UNKNOWN),
        CONFLICT(R.string.upload_conflict, UploadState.CONFLICT),
        UNAUTHORIZED(R.string.authorization_failed, UploadState.UNKNOWN),
        ERROR(R.string.upload_failed, UploadState.UNKNOWN);

        private final int messageId;

        private final UploadState uploadState;

        InboxResponseState(int messageId, UploadState uploadState) {
            this.messageId = messageId;
            this.uploadState = uploadState;
        }

        public int getMessageId() {
            return messageId;
        }

        public UploadState getUploadState() {
            return uploadState;
        }

        static class Serializer implements JsonDeserializer<InboxResponseState> {

            @Override
            public InboxResponseState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                try {
                    return InboxResponseState.valueOf(json.getAsString());
                } catch (Exception e) {
                    return ERROR;
                }
            }
        }
    }

}
