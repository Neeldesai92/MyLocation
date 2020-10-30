package com.neel.desai.mylocation.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.jaygoo.widget.OnRangeChangedListener
import com.jaygoo.widget.RangeSeekBar
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.database.DatabaseClient
import com.neel.desai.mylocation.databinding.ActivityTrackingBinding
import com.neel.desai.mylocation.fragment.IdleDialogFragment
import com.neel.desai.mylocation.fragment.RestDialogFragment
import com.neel.desai.mylocation.fragment.SosDialogFragment
import com.neel.desai.mylocation.fragment.SpeedDialogFragment
import com.neel.desai.mylocation.model.MyLocationData
import com.neel.desai.mylocation.service.LocationService
import com.neel.desai.mylocation.util.CommonMethods
import com.neel.desai.mylocation.util.Constants
import com.neel.desai.mylocation.util.SharedPrefsUtils
import com.neel.desai.mylocation.util.Utility
import com.neel.desai.mylocation.viewmodel.TrackingViewModel
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TrackingActivity : BaseActivity(), OnMapReadyCallback, BaseActivity.OnBrodcastListener {

    lateinit var trackingViewModel: TrackingViewModel
    lateinit var TrackingBinding: ActivityTrackingBinding

    var pervDialog: DialogFragment? = null
    var timer: Timer? = null
    var locationData: MyLocationData? = null

    private var mMap: GoogleMap? = null
    var currentloction: LatLng? = null
    var bitemap_Marker: Bitmap? = null
    var bitmapdescriptor: BitmapDescriptor? = null

    lateinit var mMarker: Marker

    var zoomlevel = 15f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnBrodcastListener(this)
        trackingViewModel = ViewModelProviders.of(this).get(TrackingViewModel::class.java)
        TrackingBinding = DataBindingUtil.setContentView(this, R.layout.activity_tracking);
        TrackingBinding.data = trackingViewModel
        TrackingBinding.lifecycleOwner = this

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_tracking) as? SupportMapFragment
        mapFragment!!.getMapAsync(this)

        setRepeatingAsyncTask()

        // toolbar
        TrackingBinding.toolbar.title = "Tracking"
        setSupportActionBar(TrackingBinding.toolbar)


        // add back arrow to toolbar
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }

        bitemap_Marker = BitmapFactory.decodeResource(resources, R.drawable.ic_truck)
        bitmapdescriptor = BitmapDescriptorFactory.fromBitmap(bitemap_Marker)

        TrackingBinding.seekBar1.setOnRangeChangedListener(object : OnRangeChangedListener {
            override fun onRangeChanged(view: RangeSeekBar, leftValue: Float, rightValue: Float, isFromUser: Boolean) {
                SetZoomLevel(leftValue)
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(leftValue), 100, null)
            }

            override fun onStartTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {}
            override fun onStopTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {}
        })

        dashbordDynamicSize()

        TrackingBinding.ivSos.setOnClickListener { v: View? ->


            showDialog(SosDialogFragment())
        }
    }


    fun dashbordDynamicSize() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val yInches = metrics.heightPixels / metrics.ydpi
        val xInches = metrics.widthPixels / metrics.xdpi
        val temp = Math.sqrt(xInches * xInches + yInches * yInches.toDouble())
        val diagonalInches = Math.round(temp * 2) / 2.0f.toDouble()
        if (diagonalInches >= 4 && diagonalInches < 4.5) {
            // icon size 60

            /*RelativeLayout.LayoutParams paramsSos = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            paramsSos.setMargins(0, 0, CommonMethods.dpToPx(10), CommonMethods.dpToPx(5));
            fl_Sos.setLayoutParams(paramsSos);*/
            val lp = TrackingBinding.flSos.getLayoutParams() as RelativeLayout.LayoutParams
            lp.setMargins(0, 0, CommonMethods.dpToPx(10), CommonMethods.dpToPx(5))
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            TrackingBinding.flSos.setLayoutParams(lp)
            val lp1 = TrackingBinding.ivCurrent.getLayoutParams() as RelativeLayout.LayoutParams
            lp1.setMargins(0, 0, CommonMethods.dpToPx(0), CommonMethods.dpToPx(0))
            TrackingBinding.ivCurrent.setLayoutParams(lp1)
            val lp2 = TrackingBinding.flSos.getLayoutParams() as RelativeLayout.LayoutParams
            lp2.addRule(RelativeLayout.BELOW, R.id.iv_current)
            TrackingBinding.flSos.setLayoutParams(lp2)
        } else if (diagonalInches >= 4.5 && diagonalInches < 5) {
            // icon size 70
        } else if (diagonalInches >= 5 && diagonalInches < 5.5) {
            // icon size 85
        } else if (diagonalInches >= 5.5 && diagonalInches < 6) {
// icon size 75
        } else if (diagonalInches >= 6 && diagonalInches < 6.5) {
            // icon size 75
        } else {
            // icon size 75
        }
    }

    override fun onWindowFocusChanged(hasFocus: kotlin.Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private fun showSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }


    fun showDialog(msg: DialogFragment) {
        if (pervDialog != null) {
            pervDialog!!.dismiss()
        }
        if (pervDialog != null && pervDialog!!.fragmentManager != null && (pervDialog is RestDialogFragment || pervDialog is SosDialogFragment)) {
            val fragment = pervDialog!!.fragmentManager!!.findFragmentById(R.id.map_rest)
            val fragment1 = pervDialog!!.fragmentManager!!.findFragmentById(R.id.map_sos)
            if (fragment1 != null) {
                val ft = Objects.requireNonNull(this).supportFragmentManager.beginTransaction()
                ft.remove(fragment1)
                ft.commit()
            }
            if (fragment != null) {
                val ft = Objects.requireNonNull(this).supportFragmentManager.beginTransaction()
                ft.remove(fragment)
                ft.commit()
            }
        }
        pervDialog = msg


        //closeYourDialogFragment();
        //  MapDialogFragment dialogFragment = new MapDialogFragment();
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(Constants.DIALOG_FRGAMET_KEY)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        msg.show(supportFragmentManager, Constants.DIALOG_FRGAMET_KEY)
        TrackingBinding.flSos.setEnabled(true)
    }


    private fun setRepeatingAsyncTask() {
        val handler = Handler()
        if (timer != null) {
            timer!!.cancel()
        }
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    try {
                        SetData()
                    } catch (e: Exception) {
                        val stringBuilder = StringBuilder()
                        stringBuilder.append(e.message)
                        stringBuilder.append("")
                        Log.i("Error", stringBuilder.toString())
                    }
                }
            }
        }, 0, 30000)
    }

    fun cancelTimer() {
        Log.i("cancel call", "true")
        if (timer != null) {
            timer!!.cancel()
        }
    }

    fun SetData() {
        val TotalDriveTime: List<MyLocationData> = DatabaseClient.getInstance(applicationContext).getAppDatabase()
            .taskDao().getDataTravel("1")!!
        if (TotalDriveTime != null && TotalDriveTime.size > 0) {
            var timeoverspeed: Long = 0
            val formatter: DateFormat = SimpleDateFormat("dd MMM yyyy,HH:mm")
            val size = TotalDriveTime.size
            var i = 0
            while (i < size) {
                var temptime: Long = 0
                var j = i + 1
                while (i < size) {
                    if (j < TotalDriveTime.size) {
                        try {
                            temptime = Utility.GetDate(formatter.parse(TotalDriveTime[j].time) as Date, formatter.parse(TotalDriveTime[i].time) as Date)
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }
                    } else {
                        i = j - 1
                        break
                    }
                    j++
                }
                timeoverspeed += temptime
                i++
            }
            Log.i("timeoverspeed1", TimeUnit.SECONDS.toMillis(timeoverspeed).toString() + "")
            TrackingBinding.tvDrivevalue.setText(Utility.GetTimewithoutlable(TimeUnit.SECONDS.toMillis(timeoverspeed)))
        }
        val dataIdle: List<MyLocationData> = DatabaseClient.getInstance(applicationContext).getAppDatabase()
            .taskDao().getSearch(Constants.IDLE_REMINDER)!!
        if (dataIdle != null) {
            TrackingBinding.tvIdlevalue.setText(Utility.GetTimewithoutlable((dataIdle.size * Constants.IDLE_TIME).toLong()))
        }
    }


    override fun onDestroy() {
        cancelTimer()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): kotlin.Boolean {
        // handle arrow click here
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish() // close this activity and return to preview activity (if there is any)
        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }


    //here we  Calculate Distacne for remaining Brake-in Travel Distance and Last Brake Distance
    fun CalculateDistance() {
        val precision = DecimalFormat("0.00")
        if (!SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.INTENT_IS_REST_START_KEY, false) && SharedPrefsUtils.getDoublePreference(applicationContext, Constants.INTENT_DISTANCE_REMAING_KEY, 0.0) > 0) {
            TrackingBinding.tvRemainvalue.setText(precision.format((Constants.REST_AFTER_IN_KM - SharedPrefsUtils.getDoublePreference(applicationContext, Constants.INTENT_DISTANCE_REMAING_KEY, 0.0)) / 1000))
        } else {
            TrackingBinding.tvRemainvalue.setText("0")
            SharedPrefsUtils.setDoublePreference(applicationContext, Constants.INTENT_DISTANCE_REMAING_KEY, 0.0)
        }
        if (SharedPrefsUtils.getDoublePreference(applicationContext, Constants.INTENT_DISTANCE_DIFFT_KEY, 0.0).equals(0)) {
            val TotaleDistance = DatabaseClient.getInstance(applicationContext).getAppDatabase()
                .taskDao().getDataTravel("1")
            if (TotaleDistance != null && TotaleDistance.size > 0) {
                var distance = 0.0
                val size = TotaleDistance.size
                for (i in 0 until size) {
                    if (i + 1 < size) {
                        distance += Utility.getDistancBetweenTwoPoints(TotaleDistance[i].latitude.toDouble(), TotaleDistance[i].longitude.toDouble(), TotaleDistance[i + 1].latitude.toDouble(), TotaleDistance[i + 1].longitude.toDouble())
                    }
                }
                TrackingBinding.tvCompletevalue.setText(precision.format(distance / 1000))
                TrackingBinding.tvCompletemsg.setText("TOTAL \n DRIVEN")
            }
        } else {
            TrackingBinding.tvCompletevalue.setText(precision.format(SharedPrefsUtils.getDoublePreference(applicationContext, Constants.INTENT_DISTANCE_DIFFT_KEY, 0.0) / 1000))
            TrackingBinding.tvCompletemsg.setText("TOTAL \n DRIVEN")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        zoomlevel = 15f
        TrackingBinding.seekBar1.setValue(zoomlevel)

        //   seekBar1.setMax((int) mMap.getMaxZoomLevel());
        mMap!!.isTrafficEnabled = true
        mMap!!.isIndoorEnabled = true
        mMap!!.isBuildingsEnabled = true
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mMap!!.isMyLocationEnabled = false
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        mMap!!.uiSettings.isCompassEnabled = true

        mMap!!.uiSettings.isMapToolbarEnabled = false
        mMap!!.mapType = 1
        mMap!!.uiSettings.setAllGesturesEnabled(true)
        mMap!!.uiSettings.isZoomGesturesEnabled = true
        val location = Utility.getLastKnownLoaction(true, applicationContext)
        if (location != null) {
            currentloction = LatLng(location.latitude, location.longitude)
            mMap!!.clear()

            val markerOptions = MarkerOptions()
            markerOptions.position(currentloction!!)

            markerOptions.icon(getBitmapDescriptorFromVector(applicationContext, R.drawable.ic_truck));
            mMarker = mMap!!.addMarker(markerOptions)


            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(currentloction))
            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
        }
    }


    private fun animateMarkerNew(destination: Location, marker: Marker?) {
        if (marker != null) {
            val startPosition = marker.position
            val endPosition = LatLng(destination.latitude, destination.longitude)
            val startRotation = marker.rotation
            val latLngInterpolator: LatLngInterpolatorNew = LatLngInterpolatorNew.LinearFixed()
            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.duration = 3000 // duration 3 second
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.addUpdateListener { animation ->
                try {
                    val v = animation.animatedFraction
                    val newPosition = latLngInterpolator.interpolate(v, startPosition, endPosition)
                    marker.setPosition(newPosition)
                    mMap!!.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(newPosition)
                                .zoom(zoomlevel)
                                .build()
                        )
                    )

                    marker.rotation = computeRotation(v, startRotation, destination.bearing);
                    //   marker.rotation = getBearing(startPosition, LatLng(destination.latitude, destination.longitude))
                } catch (ex: java.lang.Exception) {
                }
            }
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)

