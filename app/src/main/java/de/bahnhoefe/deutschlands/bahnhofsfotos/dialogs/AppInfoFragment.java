package de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.ContextThemeWrapper;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.bahnhoefe.deutschlands.bahnhofsfotos.BuildConfig;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class AppInfoFragment extends DialogFragment {

    @Override
    @NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final var textView = new TextView(getContext());
        textView.setLinksClickable(true);
        textView.setTextSize((float) 18);
        textView.setPadding(50, 50, 50, 50);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(Html.fromHtml(getString(R.string.app_info_text, BuildConfig.VERSION_NAME),Html.FROM_HTML_MODE_COMPACT));
        textView.setLinkTextColor(Color.parseColor("#c71c4d"));

        return new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_info_title)
                .setPositiveButton(R.string.app_info_ok, (dialog, id) -> {
                    // noop, just close dialog
                })
                .setView(textView)
                .create();
    }


}