package com.samriddha.googlemapsandgoogledirectionsapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.samriddha.googlemapsandgoogledirectionsapp.Constants.ERROR_DIALOG_REQUEST
import com.samriddha.googlemapsandgoogledirectionsapp.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import com.samriddha.googlemapsandgoogledirectionsapp.Constants.PERMISSIONS_REQUEST_ENABLE_GPS
import com.samriddha.googlemapsandgoogledirectionsapp.R
import com.samriddha.googlemapsandgoogledirectionsapp.adapters.ChatroomRecyclerAdapter
import com.samriddha.googlemapsandgoogledirectionsapp.adapters.ChatroomRecyclerAdapter.ChatroomRecyclerClickListener
import com.samriddha.googlemapsandgoogledirectionsapp.models.Chatroom
import com.samriddha.googlemapsandgoogledirectionsapp.models.User
import com.samriddha.googlemapsandgoogledirectionsapp.models.UserLocation
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity(), View.OnClickListener, ChatroomRecyclerClickListener {

    private val TAG = "MainActivity"

    //widgets
    private var mProgressBar: ProgressBar? = null

    //vars
    private val mChatrooms = ArrayList<Chatroom>()
    private val mChatroomIds: MutableSet<String> = HashSet()
    private var mChatroomRecyclerAdapter: ChatroomRecyclerAdapter? = null
    private var mChatroomRecyclerView: RecyclerView? = null
    private var mChatroomEventListener: ListenerRegistration? = null
    private var mDb: FirebaseFirestore? = null
    private var userLocation: UserLocation? = null

    /*This techneque uses google play services api for getting location.
    * This method will work if google play service is install in the devices.
    * This method will not work say in China where play service doesn't exist.
    * */
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mProgressBar = findViewById(R.id.progressBar)
        mChatroomRecyclerView = findViewById(R.id.chatrooms_recycler_view)
        findViewById<View>(R.id.fab_create_chatroom).setOnClickListener(this)

        mDb = FirebaseFirestore.getInstance()
        initSupportActionBar()
        initChatroomRecyclerView()

    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResumed")
        if (checkAllRequirementsForMap()) {
            getChatrooms()
            insertUserLocationToDb()
        }
    }

    private fun insertUserLocationToDb(){
        lifecycleScope.launch {
            if (userLocation==null){

                val user = getUserDetailsFromFireStoreDb()
                Log.d(TAG, "User details:${user}")
                val geoPointLocation = getLastLocation()
                Log.d(TAG, "User last location:$geoPointLocation")

                if (user != null && geoPointLocation != null) {
                    userLocation = UserLocation()
                    userLocation?.user = user
                    userLocation?.geoPoint = geoPointLocation
                    saveUserLocationToDb(userLocation!!)
                }

            }
        }
    }

    private fun saveUserLocationToDb(userLocation: UserLocation) {
        val locationRef = mDb?.collection(getString(R.string.collection_user_locations))
            ?.document(FirebaseAuth.getInstance().uid!!)

        locationRef
            ?.set(userLocation)
            ?.addOnSuccessListener {
                Log.d(TAG, "user location added successfully to fire store db:$userLocation")
            }
            ?.addOnFailureListener {
                Log.d(TAG, "Failed to add user location")
            }

    }

    private suspend fun getUserDetailsFromFireStoreDb(): User? {
        var user: User? = null
        val docReference = mDb
            ?.collection(getString(R.string.collection_users))
            ?.document(FirebaseAuth.getInstance().uid!!)

        suspendCoroutine<User?> { continuation ->
            docReference
                ?.get()
                ?.addOnSuccessListener {
                    Log.d(TAG, "Got the user details")
                    user = it.toObject(User::class.java)!!
                    continuation.resume(user!!)
                }
                ?.addOnFailureListener {
                    Log.d(TAG, "failed to get user details")
                    continuation.resume(null)
                }
        }

        return user
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): GeoPoint? {
        var geoPoint: GeoPoint? = null

        suspendCoroutine<GeoPoint?> { continuation ->

            val currentLocationTask = fusedLocationClient.lastLocation
            currentLocationTask.addOnSuccessListener {

                if (it == null) {
                    Log.d(TAG, "last location is null start location update")
                    continuation.resume(null)
                } else {
                    Log.d(TAG, "Last location ${it.latitude}||${it.longitude}")
                    geoPoint = GeoPoint(it.latitude, it.longitude)
                    Log.d(
                        TAG,
                        "Last location geopoint ${geoPoint!!.latitude}||${geoPoint!!.longitude}"
                    )
                    continuation.resume(geoPoint)
                }

            }.addOnFailureListener {
                val exception = it
                Log.d(TAG, "exception $exception")
                continuation.resume(null)
            }
        }
        return geoPoint
    }

    private fun checkAllRequirementsForMap(): Boolean {
        /*
         * First check if the device has google play services installed.
         * Then check if user has enabled the gps.
         * Then check if user has given location permission
         * */
        if (isPlayServiceAvailable()) {
            if (isGpsEnabled()) {
                if (hasLocationPermission()) {
                    return true
                } else {
                    askLocationPermission()
                }
            }
        }
        return false
    }

    private fun isPlayServiceAvailable(): Boolean {

        Log.d(TAG, "isServicesOK: checking google services version")
        val available =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this@MainActivity)
        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working")
            return true
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it")
            val dialog = GoogleApiAvailability.getInstance()
                .getErrorDialog(this@MainActivity, available, ERROR_DIALOG_REQUEST)
            dialog.show()
        } else {
            Toast.makeText(this, "Google play service not available", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun isGpsEnabled(): Boolean {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            return false
        }
        return true
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog: DialogInterface?, id: Int ->
                val enableGpsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(enableGpsIntent)
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: called.")
        /*
         * This part can be used to check if the user enabled gps in the settings or not and take
         * action accordingly but we don't need it here as we are check for the gps inside onResume.
         * */if (requestCode == RESULT_OK) {
            when (requestCode) {
                PERMISSIONS_REQUEST_ENABLE_GPS -> {
                    if (checkAllRequirementsForMap()) {
                        getChatrooms()
                    }
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        return (ContextCompat.checkSelfPermission(
            this.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun askLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "Permission granted")
                    getChatrooms()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Please grant location permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun initSupportActionBar() {
        title = "Chatrooms"
    }

    private fun initChatroomRecyclerView() {
        mChatroomRecyclerAdapter = ChatroomRecyclerAdapter(mChatrooms, this)
        mChatroomRecyclerView!!.adapter = mChatroomRecyclerAdapter
        mChatroomRecyclerView!!.layoutManager = LinearLayoutManager(this)
    }

    private fun getChatrooms() {
        val settings = FirebaseFirestoreSettings.Builder()
            .build()
        mDb!!.firestoreSettings = settings
        val chatroomsCollection = mDb!!
            .collection(getString(R.string.collection_chatrooms))
        mChatroomEventListener =
            chatroomsCollection.addSnapshotListener(EventListener { queryDocumentSnapshots, e ->
                Log.d(TAG, "onEvent: called.")
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e)
                    return@EventListener
                }
                if (queryDocumentSnapshots != null) {
                    for (doc in queryDocumentSnapshots) {
                        val chatroom = doc.toObject(Chatroom::class.java)
                        if (!mChatroomIds.contains(chatroom.chatroom_id)) {
                            mChatroomIds.add(chatroom.chatroom_id)
                            mChatrooms.add(chatroom)
                        }
                    }
                    Log.d(TAG, "onEvent: number of chatrooms: " + mChatrooms.size)
                    mChatroomRecyclerAdapter!!.notifyDataSetChanged()
                }
            })

    }

    private fun buildNewChatroom(chatroomName: String) {
        val chatroom = Chatroom()
        chatroom.title = chatroomName
        val settings = FirebaseFirestoreSettings.Builder()
            .build()
        mDb!!.firestoreSettings = settings
        val newChatroomRef = mDb!!
            .collection(getString(R.string.collection_chatrooms))
            .document()
        chatroom.chatroom_id = newChatroomRef.id
        newChatroomRef.set(chatroom).addOnCompleteListener { task ->
            hideDialog()
            if (task.isSuccessful) {
                navChatroomActivity(chatroom)
            } else {
                val parentLayout = findViewById<View>(android.R.id.content)
                Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun navChatroomActivity(chatroom: Chatroom) {
        val intent = Intent(this@MainActivity, ChatroomActivity::class.java)
        intent.putExtra(getString(R.string.intent_chatroom), chatroom)
        startActivity(intent)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.fab_create_chatroom -> {
                newChatroomDialog()
            }
        }
    }

    private fun newChatroomDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter a chatroom name")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("CREATE") { dialog, which ->
            if (input.text.toString() != "") {
                buildNewChatroom(input.text.toString())
            } else {
                Toast.makeText(this@MainActivity, "Enter a chatroom name", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mChatroomEventListener != null) {
            mChatroomEventListener!!.remove()
        }
    }


    override fun onChatroomSelected(position: Int) {
        navChatroomActivity(mChatrooms[position])
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_out -> {
                signOut()
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun showDialog() {
        mProgressBar!!.visibility = View.VISIBLE
    }

    private fun hideDialog() {
        mProgressBar!!.visibility = View.GONE
    }

}