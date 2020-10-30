package com.neel.desai.mylocation.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.neel.desai.mylocation.util.Constants
import com.neel.desai.mylocation.util.Utility

public abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w = window // in Activity's onCreate() for instance
            w.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            w.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    interface OnBrodcastListener {
        fun onReceive(intent: Intent?, Type: String?)
    }

    private var mOnBrodcastListener: OnBrodcastListener? = null


    open public fun setOnBrodcastListener(listener: OnBrodcastListener?) {
        mOnBrodcastListener = listener
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(brodcastLocationChnage)
        unregisterReceiver(BrodcastPunchInterval)
        unregisterReceiver(ServiceStop)
        unregisterReceiver(RestReminder)
        unregisterReceiver(OverSpeed)
        unregisterReceiver(IdleReminder)
    }


    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        if (!(getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled("gps")) {
            Utility.turnGPSOn(this)
        }
        registerReceiver(brodcastLocationChnage, IntentFilter(Constants.LOCATION_CHNGAE))
        registerReceiver(BrodcastPunchInterval, IntentFilter(Constants.INTERVAL_PUNCH))
        registerReceiver(ServiceStop, IntentFilter(Constants.SERVICE_STOP))
        registerReceiver(OverSpeed, IntentFilter(Constants.OVER_SPEED))
        registerReceiver(IdleReminder, IntentFilter(Constants.IDLE_REMINDER_BRODCAST))
        registerReceiver(RestReminder, IntentFilter(Constants.REST_REMINDER_BRODCAST))
    }


    private val BrodcastPunchInterval: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mOnBrodcastListener != null) mOnBrodcastListener!!.onReceive(
                intent,
                "PunchInterval"
            ) // event result object :)
        }
    }


    private val brodcastLocationChnage: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mOnBrodcastListener != null) mOnBrodcastListener!!.onReceive(
                intent,
                "LocationChnage"
            ) // event result object :)
        }
    }


    private val ServiceStop: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mOnBrodcastListener != null) mOnBrodcastListener!!.onReceive(
                intent,
                "ServiceStop"
            ) // event result object :)
        }
    }


    private val RestReminder: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mOnBrodcastListener != null) mOnBrodcastListener!!.onReceive(
                intent,
                "RestReminder"
            ) // event result object :)
        }
    }


    private val OverSpeed: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mOnBrodcastListener != null) mOnBrodcastListener!!.onReceive(
                intent,
                "OverSpeed"
            ) // event result object :)
        }
    }


    private val IdleReminder: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mOnBrodcastListener != null) mOnBrodcastListener!!.onReceive(
                intent,
                "IdleReminder"
            ) // event result object :)
        }
    }

}