package com.kustomer.kustomersdk.Helpers;

import android.util.Log;

import com.kustomer.kustomersdk.Interfaces.KUSLogOptions;
import com.kustomer.kustomersdk.Kustomer;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSLog {

    //region Private Methods
    private static void KUSLogMessage(int req, String value){
        if((Kustomer.getLogOptions() & req) > 0){
            Log.d("Kustomer",value);
        }
    }
    //endregion

    //region Public Methods
    public static void KUSLogInfo(String info){
        KUSLogMessage(KUSLogOptions.KUSLogOptionInfo,info);
    }

    public static void KUSLogError(String info){
        KUSLogMessage(KUSLogOptions.KUSLogOptionErrors,info);
    }

    public static void KUSLogRequest(String info){
        KUSLogMessage(KUSLogOptions.KUSLogOptionRequests,info);
    }

    public static void KUSLogPusher(String info){
        KUSLogMessage(KUSLogOptions.KUSLogOptionPusher,info);
    }

    public static void KUSLogDebug(String info){
        KUSLogMessage(KUSLogOptions.KUSLogOptionDebug,info);
    }
    //endregion

}
