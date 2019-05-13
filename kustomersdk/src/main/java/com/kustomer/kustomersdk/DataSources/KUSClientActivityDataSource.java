package com.kustomer.kustomersdk.DataSources;

import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Models.KUSClientActivity;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class KUSClientActivityDataSource extends KUSObjectDataSource {

    //region Properties
    private List<Double> intervals;
    private Date createdAt;
    private String previousPageName;
    private String currentPageName;
    Double currentPageSeconds;
    //endregion

    //region LifeCycle
    public KUSClientActivityDataSource(KUSUserSession userSession, String previousPageName,
                                       String currentPageName, Double currentPageSeconds){
        super(userSession);

        if(currentPageName == null)
            throw new AssertionError("Should not fetch client activity without a current page!");

        this.previousPageName = previousPageName;
        this.currentPageName = currentPageName;
        this.currentPageSeconds = currentPageSeconds;
    }
    //endregion

    //region Public Methods
    public List<Double> getIntervals(){
        KUSClientActivity clientActivity = (KUSClientActivity) getObject();
        return clientActivity.getIntervals();
    }

    public Date getCreatedAt(){
        KUSClientActivity clientActivity = (KUSClientActivity) getObject();
        return clientActivity.getCreatedAt();
    }

    //endregion

    //region SubClass Method
    @Override
    public void performRequest(@NonNull KUSRequestCompletionListener completionListener){
        if(getUserSession() == null) {
            completionListener.onCompletion(new Error(), null);
            return;
        }

        HashMap<String,Object> params = new HashMap<>();

        if(previousPageName != null){
            params.put("previousPage",previousPageName);
        }

        params.put("currentPage",currentPageName);
        params.put("currentPageSeconds",currentPageSeconds);

        getUserSession().getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_POST,
                KUSConstants.URL.CLIENT_ACTIVITY_ENDPOINT,
                params,
                true,
                completionListener
        );
    }

    @Override
    KUSModel objectFromJson(JSONObject jsonObject) throws KUSInvalidJsonException {
        return new KUSClientActivity(jsonObject);
    }
    //endregion

    //region Getter & Setter

    public void setIntervals(List<Double> intervals) {
        this.intervals = intervals;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getPreviousPageName() {
        return previousPageName;
    }

    public void setPreviousPageName(String previousPageName) {
        this.previousPageName = previousPageName;
    }

    public String getCurrentPageName() {
        return currentPageName;
    }

    public void setCurrentPageName(String currentPageName) {
        this.currentPageName = currentPageName;
    }

    public Double getCurrentPageSeconds() {
        return currentPageSeconds;
    }

    public void setCurrentPageSeconds(Double currentPageSeconds) {
        this.currentPageSeconds = currentPageSeconds;
    }

    //endregion
}
