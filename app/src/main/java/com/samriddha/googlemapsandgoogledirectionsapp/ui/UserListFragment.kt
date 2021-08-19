package com.samriddha.googlemapsandgoogledirectionsapp.ui

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.MapView
import com.samriddha.googlemapsandgoogledirectionsapp.adapters.UserRecyclerAdapter
import android.os.Bundle
import com.samriddha.googlemapsandgoogledirectionsapp.R
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.samriddha.googlemapsandgoogledirectionsapp.Constants.MAP_VIEW_BUNDLE_KEY
import com.samriddha.googlemapsandgoogledirectionsapp.models.User
import com.samriddha.googlemapsandgoogledirectionsapp.models.UserLocation
import timber.log.Timber
import java.util.ArrayList

class UserListFragment : Fragment(R.layout.fragment_user_list),OnMapReadyCallback {

    private val TAG = "UserListFragment"

    // google maps
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var mMapBoundary:LatLngBounds
    private lateinit var currentUserLocation:UserLocation

    //widgets
    private var mUserListRecyclerView: RecyclerView? = null

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

        findAndSetTheCurrentUser()

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
        // set my location enable to user's current location
        map.isMyLocationEnabled = true

        // initialise google map object when the map is ready
        googleMap = map

        setCameraView()

        addMarkersToUsersLocation()
    }

    private fun addMarkersToUsersLocation() {
        mUserLocationList.forEach {
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(it.geoPoint?.latitude!!,it.geoPoint?.longitude!!))
                    .title(it.user?.username)
            )
        }
    }

    private fun setCameraView(){
        //setCameraToCurrentUsersLocation()
        setCameraToCenterOfAllUserLocation()
    }

    private fun setCameraToCenterOfAllUserLocation() {

        /*
        * This will zoom the map to the a center location of all the users.
        * */
        val mMapBoundaryBuilder = LatLngBounds.builder()
        mUserLocationList.forEach {
            mMapBoundaryBuilder.include(LatLng(it.geoPoint?.latitude!!,it.geoPoint?.longitude!!))
        }
        mMapBoundary = mMapBoundaryBuilder.build()

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mMapBoundary.center, 12f))
        //googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary,0))
    }

    private fun setCameraToCurrentUsersLocation(){
        /*
        * This will zoom the map to the current users location.
        * We are using current user's lat and lang and adding 0.1 so we can get
        * a little zoom out view with the current user's location.
        * */

        val bottomBoundary = currentUserLocation.geoPoint?.latitude?.minus(0.1)
        val leftBoundary = currentUserLocation.geoPoint?.longitude?.minus(0.1)
        val topBoundary = currentUserLocation.geoPoint?.latitude?.plus(0.1)
        val rightBoundary = currentUserLocation.geoPoint?.longitude?.plus(0.1)

        mMapBoundary = LatLngBounds(
            LatLng(bottomBoundary!!,leftBoundary!!),
            LatLng(topBoundary!!,rightBoundary!!)
        )

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary,0))
    }

    private fun initUserListRecyclerView() {
        mUserRecyclerAdapter = UserRecyclerAdapter(mUserList)
        mUserListRecyclerView!!.adapter = mUserRecyclerAdapter
        mUserListRecyclerView!!.layoutManager = LinearLayoutManager(activity)
    }

    private fun findAndSetTheCurrentUser() {
        mUserLocationList.forEach {

            /*
            * Loop through the all chatroom user's location list and match with
            * FirebaseAuth current uid to find out who is the current user(using the device)
            * among all the chat room's user list
            * */
            if (it.user?.user_id==FirebaseAuth.getInstance().uid){
                currentUserLocation = it
                Timber.d("Found the current user ${it.user?.username}")
            }
        }
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