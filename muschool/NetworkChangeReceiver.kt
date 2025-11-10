package com.example.muschool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

class NetworkChangeReceiver : BroadcastReceiver() {

    private var wasConnected = true

    override fun onReceive(context: Context, intent: Intent?) {
        val isConnected = NetworkUtils.isInternetAvailable(context)

        if (!isConnected && wasConnected) {
            Toast.makeText(context, "🚫 No Internet Connection", Toast.LENGTH_SHORT).show()
            wasConnected = false
        } else if (isConnected && !wasConnected) {
            Toast.makeText(context, "✅ Internet Connected", Toast.LENGTH_SHORT).show()
            wasConnected = true
        }
    }
}
