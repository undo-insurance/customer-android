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
                try {
                    bitmap = KUSImage.getBitmapForUri(uri);
                    notifyBitmapCreated();
                } catch (SecurityException ignored) {

                } catch (OutOfMemoryError outOfMemoryError) {
                    notifyOutOfMemoryError(outOfMemoryError);
                }
            }
        }).start();
    }

    public KUSBitmap(final String imageUri, KUSBitmapListener listener) {
        this(imageUri);
        bitmapListeners.add(listener);
    }

    //endregion

    //region Private Methods

    private void notifyBitmapCreated() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (KUSBitmapListener listener : bitmapListeners)
                    listener.onBitmapCreated();
                bitmapListeners.clear();
            }
        });
    }

    private void notifyOutOfMemoryError(final OutOfMemoryError outOfMemoryError) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (KUSBitmapListener listener : bitmapListeners)
                    listener.onOutOfMemoryError(outOfMemoryError);
                bitmapListeners.clear();
            }
        });
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

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    //endregion

}
