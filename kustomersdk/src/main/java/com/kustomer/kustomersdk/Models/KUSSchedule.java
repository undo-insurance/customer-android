package com.kustomer.kustomersdk.Models;


import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Dictionary;

public class KUSSchedule extends KUSModel {
    //region Properties
    private String name;
    private JSONObject hours;
    private String timezone;
    private Boolean enabled;

    private ArrayList<KUSHoliday> holidays;
    //endregion

    //region Initializer
    public KUSSchedule (JSONObject json) throws KUSInvalidJsonException {
        super(json);

        name = JsonHelper.stringFromKeyPath(json,"attributes.name");
        hours = JsonHelper.jsonObjectFromKeyPath(json,"attributes.hours");
        timezone = JsonHelper.stringFromKeyPath(json,"attributes.timezone");
        enabled = JsonHelper.boolFromKeyPath(json,"attributes.default");
    }
    //endregion

    //region Class methods
    public String modelType(){
        return "schedule";
    }

    public void addIncludedWithJSON(JSONArray jsonArray){
        super.addIncludedWithJSON(jsonArray);

        holidays = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                KUSHoliday object = new KUSHoliday(jsonObject);
                holidays.add(object);
            } catch (JSONException | KUSInvalidJsonException e) {
                e.printStackTrace();
            }
        }
    }
    //endregion

    //region Accessors

    public String getName() {
        return name;
    }

    public JSONObject getHours() {
        return hours;
    }

    public String getTimezone() {
        return timezone;
    }

    public Boolean getEnabled() {
        return enabled != null ? enabled : false;
    }

    public ArrayList<KUSHoliday> getHolidays() {
        return holidays;
    }

    //endregion
}
