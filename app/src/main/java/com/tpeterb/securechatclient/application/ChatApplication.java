package com.tpeterb.securechatclient.application;

import android.app.Application;
import android.content.Context;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.tpeterb.securechatclient.component.AppComponent;
import com.tpeterb.securechatclient.component.DaggerAppComponent;
import com.tpeterb.securechatclient.module.AppModule;

public class ChatApplication extends Application {

    private static AppComponent daggerAppComponent;

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        daggerAppComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
        AndroidThreeTen.init(this);
        context = getApplicationContext();
    }

    public static AppComponent getAppComponent() {
        return daggerAppComponent;
    }

}
