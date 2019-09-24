package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONObject;

import java.util.Date;

public class KUSHoliday extends KUSModel {

    //region Properties
    private String name;
    private Date startDate;
    private Date endDate;
    private Boolean enabled;
    //endregion

    //region Initializer
    public KUSHoliday(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        name = JsonHelper.stringFromKeyPath(json,"attributes.name");
        startDate = JsonHelper.dateFromKeyPath(json,"attributes.startDate");
        endDate = JsonHelper.dateFromKeyPath(json,"attributes.endDate");
        enabled = JsonHelper.boolFromKeyPath(json,"attributes.enabled");
    }
    //endregion

    //region Class methods
    public String modelType(){
        return "holiday";
    }
    //endregion

    //region Accessors

    public String getName() {
        return name;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    //endregion
}
