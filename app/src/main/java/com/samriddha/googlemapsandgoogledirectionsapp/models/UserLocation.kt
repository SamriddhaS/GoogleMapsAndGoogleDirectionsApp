package com.samriddha.googlemapsandgoogledirectionsapp.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class UserLocation (
    var geoPoint:GeoPoint?,
    @ServerTimestamp
    var timeStamp:Date?=null,
    var user:User?
    ){
    constructor():this(null,null,null)
}