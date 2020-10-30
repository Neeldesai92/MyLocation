package com.neel.desai.mylocation.service


import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.*

import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.activity.HomeActivity
import com.neel.desai.mylocation.database.DatabaseClient
import com.neel.desai.mylocation.model.LocationDiffData
import com.neel.desai.mylocation.model.MyLocationData
import com.neel.desai.mylocation.util.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


class LocationService : Service(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private var location: Location? = null
    private var googleApiClient: GoogleApiClient? = null
    private var locationRequest: LocationRequest? = null
    public var broadcastReceiver: BroadcastReceiver? = null
    var broadcastReceiverRestCancel: BroadcastReceiver? = null
    var gpsReceiver: BroadcastReceiver? = null
    var Activity = "UNKNOW"
    var bm: BatteryManager? = null
    var mBinder: IBinder? = null
    var myLocationData: MyLocationData = MyLocationData()

    // Used for Send data to Activity
    var LocationChange: Intent = Intent(Constants.LOCATION_CHNGAE)
    var PunchInterval: Intent = Intent(Constants.INTERVAL_PUNCH)
    var RestReminderIntent: Intent = Intent(Constants.REST_REMINDER_BRODCAST)
    var IdleReminderIntent: Intent = Intent(Constants.IDLE_REMINDER_BRODCAST)
    var OverSpeedIntent: Intent = Intent(Constants.OVER_SPEED)
    var ServicestopIntent: Intent = Intent(Constants.SERVICE_STOP)
    var cTimer: CountDownTimer? = null
    var cTimerRest: CountDownTimer? = null
    var cTimerOverSpeed: CountDownTimer? = null
    var isStart = false
    var isStartOverspeed = false
    var dif = 0.0
    var diffRestReminder = 0.0
    var cTimerSnoozRest: CountDownTimer? = null
    var isStartSnoozRest = false

    //interval between two services(Here Service run every 5 Minute)
    private val mHandler =
        Handler() //run on another Thread to avoid crash

    var Isaccuracy = false
    var IsRestStart = false
    var IsRestCancel = false
    var locationDiffData: ArrayList<LocationDiffData> =
        ArrayList<LocationDiffData>()
    var accuracyCount = 0

    var idleTime = 0
    override fun onBind(arg0: Intent): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {


        showNotification()

        // we build google api client
        googleApiClient =
            GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build()
        if (googleApiClient != null) {
            googleApiClient!!.connect()
        }


        broadcastReceiver = object : BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.BROADCAST_REST_ACTION) {
                    IsRestStart = intent.getBooleanExtra(
                        Constants.INTENT_IS_REST_START_KEY,
                        false
                    )
                    SharedPrefsUtils.setBooleanPreference(
                        applicationContext,
                        Constants.INTENT_IS_REST_START_KEY,
                        IsRestStart
                    )
                    if (SharedPrefsUtils.getBooleanPreference(
                            applicationContext,
                            Constants.INTENT_IS_REST_START_KEY,
                            false
                        )
                    ) {
                        cancelTimersnoozRest()
                        if (location != null) {
                            myLocationData = MyLocationData()
                            myLocationData.status = Activity
                            myLocationData.activity = Constants.REST_START
                            myLocationData.confidence = 0
                            myLocationData.batteryStatus =
                                bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                            myLocationData.bearing = location!!.bearing
                            myLocationData.latitude = location!!.latitude.toString() + ""
                            myLocationData.longitude = location!!.longitude.toString() + ""
                            myLocationData.mAltitude = location!!.altitude.toString() + ""
                            myLocationData.time = CommonMethods.getCurrentDateTime()
                            myLocationData.speed =
                                Math.round(location!!.speed * 3.6).toString() + ""
                            myLocationData.messsage = "User Start Rest"
                            myLocationData.Accuracy = location!!.accuracy.toString() + ""
                            myLocationData.brake = 0
                            myLocationData.restReminder = 0
                            myLocationData.still = 0
                            myLocationData.overSpeed = 0
                            myLocationData.isaccuracy = 1

                            DatabaseClient.getInstance(
                                applicationContext
                            ).getAppDatabase()
                                .taskDao()
                                ?.insert(myLocationData)
                            LocationChange.putExtra(Constants.INTENT_LOCATION_KEY, myLocationData)
                            sendBroadcast(LocationChange)
                        }
                        startTimerForREST(location)
                        cancelTimer()
                    } else {
                        SharedPrefsUtils.setBooleanPreference(
                            applicationContext,
                            Constants.IS_REST_REMINDER,
                            false
                        )
                        dif = 0.0
                        cancelTimersnoozRest()
                        cancelTimerForRest()
                    }
                }
            }
        }
        broadcastReceiverRestCancel = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                if (intent.action == Constants.BROADCAST_REST_ACTION_CANCEl) {
                    IsRestCancel = intent.getBooleanExtra(
                        Constants.IS_REST_CANCEL,
                        false
                    )
                    if (IsRestCancel) {
                        SharedPrefsUtils.setBooleanPreference(
                            applicationContext,
                            Constants.IS_REST_REMINDER,
                            false
                        )
                        cancelTimersnoozRest()
                    }
                }
            }
        }
        gpsReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {

                if (intent.action == "android.location.PROVIDERS_CHANGED") {
                    //Do your stuff on GPS status change
                    if (!Utility.isLocationServciesAvailable(
                            context
                        )
                    ) {
                        val i = Intent(applicationContext, HomeActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        Utility.showNotification(
                            context,
                            getString(R.string.alert),
                            getString(R.string.gps_alert_meesage),
                            i,
                            10020,
                            true,
                            false
                        )
                    } else {
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(10020)
                    }
                }
            }
        }
        applicationContext.registerReceiver(
            gpsReceiver,
            IntentFilter("android.location.PROVIDERS_CHANGED")
        )
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                broadcastReceiver as BroadcastReceiver,
                IntentFilter(Constants.BROADCAST_REST_ACTION)
            )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiverRestCancel as BroadcastReceiver,
            IntentFilter(Constants.BROADCAST_REST_ACTION_CANCEl)
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {

        super.onDestroy()
        SharedPrefsUtils.setStringPreference(
            applicationContext,
            Constants.INTENT_WEATHER_KEY,
            ""
        )
        SharedPrefsUtils.setBooleanPreference(
            applicationContext,
            Constants.IS_REST_REMINDER,
            false
        )
        if (cTimer != null) {
            isStart = false
            cTimer!!.cancel()
        }
        if (cTimerRest != null) {
            IsRestStart = false
            SharedPrefsUtils.setBooleanPreference(
                applicationContext,
                Constants.INTENT_IS_REST_START_KEY,
                IsRestStart
            )
            cTimerRest!!.cancel()
        }
        if (cTimerOverSpeed != null) {
            isStartOverspeed = false
            cTimerOverSpeed!!.cancel()
        }
        if (cTimerSnoozRest != null) {
            isStartSnoozRest = false
            cTimerSnoozRest!!.cancel()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true) //true will remove notification
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)

        // Insert Data
        if (location != null) {
            myLocationData = MyLocationData()
            myLocationData.status = Activity
            myLocationData.activity = Constants.STOP
            myLocationData.confidence = 0
            myLocationData.batteryStatus =
                bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            myLocationData.bearing = location!!.bearing
            myLocationData.latitude = location!!.latitude.toString() + ""
            myLocationData.longitude = location!!.longitude.toString() + ""
            myLocationData.mAltitude = location!!.altitude.toString() + ""
            myLocationData.time = CommonMethods.getCurrentDateTime()
            myLocationData.speed = Math.round(location!!.speed * 3.6).toString() + ""
            myLocationData.messsage = "Trip is STOP"
            myLocationData.Accuracy = location!!.accuracy.toString() + ""
            myLocationData.brake = 0
            myLocationData.restReminder = 0
            myLocationData.still = 0
            myLocationData.overSpeed = 0
            if (location!!.accuracy < Constants.TARGET_ACCURACY) {
                myLocationData.isaccuracy = 1
            } else {
                myLocationData.isaccuracy = 0
            }

            DatabaseClient.getInstance(applicationContext)
                .getAppDatabase()
                .taskDao()
                ?.insert(myLocationData)
            LocationChange.putExtra(
                Constants.INTENT_LOCATION_KEY,
                myLocationData
            )
            sendBroadcast(LocationChange)
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiverRestCancel!!)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver!!)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsReceiver!!)

        // stop location updates
        if (googleApiClient != null && googleApiClient!!.isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
            googleApiClient!!.disconnect()
        }

        //removeActivityUpdatesButtonHandler();
        sendBroadcast(ServicestopIntent)

        //Neel
    }

    override fun onConnected(bundle: Bundle?) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)


        startLocationUpdates()
    }

    override fun onConnectionSuspended(i: Int) {}
    override fun onConnectionFailed(connectionResult: ConnectionResult) {}

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onLocationChanged(location: Location) {

        //if (location != null&&!Utility.isMockLocationEnabled(getApplicationContext())&&!location.isFromMockProvider()) {
        if (location != null) {
            this.location = location



            if (!SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN, false)
                && !SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN_NOTIFFY, false) &&
                distanceBetween(location) <= Constants.DISTANCE) {
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN, true)
                val i = Intent(applicationContext, HomeActivity::class.java)
                i.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                Utility.showNotification(applicationContext, getString(R.string.geo_fance), getString(R.string.geo_fance_in_message), i, Constants.GEOFANCE_IN, true, false)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN_NOTIFFY, true)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_OUT, false)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_OUT_NOTIFFY, false)
            } else if (SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN, false) &&
                !SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.IS_GEOFANCE_OUT, false) &&
                !SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.IS_GEOFANCE_OUT_NOTIFFY, false) &&
                distanceBetween(location) > Constants.DISTANCE
            ) {
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_OUT, true)
                val i = Intent(applicationContext, HomeActivity::class.java)
                i.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                Utility.showNotification(applicationContext, getString(R.string.geo_fance), getString(R.string.geo_fance_out_message), i, Constants.GEOFANCE_OUT, true, false)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_OUT_NOTIFFY, true)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN, false)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN_NOTIFFY, false)

            }

            Activity = if (location.speed * 3.6 <= 5) {
                Constants.IDLE
            } else {
                Constants.RUNNING
            }
            if (location.hasAccuracy() && location.accuracy < Constants.TARGET_ACCURACY) {
                accuracyCount = 0
                val formatter: DateFormat =
                    SimpleDateFormat("dd MMM yyyy,HH:mm", Locale.US)
                try {


                    val locationData =
                        DatabaseClient.getInstance(applicationContext).getAppDatabase().taskDao()
                            ?.getaccuracy("1")

                    if (locationData != null && locationData.isNotEmpty()) {


                        if (Utility.getDistancBetweenTwoPoints(locationData[locationData.size - 1].latitude.toDouble(), locationData[locationData.size - 1].longitude.toDouble(), location.latitude, location.longitude)
                            < Utility.GetDate(Date(), formatter.parse(locationData[locationData.size - 1].time) as Date) * 23.33
                        ) {
                            Isaccuracy = true
                            // Start/STOP IDLE Timer
                            if (!SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.INTENT_IS_REST_START_KEY, false) && Activity == "IDLE") {
                                startTimer(location)
                            } else {
                                if (locationData != null && locationData.size > 0) {
                                    if (!locationData[locationData.size - 1].activity.equals(Constants.IDLE)) {
                                        cancelTimer()
                                    }
                                } else {
                                    cancelTimer()
                                }
                            }
                            if (location.speed * 3.6 > Constants.OVER_SPEED_HIGH_KM) {
                                AudioPlayer().play(applicationContext, R.raw.max_speed)


                                val i = Intent(applicationContext, HomeActivity::class.java)
                                i.flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                Utility.showNotification(
                                    applicationContext,
                                    getString(R.string.too_fast_title),
                                    getString(R.string.to_fast_message),
                                    i,
                                    Constants.TOFAST_NOTIFICATION_ID,
                                    true,
                                    false
                                )
                            }

                            // Start/STOP Over Speed Timer
                            if (location.speed * 3.6 > Constants.OVER_SPEED_KM) {
                                startTimerOverspeed(location)
                            } else {
                                //(locationData != null && locationData.size() > 0 && Utility.GetDate(new Date(), (Date) formatter.parse(locationData.get(locationData.size() - 1).getTime())) < 3000)
                                if (locationData != null && locationData.size > 0) {
                                    if (locationData[locationData.size - 1].speed
                                            .toDouble() < Constants.OVER_SPEED_KM
                                    ) {
                                        cancelTimerOverspeed()
                                    }
                                } else {
                                    cancelTimerOverspeed()
                                }
                            }

                            // Code for Calculate total distance we used Last data should be running or rest end  and current data should be Running and Rest Start
                            if (!SharedPrefsUtils.getBooleanPreference(
                                    applicationContext,
                                    Constants.INTENT_IS_REST_START_KEY,
                                    false
                                ) && (locationData[locationData.size - 1].activity
                                    .equals(Constants.OVER_LIMIT) || locationData[locationData.size - 1].activity
                                    .equals(Constants.RUNNING) || locationData[locationData.size - 1].activity
                                    .equals(Constants.REST_START) || locationData[locationData.size - 1].activity
                                    .equals(Constants.REST_END)) && Activity == Constants.RUNNING
                            ) {
                                var diff: Double =
                                    SharedPrefsUtils.getDoublePreference(
                                        applicationContext,
                                        Constants.INTENT_DISTANCE_DIFFT_KEY,
                                        0.0
                                    )
                                diff += Utility.getDistancBetweenTwoPoints(
                                    locationData[locationData.size - 1].latitude
                                        .toDouble(),
                                    locationData[locationData.size - 1].longitude
                                        .toDouble(),
                                    location.latitude,
                                    location.longitude
                                )
                                SharedPrefsUtils.setDoublePreference(
                                    applicationContext,
                                    Constants.INTENT_DISTANCE_DIFFT_KEY,
                                    diff
                                )

                            }


                            // Check and Add entry if detect brake
                            if (Utility.GetDiffrent( Math.round(location.speed * 3.6),Math.round(locationData[locationData.size - 1].speed.toDouble())) > Constants.DIFFRENT_ACCELERATION_SPEED ) {
                                myLocationData = MyLocationData()
                                if (SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.INTENT_IS_REST_START_KEY, false)) {
                                    myLocationData.activity = Constants.IN_REST
                                } else {
                                    myLocationData.activity = Constants.HARSH_ACCELERATION
                                }
                                myLocationData.confidence = 0
                                myLocationData.batteryStatus =
                                    bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                myLocationData.bearing = location!!.bearing
                                myLocationData.latitude = location.latitude.toString() + ""
                                myLocationData.longitude = location.longitude.toString() + ""
                                myLocationData.mAltitude = location.altitude.toString() + ""
                                myLocationData.time = CommonMethods.getCurrentDateTime()
                                myLocationData.speed =
                                    Math.round(location.speed * 3.6).toString() + ""

                                myLocationData.messsage = "Harsh acceleration is detect Drive safely"

                                myLocationData.Accuracy = location.accuracy.toString() + ""
                                myLocationData.brake = 1
                                myLocationData.restReminder = 0
                                myLocationData.still = 0
                                myLocationData.overSpeed = 0
                                myLocationData.isaccuracy = 1

                                DatabaseClient.getInstance(applicationContext).getAppDatabase().taskDao()?.insert(myLocationData)
                                LocationChange.putExtra(Constants.INTENT_LOCATION_KEY, myLocationData)
                                sendBroadcast(LocationChange)


                                val i = Intent(applicationContext, HomeActivity::class.java)
                                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                Utility.showNotification(
                                    applicationContext,
                                    getString(R.string.driver_behaviour),
                                    getString(R.string.driver_behaviour_acceleration_message),
                                    i,
                                    Constants.HARSH_ACCELERATION_NOTIFICATION_ID,
                                    true,
                                    false
                                )


                                //Neel
                            }
                            else  if (Utility.GetDiffrent(Math.round(locationData[locationData.size - 1].speed.toDouble()), Math.round(location.speed * 3.6)) > Constants.DIFFRENT_BRAKE_SPEED && Math.round(locationData[locationData.size - 1].speed.toDouble()) >= Constants.LAST_BRAKE_SPEED_KM) {
                                myLocationData = MyLocationData()
                                if (SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.INTENT_IS_REST_START_KEY, false)) {
                                    myLocationData.activity = Constants.IN_REST
                                } else {
                                    myLocationData.activity = Constants.HEARD_BRAKE
                                }
                                myLocationData.confidence = 0
                                myLocationData.batteryStatus =
                                    bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                myLocationData.bearing = location!!.bearing
                                myLocationData.latitude = location.latitude.toString() + ""
                                myLocationData.longitude = location.longitude.toString() + ""
                                myLocationData.mAltitude = location.altitude.toString() + ""
                                myLocationData.time = CommonMethods.getCurrentDateTime()
                                myLocationData.speed =
                                    Math.round(location.speed * 3.6).toString() + ""

                                myLocationData.messsage =
                                    "Harsh brake is detect last speed is " + locationData[locationData.size - 1].speed
                                        .toString() + " and current speed is " + Math.round(
                                        location.speed * 3.6
                                    )

                                myLocationData.Accuracy = location.accuracy.toString() + ""
                                myLocationData.brake = 1
                                myLocationData.restReminder = 0
                                myLocationData.still = 0
                                myLocationData.overSpeed = 0
                                myLocationData.isaccuracy = 1

                                DatabaseClient.getInstance(applicationContext).getAppDatabase().taskDao()?.insert(myLocationData)
                                LocationChange.putExtra(Constants.INTENT_LOCATION_KEY, myLocationData)
                                sendBroadcast(LocationChange)


                                val i = Intent(applicationContext, HomeActivity::class.java)
                                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                Utility.showNotification(
                                    applicationContext,
                                    getString(R.string.driver_behaviour),
                                    getString(R.string.driver_behaviour_brake_message),
                                    i,
                                    Constants.HARSH_BRAKE_NOTIFICATION_ID,
                                    true,
                                    false
                                )


                                //Neel
                            } else if (Math.round(location.speed * 3.6) > Constants.OVER_SPEED_KM) {

                                // Check and add speed limit is brake
                                myLocationData = MyLocationData()
                                if (SharedPrefsUtils.getBooleanPreference(
                                        applicationContext,
                                        Constants.INTENT_IS_REST_START_KEY,
                                        false
                                    )
                                ) {
                                    myLocationData.activity = Constants.IN_REST
                                } else {
                                    myLocationData.activity = Constants.OVER_LIMIT
                                }
                                myLocationData.confidence = 0
                                myLocationData.batteryStatus =
                                    bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                myLocationData.bearing = location!!.bearing
                                myLocationData.latitude = location.latitude.toString() + ""
                                myLocationData.longitude = location.longitude.toString() + ""
                                myLocationData.mAltitude = location.altitude.toString() + ""
                                myLocationData.time = CommonMethods.getCurrentDateTime()
                                myLocationData.speed =
                                    Math.round(location.speed * 3.6).toString() + ""

                                myLocationData.messsage =
                                    "Speed is " + Math.round(
                                        location.speed * 3.6
                                    ) + Constants.OVER_LIMIT.toString() + ""

                                myLocationData.Accuracy = location.accuracy.toString() + ""
                                myLocationData.brake = 0
                                myLocationData.restReminder = 0
                                myLocationData.still = 0
                                myLocationData.overSpeed = 1
                                myLocationData.isaccuracy = 1
                                myLocationData.status = Activity
                                DatabaseClient.getInstance(
                                    applicationContext
                                ).getAppDatabase()
                                    .taskDao()
                                    ?.insert(myLocationData)
                                LocationChange.putExtra(
                                    Constants.INTENT_LOCATION_KEY,
                                    myLocationData
                                )
                                sendBroadcast(LocationChange)
                            } else {

                                // Insert Data With accuracy true
                                myLocationData = MyLocationData()
                                if (SharedPrefsUtils.getBooleanPreference(
                                        applicationContext,
                                        Constants.INTENT_IS_REST_START_KEY,
                                        false
                                    )
                                ) {
                                    myLocationData.activity = Constants.IN_REST
                                } else {
                                    myLocationData.activity = Activity
                                }
                                myLocationData.confidence = 0
                                myLocationData.batteryStatus =
                                    bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                myLocationData.latitude = location.latitude.toString() + ""
                                myLocationData.bearing = location!!.bearing
                                myLocationData.longitude = location.longitude.toString() + ""
                                myLocationData.mAltitude = location.altitude.toString() + ""
                                myLocationData.time = CommonMethods.getCurrentDateTime()
                                myLocationData.speed =
                                    Math.round(location.speed * 3.6).toString() + ""

                                myLocationData.messsage = "Location Update"
                                myLocationData.Accuracy = location.accuracy.toString() + ""
                                myLocationData.brake = 0
                                myLocationData.restReminder = 0
                                myLocationData.still = 0
                                myLocationData.overSpeed = 0

                                myLocationData.isaccuracy = 1
                                myLocationData.status = Activity
                                DatabaseClient.getInstance(
                                    applicationContext
                                ).getAppDatabase()
                                    .taskDao()
                                    ?.insert(myLocationData)
                                LocationChange.putExtra(
                                    Constants.INTENT_LOCATION_KEY,
                                    myLocationData
                                )
                                sendBroadcast(LocationChange)
                                //  isaccuracy = false;
                            }


                            // Find Diffrent  Between last latlong point and current latlong point

                            val locationDataRest = DatabaseClient.getInstance(
                                applicationContext
                            ).getAppDatabase()
                                .taskDao()
                                ?.getSearch(Constants.REST_END)

                            if (locationDataRest != null && locationDataRest.size > 0) {
                                dif = if (dif == 0.0) {
                                    Utility.getDistancBetweenTwoPoints(
                                        locationDataRest[locationDataRest.size - 1]
                                            .latitude.toDouble(),
                                        locationDataRest[locationDataRest.size - 1]
                                            .longitude.toDouble(),
                                        location.latitude,
                                        location.longitude
                                    ).toDouble()
                                } else {
                                    val difff: Double =
                                        Utility.getDistancBetweenTwoPoints(
                                            locationData[locationData.size - 1].latitude
                                                .toDouble(),
                                            locationData[locationData.size - 1].longitude
                                                .toDouble(),
                                            location.latitude,
                                            location.longitude
                                        )
                                    dif + difff
                                }
                                SharedPrefsUtils.setDoublePreference(
                                    applicationContext,
                                    Constants.INTENT_DISTANCE_REMAING_KEY,
                                    dif
                                )

                            } else {
                                dif =
                                    SharedPrefsUtils.getDoublePreference(
                                        applicationContext,
                                        Constants.INTENT_DISTANCE_DIFFT_KEY,
                                        0.0
                                    )
                                SharedPrefsUtils.setDoublePreference(
                                    applicationContext,
                                    Constants.INTENT_DISTANCE_REMAING_KEY,
                                    dif
                                )
                                //  dif = SharedPrefsUtils.getDoublePreference(getApplicationContext(), "DistanceDiff", 0);

                            }
                            var locationLastRestReminder: List<MyLocationData>? = null
                            if (locationDataRest != null && locationDataRest.size > 0) {


                                locationLastRestReminder =
                                    DatabaseClient.getInstance(applicationContext).getAppDatabase()
                                        .taskDao()
                                        ?.getLastRestReminder(locationDataRest[locationDataRest.size - 1].id)


                            } else {
                                if (locationData != null && locationData.size > 0) {

                                    locationLastRestReminder = DatabaseClient.getInstance(applicationContext).getAppDatabase().taskDao()?.getLastRestReminder(locationData[0].id)

                                }
                            }
                            val isLastReminderComplete: Boolean
                            if (locationLastRestReminder != null && locationLastRestReminder.size > 0) {

                                if (diffRestReminder == 0.0) {
                                    diffRestReminder = Utility.getDistancBetweenTwoPoints(locationLastRestReminder[locationLastRestReminder.size - 1].latitude.toDouble(), locationLastRestReminder[locationLastRestReminder.size - 1].longitude.toDouble(), location.latitude, location.longitude)
                                    SharedPrefsUtils.setDoublePreference(applicationContext, Constants.INTENT_DISTANCE_REMAING_KEY, diffRestReminder)
                                } else {
                                    val difff: Double = Utility.getDistancBetweenTwoPoints(locationData[locationData.size - 1].latitude.toDouble(), locationData[locationData.size - 1].longitude.toDouble(), location.latitude, location.longitude)
                                    diffRestReminder = diffRestReminder + difff
                                    SharedPrefsUtils.setDoublePreference(applicationContext, Constants.INTENT_DISTANCE_REMAING_KEY, diffRestReminder)
                                }

                                isLastReminderComplete = diffRestReminder < Constants.REST_AFTER_IN_KM
                            } else {
                                isLastReminderComplete = false
                            }
                            locationDiffData.add(LocationDiffData(CommonMethods.getCurrentDateTime(), dif.toString() + ""))
                            SharedPrefsUtils.setStringPreference(applicationContext, Constants.INTENTDIFF_DATA_KEY, Gson().toJson(locationDiffData))


                            // This Code is For Add Entry of Rest Reminder ANd Notify to user
                            if (dif > Constants.REST_AFTER_IN_KM && !SharedPrefsUtils.getBooleanPreference(
                                    applicationContext,
                                    Constants.INTENT_IS_REST_START_KEY,
                                    false
                                ) && !isLastReminderComplete
                            ) {
                                dif = 0.0
                                diffRestReminder = 0.0
                                SharedPrefsUtils.setDoublePreference(
                                    applicationContext,
                                    Constants.INTENT_DISTANCE_REMAING_KEY,
                                    dif
                                )

                                //  int random = new Random().nextInt(Integer.MAX_VALUE+1);
                                myLocationData = MyLocationData()
                                myLocationData.activity = Constants.REST_REMINDER
                                myLocationData.confidence = 0
                                myLocationData.batteryStatus =
                                    bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                myLocationData.latitude = location.latitude.toString() + ""
                                myLocationData.bearing = location!!.bearing
                                myLocationData.longitude = location.longitude.toString() + ""
                                myLocationData.mAltitude = location.altitude.toString() + ""
                                myLocationData.time = CommonMethods.getCurrentDateTime()
                                myLocationData.speed =
                                    Math.round(location.speed * 3.6).toString() + ""

                                myLocationData.messsage = "Take Rest User running since long"
                                myLocationData.Accuracy = location.accuracy.toString() + ""
                                myLocationData.brake = 0
                                myLocationData.restReminder = 1
                                myLocationData.still = 0
                                myLocationData.overSpeed = 0
                                myLocationData.isaccuracy = 1
                                myLocationData.notificationID = 100014
                                myLocationData.status = Activity
                                DatabaseClient.getInstance(
                                    applicationContext
                                ).getAppDatabase()
                                    .taskDao()
                                    ?.insert(myLocationData)
                                myLocationData =
                                    DatabaseClient.getInstance(
                                        applicationContext
                                    ).getAppDatabase()
                                        .taskDao()?.getLastRecord()!!
                                LocationChange.putExtra(
                                    Constants.INTENT_LOCATION_KEY,
                                    myLocationData
                                )
                                sendBroadcast(LocationChange)
                                SharedPrefsUtils.setBooleanPreference(
                                    applicationContext,
                                    Constants.IS_REST_REMINDER,
                                    true
                                )
                                SharedPrefsUtils.setStringPreference(
                                    applicationContext,
                                    Constants.IS_REST_REMINDER_DATA,
                                    Gson().toJson(myLocationData)
                                )



                                if (Utility.isApplicationSentToBackground(applicationContext)) {
                                    val i =
                                        Intent(applicationContext, HomeActivity::class.java)
                                    i.flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    i.putExtra(
                                        Constants.IS_FOR_REST,
                                        true
                                    )
                                    i.putExtra(
                                        Constants.INTENT_LOCATION_KEY,
                                        myLocationData
                                    )
                                    Utility.showNotification(
                                        applicationContext,
                                        getString(R.string.rest_reminder_title),
                                        getString(R.string.rest_reminder_message),
                                        i,
                                        Constants.REST_NOTIFICATION_ID,
                                        true,
                                        false
                                    )
                                } else {
                                    RestReminderIntent.putExtra(
                                        Constants.INTENT_LOCATION_KEY,
                                        myLocationData
                                    )
                                    sendBroadcast(RestReminderIntent)
                                }
                                //   startTimersnoozRest();
                            } else {
                                //dif = 0.0;
                                //   diffRestReminder=0.0;
                            }
                        } else {
                            Isaccuracy = false


                            // Insert Data with Accuracy false
                            if (location != null) {
                                myLocationData = MyLocationData()
                                myLocationData.activity = Activity
                                myLocationData.confidence = 0
                                myLocationData.batteryStatus =
                                    bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                myLocationData.latitude = location.latitude.toString() + ""
                                myLocationData.bearing = location!!.bearing
                                myLocationData.longitude = location.longitude.toString() + ""
                                myLocationData.mAltitude = location.altitude.toString() + ""
                                myLocationData.time = CommonMethods.getCurrentDateTime()
                                myLocationData.speed =
                                    Math.round(location.speed * 3.6).toString() + ""

                                myLocationData.messsage = "Location Update"
                                myLocationData.Accuracy = location.accuracy.toString() + ""
                                myLocationData.brake = 0
                                myLocationData.restReminder = 0
                                myLocationData.still = 0
                                myLocationData.overSpeed = 0

                                myLocationData.isaccuracy = 0
                                myLocationData.status = Activity
                                DatabaseClient.getInstance(
                                    applicationContext
                                ).getAppDatabase()
                                    .taskDao()
                                    ?.insert(myLocationData)
                                LocationChange.putExtra(
                                    Constants.INTENT_LOCATION_KEY,
                                    myLocationData
                                )
                                sendBroadcast(LocationChange)
                            }
                        }
                    } else {
                        Insert1stlocation(location)
                    }
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
            } else {
                if (accuracyCount > 10) {
                    AudioPlayer().play(applicationContext, R.raw.location_not_recognize)
                    val intent = Intent(applicationContext, HomeActivity::class.java)
                    intent.putExtra(Constants.OPEN_MAP_KEY, true)
                    intent.putExtra(
                        Constants.INTENT_LOCATION_KEY,
                        location
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    Utility.showNotification(
                        applicationContext,
                        getString(R.string.accuracy_title),
                        getString(R.string.accuracy_reminder_message),
                        intent,
                        Constants.LOCATION_ACCURCY_NOTIFICATION_ID,
                        true,
                        false
                    )
                    accuracyCount = 0
                } else {
                    accuracyCount++
                }
                Insert1stlocation(location)
            }

//        }else {
//
//        }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun Insert1stlocation(location: Location?) {
        // Insert Data if no data in DB
        if (location != null) {
            myLocationData = MyLocationData()
            myLocationData.confidence = 0
            myLocationData.batteryStatus =
                bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            myLocationData.latitude = location.latitude.toString() + ""
            myLocationData.bearing = location!!.bearing
            myLocationData.longitude = location.longitude.toString() + ""
            myLocationData.mAltitude = location.altitude.toString() + ""
            myLocationData.time = CommonMethods.getCurrentDateTime()
            myLocationData.speed = Math.round(location.speed * 3.6).toString() + ""
            myLocationData.Accuracy = location.accuracy.toString() + ""
            myLocationData.brake = 0
            myLocationData.restReminder = 0
            myLocationData.still = 0

            if (location.accuracy < Constants.TARGET_ACCURACY) {
                myLocationData.isaccuracy = 1
            } else {
                myLocationData.isaccuracy = 0
            }
            if (location.accuracy < Constants.TARGET_ACCURACY
                && Math.round(location.speed * 3.6) > Constants.OVER_SPEED_KM
            ) {
                myLocationData.activity = Constants.OVER_LIMIT
                myLocationData.messsage =
                    "Speed is " + Math.round(location.speed * 3.6) + "over limit "
                myLocationData.overSpeed = 1
                startTimerOverspeed(location)
            } else {
                myLocationData.activity = Activity
                myLocationData.messsage = "Location Update"
                myLocationData.overSpeed = 0
            }
            myLocationData.status = Activity
            DatabaseClient.getInstance(applicationContext)
                .getAppDatabase()
                .taskDao()
                ?.insert(myLocationData)
            LocationChange.putExtra(
                Constants.INTENT_LOCATION_KEY,
                myLocationData
            )
            sendBroadcast(LocationChange)
        }
    }

    // Start Location Request Config
    private fun startLocationUpdates() {
        locationRequest = LocationRequest()
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest!!.interval = 1000
        locationRequest!!.smallestDisplacement = 0f
        locationRequest!!.fastestInterval =
            Constants.FASTEST_INTERVAL
        //   locationRequest.setSmallestDisplacement(1000);
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                "You need to enable permissions to display location !",
                Toast.LENGTH_SHORT
            ).show()
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
            googleApiClient,
            locationRequest,
            this
        )
    }

    //start timer function for User Start Rest
    fun startTimerForREST(location: Location?) {
        val TickTime: Int =
            Constants.REST_TIME - Constants.REST_TIME * Constants.REST_COMPLETE_BEFORETIME_PERCENT / 100

        cTimerRest = object :
            CountDownTimer(Constants.REST_TIME.toLong(), TickTime.toLong()) {
            override fun onTick(millisUntilFinished: Long) {

                if (millisUntilFinished < TickTime) {
                    AudioPlayer().play(applicationContext, R.raw.rest_about_to_complete)
                    val i = Intent(applicationContext, HomeActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    Utility.showNotification(
                        applicationContext,
                        getString(R.string.rest_complete_reminder_title),
                        getString(R.string.rest_complete_reminder_about_message),
                        i,
                        10021,
                        true,
                        false
                    )
                    //Neel
                }
            }

            override fun onFinish() {

                //    IsRestStart = false;
                AudioPlayer().play(applicationContext, R.raw.rest_complete)
                val i = Intent(applicationContext, HomeActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                Utility.showNotification(
                    applicationContext,
                    getString(R.string.rest_complete_reminder_title),
                    getString(R.string.rest_complete_reminder_message),
                    i,
                    10021,
                    true,
                    false
                )
                startTimerForRESTComplete(location)

                //Neel
            }
        }
        (cTimerRest as CountDownTimer).start()
    }

    //cancel timer for User End Rest
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun cancelTimerForRest() {

        //Neel
        if (cTimerRest != null) {
            IsRestStart = false
            SharedPrefsUtils.setBooleanPreference(
                applicationContext,
                Constants.INTENT_IS_REST_START_KEY,
                IsRestStart
            )
            cTimerRest!!.cancel()
            if (location != null) {
                myLocationData = MyLocationData()
                myLocationData.activity = Constants.REST_END
                myLocationData.confidence = 0
                myLocationData.batteryStatus =
                    bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                myLocationData.latitude = location!!.latitude.toString() + ""
                myLocationData.bearing = location!!.bearing
                myLocationData.longitude = location!!.longitude.toString() + ""
                myLocationData.mAltitude = location!!.altitude.toString() + ""
                myLocationData.time = CommonMethods.getCurrentDateTime()
                myLocationData.speed =
                    Math.round(location!!.speed * 3.6).toString() + ""

                myLocationData.messsage = "User End Rest"
                myLocationData.Accuracy = location!!.accuracy.toString() + ""
                myLocationData.brake = 0
                myLocationData.restReminder = 0
                myLocationData.still = 0
                myLocationData.overSpeed = 0
                myLocationData.isaccuracy = 1
                myLocationData.status = Activity
                DatabaseClient.getInstance(applicationContext)
                    .getAppDatabase()
                    .taskDao()
                    ?.insert(myLocationData)
                LocationChange.putExtra(
                    Constants.INTENT_LOCATION_KEY,
                    myLocationData
                )
                sendBroadcast(LocationChange)
            }
        } else {
            IsRestStart = false
            SharedPrefsUtils.setBooleanPreference(
                applicationContext,
                Constants.INTENT_IS_REST_START_KEY,
                IsRestStart
            )
            if (location != null) {
                myLocationData = MyLocationData()
                myLocationData.activity = Constants.REST_END
                myLocationData.confidence = 0
                myLocationData.batteryStatus =
                    bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                myLocationData.latitude = location!!.latitude.toString() + ""
                myLocationData.bearing = location!!.bearing
                myLocationData.longitude = location!!.longitude.toString() + ""
                myLocationData.mAltitude = location!!.altitude.toString() + ""
                myLocationData.time = CommonMethods.getCurrentDateTime()
                myLocationData.speed =
                    Math.round(location!!.speed * 3.6).toString() + ""

                myLocationData.messsage = "User End Rest"
                myLocationData.Accuracy = location!!.accuracy.toString() + ""
                myLocationData.brake = 0
                myLocationData.restReminder = 0
                myLocationData.still = 0
                myLocationData.overSpeed = 0
                myLocationData.isaccuracy = 1
                myLocationData.status = Activity
                DatabaseClient.getInstance(applicationContext)
                    .getAppDatabase()
                    .taskDao()
                    ?.insert(myLocationData)
                LocationChange.putExtra(
                    Constants.INTENT_LOCATION_KEY,
                    myLocationData
                )
                sendBroadcast(LocationChange)
            }
        }
    }

    //start timer function for User is IDLE
    fun startTimer(location: Location?) {
        if (!isStart) {
            cTimer = object :
                CountDownTimer(Constants.IDLE_TIME.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {}

                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                override fun onFinish() {
                    isStart = false
                    idleTime += Constants.IDLE_TIME
                    AudioPlayer().play(applicationContext, R.raw.sitting_idle)
                    // Insert Data
                    // int random = (int) (Math.random() * 50 + 1);
                    if (location != null) {
                        myLocationData = MyLocationData()
                        myLocationData.activity = Constants.IDLE_REMINDER
                        myLocationData.confidence = 0
                        myLocationData.batteryStatus =
                            bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        myLocationData.latitude = location.latitude.toString() + ""
                        myLocationData.bearing = location!!.bearing
                        myLocationData.longitude = location.longitude.toString() + ""
                        myLocationData.mAltitude = location.altitude.toString() + ""
                        myLocationData.time = CommonMethods.getCurrentDateTime()
                        myLocationData.speed =
                            Math.round(location.speed * 3.6).toString() + ""

                        myLocationData.messsage = "User is not move from long time"
                        myLocationData.Accuracy = location.accuracy.toString() + ""
                        myLocationData.brake = 0
                        myLocationData.restReminder = 0
                        myLocationData.still = 1
                        myLocationData.overSpeed = 0
                        myLocationData.notificationID = Constants.IDLE_NOTIFICATION_ID
                        myLocationData.isaccuracy = 1
                        myLocationData.status = Activity
                        DatabaseClient.getInstance(
                            applicationContext
                        ).getAppDatabase()
                            .taskDao()
                            ?.insert(myLocationData)
                        myLocationData =
                            DatabaseClient.getInstance(applicationContext).getAppDatabase()
                                .taskDao()?.getLastRecord()!!
                        LocationChange.putExtra(
                            Constants.INTENT_LOCATION_KEY,
                            myLocationData
                        )
                        sendBroadcast(LocationChange)



                        if (Utility.isApplicationSentToBackground(applicationContext)) {
                            val i = Intent(applicationContext, HomeActivity::class.java)
                            i.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            i.putExtra(Constants.IS_IDLE_REMINDER, true)
                            i.putExtra(Constants.INTENT_LOCATION_KEY, myLocationData)
                            Utility.showNotification(
                                applicationContext,
                                getString(R.string.idle_reminder_title),
                                getString(R.string.idle_reminder_message),
                                i,
                                Constants.IDLE_NOTIFICATION_ID,
                                false,
                                true
                            )
                        } else {
                            IdleReminderIntent.putExtra(
                                Constants.INTENT_LOCATION_KEY,
                                myLocationData
                            )
                            sendBroadcast(IdleReminderIntent)
                        }

                    }
                }
            }
            isStart = true
            (cTimer as CountDownTimer).start()
        }
    }

    //cancel timer for User is IDLE
    fun cancelTimer() {
        if (cTimer != null) {
            idleTime = 0
            isStart = false
            cTimer!!.cancel()
        }
    }


    fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val CHANNEL_ONE_ID = "in.webxpress.drivexpress"
            val CHANNEL_ONE_NAME = "Channel One"
            var notificationChannel: NotificationChannel? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationChannel = NotificationChannel(
                    CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.setShowBadge(true)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                val manager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(notificationChannel)
            }
            val icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            val notification: Notification =
                Notification.Builder(applicationContext)
                    .setChannelId(CHANNEL_ONE_ID)
                    .setContentTitle("DriveXpress")
                    .setContentText("DriveXpress is Running in Background")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(icon)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build()
            val notificationIntent =
                Intent(applicationContext, HomeActivity::class.java)
            notificationIntent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            notificationIntent.action = System.currentTimeMillis().toString() + ""
            notification.contentIntent =
                PendingIntent.getActivity(applicationContext, 0, notificationIntent, 0)
            startForeground(
                Constants.FORGROUND_NOTIFICATION_ID,
                notification
            )
        }
    }


    //start timer function for User Start Rest
    fun startTimerForRESTComplete(location: Location?) {
        cTimerRest = object :
            CountDownTimer(Constants.EXCEED_REST_TIME.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!SharedPrefsUtils.getBooleanPreference(
                        applicationContext,
                        Constants.INTENT_IS_REST_START_KEY,
                        false
                    )
                ) {
                    cancelTimerForRestComplete()
                }
            }

            override fun onFinish() {
                //    IsRestStart = false;
                AudioPlayer().play(applicationContext, R.raw.rest_time_exceeds)
                val i = Intent(applicationContext, HomeActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                Utility.showNotification(
                    applicationContext,
                    getString(R.string.rest_complete_reminder_title),
                    getString(R.string.rest_complete_reminder_aftre_message),
                    i,
                    10021,
                    true,
                    false
                )
                //Neel

            }
        }
        (cTimerRest as CountDownTimer).start()
    }

    //cancel timer for User End Rest
    fun cancelTimerForRestComplete() {
        if (cTimerRest != null) {
            IsRestStart = false
            SharedPrefsUtils.setBooleanPreference(
                applicationContext,
                Constants.INTENT_IS_REST_START_KEY,
                IsRestStart
            )
            cTimerRest!!.cancel()
        }
    }

    //start timer function for User is Overspeed
    fun startTimerOverspeed(location: Location?) {
        if (SharedPrefsUtils.getStringPreference(
                applicationContext,
                Constants.OVERSPEED_LAT_KEY
            ).equals("0.0") &&
            SharedPrefsUtils.getStringPreference(
                applicationContext,
                Constants.OVERSPEED_LONG_KEY
            ).equals("0.0") &&
            SharedPrefsUtils.getStringPreference(
                applicationContext,
                Constants.DISTANCE_KEY
            ).equals("0")
        ) {
            Utility.setOverspeedData(
                applicationContext,
                location!!.latitude.toString() + "",
                location.longitude.toString() + "",
                "0"
            )

        } else {
            val distance: Double =
                SharedPrefsUtils.getStringPreference(applicationContext, Constants.DISTANCE_KEY)
                    .toDouble() + Math.round(
                    Utility.getDistancBetweenTwoPoints(
                        SharedPrefsUtils.getStringPreference(
                            applicationContext,
                            Constants.OVERSPEED_LAT_KEY
                        ).toDouble(),
                        SharedPrefsUtils.getStringPreference(
                            applicationContext,
                            Constants.OVERSPEED_LONG_KEY
                        ).toDouble(),
                        location!!.latitude,
                        location.longitude
                    )
                )
            Utility.setOverspeedData(
                applicationContext,
                location.latitude.toString() + "",
                location.longitude.toString() + "",
                distance.toString() + ""
            )

        }
        if (!isStartOverspeed) {
            cTimerOverSpeed = object :
                CountDownTimer(Constants.OVER_SPEED_TIME.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    AudioPlayer().play(applicationContext, R.raw.slowdown)
                    isStartOverspeed = false
                    if (Utility.isApplicationSentToBackground(
                            applicationContext
                        )
                    ) {
                        val i = Intent(applicationContext, HomeActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        i.putExtra(Constants.ISFOR_OVERSPEED, true)
                        val distance: String =
                            SharedPrefsUtils.getStringPreference(
                                applicationContext,
                                Constants.DISTANCE_KEY
                            )
                        i.putExtra(
                            Constants.DISTANCE_KEY,
                            distance
                        )
                        i.putExtra(Constants.ISFOR_OVERSPEED, true)
                        Utility.showNotification(
                            applicationContext,
                            getString(R.string.overspeed_title),
                            getString(R.string.over_speed_message),
                            i,
                            Constants.OVERSPEED_NOTIFICATION_ID,
                            true,
                            false
                        )


                    } else {
                        val distance: String = SharedPrefsUtils.getStringPreference(
                            applicationContext,
                            Constants.DISTANCE_KEY
                        )

                        OverSpeedIntent.putExtra(
                            Constants.DISTANCE_KEY,
                            distance
                        )
                        sendBroadcast(OverSpeedIntent)
                        Utility.setOverspeedData(
                            applicationContext,
                            "0.0",
                            "0.0",
                            "0"
                        )
                    }
                    // Insert Data
                }
            }
            isStartOverspeed = true
            (cTimerOverSpeed as CountDownTimer).start()
        }
    }

    //cancel timer for User is IDLE
    fun cancelTimerOverspeed() {
        if (cTimerOverSpeed != null) {
            Utility.setOverspeedData(
                applicationContext,
                "0.0",
                "0.0",
                "0"
            )
            isStartOverspeed = false
            cTimerOverSpeed!!.cancel()
        }
    }

    //start timer function for User is Overspeed
    fun startTimersnoozRest() {
        if (!isStartSnoozRest) {
            cTimerSnoozRest = object :
                CountDownTimer(Constants.SNOOZ_REST_TIME.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {

                }

                override fun onFinish() {

                    AudioPlayer().play(applicationContext, R.raw.charmingbells)
                    isStartSnoozRest = false
                    if (Utility.isApplicationSentToBackground(
                            applicationContext
                        )
                    ) {
                        val i = Intent(applicationContext, HomeActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        i.putExtra(Constants.IS_FOR_REST, true)
                        i.putExtra(
                            Constants.INTENT_LOCATION_KEY,
                            myLocationData
                        )
                        Utility.showNotification(
                            applicationContext,
                            getString(R.string.rest_reminder_title),
                            getString(R.string.rest_reminder_message),
                            i,
                            Constants.REST_NOTIFICATION_ID,
                            false,
                            true
                        )
                    } else {
                        RestReminderIntent.putExtra(
                            Constants.INTENT_LOCATION_KEY,
                            myLocationData
                        )
                        sendBroadcast(RestReminderIntent)
                    }
                }
            }
            isStartSnoozRest = true
            (cTimerSnoozRest as CountDownTimer).start()
        }
    }

    //cancel timer for User is IDLE
    fun cancelTimersnoozRest() {
        if (cTimerSnoozRest != null) {
            isStartSnoozRest = false
            cTimerSnoozRest!!.cancel()
        }
    }

    companion object {
        // Location Objects
        private const val TAG = "BOOMBOOMTESTGPS"
    }


    /**
     * get distance
     *
     * @param location
     * @return
     */

    private fun distanceBetween(location: Location): Double {
        /*var latitude = 0.0
        var longitude = 0.0
        if (model.getLatitude().length() > 0 && model.getLongitude().length() > 0) {
            latitude = model.getLatitude().toDouble()
            longitude = model.getLongitude().toDouble()
        }*/
        val latLngB = LatLng(Constants.GEOFANCE_LAT.toDouble(), Constants.GEOFANCE_LONG.toDouble())
        val To = Location("CondigneeLocation")
        To.latitude = latLngB.latitude
        To.latitude = latLngB.longitude
        return distance(location.latitude, location.longitude, latLngB.latitude, latLngB.longitude)*1000
    }


    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = (sin(deg2rad(lat1))
                * sin(deg2rad(lat2))
                + (cos(deg2rad(lat1))
                * cos(deg2rad(lat2))
                * cos(deg2rad(theta))))
        dist = acos(dist)
        dist = rad2deg(dist)
        dist *= 60 * 1.1515
        return DecimalFormat("##.##").format(dist).toDouble()
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }
}



