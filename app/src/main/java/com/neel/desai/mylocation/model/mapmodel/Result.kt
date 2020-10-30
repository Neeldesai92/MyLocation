package com.neel.desai.mylocation.model.mapmodel

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Result {

    @SerializedName("geometry")
    @Expose
     val geometry: Geometry? = null

    @SerializedName("icon")
    @Expose
     val icon: String? = null

    @SerializedName("id")
    @Expose
     val id: String? = null

    @SerializedName("name")
    @Expose
     val name: String? = null

    @SerializedName("opening_hours")
    @Expose
     val openingHours: OpeningHours? = null

    @SerializedName("photos")
    @Expose
     val photos: List<Photo>? = null

    @SerializedName("place_id")
    @Expose
     val placeId: String? = null

    @SerializedName("plus_code")
    @Expose
     val plusCode: PlusCode? = null

    @SerializedName("rating")
    @Expose
     val rating: Double? = null

    @SerializedName("reference")
    @Expose
     val reference: String? = null

    @SerializedName("scope")
    @Expose
     val scope: String? = null

    @SerializedName("types")
    @Expose
     val types: List<String>? = null

    @SerializedName("user_ratings_total")
    @Expose
     val userRatingsTotal: Int? = null

    @SerializedName("vicinity")
    @Expose
     val vicinity: String? = null

    @SerializedName("price_level")
    @Expose
     val priceLevel: Int? = null
}