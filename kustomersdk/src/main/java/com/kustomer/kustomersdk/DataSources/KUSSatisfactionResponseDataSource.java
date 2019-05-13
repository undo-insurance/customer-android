package com.kustomer.kustomersdk.DataSources;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Helpers.KUSDate;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSCSatisfactionResponse;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import static com.kustomer.kustomersdk.Enums.KUSCSatisfactionFormResponseStatus.KUS_C_SATISFACTION_RESPONSE_STATUS_COMMENTED;
import static com.kustomer.kustomersdk.Enums.KUSCSatisfactionFormResponseStatus.KUS_C_SATISFACTION_RESPONSE_STATUS_RATED;

public class KUSSatisfactionResponseDataSource extends KUSObjectDataSource {

    //region Properties

    @NonNull
    private String sessionId;
    private boolean isEnabled;
    //endregion

    //region Initializer

    KUSSatisfactionResponseDataSource(@NonNull KUSUserSession userSession, @NonNull String sessionId) {
        super(userSession);
        this.sessionId = sessionId;
        isEnabled = true;
    }

    //endregion

    //region Private Methods

    private void submitSatisfactionResponseWithRatingAndComment(int rating, @Nullable String comment) {
        if(getUserSession() == null)
            return;

        KUSCSatisfactionResponse satisfactionResponse = (KUSCSatisfactionResponse) getObject();

        if (satisfactionResponse == null)
            return;

        String endpoint = String.format(KUSConstants.URL.SATISFACTION_RESPONSE_SUBMIT_ENDPOINT,
                satisfactionResponse.getId());

        URL url = getUserSession().getRequestManager().urlForEndpoint(endpoint);

        String answerId = null;
        if (satisfactionResponse.getSatisfactionForm() != null
                && !satisfactionResponse.getSatisfactionForm().getQuestions().isEmpty()) {
            answerId = satisfactionResponse.getSatisfactionForm().getQuestions().get(0).getId();
        }

        HashMap<String, Object> response = new HashMap<>();
        if (rating > 0) {
            response.put("rating", rating);
            satisfactionResponse.setRating(rating);
            satisfactionResponse.setStatus(KUS_C_SATISFACTION_RESPONSE_STATUS_RATED);
        }

        if (comment != null && answerId != null) {
            try {
                JSONObject answer = new JSONObject();
                answer.put("id", answerId);
                answer.put("answer", comment);

                JSONArray answersArray = new JSONArray();
                answersArray.put(0, answer);

                response.put("answers", answersArray);
                satisfactionResponse.updateAnswers(answersArray);
                satisfactionResponse.setStatus(KUS_C_SATISFACTION_RESPONSE_STATUS_COMMENTED);
            } catch (JSONException e) {
                KUSLog.KUSLogError(e.getMessage());
            }
        }

        boolean shouldAddSubmittedAt =
                (satisfactionResponse.getSatisfactionForm().getQuestions().size() > 0 && comment != null)
                        || satisfactionResponse.getSatisfactionForm().getQuestions().size() == 0;

        if (shouldAddSubmittedAt) {
            Date submittedAt = Calendar.getInstance().getTime();
            response.put("submittedAt", KUSDate.stringFromDate(submittedAt));
            satisfactionResponse.setSubmittedAt(submittedAt);
        }

        getUserSession().getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_PUT,
                url,
                response,
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {
                        // No need to do anything here
                    }
                });
    }

    //endregion

    //region public Methods

    public void submitRating(int rating) {
        submitSatisfactionResponseWithRatingAndComment(rating, null);
    }

    public void submitComment(@NonNull String comment) {
        submitSatisfactionResponseWithRatingAndComment(0, comment);
    }

    public boolean cSatFormHaveSecondaryQuestion() {
        KUSCSatisfactionResponse satisfactionResponse = (KUSCSatisfactionResponse) getObject();

        return satisfactionResponse != null && satisfactionResponse.haveSecondaryQuestion();
    }

    public boolean isSatisfactionEnabled() {
        return isEnabled;
    }

    @Override
    void performRequest(@NonNull final KUSRequestCompletionListener completionListener) {
        if(getUserSession() == null) {
            completionListener.onCompletion(new Error(), null);
            return;
        }

        getUserSession().getRequestManager().performRequestType(KUSRequestType.KUS_REQUEST_TYPE_POST,
                String.format(KUSConstants.URL.SATISFACTION_RESPONSE_ENDPOINT, sessionId),
                null,
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {

                        // Check if the response is empty
                        //TODO: Improve this by sending request code in listener
                        if (error != null && error.getMessage().equalsIgnoreCase(Kustomer.getContext()
                                .getString(R.string.com_kustomer_unable_to_parse_response))) {
                            isEnabled = false;
                        }

                        completionListener.onCompletion(error, response);
                    }
                });
    }

    @Override
    protected KUSModel objectFromJson(JSONObject jsonObject) throws KUSInvalidJsonException {
        return new KUSCSatisfactionResponse(jsonObject);
    }

    //region
}
