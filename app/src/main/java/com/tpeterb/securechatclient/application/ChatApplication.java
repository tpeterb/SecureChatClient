package com.tpeterb.securechatclient.application;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.tpeterb.securechatclient.component.AppComponent;
import com.tpeterb.securechatclient.component.DaggerAppComponent;

public class ChatApplication extends Application {

    private static AppComponent daggerAppComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        daggerAppComponent = DaggerAppComponent.create();
        AndroidThreeTen.init(this);
        /*retrofit = new Retrofit.Builder()
                .baseUrl(CHAT_SERVER_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();*/
    }

    public static AppComponent getAppComponent() {
        return daggerAppComponent;
    }

}
