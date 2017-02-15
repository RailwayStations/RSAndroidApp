package de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

import static com.google.android.gms.analytics.internal.zzy.a;
import static com.google.android.gms.analytics.internal.zzy.d;
import static com.google.android.gms.analytics.internal.zzy.i;
import static com.google.android.gms.analytics.internal.zzy.s;

public class AppInfoFragment extends DialogFragment {


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        TextView textView = new TextView(getContext());
        textView.setTextSize((float) 18);
        textView.setPadding(50, 50, 50, 50);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(R.string.app_info_text);
        textView.setLinkTextColor(Color.parseColor("#c71c4d"));

        builder.setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_info_title)
                //.setMessage(R.string.app_info_text)
                .setPositiveButton(R.string.app_info_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // it is okay, that the dialog closes if clicked the ok-button
                        // no more action necessary
                    }
                });


        builder.setView(textView);

        // Creates the AlertDialog object and return it
        AlertDialog appInfoDialog = builder.create();
        return appInfoDialog;
    }


}
