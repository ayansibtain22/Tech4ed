package com.lms.ayan.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

fun replaceFragment(
    fragmentManager: FragmentManager,
    fragment: Fragment,
    container: Int,
    addToBackStack: Boolean = false,
    transactionName: String? = null
) {
    val transaction = fragmentManager.beginTransaction()
    transaction.replace(
        container,
        fragment
    )
    if (addToBackStack) {
        transaction.addToBackStack(transactionName)
    }
    transaction.commit()
}