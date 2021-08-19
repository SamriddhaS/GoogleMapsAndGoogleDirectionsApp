package com.samriddha.googlemapsandgoogledirectionsapp.models

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class ClusterMarker(
    private var position:LatLng,
    private var snippet:String,
    var user:User,
    var markerIcon:Int,
    private var title:String
    ):ClusterItem {
    override fun getPosition(): LatLng {
        return position
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String {
        return snippet
    }
}
