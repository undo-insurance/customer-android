package com.kustomer.kustomersdk.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NetworkStateReceiver extends BroadcastReceiver {

    //region Properties
    @NonNull
    protected List<NetworkStateReceiverListener> listeners;

    protected boolean connected;

    //endregion

    //region methods

    public NetworkStateReceiver() {
        listeners = new ArrayList<>();
        connected = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null)
            return;

        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = manager.getActiveNetworkInfo();

            boolean connected = ni != null && ni.isConnected();

            if (connected != this.connected) {
                this.connected = connected;
                notifyStateToAll();
            }
        } catch (Exception e) {
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private void notifyStateToAll() {
        for (NetworkStateReceiverListener listener : listeners)
            notifyState(listener);
    }

    private void notifyState(@NonNull NetworkStateReceiverListener listener) {
        if (connected) {
            listener.networkAvailable();
        } else {
            listener.networkUnavailable();
        }
    }

    public void addListener(@NonNull NetworkStateReceiverListener listener) {
        listeners.add(listener);
        notifyState(listener);
    }

    public void removeListener(@NonNull NetworkStateReceiverListener listener) {
        listeners.remove(listener);
    }

    //endregion

    //region interface

    public interface NetworkStateReceiverListener {
        void networkAvailable();

        void networkUnavailable();
    }

    //endregion
}
