package com.kustomer.kustomersdk.Models;

import com.kustomer.kustomersdk.Enums.KUSFormQuestionProperty;
import com.kustomer.kustomersdk.Enums.KUSFormQuestionType;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSFormQuestion extends KUSModel {

    //region Properties
    private String name;
    private String prompt;
    private List<String> values;
    private KUSFormQuestionType type;
    private KUSFormQuestionProperty property;
    private Boolean skipIfSatisfied;
    private KUSMLFormValue mlFormValues;
    //endregion

    //region LifeCycle
    public KUSFormQuestion(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        name = JsonHelper.stringFromKeyPath(json, "name");
        prompt = JsonHelper.stringFromKeyPath(json, "prompt");
        skipIfSatisfied = JsonHelper.boolFromKeyPath(json, "skipIfSatisfied");
        type = KUSFormQuestionTypeFromString(JsonHelper.stringFromKeyPath(json, "type"));
        property = KUSFormQuestionPropertyFromString(JsonHelper.stringFromKeyPath(json, "property"));

        if (property == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_MLV) {
            JSONObject tempJson = null;

            try {
                JSONObject valueMeta = JsonHelper.jsonObjectFromKeyPath(json, "valueMeta");

                if (valueMeta != null) {
                    tempJson = new JSONObject(valueMeta.toString());
                    tempJson.put("id", 1);
                    mlFormValues = new KUSMLFormValue(tempJson);
                }
            } catch (JSONException ignore) {
            }
        }

        values = JsonHelper.arrayListFromKeyPath(json, "values");
    }
    //endregion

    //region Class Methods
    public String modelType() {
        return null;
    }

    public boolean enforcesModelType() {
        return false;
    }

    public static boolean KUSFormQuestionRequiresResponse(KUSFormQuestion question) {
        if (question == null || question.type == null)
            return false;

        return question.type == KUSFormQuestionType.KUS_FORM_QUESTION_TYPE_PROPERTY ||
                question.type == KUSFormQuestionType.KUS_FORM_QUESTION_TYPE_RESPONSE;
    }

    private static KUSFormQuestionType KUSFormQuestionTypeFromString(String string) {

        if (string == null)
            return KUSFormQuestionType.KUS_FORM_QUESTION_TYPE_UNKNOWN;

        switch (string) {
            case "message":
                return KUSFormQuestionType.KUS_FORM_QUESTION_TYPE_MESSAGE;
            case "property":
                return KUSFormQuestionType.KUS_FORM_QUESTION_TYPE_PROPERTY;
            case "response":
                return KUSFormQuestionType.KUS_FORM_QUESTION_TYPE_RESPONSE;
        }

        return KUSFormQuestionType.KUS_FORM_QUESTION_TYPE_UNKNOWN;
    }

    private static KUSFormQuestionProperty KUSFormQuestionPropertyFromString(String string) {
        if (string == null)
            return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_UNKNOWN;

        if (string.equals("customer_name")) {
            return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_NAME;
        } else if (string.equals("customer_email")) {
            return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_EMAIL;
        } else if (string.equals("conversation_team")) {
            return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CONVERSATION_TEAM;
        } else if (string.equals("customer_phone")) {
            return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_PHONE;
        } else if (string.equals("followup_channel")) {
            return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_FOLLOW_UP_CHANNEL;
        } else if (string.endsWith("Tree")) {
            return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_MLV;
        } else if (string.endsWith("Str")) {
            return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_VALUES;
        }

        return KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_UNKNOWN;
    }
    //endregion

    //region Accessors

    public KUSFormQuestionProperty getProperty() {
        return property;
    }

    public KUSFormQuestionType getType() {
        return type;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return values;
    }

    public Boolean getSkipIfSatisfied() {
        return skipIfSatisfied != null ? skipIfSatisfied : false;
    }

    public KUSMLFormValue getMlFormValues() {
        return mlFormValues;
    }

    //endregion
}
