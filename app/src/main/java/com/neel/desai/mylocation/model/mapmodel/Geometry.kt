package com.neel.desai.mylocation.model.mapmodel

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Geometry {


    @SerializedName("location")
    @Expose
     val location: Location? = null

    @SerializedName("viewport")
    @Expose
     val viewport: Viewport? = null
}