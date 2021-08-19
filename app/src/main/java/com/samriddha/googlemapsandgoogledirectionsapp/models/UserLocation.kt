package com.samriddha.googlemapsandgoogledirectionsapp.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class UserLocation (
    var geoPoint:GeoPoint?,
    @ServerTimestamp
    var timeStamp:Date?=null,
    var user:User?
    ):Parcelable{

    constructor(parcel: Parcel) : this()

    constructor():this(null,null,null)

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeParcelable(user,flags)
    }

    companion object CREATOR : Parcelable.Creator<UserLocation> {
        override fun createFromParcel(parcel: Parcel): UserLocation {
            return UserLocation(parcel)
        }

        override fun newArray(size: Int): Array<UserLocation?> {
            return arrayOfNulls(size)
        }
    }

}