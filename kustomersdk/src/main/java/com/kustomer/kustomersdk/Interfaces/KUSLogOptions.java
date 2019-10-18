package com.kustomer.kustomersdk.Interfaces;

/**
 * Created by Junaid on 3/22/2018.
 */

public interface KUSLogOptions {
    int KUSLogOptionInfo = 1 << 0;
    int KUSLogOptionErrors = 1 << 1;
    int KUSLogOptionRequests = 1 << 2;
    int KUSLogOptionPusher = 1 << 3;
    int KUSLogOptionDebug = 1 << 4;
    int KUSLogOptionAll = 0xFFFFFF;
}
