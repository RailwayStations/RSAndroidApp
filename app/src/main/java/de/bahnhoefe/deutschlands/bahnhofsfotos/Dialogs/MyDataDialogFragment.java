package de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import static android.content.DialogInterface.OnClickListener;
import de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.MyDataActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class MyDataDialogFragment extends DialogFragment {
    private final String TAG = getClass().getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

        builder
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.dialog_title)
                .setMessage(R.string.dialog_message)
                .setPositiveButton(R.string.button_ok_text, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent myDataIntent = new Intent(getActivity(), MyDataActivity.class);
                        startActivity(myDataIntent);
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.button_cancel_text, new OnClickListener() {
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
