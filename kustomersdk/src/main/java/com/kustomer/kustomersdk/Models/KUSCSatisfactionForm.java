package com.kustomer.kustomersdk.Models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.Enums.KUSCSatisfactionScaleType;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;

import org.json.JSONObject;

import static com.kustomer.kustomersdk.Utils.KUSJsonHelper.integerFromKeyPath;
import static com.kustomer.kustomersdk.Utils.KUSJsonHelper.stringFromKeyPath;

public class KUSCSatisfactionForm extends KUSForm {

    @Nullable
    private String introduction;
    @Nullable
    private String ratingPrompt;
    @Nullable
    private String scaleLabelHigh;
    @Nullable
    private String scaleLabelLow;
    @NonNull
    private KUSCSatisfactionScaleType scaleType;
    private int scaleOptionsCount;

    //region Lifecycle
    public KUSCSatisfactionForm(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        this.introduction = stringFromKeyPath(json,"attributes.introduction");
        this.ratingPrompt = stringFromKeyPath(json, "attributes.ratingPrompt");
        this.scaleLabelHigh = stringFromKeyPath(json, "attributes.scale.labelHigh");
        this.scaleLabelLow = stringFromKeyPath(json, "attributes.scale.labelLow");
        this.scaleType = satisfactionScaleTypeFromString(
                stringFromKeyPath(json, "attributes.scale.type"));
        this.scaleOptionsCount = integerFromKeyPath(json, "attributes.scale.options");
    }

    //endregion

    //region Class Methods

    @NonNull
    private KUSCSatisfactionScaleType satisfactionScaleTypeFromString(@Nullable String string) {

        if (string == null)
            return KUSCSatisfactionScaleType.KUS_C_SATISFACTION_SCALE_TYPE_UNKNOWN;

        switch (string) {
            case "number":
                return KUSCSatisfactionScaleType.KUS_C_SATISFACTION_SCALE_TYPE_NUMBER;
            case "emoji":
                return KUSCSatisfactionScaleType.KUS_C_SATISFACTION_SCALE_TYPE_EMOJI;
            case "thumb":
                return KUSCSatisfactionScaleType.KUS_C_SATISFACTION_SCALE_TYPE_THUMB;
            default:
                return KUSCSatisfactionScaleType.KUS_C_SATISFACTION_SCALE_TYPE_UNKNOWN;
        }
    }

    @Override
    public String modelType() {
        return "satisfaction";
    }

    //endregion

    //region Accessors

    @Nullable
    public String getIntroduction() {
        return introduction;
    }

    @Nullable
    public String getRatingPrompt() {
        return ratingPrompt;
    }

    @Nullable
    public String getScaleLabelHigh() {
        return scaleLabelHigh;
    }

    @Nullable
    public String getScaleLabelLow() {
        return scaleLabelLow;
    }

    @NonNull
    public KUSCSatisfactionScaleType getScaleType() {
        return scaleType;
    }

    public int getScaleOptionsCount() {
        return scaleOptionsCount;
    }

    //endregion
}
