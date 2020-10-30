package com.neel.desai.mylocation.database

import androidx.room.*
import com.neel.desai.mylocation.model.MyLocationData


@Dao
interface TaskDao {

    @Query("SELECT * FROM MyLocationData")
    fun GetAll(): List<MyLocationData>

    @Insert
    fun insert(task: MyLocationData)

    @Delete
    fun delete(task: MyLocationData)


    @Delete
    fun deleteAll(task: List<MyLocationData>?)

    @Update
    fun update(task: MyLocationData)

    @Query("SELECT * FROM MyLocationData Where  isaccuracy='1' AND  Activity =:name")
    fun getSearch(name: String): List<MyLocationData>?


    @Query("SELECT * FROM MyLocationData Where isaccuracy =:value")
    fun getaccuracy(value: String): List<MyLocationData>?

    @Query("SELECT * FROM MyLocationData Where Activity !=:value AND isaccuracy = 1")
    fun getDatawithoutRest(value: String): List<MyLocationData>?


    @Query("SELECT * FROM MyLocationData Where isaccuracy =:value ")
    fun getspeedDistance(value: String): List<MyLocationData>?


    @Query("SELECT * FROM MyLocationData Where isaccuracy =:value AND Activity in ('Rest Start','Rest end')")
    fun getRestData(value: String): List<MyLocationData>?


    @Query("SELECT * FROM MyLocationData Where isaccuracy =:value AND status in ('IDLE','RUNNING')")
    fun getActivityData(value: String): List<MyLocationData>?


    @Query("SELECT * FROM MyLocationData Where isaccuracy =:value AND status='RUNNING'  AND Activity not in ('Rest Start','IN REST','Rest end')")
    fun getDataTravel(value: String): List<MyLocationData>?


    @Query("SELECT * FROM MyLocationData Where isaccuracy ='1' AND Activity ='Rest Reminder'  And Id>:id  ORDER BY id DESC LIMIT 1")
    fun getLastRestReminder(id: Int): List<MyLocationData>?


    @Query("SELECT * FROM MyLocationData Where isaccuracy ='1'  ORDER BY id DESC LIMIT 1")
    fun lastRecord(): MyLocationData

    @Query("SELECT avg(Speed) FROM MyLocationData Where isaccuracy ='1' AND status='RUNNING' AND Activity not in ('Rest Start','IN REST','Rest end')")
    fun speedavg(): Float


    @Query("SELECT * FROM MyLocationData Where isaccuracy ='1' AND Activity ='Rest Start' ORDER BY id DESC LIMIT 1")
    fun lastRestStart(): MyLocationData


    @Query("SELECT * FROM MyLocationData Where isaccuracy ='1' AND Activity ='Rest end' ORDER BY id DESC LIMIT 1")
    fun lastRestEnd(): MyLocationData


    @Query("SELECT * FROM MyLocationData Where isaccuracy ='1'  And Id>=:id ")
    fun getAllAfterRestEnd(id: Int): List<MyLocationData>?


    @Update
    fun updateAll(task: List<MyLocationData>)

    @Query("SELECT * FROM MyLocationData Where location_sync_status= :syncFlag ")
    fun getAllLocationNotSync(syncFlag: Boolean): List<MyLocationData>?

    @Query("SELECT * FROM MyLocationData Where isaccuracy ='1'  ORDER BY id DESC LIMIT 1")
    fun getLastRecord(): MyLocationData

    @Query("SELECT * FROM MyLocationData Where isaccuracy ='1'  ORDER BY id DESC LIMIT 1")
    fun getLastRecordNew(): List<MyLocationData>


    @Query("SELECT avg(Speed) FROM MyLocationData Where isaccuracy ='1' AND status='RUNNING' AND Activity not in ('Rest Start','IN REST','Rest end')")
    fun getSpeedavg(): Float

    @Query("SELECT * FROM MyLocationData Where isaccuracy ='1' AND Activity ='Rest end' ORDER BY id DESC LIMIT 1")
    fun getLastRestEnd(): MyLocationData?

    @Query("SELECT * FROM MyLocationData Where Isaccuracy ='1' AND Activity ='Rest Start' ORDER BY id DESC LIMIT 1")
    fun getLastRestStart(): MyLocationData?
}