package com.kustomer.kustomersdk.DataSources;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Helpers.KUSDate;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Interfaces.KUSChatMessagesDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSChatSessionCompletionListener;
import com.kustomer.kustomersdk.Interfaces.KUSFormCompletionListener;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Utils.JsonHelper;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSChatSessionsDataSource extends KUSPaginatedDataSource
        implements KUSChatMessagesDataSourceListener, KUSObjectDataSourceListener {

    //region Properties
    private JSONObject pendingCustomChatSessionAttributes;
    private JSONObject pendingCustomChatSessionAttributesForNextConversation;
    private HashMap<String, Date> localLastSeenAtBySessionId;
    //endregion

    //region Initializer
    public KUSChatSessionsDataSource(KUSUserSession userSession) {
        super(userSession);

        localLastSeenAtBySessionId = new HashMap<>();
        addListener(this);
        getUserSession().getChatSettingsDataSource().addListener(this);
    }

    @Override
    public List<KUSModel> objectsFromJSON(JSONObject jsonObject) {

        ArrayList<KUSModel> arrayList = null;

        KUSModel model = null;
        try {
            model = new KUSChatSession(jsonObject);
        } catch (KUSInvalidJsonException e) {
            e.printStackTrace();
        }

        if (model != null) {
            arrayList = new ArrayList<>();
            arrayList.add(model);
        }

        return arrayList;
    }
    //endregion

    //region KUSPaginatedDataSource methods
    public void fetchLatest() {
        if (getUserSession() == null)
            return;

        if (!getUserSession().getChatSettingsDataSource().isFetched()) {
            getUserSession().getChatSettingsDataSource().fetch();
            return;
        }
        super.fetchLatest();
    }

    public URL getFirstUrl() {
        if (getUserSession() == null)
            return null;

        return getUserSession().getRequestManager().urlForEndpoint(KUSConstants.URL.CHAT_SESSIONS_ENDPOINT);
    }
    //endregion

    //region Public Methods
    public void upsertNewSessions(List<KUSModel> chatSessions) {
        if (chatSessions.size() > 1)
            Collections.reverse(chatSessions);

        upsertAll(chatSessions);
    }

    public void createSessionWithTitle(String title, final KUSChatSessionCompletionListener listener) {
        if (getUserSession() == null)
            return;

        HashMap<String, Object> params = new HashMap<>();
        params.put("title", title);

        getUserSession().getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_POST,
                KUSConstants.URL.CHAT_SESSIONS_ENDPOINT,
                params, true, new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {
                        if (error != null) {
                            if (listener != null)
                                listener.onComplete(error, null);

                            return;
                        }

                        KUSChatSession session = null;
                        try {
                            session = new KUSChatSession(JsonHelper.jsonObjectFromKeyPath(response, "data"));
                        } catch (KUSInvalidJsonException e) {
                            e.printStackTrace();
                        }

                        if (session != null) {
                            checkShouldFlushCustomAttributesToChatSessionId(session.getId());

                            ArrayList<KUSModel> sessions = new ArrayList<>();
                            sessions.add(session);

                            upsertAll(sessions);
                        }

                        if (listener != null)
                            listener.onComplete(null, session);

                    }
                });
    }

    public void updateLastSeenAtForSessionId(String sessionId, final KUSChatSessionCompletionListener listener) {
        if (getUserSession() == null)
            return;

        if (sessionId.length() == 0) {
            if (listener != null) {
                Handler mainHandler = new Handler(Looper.getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        listener.onComplete(new Error("Session id missing"), null);
                    }
                };
                mainHandler.post(myRunnable);
            }
        }

        Date lastSeenAtDate = Calendar.getInstance().getTime();
        localLastSeenAtBySessionId.put(sessionId, lastSeenAtDate);
        final String lastSeenAtString = KUSDate.stringFromDate(lastSeenAtDate);

        String url = String.format(KUSConstants.URL.CHAT_SESSIONS_ENDPOINT + "/%s", sessionId);
        HashMap<String, Object> params = new HashMap<String, Object>() {{
            put("lastSeenAt", lastSeenAtString);
        }};

        getUserSession().getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_PUT,
                url,
                params,
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {
                        if (error != null && listener != null) {
                            listener.onComplete(error, null);
                            return;
                        }

                        KUSChatSession session = null;
                        try {
                            session = new KUSChatSession(JsonHelper.jsonObjectFromKeyPath(response, "data"));
                        } catch (KUSInvalidJsonException e) {
                            e.printStackTrace();
                        }

                        if (session != null) {
                            ArrayList<KUSModel> sessions = new ArrayList<>();
                            sessions.add(session);

                            upsertAll(sessions);
                        }

                        if (listener != null)
                            listener.onComplete(null, session);
                    }
                }

        );
    }

    public void updateLocallyLastSeenAtForSessionId(String sessionId) {
        if (TextUtils.isEmpty(sessionId)) {
            return;
        }
        localLastSeenAtBySessionId.put(sessionId, Calendar.getInstance().getTime());
    }

    public void submitFormMessages(final JSONArray messages, String formId, final KUSFormCompletionListener listener) {
        if (getUserSession() == null)
            return;

        final WeakReference<KUSChatSessionsDataSource> weakReference = new WeakReference<>(this);
        getUserSession().getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_POST,
                String.format(KUSConstants.URL.FORMS_RESPONSES_ENDPOINT, formId),
                new HashMap<String, Object>() {{
                    put("messages", messages);
                }},
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {
                        if (error != null) {
                            if (listener != null)
                                listener.onComplete(error, null, null);

                            return;
                        }

                        KUSChatSession chatSession = null;
                        ArrayList<KUSModel> chatMessages = new ArrayList<>();
                        JSONArray includedModelsJSON = JsonHelper.arrayFromKeyPath(response, "included");

                        if (includedModelsJSON != null) {
                            for (int i = 0; i < includedModelsJSON.length(); i++) {
                                try {
                                    JSONObject includedModelJSON = includedModelsJSON.getJSONObject(i);
                                    String type = JsonHelper.stringFromKeyPath(includedModelJSON, "type");

                                    if (type != null && type.equals(new KUSChatSession().modelType())) {
                                        chatSession = new KUSChatSession(includedModelJSON);
                                    } else if (type != null && type.equals(new KUSChatMessage().modelType())) {
                                        KUSChatMessage chatMessage = new KUSChatMessage(includedModelJSON);
                                        chatMessages.add(chatMessage);
                                    }

                                } catch (JSONException | KUSInvalidJsonException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (chatSession != null) {
                            checkShouldFlushCustomAttributesToChatSessionId(chatSession.getId());

                            final KUSChatSession finalChatSession = chatSession;
                            weakReference.get().upsertAll(new ArrayList<KUSModel>() {{
                                add(finalChatSession);
                            }});
                        }

                        if (listener != null)
                            listener.onComplete(null, chatSession, chatMessages);

                    }
                }
        );
    }

    public void describeActiveConversation(@Nullable JSONObject customAttributes) {
        JSONObject pendingCustomChatSessionAttributes = new JSONObject();
        if (this.pendingCustomChatSessionAttributes != null) {
            Iterator iterator = this.pendingCustomChatSessionAttributes.keys();
            while (iterator.hasNext()) {
                String key = iterator.next().toString();
                try {
                    pendingCustomChatSessionAttributes.put(key, this.pendingCustomChatSessionAttributes.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        Iterator iterator;
        if (customAttributes != null) {
            iterator = customAttributes.keys();
            while (iterator.hasNext()) {
                String key = iterator.next().toString();
                try {
                    pendingCustomChatSessionAttributes.put(key, customAttributes.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        this.pendingCustomChatSessionAttributes = pendingCustomChatSessionAttributes;

        KUSChatSession mostRecentSession = getMostRecentSession();

        String mostRecentSessionId = null;
        if (mostRecentSession != null)
            mostRecentSessionId = mostRecentSession.getId();

        if (mostRecentSessionId != null)
            flushCustomAttributes(pendingCustomChatSessionAttributes, mostRecentSessionId);
    }

    public void describeNextConversation(JSONObject customAttributes) {
        JSONObject pendingCustomChatSessionAttributesForNextConversation = new JSONObject();

        if (this.pendingCustomChatSessionAttributesForNextConversation != null) {
            Iterator iterator = this.pendingCustomChatSessionAttributesForNextConversation.keys();
            while (iterator.hasNext()) {
                String key = iterator.next().toString();
                try {
                    pendingCustomChatSessionAttributesForNextConversation.put(key,
                            this.pendingCustomChatSessionAttributesForNextConversation.get(key));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        Iterator iterator = customAttributes.keys();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            try {
                pendingCustomChatSessionAttributesForNextConversation.put(key, customAttributes.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        this.pendingCustomChatSessionAttributesForNextConversation = pendingCustomChatSessionAttributesForNextConversation;

    }
    //endregion

    //region internal methods

    public int getOpenChatSessionsCount() {
        int count = 0;
        KUSChatSession chatSession;
        for (KUSModel model : getList()) {
            chatSession = (KUSChatSession) model;
            if (chatSession.getLockedAt() == null) {
                count++;
            }
        }
        return count;
    }

    public int getOpenProactiveCampaignsCount() {
        if (getUserSession() == null)
            return 0;

        int count = 0;
        KUSChatSession chatSession;
        for (KUSModel model : getList()) {
            chatSession = (KUSChatSession) model;
            KUSChatMessagesDataSource chatDataSource = getUserSession().chatMessageDataSourceForSessionId(chatSession.getId());
            if (chatSession.getLockedAt() == null && !chatDataSource.isAnyMessageByCurrentUser()) {
                count += 1;
            }
        }
        return count;
    }
    //endregion

    //region Private Methods

    private void checkShouldFlushCustomAttributesToChatSessionId(String chatSessionId) {
        boolean hasChatSessionAttributes = pendingCustomChatSessionAttributes != null ||
                pendingCustomChatSessionAttributesForNextConversation != null;

        if (hasChatSessionAttributes) {
            JSONObject pendingCustomChatSessionAttributes = new JSONObject();

            if (this.pendingCustomChatSessionAttributes != null) {
                Iterator<String> keys = this.pendingCustomChatSessionAttributes.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        pendingCustomChatSessionAttributes.put(key,
                                this.pendingCustomChatSessionAttributes.get(key));
                    } catch (JSONException e) {
                    }
                }
            }
            if (this.pendingCustomChatSessionAttributesForNextConversation != null) {
                Iterator<String> keys = this.pendingCustomChatSessionAttributesForNextConversation.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        pendingCustomChatSessionAttributes.put(key,
                                this.pendingCustomChatSessionAttributesForNextConversation.get(key));
                    } catch (JSONException e) {
                    }
                }

                this.pendingCustomChatSessionAttributesForNextConversation = null;
            }

            flushCustomAttributes(pendingCustomChatSessionAttributes, chatSessionId);
        }
    }

    private void flushCustomAttributes(final JSONObject customAttributes, String chatSessionId) {
        if (getUserSession() == null)
            return;

        HashMap<String, Object> formData = new HashMap<String, Object>() {{
            put("custom", customAttributes);
        }};

        String endpoint = String.format(KUSConstants.URL.CONVERSATIONS_ENDPOINT + "/%s", chatSessionId);
        getUserSession().getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_PATCH,
                endpoint,
                formData,
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {
                        if (getUserSession() == null)
                            return;

                        if (error != null) {
                            KUSLog.KUSLogError(String.format("Error updating chat attributes: %s", error));
                        }
                    }
                }
        );
    }
    //endregion

    //region Helper methods
    public KUSChatSession getMostRecentSession() {
        Date mostRecentMessageAt = null;
        KUSChatSession mostRecentSession = null;

        for (KUSModel model : getList()) {
            KUSChatSession chatSession = (KUSChatSession) model;

            if (chatSession.getLockedAt() == null) {
                if (mostRecentMessageAt == null) {
                    mostRecentMessageAt = chatSession.getLastMessageAt();
                    mostRecentSession = chatSession;
                } else if (chatSession.getLastMessageAt() != null
                        && mostRecentMessageAt.before(chatSession.getLastMessageAt())) {
                    mostRecentMessageAt = chatSession.getLastMessageAt();
                    mostRecentSession = chatSession;
                }
            }
        }

        return mostRecentSession != null ? mostRecentSession : (KUSChatSession) getFirst();
    }

    public KUSChatSession mostRecentNonProactiveCampaignOpenSession() {
        if (getUserSession() == null)
            return (KUSChatSession) getFirst();

        Date mostRecentMessageAt = null;
        KUSChatSession mostRecentSession = null;

        for (KUSModel model : getList()) {
            KUSChatSession chatSession = (KUSChatSession) model;
            KUSChatMessagesDataSource chatDataSource = getUserSession().chatMessageDataSourceForSessionId(chatSession.getId());
            if (chatSession.getLockedAt() == null && chatDataSource.isAnyMessageByCurrentUser()) {

                if (mostRecentMessageAt == null) {
                    mostRecentMessageAt = chatSession.getLastMessageAt();
                    mostRecentSession = chatSession;
                } else if (mostRecentMessageAt.before(chatSession.getLastMessageAt())) {
                    mostRecentMessageAt = chatSession.getLastMessageAt();
                    mostRecentSession = chatSession;
                }
            }
        }
        return mostRecentSession != null ? mostRecentSession : (KUSChatSession) getFirst();
    }

    public Date getLastMessageAt() {
        Date mostRecentMessageAt = null;
        for (KUSModel model : getList()) {
            KUSChatSession chatSession = (KUSChatSession) model;
            if (mostRecentMessageAt == null) {
                mostRecentMessageAt = chatSession.getLastMessageAt();
            } else if (mostRecentMessageAt.before(chatSession.getLastMessageAt())) {
                mostRecentMessageAt = chatSession.getLastMessageAt();
            }
        }

        if (mostRecentMessageAt == null) {
            KUSChatSession firstSession = (KUSChatSession) getFirst();
            if (firstSession != null)
                return firstSession.getLastMessageAt();
        }
        return mostRecentMessageAt;
    }

    public Date lastSeenAtForSessionId(String sessionId) {
        KUSChatSession chatSession = (KUSChatSession) findById(sessionId);
        Date chatSessionDate = null;
        if (chatSession != null)
            chatSessionDate = chatSession.getLastSeenAt();
        Date localDate = localLastSeenAtBySessionId.get(sessionId);

        if (chatSessionDate != null) {
            if (localDate != null)
                return chatSessionDate.after(localDate) ? chatSessionDate : localDate;
            else
                return chatSessionDate;
        } else
            return localDate;
    }

    public int totalUnreadCountExcludingSessionId(String excludedSessionId) {
        if (getUserSession() == null)
            return 0;

        int count = 0;
        for (KUSModel model : getList()) {
            KUSChatSession session = (KUSChatSession) model;

            String sessionId = session.getId();
            if (excludedSessionId != null && excludedSessionId.equals(sessionId)) {
                continue;
            }

            KUSChatMessagesDataSource messagesDataSource = getUserSession().chatMessageDataSourceForSessionId(sessionId);
            Date sessionLastSeenAt = lastSeenAtForSessionId(sessionId);
            int unreadCountForSession = messagesDataSource.unreadCountAfterDate(sessionLastSeenAt);
            count += unreadCountForSession;
        }

        return count;
    }
    //endregion

    //region Listener
    @Override
    public void onLoad(KUSPaginatedDataSource dataSource) {

    }

    @Override
    public void onError(KUSPaginatedDataSource dataSource, Error error) {

    }

    @Override
    public void onContentChange(KUSPaginatedDataSource dataSource) {
        if (getUserSession() != null && dataSource == this) {
            getUserSession().getSharedPreferences().setOpenChatSessionsCount(getOpenChatSessionsCount());

            for (KUSModel model : getList()) {
                KUSChatSession chatSession = (KUSChatSession) model;
                KUSChatMessagesDataSource messagesDataSource = getUserSession()
                        .chatMessageDataSourceForSessionId(chatSession.getId());
                messagesDataSource.addListener(this);
            }

        } else if (dataSource.getClass().equals(KUSChatMessagesDataSource.class)) {
            sort();
            notifyAnnouncersOnContentChange();
        }
    }

    @Override
    public void onCreateSessionId(KUSChatMessagesDataSource source, String sessionId) {

    }

    @Override
    public void objectDataSourceOnLoad(KUSObjectDataSource dataSource) {
        fetchLatest();
    }

    @Override
    public void objectDataSourceOnError(KUSObjectDataSource dataSource, Error error) {
        if (!dataSource.isFetched()) {
            notifyAnnouncersOnError(error);
        }
    }
    //endregion

}
