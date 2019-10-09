package com.kustomer.kustomersdk.DataSources;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSForm;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONObject;


/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSFormDataSource extends KUSObjectDataSource implements KUSObjectDataSourceListener {

    @Nullable
    private String formId;

    //region LifeCycle
    public KUSFormDataSource(KUSUserSession userSession) {
        super(userSession);
        userSession.getChatSettingsDataSource().addListener(this);
    }

    KUSFormDataSource(@NonNull KUSUserSession userSession, @Nullable String formId) {
        super(userSession);
        this.formId = formId;
    }

    KUSModel objectFromJson(JSONObject jsonObject) throws KUSInvalidJsonException {
        return new KUSForm(jsonObject);
    }
    //endregion

    //region Subclass Methods
    public void performRequest(@NonNull KUSRequestCompletionListener listener) {
        if (getUserSession() == null) {
            listener.onCompletion(new Error(), null);
            return;
        }

        String formId = getFormId();

        if (formId == null)
            return;

        getUserSession().getRequestManager().getEndpoint(
                String.format(KUSConstants.URL.FORMS_ENDPOINT, formId),
                true,
                listener);
    }

    public void fetch() {
        if (getUserSession() == null)
            return;

        String formId = getFormId();

        if (formId == null && !getUserSession().getChatSettingsDataSource().isFetched()) {
            getUserSession().getChatSettingsDataSource().fetch();
            return;
        }

        if (formId == null)
            return;

        super.fetch();
    }

    public boolean isFetching() {
        if (getUserSession() != null && getUserSession().getChatSettingsDataSource().isFetching()) {
            return getUserSession().getChatSettingsDataSource().isFetching();
        }

        return super.isFetching();
    }

    public boolean isFetched() {
        KUSChatSettings chatSettings = null;

        if (getUserSession() != null)
            chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();

        if (chatSettings != null && getFormId() == null)
            return true;

        return super.isFetched();
    }

    public Error getError() {
        Error error = getUserSession().getChatSettingsDataSource().getError();
        return error != null ? error : super.getError();
    }

    @Nullable
    String getFormId() {
        if (getUserSession() == null)
            return null;

        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();

        String formId = null;

        if (this.formId != null)
            formId = this.formId;

        else if (getUserSession().getSharedPreferences().getFormId() != null)
            formId = getUserSession().getSharedPreferences().getFormId();

        else if (chatSettings != null && chatSettings.getActiveFormId() != null)
            formId = chatSettings.getActiveFormId();

        return formId;
    }
    //endregion

    //region Listener
    @Override
    public void objectDataSourceOnLoad(KUSObjectDataSource dataSource) {
        fetch();
    }

    @Override
    public void objectDataSourceOnError(final KUSObjectDataSource dataSource, Error error) {
        if (!dataSource.isFetched()) {
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    dataSource.fetch();
                }
            };
            handler.post(runnable);
        }
    }
    //endregion
}
