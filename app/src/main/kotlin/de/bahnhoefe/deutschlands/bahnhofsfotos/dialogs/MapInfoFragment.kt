package de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import de.bahnhoefe.deutschlands.bahnhofsfotos.R

class MapInfoFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val textView = TextView(context)
        textView.textSize = 18f
        textView.setPadding(50, 50, 50, 50)
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.setText(R.string.map_info_text)
        textView.setLinkTextColor(Color.parseColor("#c71c4d"))
        return AlertDialog.Builder(ContextThemeWrapper(activity, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.map_info_title)
            .setPositiveButton(R.string.app_info_ok) { _: DialogInterface?, _: Int -> }
            .setView(textView)
            .create()
    }
}