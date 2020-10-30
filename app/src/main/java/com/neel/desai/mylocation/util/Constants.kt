package com.neel.desai.mylocation.util

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.model.ReasonData
import java.util.*


class Constants {
    companion object {


        const val GEOFANCE_LAT =  20.593139
        const val GEOFANCE_LONG =  72.932822
        const val DISTANCE =1000
        const val IS_GEOFANCE_IN = "is_geofance_in"
        const val IS_GEOFANCE_OUT = "is_geofance_out"
        const val IS_GEOFANCE_IN_NOTIFFY = "is_geofance_in_notiffy"
        const val IS_GEOFANCE_OUT_NOTIFFY = "is_geofance_out_notiffy"

        const val LOCATION_CHNGAE = "in.webxpress.drivexpress.LocationChage"
        const val INTERVAL_PUNCH = "in.webxpress.drivexpress.countdown_br"
        const val SERVICE_STOP = "in.webxpress.drivexpress.stopservice"
        const val REST_REMINDER_BRODCAST = "in.webxpress.drivexpress.REST_REMINDER_BRODCAST"
        const val IDLE_REMINDER_BRODCAST = "in.webxpress.drivexpress.IDLE_REMINDER_BRODCAST"
        const val OVER_SPEED = "in.webxpress.drivexpress.OVER_SPEED "
        const val BROADCAST_REST_ACTION = "activity_intent"
        const val BROADCAST_REST_ACTION_CANCEl = "activity_intent_rest_cancel"
        const val DIFFRENT_BRAKE_SPEED = 20
        const val DIFFRENT_ACCELERATION_SPEED = 30
        const val OVER_SPEED_KM = 65
        const val OVER_SPEED_HIGH_KM = 80
        const val LAST_BRAKE_SPEED_KM = 40
        var REST_AFTER_IN_KM = 2000
        const val FASTEST_INTERVAL: Long = 2000
        const val PunchInterval = 30 * 60 * 1000
        const val REST_TIME = 5 * 60 * 1000
        const val EXCEED_REST_TIME = 5 * 60 * 1000
        const val IDLE_TIME = 2 * 60 * 1000
        const val OVER_SPEED_TIME = 20 * 1000
        const val SNOOZ_REST_TIME = 90000
        const val IDLE_NOTIFICATION_ID = 324589
        const val REST_NOTIFICATION_ID = 100014
        const val TOFAST_NOTIFICATION_ID = 100558
        const val HARSH_ACCELERATION_NOTIFICATION_ID = 7632412
        const val HARSH_BRAKE_NOTIFICATION_ID = 8951445
        const val GEOFANCE_IN = 456189
        const val GEOFANCE_OUT = 435199
        const val LOCATION_ACCURCY_NOTIFICATION_ID = 75698
        const val OVERSPEED_NOTIFICATION_ID = 10051
        const val FORGROUND_NOTIFICATION_ID = 12347
        const val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1002
        const val TARGET_ACCURACY = 25
        const val REST_COMPLETE_BEFORETIME_PERCENT = 80
        const val REST_START = "Rest Start"
        const val STOP = "STOP"
        const val IDLE = "IDLE"
        const val RUNNING = "RUNNING"
        const val IN_REST = "IN REST"
        const val HEARD_BRAKE = "Harsh braking"

        const val HARSH_ACCELERATION = "Harsh acceleration"
        const val OVER_LIMIT = "Over Limit"
        const val REST_END = "Rest end"
        const val REST_REMINDER = "Rest Reminder"
        const val IDLE_REMINDER = " Idle Reminder"
        const val CANCEL_ACTION = "activity_Cancel_rest"
        const val START_REST_ACTION = "activity_start_rest"
        const val INTENT_LOCATION_KEY = "LocationData"
        const val INTENT_WEATHER_KEY = "WEATHERDATA"
        const val INTENT_IS_REST_START_KEY = "IsRestStart"
        const val INTENT_ISUSERSTOPT_KEY = "Isuserstop"
        const val INTENT_DISTANCE_DIFFT_KEY = "DistanceDiff"
        const val INTENT_DISTANCE_REMAING_KEY = "distanceRemaing"
        const val INTENTDIFF_DATA_KEY = "diffData"
        const val DIALOG_FRGAMET_KEY = "dialog"
        const val REST_START_KEY = "start"
        const val REST_CANCEL_KEY = "cancel"
        const val IS_FOR_REST = "IsforRest"
        const val ISFOR_OVERSPEED = "IsforOverspeed"
        const val IS_IDLE_REMINDER = "IsIdleReminder"
        const val IS_REST_CANCEL = "IsRestCancel"
        const val OVERSPEED_LAT_KEY = "OverspeedLat"
        const val OVERSPEED_LONG_KEY = "OverspeedLong"
        const val DISTANCE_KEY = "Distance"
        const val OPEN_MAP_KEY = "isaccuracy"
        const val IS_REST_REMINDER = "IS_REST_REMINDER"
        const val IS_REST_REMINDER_DATA = "IS_REST_REMINDER_DATA"


        val idleReason: ArrayList<ReasonData>
            get() {
                val IdleReason: ArrayList<ReasonData> = ArrayList<ReasonData>()
                IdleReason.add(ReasonData("Stuck In Heavy Traffic", false))
                IdleReason.add(ReasonData("Vehicle Issue", false))
                IdleReason.add(ReasonData("Weather Issue", false))
                return IdleReason
            }

        val restReason: ArrayList<ReasonData>
            get() {
                val RestReason: ArrayList<ReasonData> = ArrayList<ReasonData>()
                RestReason.add(ReasonData("Don't Want To Rest", false))
                RestReason.add(ReasonData("Need To Complete trip", false))
                RestReason.add(ReasonData("Already Late for Delivery", false))
                return RestReason
            }

        fun getnearBy(context: Context): ArrayList<ReasonData> {
            val RestReason: ArrayList<ReasonData> = ArrayList<ReasonData>()
            RestReason.add(ReasonData(context.getString(R.string.label_food), false))
            RestReason.add(ReasonData(context.getString(R.string.label_fuel_nearby), false))
            RestReason.add(ReasonData(context.getString(R.string.label_hospital), false))
            RestReason.add(ReasonData(context.getString(R.string.label_truck_service), false))
            RestReason.add(ReasonData(context.getString(R.string.label_driver_friends), false))
            return RestReason
        }

        val BOUNDS_INDIA: LatLngBounds =
            LatLngBounds(LatLng(23.63936, 68.14712), LatLng(28.20453, 97.34466))
    }
}
