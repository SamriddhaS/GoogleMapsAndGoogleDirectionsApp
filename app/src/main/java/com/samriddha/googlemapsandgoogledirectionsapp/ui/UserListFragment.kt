package com.samriddha.googlemapsandgoogledirectionsapp.ui

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.MapView
import com.samriddha.googlemapsandgoogledirectionsapp.adapters.UserRecyclerAdapter
import android.os.Bundle
import com.samriddha.googlemapsandgoogledirectionsapp.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.samriddha.googlemapsandgoogledirectionsapp.Constants.MAP_VIEW_BUNDLE_KEY
import com.samriddha.googlemapsandgoogledirectionsapp.models.User
import com.samriddha.googlemapsandgoogledirectionsapp.models.UserLocation
import com.samriddha.googlemapsandgoogledirectionsapp.ui.UserListFragment
import timber.log.Timber
import java.util.ArrayList

class UserListFragment : Fragment(R.layout.fragment_user_list),OnMapReadyCallback {

    private val TAG = "UserListFragment"

    //widgets
    private var mUserListRecyclerView: RecyclerView? = null
    private lateinit var mapView: MapView

    //vars
    private var mUserList: ArrayList<User>? = ArrayList()
    private var mUserLocationList: ArrayList<UserLocation> = ArrayList()
    private var mUserRecyclerAdapter: UserRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUserList = requireArguments().getParcelableArrayList(getString(R.string.intent_user_list))
        mUserLocationList = requireArguments().getParcelableArrayList(getString(R.string.intent_user_locations))!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view)
        mapView = view.findViewById(R.id.user_list_map)

        initUserListRecyclerView()
        initGoogleMap(savedInstanceState)

        mUserLocationList.forEach {
            Timber.d("User Location ${it.user?.username} lat:${it.geoPoint?.latitude} lng:${it.geoPoint?.longitude}")
        }

    }

    private fun initGoogleMap(savedInstanceState: Bundle?){
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }

        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        map.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)).title("Marker"))
        map.isMyLocationEnabled = true
    }

    private fun initUserListRecyclerView() {
        mUserRecyclerAdapter = UserRecyclerAdapter(mUserList)
        mUserListRecyclerView!!.adapter = mUserRecyclerAdapter
        mUserListRecyclerView!!.layoutManager = LinearLayoutManager(activity)
    }

    companion object {
        @JvmStatic
        fun newInstance(): UserListFragment {
            return UserListFragment()
        }
    }

    ///////////// Need to override these methods if we are using map view /////////////////////////////
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}