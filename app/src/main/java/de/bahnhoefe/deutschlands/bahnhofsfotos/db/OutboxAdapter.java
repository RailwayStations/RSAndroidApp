package de.bahnhoefe.deutschlands.bahnhofsfotos.db;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemUploadBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

public class OutboxAdapter extends CursorAdapter {

    private final Activity activity;

    private static LayoutInflater inflater = null;

    public OutboxAdapter(final Activity activity, final Cursor uploadCursor) {
        super(activity, uploadCursor, 0);
        this.activity = activity;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final ItemUploadBinding binding = ItemUploadBinding.inflate(activity.getLayoutInflater(), parent, false);
        final View view = binding.getRoot();
        view.setTag(binding);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ItemUploadBinding binding = (ItemUploadBinding) view.getTag();
        final long id = cursor.getLong(cursor.getColumnIndex(Constants.CURSOR_ADAPTER_ID));
        final long remoteId = cursor.getLong(cursor.getColumnIndex(Constants.UPLOADS.REMOTE_ID));
        final String uploadTitle = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.TITLE));
        final String stationTitle = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.JOIN_STATION_TITLE));
        final String problemType = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.PROBLEM_TYPE));
        final String uploadStateStr = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.UPLOAD_STATE));
        final String comment = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.COMMENT));
        final String rejectReason = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.REJECTED_REASON));

        final UploadState uploadState = UploadState.valueOf(uploadStateStr);
        final String textState = id + (remoteId > 0 ? "/" + remoteId : "" ) + ": " + context.getString(uploadState.getTextId());
        binding.txtState.setText(textState);
        binding.txtState.setTextColor(context.getResources().getColor(uploadState.getColorId()));

        if (problemType != null) {
            binding.uploadType.setImageDrawable(context.getDrawable(R.drawable.ic_bullhorn_48px));
            binding.txtComment.setText(context.getText(ProblemType.valueOf(problemType).getMessageId()) + ": " + comment);
        } else {
            binding.uploadType.setImageDrawable(context.getDrawable(R.drawable.ic_photo_red_48px));
            binding.txtComment.setText(comment);
        }
        binding.txtComment.setVisibility(comment == null ? View.GONE : View.VISIBLE);

        binding.txtStationName.setText(uploadTitle != null ? uploadTitle : stationTitle);
        binding.txtRejectReason.setText(rejectReason);
        binding.txtRejectReason.setVisibility(rejectReason == null ? View.GONE : View.VISIBLE);
    }

}
