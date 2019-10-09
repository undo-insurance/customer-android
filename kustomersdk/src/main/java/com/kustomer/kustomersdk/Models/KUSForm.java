package com.kustomer.kustomersdk.Models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.Enums.KUSFormQuestionProperty;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.KUSJsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSForm extends KUSModel{

    // region Properties
    @NonNull
    private List<KUSFormQuestion> questions;
    //endregion

    //region LifeCycle
    public KUSForm(JSONObject jsonObject) throws KUSInvalidJsonException {
        super(jsonObject);

        questions = getQuestionsFromJsonArray(KUSJsonHelper.arrayFromKeyPath(jsonObject,
                "attributes.questions"));
    }

    public String modelType(){
        return "form";
    }
    //endregion

    //region Public Methods
    public boolean containsEmailQuestion(){

        for (KUSFormQuestion question : questions) {
            if (question.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_EMAIL)
                return true;
        }

        return false;
    }
    //endregion

    //region Static Methods
    private static List<KUSFormQuestion> getQuestionsFromJsonArray(@Nullable JSONArray jsonArray){
        ArrayList<KUSFormQuestion> objects = new ArrayList<>();

        if (jsonArray == null) {
            return objects;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                KUSFormQuestion object = new KUSFormQuestion(jsonObject);
                objects.add(object);
            } catch (JSONException | KUSInvalidJsonException e) {
                e.printStackTrace();
            }
        }
        return objects;
    }
    //endregion

    //region Accessors

    @NonNull
    public List<KUSFormQuestion> getQuestions() {
        return questions;
    }

    //endregion
}

