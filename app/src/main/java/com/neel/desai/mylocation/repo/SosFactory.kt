package com.neel.desai.mylocation.repo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neel.desai.mylocation.viewmodel.SosViewModel


@Suppress("UNCHECKED_CAST")
class SosFactory(
    private val mcontext: Context

) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SosViewModel(mcontext) as T
    }

}