package com.lms.ayan.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Ensure we are checking the active network
    val activeNetwork = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

    // Check if the network has internet capabilities
    return when {
        networkCapabilities == null -> false
        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> {
            // Check if the device is connected to Wi-Fi or Mobile Data
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    context.logD("INTERNET") { "Connected to Wi-Fi" }
                    return true
                }

                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    context.logD("INTERNET")  { "Connected to Mobile Data" }
                    return true
                }

                else -> {
                    context.logD("INTERNET")  { "Internet is not connected" }
                    return false
                }
            }
        }

        else -> {
            context.logD("INTERNET")  { "Internet is not connected" }
            false
        }
    }
}