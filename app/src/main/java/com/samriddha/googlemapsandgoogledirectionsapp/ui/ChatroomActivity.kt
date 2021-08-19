package com.samriddha.googlemapsandgoogledirectionsapp.ui

import com.samriddha.googlemapsandgoogledirectionsapp.ui.UserListFragment.Companion.newInstance
import androidx.appcompat.app.AppCompatActivity
import com.samriddha.googlemapsandgoogledirectionsapp.models.Chatroom
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.samriddha.googlemapsandgoogledirectionsapp.adapters.ChatMessageRecyclerAdapter
import com.samriddha.googlemapsandgoogledirectionsapp.models.ChatMessage
import com.samriddha.googlemapsandgoogledirectionsapp.models.UserLocation
import com.samriddha.googlemapsandgoogledirectionsapp.ui.UserListFragment
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.samriddha.googlemapsandgoogledirectionsapp.R
import timber.log.Timber
import androidx.recyclerview.widget.LinearLayoutManager
import com.samriddha.googlemapsandgoogledirectionsapp.UserClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.samriddha.googlemapsandgoogledirectionsapp.models.User
import java.util.ArrayList
import java.util.HashSet

class ChatroomActivity : AppCompatActivity(), View.OnClickListener {
    //widgets
    private var mChatroom: Chatroom? = null
    private var mMessage: EditText? = null

