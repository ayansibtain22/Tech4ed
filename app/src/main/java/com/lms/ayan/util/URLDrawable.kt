package com.lms.ayan.util

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

class URLDrawable : BitmapDrawable() {
    private var drawable: Drawable? = null

    override fun draw(canvas: Canvas) {
        drawable?.draw(canvas)
    }

    fun setDrawable(drawable: Drawable) {
        this.drawable = drawable
        setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.setBounds(bounds)
    }
}
