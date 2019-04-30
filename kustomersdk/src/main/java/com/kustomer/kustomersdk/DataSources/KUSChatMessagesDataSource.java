package com.kustomer.kustomersdk.DataSources;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.kustomer.kustomersdk.API.KUSSessionQueuePollingManager;
import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSChatMessageState;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Enums.KUSVolumeControlMode;
import com.kustomer.kustomersdk.Helpers.KUSAudio;
import com.kustomer.kustomersdk.Helpers.KUSCache;
import com.kustomer.kustomersdk.Helpers.KUSDate;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Helpers.KUSUpload;
import com.kustomer.kustomersdk.Interfaces.KUSChatMessagesDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSChatSessionCompletionListener;
import com.kustomer.kustomersdk.Interfaces.KUSFormCompletionListener;
import com.kustomer.kustomersdk.Interfaces.KUSImageUploadListener;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSPaginatedDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Interfaces.KUSSessionQueuePollingListener;
import com.kustomer.kustomersdk.Interfaces.KUSVolumeControlTimerListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Managers.KUSVolumeControlTimerManager;
import com.kustomer.kustomersdk.Models.KUSChatAttachment;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSForm;
import com.kustomer.kustomersdk.Models.KUSFormQuestion;
import com.kustomer.kustomersdk.Models.KUSFormRetry;
import com.kustomer.kustomersdk.Models.KUSMessageRetry;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSRetry;
import com.kustomer.kustomersdk.Models.KUSSessionQueue;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.Utils.JsonHelper;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kustomer.kustomersdk.Models.KUSChatMessage.KUSChatMessageSentByUser;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSChatMessagesDataSource extends KUSPaginatedDataSource implements KUSChatMessagesDataSourceListener,
        KUSObjectDataSourceListener, KUSSessionQueuePollingListener {

    //region Properties
    private static final int KUS_CHAT_AUTO_REPLY_DELAY = 2 * 1000;

    private String sessionId;
    private boolean createdLocally;
    private KUSForm form;
    private Set<String> delayedChatMessageIds;
    private int questionIndex;
    private KUSFormQuestion formQuestion;
    private boolean submittingForm = false;
    private boolean creatingSession = false;

    private int vcFormQuestionIndex;
    private boolean vcTrackingStarted;
    private boolean vcTrackingDelayCompleted;
    private boolean vcFormActive;
    private boolean vcFormEnd;
    private boolean vcChatClosed;
    private boolean isProactiveCampaign;
    private ArrayList<KUSModel> temporaryVCMessagesResponses;

    private boolean nonBusinessHours;
    private KUSSessionQueuePollingManager sessionQueuePollingManager;

    private ArrayList<onCreateSessionListener> onCreateSessionListeners;
    private HashMap<String, KUSRetry> messageRetryHashMap;
    //endregion

    //region Initializer
    public KUSChatMessagesDataSource(KUSUserSession userSession) {
        super(userSession);

        questionIndex = -1;
        vcFormQuestionIndex = 0;
        vcFormActive = false;
        vcChatClosed = false;
        nonBusinessHours = false;
        temporaryVCMessagesResponses = new ArrayList<>();
        delayedChatMessageIds = new HashSet<>();
        messageRetryHashMap = new HashMap<>();

        userSession.getChatSettingsDataSource().addListener(this);
        addListener(this);
    }

    public KUSChatMessagesDataSource(KUSUserSession userSession, boolean startNewConversation) {
        this(userSession);

        if (startNewConversation) {
            createdLocally = true;
            userSession.getFormDataSource().addListener(this);

            userSession.getFormDataSource().fetch();
        }
    }

    public KUSChatMessagesDataSource(KUSUserSession userSession, String sessionId) {
        this(userSession);

        if (sessionId == null || sessionId.isEmpty())
            throw new AssertionError("Cannot create messages datasource without valid sessionId");

        this.sessionId = sessionId;
    }


    //endregion

    //region Public Methods

    public boolean isActualSession() {
        return sessionId != null && !sessionId.equals(KUSConstants.ChatSession.TEMP_SESSION_ID);
    }

    public void upsertNewMessages(List<KUSModel> chatMessages) {
        if (chatMessages.size() > 1)
            Collections.reverse(chatMessages);

        upsertAll(chatMessages);
    }

    public KUSChatMessage getLatestMessage() {
        if (getSize() > 0)
            return (KUSChatMessage) get(0);

        return null;
    }

    public void addListener(KUSChatMessagesDataSourceListener listener) {
        super.addListener(listener);
    }

    public URL getFirstUrl() {
        if (getUserSession() != null && isActualSession()) {
            String endPoint = String.format(KUSConstants.URL.MESSAGES_LIST_ENDPOINT, sessionId);
            return getUserSession().getRequestManager().urlForEndpoint(endPoint);
        }
        return null;
    }

    public void sendMessageWithText(String text, List<Bitmap> attachments) {
        sendMessageWithText(text, attachments, null);
    }

    public void sendMessageWithText(String text, List<Bitmap> attachments, String value) {
        if (getUserSession() == null)
            return;

        isProactiveCampaign = !isAnyMessageByCurrentUser();

        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();
        if (chatSettings != null && chatSettings.getActiveFormId() != null && !isActualSession()) {

            if (attachments != null && attachments.size() > 0)
                throw new AssertionError("Should not have been able to send attachments without a sessionId");

            if (sessionId == null) {
                JSONObject attributes = new JSONObject();
                try {
                    attributes.put("body", text);
                    attributes.put("direction", "in");
                    attributes.put("createdAt", KUSDate.stringFromDate(Calendar.getInstance().getTime()));
                } catch (JSONException e) {
                    KUSLog.KUSLogError(e.getMessage());
                }

                JSONObject relationships = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    data.put("id", KUSConstants.ChatSession.TEMP_SESSION_ID);

                    JSONObject session = new JSONObject();
                    session.put("data", data);

                    relationships.put("session", session);
                } catch (JSONException e) {
                    KUSLog.KUSLogError(e.getMessage());
                }

                JSONObject messageJSON = new JSONObject();
                try {
                    messageJSON.put("type", "chat_message");
                    messageJSON.put("id", UUID.randomUUID().toString());
                    messageJSON.put("attributes", attributes);
                    messageJSON.put("relationships", relationships);
                } catch (JSONException e) {
                    KUSLog.KUSLogError(e.getMessage());
                }

                try {
                    KUSChatMessage tempMessage = new KUSChatMessage(messageJSON);
                    KUSChatSession tempSession = KUSChatSession.tempSessionFromChatMessage(tempMessage);

                    upsertNewMessages(Collections.singletonList((KUSModel) tempMessage));
                    getUserSession().getChatSessionsDataSource()
                            .upsertNewSessions(Collections.singletonList((KUSModel) tempSession));

                    sessionId = KUSConstants.ChatSession.TEMP_SESSION_ID;
                    getUserSession().getChatMessagesDataSources().put(sessionId, this);

                    //Notify Listeners
                    for (KUSPaginatedDataSourceListener listener : listeners) {
                        if (listener instanceof KUSChatMessagesDataSourceListener) {
                            ((KUSChatMessagesDataSourceListener) listener).onCreateSessionId(
                                    KUSChatMessagesDataSource.this, KUSConstants.ChatSession.TEMP_SESSION_ID);
                        }
                    }

                } catch (KUSInvalidJsonException e) {
                    KUSLog.KUSLogError(e.getMessage());
                }

            } else if (sessionId.equals(KUSConstants.ChatSession.TEMP_SESSION_ID)) {
                JSONObject attributes = new JSONObject();
                try {
                    attributes.put("body", text);
                    attributes.put("direction", "in");
                    attributes.put("createdAt", KUSDate.stringFromDate(Calendar.getInstance().getTime()));
                } catch (JSONException e) {
                    KUSLog.KUSLogError(e.getMessage());
                }

                JSONObject messageJSON = new JSONObject();
                try {
                    messageJSON.put("type", "chat_message");
                    messageJSON.put("id", UUID.randomUUID().toString());
                    messageJSON.put("attributes", attributes);
                } catch (JSONException e) {
                    KUSLog.KUSLogError(e.getMessage());
                }

                try {
                    KUSChatMessage tempMessage = new KUSChatMessage(messageJSON);
                    tempMessage.setValue(value);

                    upsertNewMessages(Collections.singletonList((KUSModel) tempMessage));
                } catch (KUSInvalidJsonException e) {
                    KUSLog.KUSLogError(e.getMessage());
                }
            }

            return;

        } else if (isActualSession() && vcFormActive) {
            if (attachments != null && attachments.size() > 0)
                throw new AssertionError("Should not have been able to send attachments without a sessionId");
            JSONObject attributes = new JSONObject();
            try {
                attributes.put("body", text);
                attributes.put("direction", "in");
                attributes.put("createdAt", KUSDate.stringFromDate(Calendar.getInstance().getTime()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject messageJSON = new JSONObject();
            try {
                messageJSON.put("type", "chat_message");
                messageJSON.put("id", UUID.randomUUID().toString());
                messageJSON.put("attributes", attributes);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            List<KUSModel> temporaryMessages = objectsFromJSON(messageJSON);

            if (temporaryMessages != null) {

                for (KUSModel model : temporaryMessages) {
                    KUSChatMessage message = (KUSChatMessage) model;
                    message.setValue(value);
                    temporaryVCMessagesResponses.add(message);
                }
                upsertNewMessages(temporaryMessages);
            }

            return;
        }

        actuallySendMessage(text, attachments);
    }

    public void createSessionIfNecessaryWithTitle(String title, final onCreateSessionListener listener) {
        if (isActualSession()) {
            listener.onComplete(true, null);
        } else {
            if (onCreateSessionListeners != null) {
                if (listener != null)
                    onCreateSessionListeners.add(listener);
            } else {
                if (getUserSession() == null)
                    return;
                onCreateSessionListeners = new ArrayList<onCreateSessionListener>() {{
                    add(listener);
                }};
                creatingSession = true;
                getUserSession().getChatSessionsDataSource().createSessionWithTitle(title, new KUSChatSessionCompletionListener() {
                    @Override
                    public void onComplete(Error error, KUSChatSession session) {
                        ArrayList<onCreateSessionListener> callbacks = new ArrayList<>(onCreateSessionListeners);
                        onCreateSessionListeners = null;

                        if (error != null || session == null) {
                            KUSLog.KUSLogError(String.format("Error creating session: %s",
                                    error != null ? error.toString() : ""));
                            for (onCreateSessionListener listener1 : callbacks)
                                listener1.onComplete(false, error);

                            return;
                        }

                        //Grab the sessionId
                        sessionId = session.getId();
                        creatingSession = false;

                        // Create queue polling manager for volume control form
                        sessionQueuePollingManager = new KUSSessionQueuePollingManager(getUserSession(), sessionId);

                        //Insert the current messages data source into the userSession's lookup table
                        getUserSession().getChatMessagesDataSources().put(session.getId(), KUSChatMessagesDataSource.this);

                        //Notify Listeners
                        for (KUSPaginatedDataSourceListener listener : listeners) {
                            if (listener instanceof KUSChatMessagesDataSourceListener) {
                                ((KUSChatMessagesDataSourceListener) listener).onCreateSessionId(
                                        KUSChatMessagesDataSource.this, sessionId);
                            }
                        }

                        for (onCreateSessionListener listener1 : callbacks)
                            listener1.onComplete(true, null);

                    }
                });
            }

        }
    }

    private void actuallySendMessage(String text, List<Bitmap> attachments) {
        String tempMessageId = UUID.randomUUID().toString();
        JSONArray attachmentObjects = new JSONArray();
        List<String> cachedImageKeys = null;

        if (attachments != null) {
            cachedImageKeys = new ArrayList<>();

            for (Bitmap bitmap : attachments) {
                final String attachmentId = UUID.randomUUID().toString();
                try {

                    URL attachmentURL = KUSChatMessage.attachmentUrlForMessageId(tempMessageId, attachmentId);
                    String imageKey = attachmentURL.toString();
                    new KUSCache().addBitmapToMemoryCache(imageKey, bitmap);
                    attachmentObjects.put(new JSONObject() {{
                        put("id", attachmentId);
                    }});
                    cachedImageKeys.add(imageKey);

                } catch (MalformedURLException | JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        JSONObject attachment = new JSONObject();
        try {
            attachment.put("data", attachmentObjects);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        JSONObject relationships = new JSONObject();
        try {
            relationships.put("attachments", attachment);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject attributes = new JSONObject();
        try {
            if (!TextUtils.isEmpty(text))
                attributes.put("body", text);

            attributes.put("direction", "in");
            attributes.put("createdAt", KUSDate.stringFromDate(Calendar.getInstance().getTime()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject messageJSON = new JSONObject();
        try {
            messageJSON.put("type", "chat_message");
            messageJSON.put("id", tempMessageId);
            messageJSON.put("attributes", attributes);
            messageJSON.put("relationships", relationships);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        List<KUSModel> temporaryMessages = objectsFromJSON(messageJSON);
        if (temporaryMessages != null) {
            for (KUSModel model : temporaryMessages) {
                KUSChatMessage temporaryMessage = (KUSChatMessage) model;
                messageRetryHashMap.put(temporaryMessage.getId(), new KUSMessageRetry(temporaryMessages,
                        attachments, text, cachedImageKeys));
            }

            fullySendMessage(temporaryMessages, attachments, text, cachedImageKeys);
        }
    }

    public void resendMessage(KUSChatMessage chatMessage) {
        if (chatMessage != null) {
            KUSRetry retry = messageRetryHashMap.get(chatMessage.getId());
            if (retry instanceof KUSMessageRetry) {
                KUSMessageRetry messageRetry = (KUSMessageRetry) retry;
                fullySendMessage(messageRetry.getTemporaryMessages(), messageRetry.getAttachments(),
                        messageRetry.getText(), messageRetry.getCachedImages());

            } else if (retry instanceof KUSFormRetry) {
                KUSFormRetry formRetry = (KUSFormRetry) retry;
                retrySubmittingForm(formRetry);
            }
        }
    }

    public void endChat(final String reason, final OnEndChatListener onEndChatListener) {
        if (getUserSession() == null)
            return;

        getUserSession().getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_PUT,
                String.format(KUSConstants.URL.SESSION_LOCK_ENDPOINT, sessionId),
                new HashMap<String, Object>() {{
                    put("locked", true);
                    put("lockReason", reason);
                }},
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {
                        if (getUserSession() == null)
                            return;

                        if (error != null) {
                            if (onEndChatListener != null)
                                onEndChatListener.onComplete(false);
                            return;
                        }
                        // Temporary set locked at to reflect changes in UI
                        KUSChatSession session = (KUSChatSession) getUserSession().getChatSessionsDataSource().findById(sessionId);
                        if (session != null)
                            session.setLockedAt(new Date());

                        // Cancel Volume Control Polling if necessary
                        if (sessionQueuePollingManager != null)
                            sessionQueuePollingManager.cancelPolling();

                        notifyAnnouncersOnContentChange();
                        if (onEndChatListener != null)
                            onEndChatListener.onComplete(true);
                    }
                });
    }

    public int unreadCountAfterDate(Date date) {
        int count = 0;

        for (KUSModel model : getList()) {
            KUSChatMessage message = (KUSChatMessage) model;

            if (KUSChatMessageSentByUser(message)) {
                return count;
            }

            if (message.getCreatedAt() != null) {
                if (date != null && message.getCreatedAt().before(date)) {
                    return count;
                }

                count++;
            }

        }

        return count;
    }

    public boolean shouldPreventSendingMessage() {
        // If we haven't loaded the chat settings data source, prevent input
        if (getUserSession() == null || !getUserSession().getChatSettingsDataSource().isFetched()) {
            return true;
        }

        // If we are about to insert an artificial message, prevent input
        if (delayedChatMessageIds.size() > 0) {
            return true;
        }

        // When submitting the form or creating session, prevent sending more responses
        if (submittingForm || creatingSession) {
            return true;
        }

        // If the user sent their first message and it is not yet sent, prevent input

        if (getList().size() > 0) {
            KUSChatMessage firstMessage = (KUSChatMessage) getList().get(getList().size() - 1);
            if (!isActualSession()
                    && getSize() == 1
                    && firstMessage.getState() != KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_SENT)
                return true;
        }

        return false;
    }

    public KUSFormQuestion currentQuestion() {
        if (isActualSession())
            return null;

        if (KUSChatMessageSentByUser(this.getLatestMessage()))
            return null;

        return formQuestion;
    }

    public KUSFormQuestion volumeControlCurrentQuestion() {
        if (getUserSession() == null)
            return null;

        if (!vcFormActive)
            return null;

        if (!isActualSession())
            return null;

        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();
        if (!chatSettings.isVolumeControlEnabled()) {
            return null;
        }

        if (vcFormEnd)
            return null;

        if (getOtherUserIds().size() > 0)
            return null;

        return formQuestion;
    }

    public boolean isChatClosed() {

        //For business hours
        if (nonBusinessHours) {
            return true;
        }

        if (vcFormActive) {
            return false;
        }
        if (!isActualSession()) {
            return false;
        }

        if (getUserSession() == null)
            return true;

        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();
        if (chatSettings != null && !chatSettings.isVolumeControlEnabled()) {
            return false;
        }

        if (getOtherUserIds().size() > 0)
            return false;

        if (vcChatClosed) {
            return true;
        }

        return false;
    }

    public void closeProactiveCampaignIfNecessary() {
        if (getUserSession() == null)
            return;

        KUSChatSettings settings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();

        if (settings.getSingleSessionChat()) {

            HashMap<String, KUSChatMessagesDataSource> chatSessionsHashMap = getUserSession().getChatMessagesDataSources();
            ArrayList<KUSChatMessagesDataSource> chatSessions = new ArrayList<>(chatSessionsHashMap.values());

            for (KUSChatMessagesDataSource chatMessagesDataSource : chatSessions) {

                if (!chatMessagesDataSource.isAnyMessageByCurrentUser()) {

                    getUserSession().getChatSessionsDataSource()
                            .updateLastSeenAtForSessionId(chatMessagesDataSource.sessionId, null);

                    chatMessagesDataSource.endChat("customer_ended", null);
                }
            }
        }
    }

    //endregion

    //region Private Methods
    private void fullySendMessage(final List<KUSModel> temporaryMessages, final List<Bitmap> attachments,
                                  final String text, final List<String> cachedImageKeys) {
        insertMessagesWithState(KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_SENDING, temporaryMessages);

        createSessionIfNecessaryWithTitle(text, new onCreateSessionListener() {
            @Override
            public void onComplete(boolean success, Error error) {

                if (success)
                    sendMessage(attachments, temporaryMessages, text, cachedImageKeys);
                else
                    insertMessagesWithState(KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_FAILED, temporaryMessages);

            }
        });
    }

    private void insertMessagesWithState(KUSChatMessageState state, List<KUSModel> temporaryMessages) {
        removeAll(temporaryMessages);
        for (KUSModel message : temporaryMessages) {
            if (message.getClass().equals(KUSChatMessage.class)) {
                ((KUSChatMessage) message).setState(state);
            }
        }

        upsertNewMessages(temporaryMessages);
    }

    private void sendMessage(final List<Bitmap> imageAttachments, final List<KUSModel> temporaryMessages,
                             final String text, final List<String> cachedImageKeys) {
        if (getUserSession() == null)
            return;

        new KUSUpload().uploadImages(imageAttachments, getUserSession(), new KUSImageUploadListener() {
            @Override
            public void onCompletion(Error error, final List<KUSChatAttachment> attachments) {
                if (getUserSession() == null)
                    return;

                if (error != null) {
                    insertMessagesWithState(KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_FAILED, temporaryMessages);
                    return;
                }

                final JSONArray attachmentIds = getAttachmentIds(attachments);
                HashMap<String, Object> params = new HashMap<String, Object>() {{
                    put("session", sessionId);
                    put("attachments", attachmentIds);
                }};

                if (!TextUtils.isEmpty(text))
                    params.put("body", text);

                getUserSession().getRequestManager().performRequestType(
                        KUSRequestType.KUS_REQUEST_TYPE_POST,
                        KUSConstants.URL.SEND_MESSAGE_ENDPOINT,
                        params,
                        true,
                        new KUSRequestCompletionListener() {
                            @Override
                            public void onCompletion(Error error, JSONObject response) {
                                if (getUserSession() == null)
                                    return;

                                if (error != null) {
                                    insertMessagesWithState(KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_FAILED,
                                            temporaryMessages);
                                    return;
                                }

                                handleMessageSent(response, temporaryMessages, imageAttachments, cachedImageKeys);
                            }
                        });
            }
        });
    }

    private void insertFormMessageIfNecessary() {
        if (getUserSession() == null)
            return;

        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();
        if (chatSettings != null && chatSettings.getActiveFormId() == null)
            return;

        if (getSize() == 0)
            return;

        if (isActualSession())
            return;

        if (form == null)
            return;

        // Make sure we submit the form if we just inserted a non-response question
        if (!submittingForm && !KUSFormQuestion.KUSFormQuestionRequiresResponse(formQuestion)
                && form.getQuestions() != null
                && questionIndex == form.getQuestions().size() - 1 && delayedChatMessageIds.size() == 0)
            submitFormResponses();

        KUSChatMessage lastMessage = getLatestMessage();
        if (!KUSChatMessageSentByUser(lastMessage))
            return;

        if (shouldPreventSendingMessage())
            return;

        long additionalInsertDelay = 0;
        int latestQuestionIndex = questionIndex;
        int startingOffset = formQuestion != null ? 1 : 0;
        for (int i = Math.max(questionIndex + startingOffset, 0); i < form.getQuestions().size(); i++) {
            KUSFormQuestion question = form.getQuestions().get(i);

            Date createdAt = new Date(lastMessage.getCreatedAt().getTime()
                    + KUS_CHAT_AUTO_REPLY_DELAY + additionalInsertDelay);

            String questionId = String.format("question_%s", question.getId());

            JSONObject attributes = new JSONObject();
            try {
                attributes.put("body", question.getPrompt());
                attributes.put("direction", "out");
                attributes.put("createdAt", KUSDate.stringFromDate(createdAt));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject messageJSON = new JSONObject();
            try {
                messageJSON.put("type", "chat_message");
                messageJSON.put("id", questionId);
                messageJSON.put("attributes", attributes);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                KUSChatMessage formMessage = new KUSChatMessage(messageJSON);
                insertDelayedMessage(formMessage);
                additionalInsertDelay += KUS_CHAT_AUTO_REPLY_DELAY;

            } catch (KUSInvalidJsonException e) {
                e.printStackTrace();
            }

            latestQuestionIndex = i;
            if (KUSFormQuestion.KUSFormQuestionRequiresResponse(question))
                break;
        }

        if (latestQuestionIndex == questionIndex)
            submitFormResponses();
        else
            questionIndex = latestQuestionIndex;
        formQuestion = form.getQuestions().get(questionIndex);
    }

    private boolean shouldPreventVCFormQuestionMessage() {
        if (getUserSession() == null)
            return true;

        if (!isActualSession()) {
            return true;
        }

        // If we haven't loaded the chat settings data source, prevent input
        if (!getUserSession().getChatSettingsDataSource().isFetched()) {
            return true;
        }

        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();
        if (!chatSettings.isVolumeControlEnabled()) {
            return true;
        }
        if (!vcTrackingDelayCompleted) {
            return true;
        }

        // If we are about to insert an artificial message, prevent input
        if (delayedChatMessageIds.size() > 0) {
            return true;
        }

        // When submitting the form, prevent sending more responses
        if (submittingForm) {
            return true;
        }
        if (vcFormEnd) {
            return true;
        }

        // Check that last message is VC form last message
        KUSChatMessage lastMessage = this.getLatestMessage();
        if (lastMessage.getId().equals("vc_question_2")) {
            return false;
        }

        // Check that response of previous asked question is already entered ? if not return
        if (vcFormActive && !KUSChatMessageSentByUser(lastMessage) && getOtherUserIds().size() == 0) {
            return true;
        }

        return false;
    }

    // Volume control form message sending
    private void insertVolumeControlFormMessageIfNecessary() {
        if (getUserSession() == null)
            return;

        // If any pre-condition not fulfilled
        if (shouldPreventVCFormQuestionMessage()) {
            return;
        }
        // If any message sent by Server apart from auto response or form message.
        if (getOtherUserIds().size() > 0) {
            endVolumeControlTracking();

            // Update Listeners that chat ended
            notifyAnnouncersOnContentChange();
            return;
        }

        KUSChatSession session = (KUSChatSession) getUserSession().getChatSessionsDataSource().findById(sessionId);
        if (session.getLockedAt() != null) {
            endVolumeControlTracking();

            // Update Listeners that chat ended
            notifyAnnouncersOnContentChange();
            return;
        }

        KUSChatMessage lastMessage = getLatestMessage();
        String previousMessage = lastMessage.getBody();
        if (vcFormQuestionIndex == 1 && previousMessage.equals("I'll wait")) {
            endVolumeControlTracking();

            // Update Listeners that chat ended
            notifyAnnouncersOnContentChange();
            return;
        }

        // If last question, send request on backend
        if (vcFormQuestionIndex == 3) {
            endVolumeControlTracking();
            submitVCFormResponses();
            return;

        }

        // Ask next question
        Date createdAt = new Date(lastMessage.getCreatedAt().getTime() + KUS_CHAT_AUTO_REPLY_DELAY);
        if (!vcFormActive) {
            long currentDate = (new Date()).getTime();
            if (currentDate + KUS_CHAT_AUTO_REPLY_DELAY > lastMessage.getCreatedAt().getTime()) {
                createdAt = new Date(currentDate + KUS_CHAT_AUTO_REPLY_DELAY);
            }
        }

        vcFormActive = true;


        String previousChannel = lastMessage.getBody().toLowerCase();
        KUSFormQuestion vcFormQuestion = getNextVCFormQuestion(vcFormQuestionIndex, previousChannel);

        JSONObject attributes = new JSONObject();
        try {
            attributes.put("body", vcFormQuestion.getPrompt());
            attributes.put("direction", "out");
            attributes.put("createdAt", KUSDate.stringFromDate(createdAt));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject messageJSON = new JSONObject();
        try {
            messageJSON.put("type", "chat_message");
            messageJSON.put("id", vcFormQuestion.getId());
            messageJSON.put("attributes", attributes);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            KUSChatMessage formMessage = new KUSChatMessage(messageJSON);
            insertDelayedMessage(formMessage);

        } catch (KUSInvalidJsonException e) {
            e.printStackTrace();
        }
        formQuestion = vcFormQuestion;

        // If first options response input, update view by remove options component
        if (vcFormQuestionIndex == 1) {
            notifyAnnouncersOnContentChange();
        }

        vcFormQuestionIndex++;
    }

    private void startVolumeControlFormTrackingAfterDelay(long delay) {
        final WeakReference<KUSChatMessagesDataSource> weakReference = new WeakReference<>(this);

        KUSVolumeControlTimerManager.getSharedInstance().createVolumeControlTimer(sessionId, delay,
                new KUSVolumeControlTimerListener() {
                    @Override
                    public void onDelayComplete() {
                        KUSChatMessagesDataSource strongReference = weakReference.get();
                        if (strongReference == null) {
                            return;
                        }
                        strongReference.vcTrackingDelayCompleted = true;
                        strongReference.insertVolumeControlFormMessageIfNecessary();
                    }
                });
    }

    private void endVolumeControlFormAfterDelayIfNecessary(long delay) {
        final WeakReference<KUSChatMessagesDataSource> weakReference = new WeakReference<>(this);

        Handler timeOutHandler = new Handler(Looper.getMainLooper());
        Runnable timeOutRunnable = new Runnable() {
            @Override
            public void run() {
                KUSChatMessagesDataSource strongReference = weakReference.get();
                if (strongReference == null) {
                    return;
                }

                // End Control Tracking and Automatically marked it Closed, if form not end
                if (!strongReference.vcFormEnd) {
                    strongReference.endVolumeControlTracking();
                    strongReference.endChat("timed_out", null);
                }
            }
        };
        timeOutHandler.postDelayed(timeOutRunnable, delay);
    }

    private void submitVCFormResponses() {
        if (getUserSession() == null)
            return;

        if (this.getSize() <= 5) {
            return;
        }
        if (this.getOtherUserIds().size() > 0) {
            return;

        }

        final ArrayList<HashMap<String, Object>> messagesJSON = new ArrayList<>();

        int currentMessageIndex = 4;
        String property = null;
        for (int i = 0; i < 3; i++) {
            HashMap<String, Object> formMessage = new HashMap<>();

            KUSChatMessage questionMessage = (KUSChatMessage) getList().get(currentMessageIndex);
            currentMessageIndex--;
            formMessage.put("prompt", questionMessage.getBody());
            formMessage.put("promptAt", KUSDate.stringFromDate(questionMessage.getCreatedAt()));

            if (i != 2) {
                KUSChatMessage responseMessage = (KUSChatMessage) getList().get(currentMessageIndex);
                currentMessageIndex--;
                formMessage.put("input", responseMessage.getBody());
                formMessage.put("inputAt", KUSDate.stringFromDate(responseMessage.getCreatedAt()));
                if (i == 0) {
                    property = responseMessage.getBody();
                }
            }

            if (i == 0) {
                formMessage.put("property", "conversation_replyChannel");
            } else if (i == 1) {

                if (property.toLowerCase().equals("email"))
                    formMessage.put("property", "customer_email");
                else
                    formMessage.put("property", "customer_phone");

            }
            messagesJSON.add(formMessage);
        }

        submittingForm = true;

        getUserSession().getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_POST,
                KUSConstants.URL.VOLUME_CONTROL_ENDPOINT,
                new HashMap<String, Object>() {{
                    put("messages", new JSONArray(messagesJSON));
                    put("session", getSessionId());
                }},
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {
                        if (getUserSession() == null)
                            return;

                        if (error != null) {
                            return;
                        }

                        ArrayList<KUSModel> chatMessages = new ArrayList<>();
                        JSONArray includedModelsJSON = JsonHelper.arrayFromKeyPath(response, "included");

                        if (includedModelsJSON != null) {
                            for (int i = 0; i < includedModelsJSON.length(); i++) {
                                try {
                                    JSONObject includedModelJSON = includedModelsJSON.getJSONObject(i);
                                    String type = JsonHelper.stringFromKeyPath(includedModelJSON, "type");

                                    if (type != null && type.equals(new KUSChatMessage().modelType())) {
                                        KUSChatMessage chatMessage = new KUSChatMessage(includedModelJSON);
                                        chatMessages.add(chatMessage);
                                    }
                                } catch (JSONException | KUSInvalidJsonException e) {
                                    e.printStackTrace();
                                }
                            }

                            ArrayList<KUSModel> temporaryMessages = new ArrayList<>();
                            for (KUSModel model : getList()) {
                                KUSChatMessage chatMessage = (KUSChatMessage) model;
                                if (chatMessage.getId().contains("vc_question_")) {
                                    temporaryMessages.add(chatMessage);
                                }
                            }

                            removeAll(temporaryMessages);
                            removeAll(temporaryVCMessagesResponses);
                            upsertNewMessages(chatMessages);

                        }
                        vcChatClosed = true;
                        submittingForm = false;

                        // Cancel Volume Control Polling if necessary
                        if (sessionQueuePollingManager != null)
                            sessionQueuePollingManager.cancelPolling();
                    }
                });
    }

    private void submitFormResponses() {
        JSONArray messagesJSON = new JSONArray();

        int currentMessageIndex = getSize() - 1;
        KUSChatMessage firstUserMessage = (KUSChatMessage) get(currentMessageIndex);
        currentMessageIndex--;

        JSONObject message = new JSONObject();
        try {
            message.put("input", firstUserMessage.getBody());
            message.put("inputAt", KUSDate.stringFromDate(firstUserMessage.getCreatedAt()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        messagesJSON.put(message);

        for (KUSFormQuestion question : form.getQuestions()) {

            if (currentMessageIndex < 0)
                continue;

            KUSChatMessage questionMessage = (KUSChatMessage) get(currentMessageIndex);
            currentMessageIndex--;

            JSONObject formMessage = new JSONObject();

            try {
                formMessage.put("id", question.getId());
                formMessage.put("prompt", question.getPrompt());
                formMessage.put("promptAt", KUSDate.stringFromDate(questionMessage.getCreatedAt()));


                if (KUSFormQuestion.KUSFormQuestionRequiresResponse(question)) {

                    if (currentMessageIndex >= 0) {
                        KUSChatMessage responseMessage = (KUSChatMessage) get(currentMessageIndex);
                        currentMessageIndex--;

                        formMessage.put("input", responseMessage.getBody());
                        formMessage.put("inputAt", KUSDate.stringFromDate(responseMessage.getCreatedAt()));
                        if (responseMessage.getValue() != null)
                            formMessage.put("value", responseMessage.getValue());
                    }
                }
            } catch (JSONException ignore) {
            }

            messagesJSON.put(formMessage);
        }

        submittingForm = true;
        KUSChatMessage lastUserChatMessage = null;
        for (KUSModel model : getList()) {
            KUSChatMessage chatMessage = (KUSChatMessage) model;
            if (KUSChatMessageSentByUser(chatMessage)) {
                lastUserChatMessage = chatMessage;
                break;
            }
        }

        if (lastUserChatMessage != null)
            messageRetryHashMap.put(lastUserChatMessage.getId(),
                    new KUSFormRetry(messagesJSON, form.getId(), lastUserChatMessage));

        actuallySubmitForm(messagesJSON, form.getId(), lastUserChatMessage);
    }

    private void retrySubmittingForm(final KUSFormRetry formRetry) {
        if (formRetry.getLastUserChatMessage() != null) {
            removeAll(new ArrayList<KUSModel>() {{
                add(formRetry.getLastUserChatMessage());
            }});
            formRetry.getLastUserChatMessage().setState(KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_SENDING);
            upsertNewMessages(new ArrayList<KUSModel>() {{
                add(formRetry.getLastUserChatMessage());
            }});
        }

        actuallySubmitForm(formRetry.getMessagesJSON(), formRetry.getFormId(), formRetry.getLastUserChatMessage());
    }

    private void actuallySubmitForm(final JSONArray messagesJSON, String formId,
                                    final KUSChatMessage lastUserChatMessage) {
        if (getUserSession() == null)
            return;

        getUserSession().getChatSessionsDataSource().submitFormMessages(
                messagesJSON,
                formId,
                new KUSFormCompletionListener() {
                    @Override
                    public void onComplete(Error error, KUSChatSession chatSession, List<KUSModel> chatMessages) {

                        if (error != null) {
                            handleError(lastUserChatMessage);
                            return;
                        }

                        if (getUserSession() == null)
                            return;

                        // If the form contained an email prompt, mark the local session as having submitted email
                        if (form != null && form.containsEmailQuestion())
                            getUserSession().getSharedPreferences().setDidCaptureEmail(true);

                        // Set variable for business hours
                        if (!getUserSession().getScheduleDataSource().isActiveBusinessHours()
                                && form != null && form.getQuestions().size() > 0) {
                            nonBusinessHours = true;
                        }

                        if (!isActualSession()) {
                            KUSChatSession tempSession = (KUSChatSession) getUserSession()
                                    .getChatSessionsDataSource().findById(sessionId);

                            getUserSession().getChatMessagesDataSources().remove(sessionId);
                            getUserSession().getChatSessionsDataSource()
                                    .removeAll(Collections.singletonList((KUSModel) tempSession));
                        }

                        // Grab the session id
                        sessionId = chatSession != null ? chatSession.getId() : null;
                        form = null;
                        questionIndex = -1;
                        formQuestion = null;
                        submittingForm = false;

                        //Replace all of the local messages with the new ones
                        removeAll(getList());
                        upsertNewMessages(chatMessages);
                        messageRetryHashMap.remove(lastUserChatMessage.getId());

                        // Create queue polling manager for volume control form
                        sessionQueuePollingManager = new KUSSessionQueuePollingManager(getUserSession(), sessionId);

                        // Insert the current messages data source into the userSession's lookup table
                        getUserSession().getChatMessagesDataSources().put(sessionId, KUSChatMessagesDataSource.this);

                        //Notify Listeners
                        for (KUSPaginatedDataSourceListener listener : listeners) {
                            if (listener instanceof KUSChatMessagesDataSourceListener) {
                                ((KUSChatMessagesDataSourceListener) listener).onCreateSessionId(
                                        KUSChatMessagesDataSource.this, sessionId);
                            }
                        }

                        // Update last seen locally for the session
                        getUserSession().getChatSessionsDataSource().updateLocallyLastSeenAtForSessionId(sessionId);
                    }
                }
        );
    }

    private void handleError(final KUSChatMessage lastUserChatMessage) {
        if (lastUserChatMessage != null) {
            removeAll(new ArrayList<KUSModel>() {{
                add(lastUserChatMessage);
            }});
            lastUserChatMessage.setState(KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_FAILED);
            upsertAll(new ArrayList<KUSModel>() {{
                add(lastUserChatMessage);
            }});
        }
    }

    private void insertDelayedMessage(final KUSChatMessage chatMessage) {

        //Sanity Check
        if (chatMessage.getId().length() == 0)
            return;

        //Only insert the message if it doesn't exist already
        if (findById(chatMessage.getId()) != null)
            return;

        long delay = chatMessage.getCreatedAt().getTime() - Calendar.getInstance().getTime().getTime();
        if (delay <= 0) {
            upsertAll(new ArrayList<KUSModel>() {{
                add(chatMessage);
            }});
            return;
        }

        delayedChatMessageIds.add(chatMessage.getId());
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                delayedChatMessageIds.remove(chatMessage.getId());
                boolean doesNotAlreadyContainMessage = findById(chatMessage.getId()) == null;
                upsertAll(new ArrayList<KUSModel>() {{
                    add(chatMessage);
                }});
                if (doesNotAlreadyContainMessage)
                    KUSAudio.playMessageReceivedSound();
            }
        };
        mainHandler.postDelayed(myRunnable, delay);

    }

    private void startVolumeControlTracking() {
        if (getUserSession() == null)
            return;

        if (!isActualSession()) {
            return;
        }

        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();
        if (!chatSettings.isVolumeControlEnabled()) {
            return;
        }

        // Check if business hours enabled and not in business hours
        if (!getUserSession().getScheduleDataSource().isActiveBusinessHours()) {
            return;
        }

        if (vcTrackingStarted) {
            return;
        }
        vcTrackingStarted = true;

        if (chatSettings.getVolumeControlMode() == KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_DELAYED) {
            long delay = chatSettings.getPromptDelay() * 1000;
            startVolumeControlFormTrackingAfterDelay(delay);

            // Automatically end chat
            if (chatSettings.isMarkDoneAfterTimeout()) {
                long timeOutDelay = (chatSettings.getTimeOut() + chatSettings.getPromptDelay()) * 1000;
                endVolumeControlFormAfterDelayIfNecessary(timeOutDelay);
            }
        } else if (sessionQueuePollingManager != null && chatSettings.getVolumeControlMode() ==
                KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_UPFRONT) {

            sessionQueuePollingManager.addListener(this);
            sessionQueuePollingManager.startPolling();
        }
    }

    private void endVolumeControlTracking() {
        vcFormEnd = true;
        vcFormActive = false;
        KUSVolumeControlTimerManager.getSharedInstance().removeVcTimer(sessionId);
    }

    private KUSFormQuestion getNextVCFormQuestion(int index, String previousChannel) {
        if (getUserSession() == null)
            return null;

        if (index == 0) {
            KUSChatSettings chatSettings = (KUSChatSettings) getUserSession()
                    .getChatSettingsDataSource().getObject();

            List<String> options = new ArrayList<>();
            for (String option : chatSettings.getFollowUpChannels()) {
                options.add(option.substring(0, 1).toUpperCase() + option.substring(1).toLowerCase());
            }

            if (!chatSettings.isHideWaitOption()) {
                options.add("I'll wait");

            }

            //volume_control_alternative_method_question
            String prompt = Kustomer.getContext()
                    .getString(R.string.com_kustomer_volume_control_alternative_method_question);
            KUSSessionQueue sessionQueue = sessionQueuePollingManager.getSessionQueue();

            if (chatSettings.getVolumeControlMode() == KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_UPFRONT
                    && sessionQueue.getEstimatedWaitTimeSeconds() != 0) {

                String currentWaitTime = Kustomer.getContext()
                        .getString(R.string.com_kustomer_our_current_wait_time_is_approximately);
                String upfrontAlternatePrompt = Kustomer.getContext()
                        .getString(R.string.com_kustomer_upfront_volume_control_alternative_method_question);

                String humanReadableTextFromSeconds = KUSDate.humanReadableTextFromSeconds(
                        Kustomer.getContext(), sessionQueue.getEstimatedWaitTimeSeconds());
                prompt = String.format("%s %s. %s", currentWaitTime, humanReadableTextFromSeconds,
                        upfrontAlternatePrompt);
            }


            JSONObject formMessage = new JSONObject();
            try {
                formMessage.put("id", "vc_question_0");
                formMessage.put("name", "Volume Form 0");
                formMessage.put("prompt", prompt);
                formMessage.put("type", "property");
                formMessage.put("property", "followup_channel");
                formMessage.put("values", new JSONArray(options));
            } catch (JSONException ignore) {
            }

            try {
                return new KUSFormQuestion(formMessage);
            } catch (KUSInvalidJsonException e) {
                e.printStackTrace();
            }

        } else if (index == 1) {
            String property;
            String prompt;
            if (previousChannel.toLowerCase().equals("email")) {
                property = "customer_email";
                prompt = Kustomer.getContext()
                        .getString(R.string.com_kustomer_volume_control_email_question);
            } else {
                property = "customer_phone";
                prompt = Kustomer.getContext()
                        .getString(R.string.com_kustomer_volume_control_phone_question);
            }
            JSONObject formMessage = new JSONObject();

            try {
                formMessage.put("id", "vc_question_1");
                formMessage.put("name", "Volume Form 1");
                formMessage.put("prompt", prompt);
                formMessage.put("type", "response");
                formMessage.put("property", property);
            } catch (JSONException ignore) {
            }

            try {
                return new KUSFormQuestion(formMessage);
            } catch (KUSInvalidJsonException e) {
                e.printStackTrace();
            }

        } else if (index == 2) {
            JSONObject formMessage = new JSONObject();
            String message = Kustomer.getContext()
                    .getString(R.string.com_kustomer_volume_control_thankyou_response);

            try {
                formMessage.put("id", "vc_question_2");
                formMessage.put("name", "Volume Form 2");
                formMessage.put("prompt", message);
                formMessage.put("type", "message");
            } catch (JSONException ignore) {
            }

            try {
                return new KUSFormQuestion(formMessage);
            } catch (KUSInvalidJsonException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public List<KUSModel> objectsFromJSON(JSONObject jsonObject) {
        return JsonHelper.kusChatModelsFromJSON(Kustomer.getContext(), jsonObject);
    }

    private void handleMessageSent(JSONObject response, List<KUSModel> temporaryMessages,
                                   List<Bitmap> attachments, List<String> cachedImageKeys) {
        List<KUSModel> finalMessages = objectsFromJSON(JsonHelper.jsonObjectFromKeyPath(response, "data"));

        if (finalMessages == null)
            return;

        //Store the local image data in our cache for the remote image urls
        KUSChatMessage firstMessage = (KUSChatMessage) finalMessages.get(0);
        String messageId = firstMessage.getId().split("_")[0];

        for (int i = 0; i < (firstMessage.getAttachmentIds() != null ? firstMessage.getAttachmentIds().size() : 0); i++) {
            Bitmap attachment = attachments.get(i);
            String attachmentId = (String) firstMessage.getAttachmentIds().get(i);
            try {
                URL attachmentURL = KUSChatMessage.attachmentUrlForMessageId(messageId, attachmentId);
                new KUSCache().addBitmapToMemoryCache(attachmentURL.toString(), attachment);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        // Remove the temporary objects and insert the new/sent objects
        removeAll(temporaryMessages);
        upsertNewMessages(finalMessages);

        // Remove the temporary images from the cache

        if (cachedImageKeys != null)
            for (String imageKey : cachedImageKeys) {
                new KUSCache().removeBitmapFromMemCache(imageKey);
            }

        for (KUSModel model : temporaryMessages) {
            KUSChatMessage temporaryMessage = (KUSChatMessage) model;
            messageRetryHashMap.remove(temporaryMessage.getId());
        }

        if (isProactiveCampaign)
            closeProactiveCampaignIfNecessary();

        // Update the locally session last seen
        getUserSession().getChatSessionsDataSource().updateLocallyLastSeenAtForSessionId(sessionId);
    }

    private JSONArray getAttachmentIds(List<KUSChatAttachment> attachments) {

        if (attachments.size() == 0)
            return null;

        JSONArray ids = new JSONArray();

        for (KUSChatAttachment attachment : attachments) {
            ids.put(attachment.getId());
        }

        return ids;
    }
    //endregion

    //region Accessors

    public boolean shouldAllowAttachments() {
        return isActualSession() && !vcFormActive;
    }

    public boolean isAnyMessageByCurrentUser() {
        for (KUSModel message : getList()) {
            KUSChatMessage chatMessage = (KUSChatMessage) message;
            if (KUSChatMessageSentByUser(chatMessage))
                return true;
        }
        return false;
    }

    public String getFirstOtherUserId() {
        for (KUSModel message : getList()) {
            KUSChatMessage chatMessage = (KUSChatMessage) message;
            if (!KUSChatMessageSentByUser(chatMessage))
                return chatMessage.getSentById();
        }
        return null;
    }

    public List<String> getOtherUserIds() {
        HashSet<String> userIdsSet = new HashSet<>();
        List<String> otherUserIds = new ArrayList<>();

        for (KUSModel message : getList()) {
            KUSChatMessage kusChatMessage = (KUSChatMessage) message;
            if (!KUSChatMessageSentByUser(kusChatMessage)) {
                String sentById = kusChatMessage.getSentById();
                if (sentById != null && !userIdsSet.contains(sentById)) {
                    userIdsSet.add(sentById);
                    otherUserIds.add(sentById);
                }
            }
        }

        return otherUserIds;
    }

    @Override
    public boolean isFetched() {
        return createdLocally || super.isFetched();
    }

    @Override
    public boolean isFetchedAll() {
        return createdLocally || super.isFetchedAll();
    }

    public String getSessionId() {
        return sessionId;
    }

    public KUSSessionQueuePollingManager getSessionQueuePollingManager() {
        return sessionQueuePollingManager;
    }

    //endregion

    //region Listener
    @Override
    public void objectDataSourceOnLoad(KUSObjectDataSource dataSource) {

        if (form == null && dataSource.getClass().equals(KUSFormDataSource.class))
            form = (KUSForm) dataSource.getObject();

        insertFormMessageIfNecessary();
    }

    @Override
    public void objectDataSourceOnError(final KUSObjectDataSource dataSource, Error error) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                dataSource.fetch();
            }
        };
        mainHandler.postDelayed(myRunnable, 1000);

    }

    @Override
    public void onCreateSessionId(KUSChatMessagesDataSource source, String sessionId) {
        startVolumeControlTracking();
        closeProactiveCampaignIfNecessary();
    }

    @Override
    public void onLoad(KUSPaginatedDataSource dataSource) {

    }

    @Override
    public void onError(KUSPaginatedDataSource dataSource, Error error) {
    }

    @Override
    public void onContentChange(KUSPaginatedDataSource dataSource) {
        insertFormMessageIfNecessary();
        insertVolumeControlFormMessageIfNecessary();
    }

    @Override
    public void onPollingStarted(KUSSessionQueuePollingManager manager) {

    }

    @Override
    public void onSessionQueueUpdated(KUSSessionQueuePollingManager manager, KUSSessionQueue sessionQueue) {
        if (getUserSession() == null)
            return;

        // Automatically end chat
        KUSChatSettings chatSettings = (KUSChatSettings) getUserSession().getChatSettingsDataSource().getObject();

        if (chatSettings == null)
            return;

        boolean estimatedWaitTimeIsOverThreshold = sessionQueue.getEstimatedWaitTimeSeconds() != 0
                && sessionQueue.getEstimatedWaitTimeSeconds() > chatSettings.getUpfrontWaitThreshold();

        if (!vcTrackingDelayCompleted && estimatedWaitTimeIsOverThreshold) {
            startVolumeControlFormTrackingAfterDelay(0);

            if (chatSettings.getMarkDoneAfterTimeout()) {
                long delay = chatSettings.getTimeOut() * 1000;
                endVolumeControlFormAfterDelayIfNecessary(delay);
            }
        }
    }

    @Override
    public void onPollingEnd(KUSSessionQueuePollingManager manager) {

    }

    @Override
    public void onPollingCanceled(KUSSessionQueuePollingManager manager) {

    }

    @Override
    public void onFailure(Error error, KUSSessionQueuePollingManager manager) {

    }

    //endregion

    //region Interface
    public interface onCreateSessionListener {
        void onComplete(boolean success, Error error);
    }

    public interface OnEndChatListener {
        void onComplete(boolean success);
    }
    //endregion
}
