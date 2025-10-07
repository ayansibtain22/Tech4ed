package com.lms.ayan.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.lms.ayan.databinding.DialogGeneralMessageBinding
import com.lms.ayan.databinding.DialogLoadingBinding

object DialogUtil {
    fun initLoadingDialog(context: Context): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val dialogBinding =
            DialogLoadingBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        dialog.setContentView(dialogBinding.root)
        val width = (context.resources.displayMetrics.widthPixels * 0.70).toInt()
        //val height = (context.resources.displayMetrics.heightPixels * 0.50).toInt()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }

    fun showMessageDialog(
        context: Context,
        title: String,
        message: String,
        buttonText: String,
        onYesBtnClicked: () -> Unit,
        onNoBtnClicked: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val dialogBinding =
            DialogGeneralMessageBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        dialog.setContentView(dialogBinding.root)
        val width = (context.resources.displayMetrics.widthPixels * 0.70).toInt()
        //val height = (context.resources.displayMetrics.heightPixels * 0.50).toInt()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogBinding.titleTV.text = title
        dialogBinding.loadingMessage.text = message
        dialogBinding.button.text = buttonText
        dialogBinding.button.setOnClickListener {
            onYesBtnClicked()
            dialog.dismiss()
        }
        dialogBinding.noButton.setOnClickListener {
            onNoBtnClicked()
            dialog.dismiss()
        }
        dialog.show()
    }
}