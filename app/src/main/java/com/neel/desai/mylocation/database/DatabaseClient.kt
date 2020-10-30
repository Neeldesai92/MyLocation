package com.neel.desai.mylocation.database

import android.content.Context
import androidx.room.Room
import com.neel.desai.workdemo.database.AppDatabase

class DatabaseClient {
    lateinit var ctx: Context

    private lateinit var appDatabase: AppDatabase

    constructor(ctx: Context) {
        this.ctx = ctx
        appDatabase =
            Room.databaseBuilder(this.ctx!!, AppDatabase::class.java, "Demo")
                .allowMainThreadQueries()
                .build();

    }

    companion object {
        lateinit var  mInstance: DatabaseClient

        @Synchronized
        fun getInstance(mCtx: Context): DatabaseClient {
            if(!this::mInstance.isInitialized){

                mInstance = DatabaseClient(mCtx)
            }
            return mInstance
        }
    }

    fun getAppDatabase(): AppDatabase {
        return appDatabase
    }
}