package com.neel.desai.mylocation.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import androidx.legacy.content.WakefulBroadcastReceiver
import com.neel.desai.mylocation.activity.HomeActivity
import com.neel.desai.mylocation.util.Utility


class LocationProviderChangeReceiver : WakefulBroadcastReceiver() {
    private var context: Context? = null
    private val locationMode = 0
    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        Log.i("onReceive", intent.action)
        if (intent.action == "android.location.GPS_ENABLED_CHANGE") {
            if (isLocationServciesAvailable(context)) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(10020)
            } else {
                val i = Intent(context, HomeActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                Utility.showNotification(
                    context,
                    "Alert",
                    "GPS is turn off pleas Start GPS",
                    i,
                    10020,
                    true,
                    false
                )
            }
        }
    }

    fun isLocationServciesAvailable(context: Context): Boolean {
        val lm =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    companion object {
        private const val GPSEnable = false
    }
}