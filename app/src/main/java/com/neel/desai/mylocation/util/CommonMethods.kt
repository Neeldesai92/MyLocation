package com.neel.desai.mylocation.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import androidx.annotation.Nullable
import androidx.lifecycle.LiveData
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class CommonMethods {
    companion object {
        fun dpToPx(dp: Int): Int {
            return (dp * Resources.getSystem().displayMetrics.density).toInt()
        }


        fun isNetworkConnected(activtiy: Context): Boolean {
            // TODO Auto-generated method s
            val cmss = activtiy
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val niss = cmss.activeNetworkInfo
            return niss != null
        }


        @SuppressLint("SimpleDateFormat")
        fun getCurrentDateTime(): String {
            // TODO Auto-generated method stub
            val dateFormat: DateFormat =
                SimpleDateFormat("dd MMM yyyy,HH:mm", Locale.US)
            val date = Date()
            println(dateFormat.format(date))
            return dateFormat.format(date)
        }



    }





}