package com.kustomer.kustomersdk.Models;

import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class KUSMLNode extends KUSModel {

    //region Properties
    private String displayName;
    private String nodeId;
    private Boolean deleted;
    private ArrayList<KUSMLNode> childNodes;
    //endregion

    //region Initializer

    public KUSMLNode(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        displayName = JsonHelper.stringFromKeyPath(json, "displayName");
        nodeId = JsonHelper.stringFromKeyPath(json, "id");
        deleted = JsonHelper.boolFromKeyPath(json, "deleted");

        //Filter deleted nodes
        ArrayList<KUSMLNode> nodes = KUSMLNode.objectsFromJSONs(JsonHelper.arrayFromKeyPath(json,
                "children"));
        ArrayList<KUSMLNode> filteredNodes = new ArrayList<>();

        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                if (!nodes.get(i).isDeleted())
                    filteredNodes.add(nodes.get(i));
            }
        }

        childNodes = filteredNodes;
    }
    //endregion


    //region Class methods

    public String modelType() {
        return null;
    }

    public boolean enforcesModelType() {
        return false;
    }

    @Nullable
    public static ArrayList<KUSMLNode> objectsFromJSONs(@Nullable JSONArray jsonArray) {
        if (jsonArray == null)
            return null;

        ArrayList<KUSMLNode> arrayList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject json = (JSONObject) jsonArray.get(i);
                KUSMLNode node = new KUSMLNode(json);
                arrayList.add(node);
            } catch (JSONException | KUSInvalidJsonException ignore) {
            }
        }

        return arrayList;
    }
    //endregion

    //region Accessors

    public String getDisplayName() {
        return displayName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public ArrayList<KUSMLNode> getChildNodes() {
        return childNodes;
    }

    public boolean isDeleted() {
        return deleted != null ? deleted : false;
    }

    //endregion
}