    //vars
    private var mChatMessageEventListener: ListenerRegistration? = null
    private var mUserListEventListener: ListenerRegistration? = null
    private var mChatMessageRecyclerView: RecyclerView? = null
    private var mChatMessageRecyclerAdapter: ChatMessageRecyclerAdapter? = null
    private var mDb: FirebaseFirestore? = null
    private val mMessages = ArrayList<ChatMessage>()
    private val mMessageIds: MutableSet<String> = HashSet()
    private var mUserList = ArrayList<User>()
    private val mUserLocationList = ArrayList<UserLocation>()
    private val mUserListFragment: UserListFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatroom)
        mMessage = findViewById(R.id.input_message)
        mChatMessageRecyclerView = findViewById(R.id.chatmessage_recycler_view)
        findViewById<View>(R.id.checkmark).setOnClickListener(this)
        mDb = FirebaseFirestore.getInstance()
        incomingIntent
        initChatroomRecyclerView()
        chatroomUsers()
    }

    private fun chatMessages(){
            val messagesRef = mDb
                ?.collection(getString(R.string.collection_chatrooms))
                ?.document(mChatroom!!.chatroom_id)
                ?.collection(getString(R.string.collection_chat_messages))
            mChatMessageEventListener = messagesRef
                ?.orderBy("timestamp", Query.Direction.ASCENDING)
                ?.addSnapshotListener(EventListener { queryDocumentSnapshots, e ->
                    if (e != null) {
                        Timber.d("onEvent: Listen failed.")
                        return@EventListener
                    }
                    if (queryDocumentSnapshots != null) {
                        for (doc in queryDocumentSnapshots) {
                            val message = doc.toObject(ChatMessage::class.java)
                            if (!mMessageIds.contains(message.message_id)) {
                                mMessageIds.add(message.message_id)
                                mMessages.add(message)
                                mChatMessageRecyclerView!!.smoothScrollToPosition(mMessages.size - 1)
                            }
                        }
                        mChatMessageRecyclerAdapter!!.notifyDataSetChanged()
                    }
                })
        }

    // Clear the list and add all the users again
    private fun chatroomUsers() {
            val usersRef = mDb
                ?.collection(getString(R.string.collection_chatrooms))
                ?.document(mChatroom!!.chatroom_id)
                ?.collection(getString(R.string.collection_chatroom_user_list))
            mUserListEventListener = usersRef
                ?.addSnapshotListener { queryDocumentSnapshots: QuerySnapshot?, e: FirebaseFirestoreException? ->
                    if (e != null) {
                        Timber.d("onEvent: Listen failed.")
                        return@addSnapshotListener
                    }
                    if (queryDocumentSnapshots != null) {

                        // Clear the list and add all the users again
                        mUserList.clear()
                        mUserList = ArrayList()
                        for (doc in queryDocumentSnapshots) {
                            val user = doc.toObject(
                                User::class.java
                            )
                            mUserList.add(user)
                            getChatRoomUserLocationFromDb(user)
                        }
                        Timber.d("onEvent: user list size: " + mUserList.size)
                    }
                }
        }

    private fun getChatRoomUserLocationFromDb(user:User){
        val locationRef = mDb
            ?.collection(getString(R.string.collection_user_locations))
            ?.document(user.user_id)

        locationRef
            ?.get()
            ?.addOnSuccessListener { docSnapshot ->
                val userLocation = docSnapshot.toObject(UserLocation::class.java)
                userLocation?.let {
                    Timber.d("Got user location: ${it.user?.username} lat:${it.geoPoint?.latitude} lng:${it.geoPoint?.longitude}")
                    mUserLocationList.add(it)
                } ?: kotlin.run {
                    Timber.d("On Success but user location is null $docSnapshot")
                }
            }
            ?.addOnFailureListener {
                Timber.d("Failed to get user location from fierstore ${it.message}")
            }
    }

    private fun initChatroomRecyclerView() {
        mChatMessageRecyclerAdapter = ChatMessageRecyclerAdapter(mMessages, ArrayList(), this)
        mChatMessageRecyclerView!!.adapter = mChatMessageRecyclerAdapter
        mChatMessageRecyclerView!!.layoutManager = LinearLayoutManager(this)
        mChatMessageRecyclerView!!.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                mChatMessageRecyclerView!!.postDelayed({
                    if (mMessages.size > 0) {
                        mChatMessageRecyclerView!!.smoothScrollToPosition(
                            mChatMessageRecyclerView!!.adapter!!.itemCount - 1
                        )
                    }
                }, 100)
            }
        }
    }

    private fun insertNewMessage() {
        var message = mMessage!!.text.toString()
        if (message != "") {
            message = message.replace(System.getProperty("line.separator").toRegex(), "")
            val newMessageDoc = mDb
                ?.collection(getString(R.string.collection_chatrooms))
                ?.document(mChatroom!!.chatroom_id)
                ?.collection(getString(R.string.collection_chat_messages))
                ?.document()
            val newChatMessage = ChatMessage()
            newChatMessage.message = message
            newChatMessage.message_id = newMessageDoc?.id
            val user = (applicationContext as UserClient).user
            Timber.d("insertNewMessage: retrieved user client: $user")
            newChatMessage.user = user
            newMessageDoc?.set(newChatMessage)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    clearMessage()
                } else {
                    val parentLayout = findViewById<View>(android.R.id.content)
                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun clearMessage() {
        mMessage!!.setText("")
    }

    private fun inflateUserListFragment() {
        hideSoftKeyboard()
        val fragment = newInstance()
        val bundle = Bundle()
        bundle.putParcelableArrayList(getString(R.string.intent_user_list), mUserList)
        bundle.putParcelableArrayList(getString(R.string.intent_user_locations), mUserLocationList)
        fragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up)
        transaction.replace(
            R.id.user_list_container,
            fragment,
            getString(R.string.fragment_user_list)
        )
        transaction.addToBackStack(getString(R.string.fragment_user_list))
        transaction.commit()
    }

    private fun hideSoftKeyboard() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    private val incomingIntent: Unit
        private get() {
            if (intent.hasExtra(getString(R.string.intent_chatroom))) {
                mChatroom = intent.getParcelableExtra(getString(R.string.intent_chatroom))
                setChatroomName()
                joinChatroom()
            }
        }

    private fun leaveChatroom() {
        val joinChatroomRef = mDb
            ?.collection(getString(R.string.collection_chatrooms))
            ?.document(mChatroom!!.chatroom_id)
            ?.collection(getString(R.string.collection_chatroom_user_list))
            ?.document(FirebaseAuth.getInstance().uid!!)
        joinChatroomRef?.delete()
    }

    private fun joinChatroom() {
        val joinChatroomRef = mDb
            ?.collection(getString(R.string.collection_chatrooms))
            ?.document(mChatroom!!.chatroom_id)
            ?.collection(getString(R.string.collection_chatroom_user_list))
            ?.document(FirebaseAuth.getInstance().uid!!)
        val user = (applicationContext as UserClient).user
        joinChatroomRef?.set(user) // Don't care about listening for completion.
    }

    private fun setChatroomName() {
        supportActionBar!!.title = mChatroom!!.title
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        chatMessages()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mChatMessageEventListener != null) {
            mChatMessageEventListener!!.remove()
        }
        if (mUserListEventListener != null) {
            mUserListEventListener!!.remove()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chatroom_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val fragment =
                    supportFragmentManager.findFragmentByTag(getString(R.string.fragment_user_list)) as UserListFragment?
                if (fragment != null) {
                    if (fragment.isVisible) {
                        supportFragmentManager.popBackStack()
                        return true
                    }
                }
                finish()
                true
            }
            R.id.action_chatroom_user_list -> {
                inflateUserListFragment()
                true
            }
            R.id.action_chatroom_leave -> {
                leaveChatroom()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.checkmark -> {
                insertNewMessage()
            }
        }
    }
}