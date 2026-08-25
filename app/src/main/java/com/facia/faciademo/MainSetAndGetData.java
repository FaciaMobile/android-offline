package com.facia.faciademo;

import android.app.Activity;
import android.content.Context;

import com.facia.faciademo.databinding.ActivityMainBinding;

public class MainSetAndGetData {
    private static MainSetAndGetData data;
    private Activity activity;
    private Context context;
    private Boolean isSplashGone = false;

    private MainSetAndGetData() {
    }

    public static MainSetAndGetData getInstance() {
        if (data == null) {
            data = new MainSetAndGetData();
        }
        return data;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Boolean isSplashGone() {
        return isSplashGone;
    }

    public void setSplashGone(Boolean splashGone) {
        isSplashGone = splashGone;
    }
}
