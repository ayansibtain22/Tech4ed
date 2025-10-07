package com.lms.ayan.common

import android.text.TextUtils
import android.util.Patterns


fun isValidEmail(email: String): Boolean {
    return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}