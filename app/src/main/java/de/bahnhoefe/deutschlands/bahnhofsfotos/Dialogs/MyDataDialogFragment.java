package de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.MyDataActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

import static android.content.DialogInterface.*;

/**
 * Created by android_oma on 21.01.17.
 */

public class MyDataDialogFragment extends DialogFragment {
    private final String TAG = getClass().getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

        builder
        .setIcon(R.mipmap.ic_launcher)
        .setTitle(R.string.dialog_title)
        .setMessage(R.string.dialog_message)
        .setPositiveButton("OK", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent myDataIntent = new Intent(getActivity(),MyDataActivity.class);
                startActivity(myDataIntent);
                dismiss();
            }
        })
        .setNegativeButton("ABBRECHEN", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                startActivity(mainIntent);
                dismiss();
            }
        });
        return builder.create();
    }

}
