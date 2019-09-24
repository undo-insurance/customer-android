package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONObject;

import java.util.Date;

public class KUSSessionQueue extends KUSModel {

    //region Properties
    private Date enteredAt;
    private Integer estimatedWaitTimeSeconds;
    private Integer latestWaitTimeSeconds;
    private String name;
    //endregion

    //region Initializer
    public KUSSessionQueue(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        enteredAt = JsonHelper.dateFromKeyPath(json,"attributes.enteredAt");
        estimatedWaitTimeSeconds = JsonHelper.integerFromKeyPath(json,"attributes.estimatedWaitTimeSeconds");
        latestWaitTimeSeconds = JsonHelper.integerFromKeyPath(json,"attributes.latestWaitTimeSeconds");
        name = JsonHelper.stringFromKeyPath(json,"attributes.name");

    }
    //endregion

    //region Class methods
    public String modelType(){
        return "session_queue";
    }
    //endregion


    //region Accessors

    public Date getEnteredAt() {
        return enteredAt;
    }

    public int getEstimatedWaitTimeSeconds() {
        return estimatedWaitTimeSeconds;
    }

    public int getLatestWaitTimeSeconds() {
        return latestWaitTimeSeconds;
    }

    public String getName() {
        return name;
    }

    //endregion
}
