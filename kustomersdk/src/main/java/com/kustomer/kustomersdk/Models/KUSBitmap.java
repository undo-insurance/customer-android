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

                } catch (OutOfMemoryError memoryError) {
                    notifyError(memoryError);
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
        for (KUSBitmapListener listener : bitmapListeners)
            listener.onBitmapCreated();
        bitmapListeners.clear();
    }

    private void notifyError(final OutOfMemoryError memoryError) {
        for (KUSBitmapListener listener : bitmapListeners)
            listener.onMemoryError(memoryError);
        bitmapListeners.clear();
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
