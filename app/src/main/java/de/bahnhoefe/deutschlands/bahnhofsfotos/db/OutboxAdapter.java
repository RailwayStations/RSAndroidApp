package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import androidx.appcompat.content.res.AppCompatResources;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemUploadBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

public class OutboxAdapter extends CursorAdapter {

    private final Activity activity;

    public OutboxAdapter(Activity activity, Cursor uploadCursor) {
        super(activity, uploadCursor, 0);
        this.activity = activity;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        var binding = ItemUploadBinding.inflate(activity.getLayoutInflater(), parent, false);
        var view = binding.getRoot();
        view.setTag(binding);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        var binding = (ItemUploadBinding) view.getTag();
        var id = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.CURSOR_ADAPTER_ID));
        var remoteId = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.UPLOADS.REMOTE_ID));
        var stationId = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.STATION_ID));
        var uploadTitle = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.TITLE));
        var stationTitle = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.JOIN_STATION_TITLE));
        var problemType = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.PROBLEM_TYPE));
        var uploadStateStr = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.UPLOAD_STATE));
        var comment = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.COMMENT));
        var rejectReason = cursor.getString(cursor.getColumnIndexOrThrow(Constants.UPLOADS.REJECTED_REASON));

        var uploadState = UploadState.valueOf(uploadStateStr);
        var textState = id + (remoteId > 0 ? "/" + remoteId : "") + ": " + context.getString(uploadState.getTextId());
        binding.txtState.setText(textState);
        binding.txtState.setTextColor(context.getResources().getColor(uploadState.getColorId(), null));

        if (problemType != null) {
            binding.uploadType.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_bullhorn_48px));
            binding.txtComment.setText(String.format("%s: %s", context.getText(ProblemType.valueOf(problemType).getMessageId()), comment));
        } else if (stationId == null) {
            binding.uploadType.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_station_red_24px));
            binding.txtComment.setText(comment);
        } else {
            binding.uploadType.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_photo_red_48px));
            binding.txtComment.setText(comment);
        }
        binding.txtComment.setVisibility(comment == null ? View.GONE : View.VISIBLE);

        binding.txtStationName.setText(uploadTitle != null ? uploadTitle : stationTitle);
        binding.txtRejectReason.setText(rejectReason);
        binding.txtRejectReason.setVisibility(rejectReason == null ? View.GONE : View.VISIBLE);
    }

}
