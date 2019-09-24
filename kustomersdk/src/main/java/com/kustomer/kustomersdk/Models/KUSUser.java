package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONObject;

import java.net.URL;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSUser extends KUSModel {
    //region Properties
    private String displayName;
    private URL avatarURL;
    //endregion

    //region Initializer
    public KUSUser(JSONObject json) throws KUSInvalidJsonException {
        super(json);
        displayName = JsonHelper.stringFromKeyPath(json,"attributes.displayName");
        avatarURL = JsonHelper.urlFromKeyPath(json,"attributes.avatarUrl");
    }

    @Override
    public String modelType(){
        return "user";
    }
    //endregion

    //region Accessors

    public String getDisplayName() {
        return displayName;
    }

    public URL getAvatarURL() {
        return avatarURL;
    }

    //endregion
}
