package de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import de.bahnhoefe.deutschlands.bahnhofsfotos.DetailsActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.MainActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.MyDataActivity;
import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

import static android.content.DialogInterface.*;

/**
 * Created by android_oma on 21.01.17.
 */

public class MyDataDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_mydata_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(view).setPositiveButton("OK", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent myDataIntent = new Intent(getActivity(),MyDataActivity.class);
                startActivity(myDataIntent);
            }
        })
        .setNegativeButton("ABBRECHECN", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //MyDataDialogFragment.this.getDialog().cancel();
                Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                startActivity(mainIntent);
            }
        });
        return builder.create();
    }
}
