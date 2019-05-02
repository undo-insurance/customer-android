package com.kustomer.kustomersdk.Interfaces;

import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.Models.KUSTypingIndicator;

public interface KUSTypingStatusListener {
    void onTypingStatusChanged(@NonNull KUSTypingIndicator typingIndicator);
}