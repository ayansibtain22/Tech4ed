package com.lms.ayan.util

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class GlideImageGetter(
    private val textView: TextView
) : Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable {
        val urlDrawable = URLDrawable()

        Glide.with(textView.context)
            .asBitmap()
            .load(source)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    val drawable = BitmapDrawable(textView.resources, resource)
                    drawable.setBounds(0, 0, resource.width, resource.height)

                    urlDrawable.setDrawable(drawable)

                    // Refresh TextView so the image is visible
                    textView.text = textView.text
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        return urlDrawable
    }
}
