package com.kustomer.kustomersdk.DataSources;

import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Helpers.KUSSharedPreferences;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSTrackingToken;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSTrackingTokenDataSource extends KUSObjectDataSource implements KUSObjectDataSourceListener {

    //region Properties
    private boolean wantsReset;
    //endregion

    //region Initializer
    public KUSTrackingTokenDataSource(KUSUserSession userSession){
        super(userSession);
        addListener(this);
    }
    //endregion

    //region Public Methods
    public void performRequest(@NonNull KUSRequestCompletionListener listener){
        if(getUserSession() == null) {
            listener.onCompletion(new Error(), null);
            return;
        }

        String endPoint = wantsReset ? KUSConstants.URL.TRACKING_TOKEN_ENDPOINT :
                KUSConstants.URL.CURRENT_TRACKING_TOKEN_ENDPOINT;

        URL url = getUserSession().getRequestManager().urlForEndpoint(endPoint);
        KUSRequestType requestType = wantsReset ? KUSRequestType.KUS_REQUEST_TYPE_POST :
                KUSRequestType.KUS_REQUEST_TYPE_GET;

        getUserSession().getRequestManager().performRequestType(
                requestType,
                url,
                null,
                false,
                getAdditionalHeaders(),
                listener );
    }

    public void reset(){
        wantsReset = true;
        cancel();
        fetch();
    }

    @Override
    KUSModel objectFromJson(JSONObject jsonObject) throws KUSInvalidJsonException {
        return new KUSTrackingToken(jsonObject);
    }
    //endregion

    //region Private Methods
    private HashMap<Object, Object> getAdditionalHeaders(){

        String currentTrackingToken = getCurrentTrackingToken();
        HashMap<Object, Object> headers = new HashMap<>();
        if(currentTrackingToken != null) {
            headers.put(KUSConstants.Keys.K_KUSTOMER_TRACKING_TOKEN_HEADER_KEY, currentTrackingToken);
            return headers;
        }
        else {
            KUSSharedPreferences sharedPreferences = null;

            if(getUserSession() != null)
                sharedPreferences = getUserSession().getSharedPreferences();

            if(sharedPreferences != null) {
                String cachedTrackingToken = sharedPreferences.getTrackingToken();

                if (cachedTrackingToken != null) {
                    headers.put(KUSConstants.Keys.K_KUSTOMER_TRACKING_TOKEN_HEADER_KEY, cachedTrackingToken);
                    return headers;
                }
            }
        }

        return null;
    }
    //endregion

    //region Accessors
    public String getCurrentTrackingToken() {
        KUSTrackingToken trackingTokenObj = (KUSTrackingToken) getObject();

        if(trackingTokenObj != null)
            return trackingTokenObj.getToken();
        else
            return null;
    }

    //endregion

    //region Listener
    @Override
    public void objectDataSourceOnLoad(KUSObjectDataSource dataSource) {
        wantsReset = false;

        String currentTrackingToken = getCurrentTrackingToken();
        if(getUserSession() != null && currentTrackingToken != null)
            getUserSession().getSharedPreferences().setTrackingToken(currentTrackingToken);
    }

    @Override
    public void objectDataSourceOnError(KUSObjectDataSource dataSource, Error error) {

    }
    //endregion

}
