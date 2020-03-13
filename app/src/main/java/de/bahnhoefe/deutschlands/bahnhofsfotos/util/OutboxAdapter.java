package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState;

public class OutboxAdapter extends CursorAdapter {

    private final Activity activity;

    private static LayoutInflater inflater = null;

    public OutboxAdapter(final Activity activity, final Cursor uploadCursor) {
        super(activity, uploadCursor, 0);
        this.activity = activity;
        inflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final View view = inflater.inflate(R.layout.item_upload, parent, false);
        final ViewHolder holder = new ViewHolder();
        holder.txtState = view.findViewById(R.id.txtState);
        holder.txtStationName = view.findViewById(R.id.txtStationName);
        holder.uploadType = view.findViewById(R.id.uploadType);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final long id = cursor.getLong(cursor.getColumnIndex("_id"));
        final long remoteId = cursor.getLong(cursor.getColumnIndex(Constants.UPLOADS.REMOTE_ID));
        final String uploadTitle = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.TITLE));
        final String stationTitle = cursor.getString(cursor.getColumnIndex("stationTitle"));
        final String problemType = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.PROBLEM_TYPE));
        final String uploadState = cursor.getString(cursor.getColumnIndex(Constants.UPLOADS.UPLOAD_STATE));

        final String textState = id + (remoteId > 0 ? "/" + remoteId : "" ) + ": " + context.getString(UploadState.valueOf(uploadState).getTextId());
        holder.txtState.setText(textState);

        if (problemType != null) {
            holder.uploadType.setImageDrawable(context.getDrawable(R.drawable.ic_bullhorn_48px));
        } else {
            holder.uploadType.setImageDrawable(context.getDrawable(R.drawable.ic_photo_red_48px));
        }

        holder.txtStationName.setText(uploadTitle != null ? uploadTitle : stationTitle);
    }

    static class ViewHolder {
        TextView txtState;
        TextView txtStationName;
        ImageView uploadType;
    }

}
