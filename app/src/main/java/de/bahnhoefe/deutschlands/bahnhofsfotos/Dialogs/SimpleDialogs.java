package de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.widget.ArrayAdapter;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class SimpleDialogs {

    public void confirm(Context context, int message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setNeutralButton(R.string.button_ok_text, null).create().show();
    }

    public void confirm(Context context, CharSequence message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setNeutralButton(R.string.button_ok_text, null).create().show();
    }

    public void confirm(Context context, int message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(R.string.button_ok_text, listener)
                .setNegativeButton(R.string.button_cancel_text, null)
                .create().show();
    }

    public void confirm(Context context, String message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(R.string.button_ok_text, listener)
                .setNegativeButton(R.string.button_cancel_text, null)
                .create().show();
    }

    public void select(Context context, CharSequence message, CharSequence[] items, int checkedItem, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setSingleChoiceItems(items, checkedItem, null)
                .setPositiveButton(message, listener)
                .create().show();
    }

    public void simpleSelect(Context context, CharSequence message, CharSequence[] items, int checkedItem, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(message)
                .setAdapter(new ArrayAdapter(context, android.R.layout.simple_list_item_1, 0, items), listener)
                .create().show();
    }

}
