package com.samriddha.googlemapsandgoogledirectionsapp;

import android.app.Application;

import com.samriddha.googlemapsandgoogledirectionsapp.models.User;

import timber.log.Timber;

public class UserClient extends Application {

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
