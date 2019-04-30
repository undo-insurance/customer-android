package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONObject;

import java.util.ArrayList;

public class KUSMLFormValue extends KUSModel {

    //region Properties
    private String displayName;
    private Boolean lastNodeRequired;
    private ArrayList<KUSMLNode> mlNodes;
    //endregion

    //region Initializer
    KUSMLFormValue(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        displayName = JsonHelper.stringFromKeyPath(json,"displayName");
        lastNodeRequired = JsonHelper.boolFromKeyPath(json,"lastNodeRequired");

        ArrayList<KUSMLNode> nodes = KUSMLNode.objectsFromJSONs(JsonHelper.arrayFromKeyPath(json,
                "tree.children"));
        ArrayList<KUSMLNode> filteredNodes = new ArrayList<>();

        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                if (!nodes.get(i).isDeleted())
                    filteredNodes.add(nodes.get(i));
            }
        }

        mlNodes = filteredNodes;
    }
    //endregion

    //region Class methods
    public String modelType(){
        return null;
    }

    public boolean enforcesModelType(){
        return false;
    }
    //endregion

    //region Accessors

    public String getDisplayName() {
        return displayName;
    }

    public Boolean getLastNodeRequired() {
        return lastNodeRequired != null ? lastNodeRequired : false;
    }

    public ArrayList<KUSMLNode> getMlNodes() {
        return mlNodes;
    }

    //endregion
}