//                    if (mMarker != null) {
//                        mMarker.remove();
//                    }
//                    mMarker = mMap.addMarker(new MarkerOptions().position(endPosition).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_truck)));
                }
            })
            valueAnimator.start()
        }
    }

    interface LatLngInterpolatorNew {
        fun interpolate(fraction: Float, a: LatLng?, b: LatLng?): LatLng
        class LinearFixed : LatLngInterpolatorNew {
            override fun interpolate(fraction: Float, a: LatLng?, b: LatLng?): LatLng {
                val lat = (b!!.latitude - a!!.latitude) * fraction + a.latitude
                var lngDelta = b.longitude - a.longitude
                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360
                }
                val lng = lngDelta * fraction + a.longitude
                return LatLng(lat, lng)
            }


        }
    }


    //Method for finding bearing between two points
    private fun getBearing(begin: LatLng, end: LatLng): Float {
        val lat = Math.abs(begin.latitude - end.latitude)
        val lng = Math.abs(begin.longitude - end.longitude)
        if (begin.latitude < end.latitude && begin.longitude < end.longitude) return Math.toDegrees(Math.atan(lng / lat)).toFloat() else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) return (90 - Math.toDegrees(Math.atan(lng / lat)) + 90).toFloat() else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) return (Math.toDegrees(Math.atan(lng / lat)) + 180).toFloat() else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) return (90 - Math.toDegrees(Math.atan(lng / lat)) + 270).toFloat()
        return (-1).toFloat()
    }


    private fun SetZoomLevel(leftValue: Float) {
        zoomlevel = leftValue
    }

    override fun onReceive(intent: Intent?, Type: String?) {
        if (Type.equals("LocationChnage", ignoreCase = true)) {
            locationData = intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY) as MyLocationData
            if (locationData!!.isaccuracy === 1) {

                currentloction = LatLng(locationData!!.latitude.toDouble(), locationData!!.longitude.toDouble())
                mMap!!.isTrafficEnabled = true
                mMap!!.isIndoorEnabled = true
                mMap!!.isBuildingsEnabled = true
                mMap!!.uiSettings.isCompassEnabled = false
                mMap!!.uiSettings.isMapToolbarEnabled = false
                mMap!!.mapType = 1
                mMap!!.uiSettings.isZoomControlsEnabled = false
                mMap!!.uiSettings.setAllGesturesEnabled(true)
                mMap!!.uiSettings.isZoomGesturesEnabled = true
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                mMap!!.isMyLocationEnabled = false
                mMap!!.uiSettings.isMyLocationButtonEnabled = false
                mMap!!.uiSettings.isCompassEnabled = true
                animateMarker(locationData!!, mMarker)
                if (locationData!!.isaccuracy === 1) {
                    TrackingBinding.tvCurrentvalue.setText(locationData!!.speed)
                }
                CalculateDistance()
            }
        } else if (Type.equals("ServiceStop", ignoreCase = true)) {
            if (!SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.INTENT_ISUSERSTOPT_KEY, false)) {
                Log.e("TTT", "Start Again")
                if (Build.VERSION.SDK_INT < 26) {
                    startService(Intent(applicationContext, LocationService::class.java))
                } else {
                    startForegroundService(Intent(applicationContext, LocationService::class.java))
                }

            }
        } else if (Type.equals("RestReminder", ignoreCase = true)) {
            Log.e("REST_REMINDER_BRODCAST", "REST_REMINDER_BRODCAST")
            invalidateOptionsMenu()
            val restDialogFragment = RestDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.INTENT_LOCATION_KEY, intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY))
            restDialogFragment.arguments = bundle
            showDialog(restDialogFragment)
        } else if (Type.equals("OverSpeed", ignoreCase = true)) {
            Log.e("OVER_SPEED", "OVER_SPEED" + "  " + intent!!.getStringExtra(Constants.DISTANCE_KEY))
            val speedDialogFragment = SpeedDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.DISTANCE_KEY, intent!!.getStringExtra(Constants.DISTANCE_KEY))
            speedDialogFragment.setArguments(bundle)
            showDialog(speedDialogFragment)
        } else if (Type.equals("IdleReminder", ignoreCase = true)) {
            Log.e("IDLE_REMINDER_BRODCAST", "IDLE_REMINDER_BRODCAST")
            val idleDialogFragment = IdleDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.INTENT_LOCATION_KEY, intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY))
            idleDialogFragment.setArguments(bundle)
            showDialog(idleDialogFragment)
        }
    }


    fun getBitmapDescriptorFromVector(context: Context, @DrawableRes vectorDrawableResourceId: Int): BitmapDescriptor? {

        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        val bitmap = Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun computeRotation(fraction: Float, start: Float, end: Float): Float {
        val normalizeEnd = end - start // rotate start to 0
        val normalizedEndAbs = (normalizeEnd + 360) % 360
        val direction = if (normalizedEndAbs > 180) (-1).toFloat() else 1.toFloat() // -1 = anticlockwise, 1 = clockwise
        val rotation: Float
        rotation = if (direction > 0) {
            normalizedEndAbs
        } else {
            normalizedEndAbs - 360
        }
        val result = fraction * rotation + start
        return (result + 360) % 360
    }


    /**
     * Method to animate marker to destination location
     * @param destination destination location (must contain bearing attribute, to ensure
     * marker rotation will work correctly)
     * @param marker marker to be animated
     */
    fun animateMarker(destination: MyLocationData, marker: Marker?) {
        if (marker != null) {
            val startPosition = marker.position
            val endPosition = LatLng(destination.latitude.toDouble(), destination.longitude.toDouble())
            val startRotation = marker.rotation
            val latLngInterpolator: LatLngInterpolator = LatLngInterpolator.LinearFixed()
            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.duration = 1000 // duration 1 second
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.addUpdateListener { animation ->
                try {
                    val v = animation.animatedFraction
                    val newPosition: LatLng? = latLngInterpolator.interpolate(v, startPosition, endPosition)
                    if (newPosition != null) {
                        marker.setPosition(newPosition)
                    }
                    marker.rotation = computeRotation(v, startRotation, destination.bearing.toFloat())
                } catch (ex: java.lang.Exception) {
                    // I don't care atm..
                }
            }
            valueAnimator.start()
        }
    }


    private interface LatLngInterpolator {
        fun interpolate(fraction: Float, a: LatLng?, b: LatLng?): LatLng?
        class LinearFixed : LatLngInterpolator {
            override fun interpolate(fraction: Float, a: LatLng?, b: LatLng?): LatLng? {
                val lat = (b!!.latitude - a!!.latitude) * fraction + a.latitude
                var lngDelta = b.longitude - a.longitude
                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360
                }
                val lng = lngDelta * fraction + a.longitude
                return LatLng(lat, lng)
            }


        }
    }

}