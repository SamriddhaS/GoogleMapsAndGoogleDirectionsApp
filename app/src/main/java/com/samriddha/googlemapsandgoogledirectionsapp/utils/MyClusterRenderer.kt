package com.samriddha.googlemapsandgoogledirectionsapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.samriddha.googlemapsandgoogledirectionsapp.R
import com.samriddha.googlemapsandgoogledirectionsapp.models.ClusterMarker

class MyClusterRenderer(
    context: Context?,
    map: GoogleMap?,
    clusterManager: ClusterManager<ClusterMarker>?
) : DefaultClusterRenderer<ClusterMarker>(context, map, clusterManager) {


    private val iconGenerator = IconGenerator(context?.applicationContext)
    private val imageView: ImageView = ImageView(context?.applicationContext)
    private val markerWidth  = context?.resources?.getDimension(R.dimen.custom_marker_image)?.toInt()
    private val markerHeight  = context?.resources?.getDimension(R.dimen.custom_marker_image)?.toInt()

    init {
        val padding = context?.resources?.getDimension(R.dimen.custom_marker_padding)?.toInt()
        imageView.layoutParams = ViewGroup.LayoutParams(markerWidth!!,markerHeight!!)
        imageView.setPadding(padding!!,padding,padding,padding)
        iconGenerator.setContentView(imageView)
    }

    override fun onBeforeClusterItemRendered(item: ClusterMarker, markerOptions: MarkerOptions) {
        imageView.setImageResource(item.markerIcon)
        val icon = iconGenerator.makeIcon()
        markerOptions
            .icon(BitmapDescriptorFactory.fromBitmap(icon))
            .title(item.title)
            .snippet(item.snippet)
    }

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterMarker>): Boolean {
        return false
    }
}