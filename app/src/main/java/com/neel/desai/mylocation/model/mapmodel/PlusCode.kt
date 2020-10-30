package com.neel.desai.mylocation.model.mapmodel

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PlusCode {

    @SerializedName("compound_code")
    @Expose
    private val compoundCode: String? = null

    @SerializedName("global_code")
    @Expose
    private val globalCode: String? = null
}