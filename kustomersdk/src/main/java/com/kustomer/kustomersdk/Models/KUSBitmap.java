package com.kustomer.kustomersdk.Models;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.kustomer.kustomersdk.Helpers.KUSImage;
import com.kustomer.kustomersdk.Interfaces.BitmapListener;

import java.util.ArrayList;

public class KUSBitmap {
    //region properties

    private String uri;
    private Bitmap bitmap;
    private ArrayList<BitmapListener> bitmapListeners = new ArrayList<>();
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
                        for (BitmapListener listener : bitmapListeners)
                            listener.onBitmapCreated();
                        bitmapListeners.clear();
                    }
                });
            }
        }).start();
    }

    public KUSBitmap(final String imageUri, BitmapListener listeners) {
        this(imageUri);
        bitmapListeners.add(listeners);
    }

    //endregion

    //region getter & setter

    public String getUri() {
        return uri;
    }

    public void addBitmapListeners(BitmapListener bitmapListener) {
        this.bitmapListeners.add(bitmapListener);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    //endregion

}
