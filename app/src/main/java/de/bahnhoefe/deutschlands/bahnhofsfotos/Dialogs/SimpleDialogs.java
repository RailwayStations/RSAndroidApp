package de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.PromptBinding;

public class SimpleDialogs {

    public void confirm(final Context context, final int message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setNeutralButton(R.string.button_ok_text, null).create().show();
    }

    public void confirm(final Context context, final CharSequence message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setNeutralButton(R.string.button_ok_text, null).create().show();
    }

    public void confirm(final Context context, final int message, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok_text, listener)
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show();
    }

    public void confirm(final Context context, final String message, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok_text, listener)
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show();
    }

    public void select(final Context context, final CharSequence message, final CharSequence[] items, final int checkedItem, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setSingleChoiceItems(items, checkedItem, null)
            .setPositiveButton(message, listener)
            .create().show();
    }

    public void simpleSelect(final Context context, final CharSequence message, final CharSequence[] items, final int checkedItem, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(message)
            .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, 0, items), listener)
            .create().show();
    }

    public void prompt(final Context context, final int message, final int inputType, final int hint, final String text, final PromptListener listener) {
        final androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
        final PromptBinding binding = PromptBinding.inflate(LayoutInflater.from(context));
        final View dialogView = binding.getRoot();
        if (text != null) {
            binding.etPrompt.setText(text);
        }
        binding.etPrompt.setHint(hint);
        binding.etPrompt.setInputType(inputType);

        builder.setTitle(message)
            .setView(dialogView)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, (dialog, id1) -> dialog.cancel());

        final androidx.appcompat.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            alertDialog.dismiss();
            listener.prompt(binding.etPrompt.getText().toString());
        });
    }

    public interface PromptListener {
        void prompt(String prompt);
    }

}
