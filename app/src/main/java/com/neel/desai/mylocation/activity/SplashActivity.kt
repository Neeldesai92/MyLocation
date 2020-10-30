package com.neel.desai.mylocation.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler


import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.neel.desai.mylocation.R


class SplashActivity : AppCompatActivity() {

    private val _splashTime: Long = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.window.statusBarColor = ContextCompat.getColor(
                this@SplashActivity,
                android.R.color.transparent
            )
        }
        setContentView(R.layout.activity_splesh)
        validatePermissions()
    }


    override fun onResume() {

        super.onResume()
    }


    /**
     * call next activity thread
     */
    private fun callNextScreen() {

        Handler().postDelayed({
            // This method will be executed once the timer is over
            // Start your app main activity

            goNextScreen()

            // close this activity
            finish()
        }, _splashTime)
    }

    private fun goNextScreen() {
        // Provide one time selection.
        val obj: Intent
        obj = Intent(this@SplashActivity, LoginActivity::class.java)
        startActivity(obj)
        finish()
    }

    private fun validatePermissions() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    callNextScreen()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    validatePermissions()
                }
            })
            .check()
    }
}