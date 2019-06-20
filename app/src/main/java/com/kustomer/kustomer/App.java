package com.kustomer.kustomer;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.kustomer.kustomersdk.Kustomer;
import com.squareup.leakcanary.LeakCanary;

import io.fabric.sdk.android.Fabric;

public class App extends Application {

    private static final String K_KUSTOMER_API_KEY =
            "[INSERT_API_KEY]";

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        Kustomer.init(this, K_KUSTOMER_API_KEY);
        Fabric.with(this, new Crashlytics());
    }

}
