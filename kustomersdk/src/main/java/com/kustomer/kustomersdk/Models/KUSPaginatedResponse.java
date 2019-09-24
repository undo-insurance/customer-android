package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.kustomer.kustomersdk.Utils.JsonHelper.integerFromKeyPath;
import static com.kustomer.kustomersdk.Utils.JsonHelper.stringFromKeyPath;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSPaginatedResponse {

    //region Properties
    private List<KUSModel> objects;
    private Integer page;
    private Integer pageSize;

    private String selfPath;
    private String firstPath;
    private String prevPath;
    private String nextPath;
    //endregion

    //region LifeCycle
    public KUSPaginatedResponse() {

    }

    public KUSPaginatedResponse(JSONObject json, KUSPaginatedDataSource dataSource) throws JSONException, KUSInvalidJsonException {

        if(json == null)
            return;

        Object data = json.get("data");
        boolean dataIsArray = data.getClass().equals(JSONArray.class);
        boolean dataIsJsonObject = data.getClass().equals(JSONObject.class);

        if(!dataIsArray && !dataIsJsonObject)
            throw  new KUSInvalidJsonException("Json Format for \"data\" is invalid.");

        ArrayList<KUSModel> objects = new ArrayList<>();

        if(dataIsArray){
            JSONArray array = (JSONArray) json.get("data");
            for (int i = 0; i<array.length();i++){
                JSONObject jsonObject = array.getJSONObject(i);

                List<KUSModel> models = dataSource.objectsFromJSON(jsonObject);
                if(models != null) {
                    for (int j = models.size() - 1; j >= 0; j--) {
                        objects.add(models.get(j));
                    }
                }
            }
        }else{
            List<KUSModel> models = dataSource.objectsFromJSON(json);

            if(models != null) {
                for (int j = models.size() - 1; j >= 0; j--) {
                    objects.add(models.get(j));
                }
            }
        }

        this.objects = objects;

        page = integerFromKeyPath(json,"meta.page");

        Integer a= integerFromKeyPath(json,"meta.pageSize");
        if(a!= null)
            pageSize = Math.max(a,objects.size());

        selfPath = stringFromKeyPath(json,"links.self");
        firstPath = stringFromKeyPath(json,"links.first");
        prevPath = stringFromKeyPath(json,"links.prev");
        nextPath = stringFromKeyPath(json,"links.next");
    }

    //region Accessors

    public List<KUSModel> getObjects() {
        return objects;
    }

    public void setObjects(List<KUSModel> objects) {
        this.objects = objects;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSelfPath() {
        return selfPath;
    }

    public void setSelfPath(String selfPath) {
        this.selfPath = selfPath;
    }

    public String getFirstPath() {
        return firstPath;
    }

    public void setFirstPath(String firstPath) {
        this.firstPath = firstPath;
    }

    public String getPrevPath() {
        return prevPath;
    }

    public void setPrevPath(String prevPath) {
        this.prevPath = prevPath;
    }

    public String getNextPath() {
        return nextPath;
    }

    public void setNextPath(String nextPath) {
        this.nextPath = nextPath;
    }

    //endregion
}
