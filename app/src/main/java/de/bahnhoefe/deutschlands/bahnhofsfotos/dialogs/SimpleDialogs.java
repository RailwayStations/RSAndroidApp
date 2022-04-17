package de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.PromptBinding;

public class SimpleDialogs {

    private SimpleDialogs(){}

    public static void confirm(final Context context, final int message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setNeutralButton(R.string.button_ok_text, null).create().show();
    }

    public static void confirm(final Context context, final CharSequence message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setNeutralButton(R.string.button_ok_text, null).create().show();
    }

    public static void confirm(final Context context, final int message, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok_text, listener)
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show();
    }

    public static void confirm(final Context context, final String message, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok_text, listener)
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show();
    }

    public static void simpleSelect(final Context context, final CharSequence message, final CharSequence[] items, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(message)
            .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, 0, items), listener)
            .create().show();
    }

    public static void prompt(final Context context, final int message, final int inputType, final int hint, final String text, final PromptListener listener) {
        final var binding = PromptBinding.inflate(LayoutInflater.from(context));
        if (text != null) {
            binding.etPrompt.setText(text);
        }
        binding.etPrompt.setHint(hint);
        binding.etPrompt.setInputType(inputType);

        final var alertDialog = new androidx.appcompat.app.AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setTitle(message)
                .setView(binding.getRoot())
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id1) -> dialog.cancel())
                .create();

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
