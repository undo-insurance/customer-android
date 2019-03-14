package com.kustomer.kustomersdk.Models;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.kustomer.kustomersdk.Helpers.KUSImage;
import com.kustomer.kustomersdk.Interfaces.KUSBitmapListener;

import java.util.ArrayList;

public class KUSBitmap {
    //region properties

    private String uri;
    private Bitmap bitmap;
    private ArrayList<KUSBitmapListener> bitmapListeners = new ArrayList<>();
    //endregion

    //region constructor
    public KUSBitmap(final String imageUri) {
        uri = imageUri;
        new Thread(new Runnable() {
            @Override
            public void run() {
                bitmap = KUSImage.getBitmapForUri(uri);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        for (KUSBitmapListener listener : bitmapListeners)
                            listener.onBitmapCreated();
                        bitmapListeners.clear();
                    }
                });
            }
        }).start();
    }

    public KUSBitmap(final String imageUri, KUSBitmapListener listener) {
        this(imageUri);
        bitmapListeners.add(listener);
    }

    //endregion

    //region getter & setter

    public String getUri() {
        return uri;
    }

    public void addListener(KUSBitmapListener listener) {
        this.bitmapListeners.add(listener);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    //endregion

}
