package com.kustomer.kustomersdk.DataSources;


import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Interfaces.KUSChatAvailableListener;
import com.kustomer.kustomersdk.Helpers.KUSLocalization;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Utils.KUSJsonHelper;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSChatSettingsDataSource extends KUSObjectDataSource implements Serializable {


    //region Initializer
    public KUSChatSettingsDataSource(KUSUserSession userSession) {
        super(userSession);
    }
    //endregion

    //region public Methods
    @Override
    void performRequest(@NonNull KUSRequestCompletionListener completionListener) {
        if (getUserSession() == null) {
            completionListener.onCompletion(new Error(), null);
            return;
        }

        getUserSession().getRequestManager().performRequestType(KUSRequestType.KUS_REQUEST_TYPE_GET,
                KUSConstants.URL.SETTINGS_ENDPOINT,
                new HashMap<String, Object>() {
                    {
                        put(KUSConstants.HeaderKeys.K_KUSTOMER_LANGUAGE_KEY,
                                KUSLocalization.getSharedInstance().getUserLocale().getLanguage());
                    }
                }, true,
                completionListener);
    }

    @Override
    KUSModel objectFromJson(JSONObject jsonObject) throws KUSInvalidJsonException {
        return new KUSChatSettings(jsonObject);
    }

    public void isChatAvailable(final KUSChatAvailableListener listener){

        performRequest(new KUSRequestCompletionListener() {
            @Override
            public void onCompletion(final Error error, JSONObject response) {

                KUSChatSettings settings = null;
                try {
                    settings = (KUSChatSettings) objectFromJson(
                            KUSJsonHelper.jsonObjectFromKeyPath(response,"data"));

                } catch (KUSInvalidJsonException e) {
                    e.printStackTrace();
                }

                if(error == null && settings !=null)
                    listener.onSuccess(settings.getEnabled());
                else
                    listener.onFailure();
            }
        });

    }
    //endregion
}
