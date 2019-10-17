package com.kustomer.kustomersdk.Managers;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Interfaces.KUSCustomerStatsListener;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Utils.KUSJsonHelper;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Date;

public class KUSStatsManager {

    //region Properties

    @NonNull
    private WeakReference<KUSUserSession> userSession;
    @Nullable
    private Date lastActivity;

    //endregion

    //region LifeCycle

    public KUSStatsManager(@NonNull KUSUserSession userSession) {
        this.userSession = new WeakReference<>(userSession);
    }

    //endregion

    //region Public Methods

    public void updateStats(@NonNull final KUSCustomerStatsListener listener) {
        if (userSession.get() == null)
            return;

        // Fetch last activity time of the client

        KUSLog.KUSLogInfo("Calling Stats API");
        userSession.get().getRequestManager().getEndpoint(KUSConstants.URL.CUSTOMER_STATS_ENDPOINT,
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, final JSONObject response) {
                        if (userSession.get() == null)
                            return;

                        if (error != null) {
                            listener.onCompletion(false);
                            return;
                        }

                        JSONObject jsonObject = KUSJsonHelper.jsonObjectFromKeyPath(response,
                                "data");
                        Date lastActivity = KUSJsonHelper.dateFromKeyPath(jsonObject,
                                "attributes.lastActivity");

                        final boolean sessionUpdated = (KUSStatsManager.this.lastActivity == null
                                && lastActivity != null)
                                || (KUSStatsManager.this.lastActivity != null
                                && !KUSStatsManager.this.lastActivity.equals(lastActivity));
                        KUSStatsManager.this.lastActivity = lastActivity;

                        Handler handler = new Handler(Looper.getMainLooper());
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                listener.onCompletion(sessionUpdated);
                            }
                        };
                        handler.post(runnable);
                    }
                });
    }

    //endregion
}
