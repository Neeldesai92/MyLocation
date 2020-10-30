package com.neel.desai.mylocation.model.mapmodel

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MapData {

    @SerializedName("html_attributions")
    @Expose
    private val htmlAttributions: List<Any>? = null

    @SerializedName("next_page_token")
    @Expose
    private val nextPageToken: String? = null

    @SerializedName("results")
    @Expose
    val results: List<Result>? = null

    @SerializedName("status")
    @Expose
    private val status: String? = null
}