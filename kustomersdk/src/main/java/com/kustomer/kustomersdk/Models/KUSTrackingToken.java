package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import static com.kustomer.kustomersdk.Utils.JsonHelper.boolFromKeyPath;
import static com.kustomer.kustomersdk.Utils.JsonHelper.stringFromKeyPath;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSTrackingToken extends KUSModel {

    //region Properties
    private String trackingId;
    private String token;
    private Boolean verified;
    //endregion

    //region Initializer
    public KUSTrackingToken (JSONObject json) throws KUSInvalidJsonException {
        super(json);

        trackingId = stringFromKeyPath(json, "attributes.trackingId");
        token = stringFromKeyPath(json, "attributes.token");
        verified = boolFromKeyPath(json, "attributes.verified");
    }
    //endregion


    public String modelType() {
        return "tracking_token";
    }

    //region Accessors

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getVerified() {
        return verified != null ? verified : false;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    //endregion
}
