package com.smartisanos.textboom;

import android.app.Application;

import com.smartisanos.textboom.util.ConfigUtils;

public class BoomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ConfigUtils.init(this);
    }
}
