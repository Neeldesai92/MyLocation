package com.neel.desai.mylocation.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class SharedPreference() {


    companion object {
        private lateinit var PREF_APP: SharedPreferences
        private lateinit var editor: SharedPreferences.Editor


        val SP_APPLICATION_DETAILS = "Application_Details"
        val SP_STRING_DEFAULT_VALUE = ""
        val SP_STRING_DEFAULT_VALUE_FOR_DATETIME = "01 Jan 1990 00:00"
        val SP_BOOLEAN_DEFAULT_VALUE = false
        val SP_INT_DEFAULT_VALUE = 1
        val SP_FLOAT_DEFAULT_VALUE = 0.0.toFloat()
        val SP_LONG_DEFAULT_VALUE = 0.0.toLong()

        // Encrypted
        val PREF_APP_KEY_USER_ID = "UserId"
        val PREF_APP_KEY_PASSWORD = "Password"
        val PREF_APP_KEY_EVENt_ID = "EventId"
        val PREF_APP_KEY_USER_NAME = "UserName"


        private fun SharedPreference() {} //prevent creating multiple instances by making the constructor private
        fun getInstance(context: Context) {
            if (!this::PREF_APP.isInitialized) {
                PREF_APP =
                    context.getSharedPreferences(SP_APPLICATION_DETAILS, Activity.MODE_PRIVATE)
                editor = PREF_APP.edit()
            }
        }
        //The context passed into the getInstance should be application level context.

        fun clearSP(context: Context) {
            if (this::PREF_APP.isInitialized) {
                PREF_APP =
                    context.getSharedPreferences(SP_APPLICATION_DETAILS, Activity.MODE_PRIVATE)
                editor = PREF_APP.edit()
                editor.clear()
                editor.commit()
            }
        }

        fun getStringValue(key: String?): String? {
            return PREF_APP!!.getString(key, SP_STRING_DEFAULT_VALUE)
        }

        fun getStringValueForLastModifyDateTime(key: String?): String? {
            return PREF_APP!!.getString(key, SP_STRING_DEFAULT_VALUE_FOR_DATETIME)
        }

        fun setStringValue(key: String?, value: String?) {
            editor!!.putString(key, value)
            editor!!.commit()
        }

        fun getDecryptedStringValue(key: String?): String? {
            var strDecryptedValue = ""
            try {
                val strDecryptValue =
                    PREF_APP!!.getString(key, defaultEncryptedValueForString())
                strDecryptedValue = Security.decrypt(
                    strDecryptValue,
                    Security.passwordKey
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return strDecryptedValue
        }

        fun getDecryptedStringValueOfTenantID(key: String?): String? {
            var strDecryptedValue = "0"
            try {
                val strDecryptValue =
                    PREF_APP!!.getString(key, defaultEncryptedValueForTenantID())
                strDecryptedValue = Security.decrypt(
                    strDecryptValue,
                    Security.passwordKey
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return strDecryptedValue
        }

        fun getDecryptedStringValueOfLatLong(key: String?): String? {
            var strDecryptedValue = ""
            try {
                val strDecryptValue =
                    PREF_APP!!.getString(key, defaultEncryptedValueForLatLong())
                strDecryptedValue = Security.decrypt(
                    strDecryptValue,
                    Security.passwordKey
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return strDecryptedValue
        }

        fun setEncryptedStringValue(key: String?, value: String) {
            try {
                val encryptedValue: String = Security.encrypt(
                    value,
                    Security.passwordKey
                )
                editor!!.putString(key, encryptedValue)
                editor!!.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getBooleanValue(key: String?): Boolean {
            return PREF_APP!!.getBoolean(key, SP_BOOLEAN_DEFAULT_VALUE)
        }

        fun setBooleanValue(key: String?, value: Boolean) {
            editor!!.putBoolean(key, value)
            editor!!.commit()
        }

        fun getIntValue(key: String?): Int? {
            return PREF_APP!!.getInt(key, SP_INT_DEFAULT_VALUE)
        }

        fun setIntValue(key: String?, value: Int?) {
            editor!!.putInt(key, value!!)
            editor!!.commit()
        }

        fun getFloatValue(key: String?): Float {
            return PREF_APP!!.getFloat(key, SP_FLOAT_DEFAULT_VALUE)
        }

        fun setFloatValue(key: String?, value: Float) {
            editor!!.putFloat(key, value)
            editor!!.commit()
        }

        fun getLongValue(key: String?): Long {
            return PREF_APP!!.getLong(key, SP_LONG_DEFAULT_VALUE)
        }

        fun setLongValue(key: String?, value: Long) {
            editor!!.putFloat(key, value.toFloat())
            editor!!.commit()
        }

        fun removeValueFromSP(key: String?) {
            editor!!.remove(key)
            editor!!.commit()
        }

        private fun defaultEncryptedValueForString(): String? {
            var encryptedValue = ""
            try {
                encryptedValue = Security.encrypt(
                    "",
                    Security.passwordKey
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return encryptedValue
        }

        private fun defaultEncryptedValueForTenantID(): String? {
            var encryptedValue = "0"
            try {
                encryptedValue = Security.encrypt(
                    "0",
                    Security.passwordKey
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return encryptedValue
        }

        private fun defaultEncryptedValueForLatLong(): String? {
            var encryptedValue = "0.00"
            try {
                encryptedValue = Security.encrypt(
                    "0.00",
                    Security.passwordKey
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return encryptedValue
        }
    }
}