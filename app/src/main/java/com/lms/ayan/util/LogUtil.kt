package com.lms.ayan.util

import android.util.Log
import com.lms.ayan.common.Common.LOGGING

fun Any.logD(tag:String, message: () -> String) {
    if (LOGGING) {
        Log.d(tag, message())
    }
}
fun Any.logE(tag:String,message: () -> String) {
    if (LOGGING) {
        Log.e(tag, message())
    }
}


fun Any.logI(tag:String,message: () -> String) {
    if (LOGGING) {
        Log.i(tag, message())
    }
}
