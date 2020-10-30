package com.neel.desai.mylocation.model.mapmodel

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Viewport {

    @SerializedName("northeast")
    @Expose
    private val northeast: Northeast? = null

    @SerializedName("southwest")
    @Expose
    private val southwest: Southwest? = null
}