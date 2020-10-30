package com.neel.desai.mylocation.model

class NearByData {

    var Latitude :Double = 0.0
    var Longitude :Double= 0.0
    var Name: String? = null
    var Rating: Double? = null
    var Address: String? = null
    var IsOpen :Boolean= false
    var Url: String? = null
    var Contactno: String? = null
    var Distance :Double= 0.0

    constructor(Latitude: Double, Longitude: Double, Name: String?, Rating: Double?, Address: String?, IsOpen: Boolean, Url: String?, Contactno: String?, Distance: Double) {
        this.Latitude = Latitude
        this.Longitude = Longitude
        this.Name = Name
        this.Rating = Rating
        this.Address = Address
        this.IsOpen = IsOpen
        this.Url = Url
        this.Contactno = Contactno
        this.Distance = Distance
    }
}