package com.kustomer.kustomersdk.Models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.Enums.KUSTypingStatus;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;

import org.json.JSONObject;

import static com.kustomer.kustomersdk.Utils.JsonHelper.stringFromKeyPath;

public class KUSTypingIndicator {

    //region properties

    @NonNull
    private String id;

    @Nullable
    private String userId;

    @NonNull
    private KUSTypingStatus status;

    //endregion

    //region Constructor

    public KUSTypingIndicator(JSONObject json) throws KUSInvalidJsonException {
        String objectId = stringFromKeyPath(json, "id");
        if (objectId == null)
            throw new KUSInvalidJsonException("Object Id not found.");

        id = objectId;
        userId = stringFromKeyPath(json, "userId");
        status = typingStatusFromString(stringFromKeyPath(json, "status"));
    }

    //endregion

    //region private methods

    @NonNull
    private KUSTypingStatus typingStatusFromString(@Nullable String string) {
        if (string == null)
            return KUSTypingStatus.KUS_TYPING_UNKNOWN;

        if (string.equals("typing")) {
            return KUSTypingStatus.KUS_TYPING;

        } else if (string.equals("typing-ended")) {
            return KUSTypingStatus.KUS_TYPING_ENDED;
        }

        return KUSTypingStatus.KUS_TYPING_UNKNOWN;
    }

    //region public methods


    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null)
            return false;

        if (obj == this)
            return true;

        if (!obj.getClass().equals(this.getClass()))
            return false;

        KUSTypingIndicator typingIndicator = (KUSTypingIndicator) obj;

        if (this.userId == null || !this.userId.equals(typingIndicator.userId))
            return false;

        if (typingIndicator.status != this.status)
            return false;

        return true;
    }

    public void setStatus(@NonNull KUSTypingStatus status) {
        this.status = status;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @NonNull
    public KUSTypingStatus getStatus() {
        return status;
    }

    //endregion
}
