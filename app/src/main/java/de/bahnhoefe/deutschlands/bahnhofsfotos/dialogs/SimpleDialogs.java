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

    private SimpleDialogs() {
    }

    public static void confirmOk(Context context, int message) {
        confirmOk(context, message, null);
    }

    public static void confirmOk(Context context, int message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setNeutralButton(R.string.button_ok_text, listener).create().show();
    }

    public static void confirmOk(Context context, CharSequence message) {
        confirmOk(context, message, null);
    }

    public static void confirmOk(Context context, CharSequence message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setNeutralButton(R.string.button_ok_text, listener).create().show();
    }

    public static void confirmOkCancel(Context context, int message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(R.string.button_ok_text, listener)
                .setNegativeButton(R.string.button_cancel_text, null)
                .create().show();
    }

    public static void confirmOkCancel(Context context, String message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(R.string.button_ok_text, listener)
                .setNegativeButton(R.string.button_cancel_text, null)
                .create().show();
    }

    public static void simpleSelect(Context context, CharSequence message, CharSequence[] items, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(message)
                .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, 0, items), listener)
                .create().show();
    }

    public static void prompt(Context context, int message, int inputType, int hint, String text, PromptListener listener) {
        var binding = PromptBinding.inflate(LayoutInflater.from(context));
        if (text != null) {
            binding.etPrompt.setText(text);
        }
        binding.etPrompt.setHint(hint);
        binding.etPrompt.setInputType(inputType);

        var alertDialog = new androidx.appcompat.app.AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom))
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
