package com.kustomer.kustomersdk.Interfaces;

public interface KUSBitmapListener {
    void onBitmapCreated();

    void onOutOfMemoryError(OutOfMemoryError outOfMemoryError);
}