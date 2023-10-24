package de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.PromptBinding

object SimpleDialogs {
    fun confirmOk(
        context: Context,
        message: Int,
        listener: DialogInterface.OnClickListener? = null
    ) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setNeutralButton(R.string.button_ok_text, listener).create().show()
    }

    fun confirmOk(
        context: Context,
        message: CharSequence,
        listener: DialogInterface.OnClickListener? = null
    ) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setNeutralButton(R.string.button_ok_text, listener).create().show()
    }

    fun confirmOkCancel(
        context: Context,
        message: Int,
        listener: DialogInterface.OnClickListener?
    ) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok_text, listener)
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show()
    }

    fun confirmOkCancel(
        context: Context,
        message: String,
        listener: DialogInterface.OnClickListener?
    ) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok_text, listener)
            .setNegativeButton(R.string.button_cancel_text, null)
            .create().show()
    }

    fun simpleSelect(
        context: Context,
        message: CharSequence,
        items: Array<String>,
        listener: DialogInterface.OnClickListener?
    ) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(message)
            .setAdapter(
                ArrayAdapter(context, android.R.layout.simple_list_item_1, 0, items),
                listener
            )
            .create().show()
    }

    fun prompt(
        context: Context,
        message: Int,
        inputType: Int,
        hint: Int,
        text: String?,
        listener: PromptListener
    ) {
        val binding = PromptBinding.inflate(LayoutInflater.from(context))
        text?.let {
            binding.etPrompt.setText(it)
        }
        binding.etPrompt.setHint(hint)
        binding.etPrompt.inputType = inputType
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(
            ContextThemeWrapper(
                context,
                R.style.AlertDialogCustom
            )
        )
            .setTitle(message)
            .setView(binding.root)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
            .create()
        alertDialog.show()
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener {
                alertDialog.dismiss()
                listener.prompt(binding.etPrompt.text.toString())
            }
    }

    fun interface PromptListener {
        fun prompt(prompt: String)
    }
}