package de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

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

    public void prompt(Context context, int message, int inputType, int hint, String text, PromptListener listener) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.prompt, null);
        final EditText etPrompt = dialogView.findViewById(R.id.et_prompt);
        if (text != null) {
            etPrompt.setText(text);
        }
        etPrompt.setHint(hint);
        etPrompt.setInputType(inputType);

        builder.setTitle(message)
                .setView(dialogView)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id1) -> dialog.cancel());

        final androidx.appcompat.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            alertDialog.dismiss();
            listener.prompt(etPrompt.getText().toString());
        });
    }

    public interface PromptListener {
        void prompt(String prompt);
    }

}
