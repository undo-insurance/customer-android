package com.kustomer.kustomersdk.Interfaces;

public interface KUSBitmapListener {
    void onBitmapCreated();

    void onMemoryError(OutOfMemoryError memoryError);
}