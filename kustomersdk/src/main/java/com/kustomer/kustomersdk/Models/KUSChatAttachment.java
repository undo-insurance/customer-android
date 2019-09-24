package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSChatAttachment extends KUSModel {
    //region Properties
    private String name;
    private Date createdAt;
    private Date updatedAt;
    //endregion

    //region Initializer
    public KUSChatAttachment(JSONObject jsonObject) throws KUSInvalidJsonException {
        super(jsonObject);

        name = JsonHelper.stringFromKeyPath(jsonObject,"attributes.name");
        createdAt = JsonHelper.dateFromKeyPath(jsonObject,"attributes.createdAt");
        updatedAt = JsonHelper.dateFromKeyPath(jsonObject,"attributes.updatedAt");
    }

    @Override
    public String modelType(){
        return "attachment";
    }
    //endregion

    //region Getter & Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    //endregion
}
