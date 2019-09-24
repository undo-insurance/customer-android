package com.kustomer.kustomersdk.Models;


import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KUSClientActivity extends KUSModel {

    //region Properties
    private List<Double> intervals;
    private String currentPage;
    private String previousPage;
    private Double currentPageSeconds;
    private Date createdAt;
    //endregion

    //region LifeCycle
    public KUSClientActivity(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        JSONArray intervalsArray = JsonHelper.arrayFromKeyPath(json,"attributes.intervals");

        if(intervalsArray != null)
            this.intervals = arrayListFromJsonArray(intervalsArray,"seconds");

        currentPage = JsonHelper.stringFromKeyPath(json,"attributes.currentPage");
        previousPage = JsonHelper.stringFromKeyPath(json,"attributes.previousPage");
        currentPageSeconds = JsonHelper.doubleFromKeyPath(json,"attributes.currentPageSeconds");
        createdAt = JsonHelper.dateFromKeyPath(json,"attributes.createdAt");
    }

    @Override
    public String modelType(){
        return "client_activity";
    }
    //endregion

    //region Private Methods
    private List<Double> arrayListFromJsonArray(JSONArray array, String id) {
        List<Double> list = new ArrayList<>();

        for(int i = 0 ; i<array.length() ; i++){
            try {
                JSONObject jsonObject = array.getJSONObject(i);
                list.add(jsonObject.getDouble(id));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return list;
    }
    //endregion

    //region Getters & Setters

    public List<Double> getIntervals() {
        return intervals;
    }

    public void setIntervals(List<Double> intervals) {
        this.intervals = intervals;
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public String getPreviousPage() {
        return previousPage;
    }

    public void setPreviousPage(String previousPage) {
        this.previousPage = previousPage;
    }

    public Double getCurrentPageSeconds() {
        return currentPageSeconds;
    }

    public void setCurrentPageSeconds(Double currentPageSeconds) {
        this.currentPageSeconds = currentPageSeconds;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    //endregion
}
