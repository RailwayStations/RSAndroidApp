package de.bahnhoefe.deutschlands.bahnhofsfotos.db

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import androidx.appcompat.content.res.AppCompatResources
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ItemUploadBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.ProblemType
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UploadState
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants.UPLOADS
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.FileUtils

class OutboxAdapter(private val activity: Activity, uploadCursor: Cursor?) : CursorAdapter(
    activity, uploadCursor, 0
) {
    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val binding = ItemUploadBinding.inflate(
            activity.layoutInflater, parent, false
        )
        val view = binding.root
        view.tag = binding
        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val binding = view.tag as ItemUploadBinding
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.CURSOR_ADAPTER_ID))
        val remoteId = cursor.getLong(cursor.getColumnIndexOrThrow(UPLOADS.REMOTE_ID))
        val stationId = cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.STATION_ID))
        val uploadTitle = cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.TITLE))
        val stationTitle =
            cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.JOIN_STATION_TITLE))
        val problemType = cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.PROBLEM_TYPE))
        val uploadStateStr = cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.UPLOAD_STATE))
        val comment = cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.COMMENT))
        val rejectReason = cursor.getString(cursor.getColumnIndexOrThrow(UPLOADS.REJECTED_REASON))
        val uploadState = UploadState.valueOf(uploadStateStr)
        val textState =
            id.toString() + (if (remoteId > 0) "/$remoteId" else "") + ": " + context.getString(
                uploadState.textId
            )
        binding.txtStationKey.text = textState
        binding.txtStationKey.setTextColor(context.resources.getColor(uploadState.colorId, null))
        binding.uploadPhoto.setImageBitmap(null)
        binding.uploadPhoto.visibility = View.GONE
        if (problemType != null) {
            binding.uploadType.setImageDrawable(
                AppCompatResources.getDrawable(
                    context,
                    R.drawable.ic_bullhorn_red_48px
                )
            )
            binding.txtComment.text = String.format(
                "%s: %s",
                context.getText(ProblemType.valueOf(problemType).messageId),
                comment
            )
        } else {
            binding.uploadType.setImageDrawable(
                AppCompatResources.getDrawable(
                    context,
                    if (stationId == null) R.drawable.ic_station_red_24px else R.drawable.ic_photo_red_48px
                )
            )
            binding.txtComment.text = comment
            val file = FileUtils.getStoredMediaFile(context, id)
            if (file != null) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                binding.uploadPhoto.setImageBitmap(bitmap)
                binding.uploadPhoto.visibility = View.VISIBLE
            }
        }
        binding.txtComment.visibility =
            if (comment == null) View.GONE else View.VISIBLE
        binding.txtStationName.text = uploadTitle ?: stationTitle
        binding.txtRejectReason.text = rejectReason
        binding.txtRejectReason.visibility = if (rejectReason == null) View.GONE else View.VISIBLE
    }
}