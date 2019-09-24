package com.kustomer.kustomersdk.Models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.Enums.KUSCSatisfactionFormResponseStatus;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;

import static com.kustomer.kustomersdk.Enums.KUSCSatisfactionFormResponseStatus.*;
import static com.kustomer.kustomersdk.Utils.JsonHelper.*;

public class KUSCSatisfactionResponse extends KUSModel {

    @Nullable
    private KUSCSatisfactionForm satisfactionForm;

    @NonNull
    private KUSCSatisfactionFormResponseStatus status;
    private int rating;
    @Nullable
    private Date lockedAt;
    @Nullable
    private Date updatedAt;
    @Nullable
    private Date createdAt;
    @Nullable
    private Date submittedAt;
    @NonNull
    private HashMap<String, String> answers;

    //region LifeCycle

    public KUSCSatisfactionResponse(JSONObject json) throws KUSInvalidJsonException {
        super(json);
        answers = new HashMap<>();
        status = KUS_C_SATISFACTION_RESPONSE_STATUS_UNKNOWN;
        updateResponseData(json);
    }

    //endregion

    //region Class Methods

    public void addIncludedWithJSON(JSONArray jsonArray) {
        super.addIncludedWithJSON(jsonArray);

        try {
            JSONObject jsonObject = (JSONObject) jsonArray.get(0);
            satisfactionForm = new KUSCSatisfactionForm(jsonObject);
        } catch (JSONException | KUSInvalidJsonException e) {
            KUSLog.KUSLogError(e.getMessage());
        }
    }

    private void updateResponseData(JSONObject json) {
        status = satisfactionResponseStatusFromString(JsonHelper.stringFromKeyPath(json,
                "attributes.status"));
        lockedAt = dateFromKeyPath(json, "attributes.lockedAt");
        updatedAt = dateFromKeyPath(json, "attributes.updatedAt");
        createdAt = dateFromKeyPath(json, "attributes.createdAt");
        submittedAt = dateFromKeyPath(json, "attributes.submittedAt");
        rating = integerFromKeyPath(json, "attributes.rating");

        JSONArray array = arrayFromKeyPath(json, "attributes.answers");
        updateAnswers(array);
    }

    public void updateAnswers(@Nullable JSONArray array) {
        if (array != null) {

            for (int i = 0; i < array.length(); i++) {
                try {
                    answers.put(JsonHelper.stringFromKeyPath(array.getJSONObject(i), "id"),
                            JsonHelper.stringFromKeyPath(array.getJSONObject(i), "answer"));
                } catch (JSONException e) {
                    KUSLog.KUSLogError(e.getMessage());
                }
            }
        }
    }

    @NonNull
    private KUSCSatisfactionFormResponseStatus satisfactionResponseStatusFromString(@Nullable String string) {
        if (string == null)
            return KUS_C_SATISFACTION_RESPONSE_STATUS_UNKNOWN;

        switch (string) {
            case "offered":
                return KUS_C_SATISFACTION_RESPONSE_STATUS_OFFERED;
            case "rated":
                return KUS_C_SATISFACTION_RESPONSE_STATUS_RATED;
            case "commented":
                return KUS_C_SATISFACTION_RESPONSE_STATUS_COMMENTED;
                default:
                    return KUS_C_SATISFACTION_RESPONSE_STATUS_UNKNOWN;
        }
    }

    @Override
    public String modelType() {
        return "satisfaction_response";
    }

    public boolean haveSecondaryQuestion() {
        return satisfactionForm != null && !satisfactionForm.getQuestions().isEmpty();
    }

    @Nullable
    public KUSCSatisfactionForm getSatisfactionForm() {
        return satisfactionForm;
    }

    @NonNull
    public KUSCSatisfactionFormResponseStatus getStatus() {
        return status;
    }

    public void setStatus(@NonNull KUSCSatisfactionFormResponseStatus status) {
        this.status = status;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setSubmittedAt(@Nullable Date submittedAt) {
        this.submittedAt = submittedAt;
    }

    public int getRating() {
        return rating;
    }

    @Nullable
    public Date getLockedAt() {
        return lockedAt;
    }

    @Nullable
    public Date getUpdatedAt() {
        return updatedAt;
    }

    @Nullable
    public Date getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public Date getSubmittedAt() {
        return submittedAt;
    }

    @NonNull
    public HashMap<String, String> getAnswers() {
        return answers;
    }

    //endregion
}
