package com.kustomer.kustomersdk.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import com.kustomer.kustomersdk.Kustomer;

import static com.kustomer.kustomersdk.Utils.KUSConstants.BundleName.NOTIFICATION_ID_BUNDLE_KEY;

public class NotificationDismissReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra(NOTIFICATION_ID_BUNDLE_KEY,0);
        NotificationManagerCompat.from(Kustomer.getContext()).cancel(notificationId);
    }
}
