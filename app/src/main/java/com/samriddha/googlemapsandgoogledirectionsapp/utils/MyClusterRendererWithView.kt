package com.samriddha.googlemapsandgoogledirectionsapp.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.samriddha.googlemapsandgoogledirectionsapp.R
import com.samriddha.googlemapsandgoogledirectionsapp.models.ClusterMarker
import timber.log.Timber

class MyClusterRendererWithView(
    val context: Context,
    map: GoogleMap?,
    clusterManager: ClusterManager<ClusterMarker>?
) : DefaultClusterRenderer<ClusterMarker>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(item: ClusterMarker, markerOptions: MarkerOptions) {

        val view: View = View.inflate(context,R.layout.layout_custom_marker,null)
        val iconGenerator = IconGenerator(context.applicationContext)
        val markerWidth  = context.resources?.getDimension(R.dimen.custom_marker_view_width)?.toInt()
        val markerHeight  = context.resources?.getDimension(R.dimen.custom_marker_view_height)?.toInt()

        view.layoutParams = ViewGroup.LayoutParams(markerWidth!!,markerHeight!!)
        iconGenerator.setContentView(view)

        view.findViewById<TextView>(R.id.tv_name).text = item.user.username
        view.findViewById<TextView>(R.id.tv_email).text = item.user.email
        view.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.chef)

        markerOptions
            .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon()))
            .title(item.title)
            .snippet(item.snippet)
    }

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterMarker>): Boolean {
        return false
    }

    fun setUpdateMarker(clusterMarker: ClusterMarker){
        val marker = getMarker(clusterMarker)
        Timber.d("set update marker0 ${clusterMarker.title} ${clusterMarker.position}")
        marker?.let {
            it.position = clusterMarker.position
            Timber.d("set update marker ${it.title} ${it.position}")
        }
    }
}