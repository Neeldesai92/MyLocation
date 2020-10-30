package com.neel.desai.mylocation.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
open class MyLocationData : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "Time")
    var time: String = ""

    @ColumnInfo(name = "Speed")
    var speed: String = ""

    @ColumnInfo(name = "Activity")
    var activity: String = ""

    @ColumnInfo(name = "Latitude")
    var latitude: String = ""

    @ColumnInfo(name = "Longitude")
    var longitude: String = ""

    @ColumnInfo(name = "confidence")
    var confidence: Int = 0

    @ColumnInfo(name = "mAltitude")
    var mAltitude: String = ""

    @ColumnInfo(name = "Messsage")
    var messsage: String = ""

    @ColumnInfo(name = "Accuracy")
    var Accuracy: String = ""

    @ColumnInfo(name = "isBrake")
    var brake: Int = 0

    @ColumnInfo(name = "isRestReminder")
    var restReminder: Int = 0

    @ColumnInfo(name = "isStill")
    var still: Int = 0

    @ColumnInfo(name = "isOverSpeed")
    var overSpeed: Int = 0

    @ColumnInfo(name = "isaccuracy")
    var isaccuracy: Int = 0

    @ColumnInfo(name = "Reason")
    var reason: String = ""

    @ColumnInfo(name = "NotificationID")
    var notificationID: Int = 0

    @ColumnInfo(name = "status")
    var status: String = ""

    @ColumnInfo(name = "Address")
    var address: String = ""

    var batteryStatus: Int = 0

    @ColumnInfo(name = "location_sync_status")
    var isLocation_sync_status: Boolean = false

    @ColumnInfo(name = "bearing")
    var bearing: Float = 0f


}