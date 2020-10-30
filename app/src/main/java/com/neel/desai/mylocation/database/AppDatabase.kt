package com.neel.desai.workdemo.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neel.desai.mylocation.database.TaskDao
import com.neel.desai.mylocation.model.MyLocationData

@Database(entities = [MyLocationData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}