package com.samriddha.googlemapsandgoogledirectionsapp.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.samriddha.googlemapsandgoogledirectionsapp.Constants.LOCATION_FASTEST_INTERVAL
import com.samriddha.googlemapsandgoogledirectionsapp.Constants.LOCATION_UPDATE_INTERVAL
import com.samriddha.googlemapsandgoogledirectionsapp.R
import com.samriddha.googlemapsandgoogledirectionsapp.UserClient
import com.samriddha.googlemapsandgoogledirectionsapp.models.UserLocation
import timber.log.Timber

class LocationService : Service() {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationLiveData = MutableLiveData<GeoPoint>()

    private val observer by lazy {
        Observer<GeoPoint> {
            // save location to db
            val userLocation = UserLocation()
            userLocation.geoPoint = it
            userLocation.user = (applicationContext as UserClient).user

            saveUserLocationToDb(userLocation)
        }
    }

    private val locationCallBack by lazy {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Timber.d("on location result")
                locationResult?.locations?.forEach { locations ->
                    Timber.d("got location $locations")
                    locations?.let {
                        locationLiveData.postValue(GeoPoint(it.latitude, it.longitude))
                    }
                }
            }
        }
    }

    private val locationRequest = LocationRequest.create().apply {
        interval = LOCATION_UPDATE_INTERVAL
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fastestInterval = LOCATION_FASTEST_INTERVAL
    }

    private val mDb by lazy {
        FirebaseFirestore.getInstance()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            showNotificationForApi26Above()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNotificationForApi26Above() {

        /*
        * From api level > 26 ,to run a service which uses gps we must show a notification to the user
        * otherwise the service will not work.
        * */

        val CHANNEL_ID = "notification_channel"
        val notificationChannel =
            NotificationChannel(CHANNEL_ID, "My Channel", NotificationManager.IMPORTANCE_DEFAULT)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            notificationChannel
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        notification.setContentText("GoogleMapsAndDirectionsApp")
        notification.setContentTitle("Running in the background")

        startForeground(1, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("on Start command")
        startLocationUpdates()
        startObserver()
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopLocationUpdates()
        removeObserver()
        return true
    }

    private fun saveUserLocationToDb(userLocation: UserLocation) {

        val locationRef = mDb
            .collection(getString(R.string.collection_user_locations))
            .document(FirebaseAuth.getInstance().uid!!)

        locationRef
            ?.set(userLocation)
            ?.addOnSuccessListener {
                Timber.d("user location added successfully to fire store db:$userLocation")
            }
            ?.addOnFailureListener {
                Timber.d("Failed to add user location")
            }

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() = fusedLocationClient.requestLocationUpdates(
        locationRequest,
        locationCallBack,
        Looper.getMainLooper()
    )

    private fun stopLocationUpdates() = fusedLocationClient.removeLocationUpdates(locationCallBack)

    private fun startObserver() = locationLiveData.observeForever(observer)

    private fun removeObserver() = locationLiveData.removeObserver(observer)
}