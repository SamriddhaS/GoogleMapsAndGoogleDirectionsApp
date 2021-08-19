package com.samriddha.googlemapsandgoogledirectionsapp;

import android.app.Application;
import com.samriddha.googlemapsandgoogledirectionsapp.models.User;
import timber.log.Timber;

public class UserClient extends Application {

    /*
    * This user object will get initialised inside main activity and will
    * exist till the application is running.Because it is extending Application class.
    * We will initialise it once inside MainActivity then we can use this
    * object from every class/activity/fragment of the app.
    *
    * */
    private User user = null;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        /*
        * Initialising timber debug tree
        * */
        Timber.plant(new Timber.DebugTree());
    }
}
