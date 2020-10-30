package com.neel.desai.mylocation.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neel.desai.mylocation.model.UserInput

class LoginViewModel:ViewModel() {
    var EmailAddress = MutableLiveData<String>()
    var Password = MutableLiveData<String>()

    private var userMutableLiveData = MutableLiveData<UserInput>()

    fun getUser(): MutableLiveData<UserInput> {
        if (userMutableLiveData.value == null) {
            userMutableLiveData = MutableLiveData<UserInput>()
        }
        return userMutableLiveData
    }

    fun onClick() {
        Log.i("Data",EmailAddress?.value+ "  "+Password?.value)
        val loginUser = UserInput(EmailAddress?.value, Password?.value)

        userMutableLiveData.value=loginUser
    }
}