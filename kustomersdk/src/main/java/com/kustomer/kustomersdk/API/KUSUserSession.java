package com.kustomer.kustomersdk.API;

import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.DataSources.KUSChatMessagesDataSource;
import com.kustomer.kustomersdk.DataSources.KUSChatSessionsDataSource;
import com.kustomer.kustomersdk.DataSources.KUSChatSettingsDataSource;
import com.kustomer.kustomersdk.DataSources.KUSDelegateProxy;
import com.kustomer.kustomersdk.DataSources.KUSFormDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.DataSources.KUSScheduleDataSource;
import com.kustomer.kustomersdk.DataSources.KUSTrackingTokenDataSource;
import com.kustomer.kustomersdk.DataSources.KUSUserDataSource;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Helpers.KUSSharedPreferences;
import com.kustomer.kustomersdk.Interfaces.KUSPaginatedDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Managers.KUSStatsManager;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSCustomerDescription;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSTrackingToken;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import org.json.JSONObject;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSUserSession implements Serializable, KUSPaginatedDataSourceListener {

    //region Properties
    private String orgId;
    private String orgName;
    private String organizationName; //UserFacing


    private KUSChatSessionsDataSource chatSessionsDataSource;
    private KUSChatSettingsDataSource chatSettingsDataSource;
    private KUSTrackingTokenDataSource trackingTokenDataSource;
    private KUSFormDataSource formDataSource;
    private KUSScheduleDataSource scheduleDataSource;

    private HashMap<String, KUSUserDataSource> userDataSources;
    private HashMap<String, KUSChatMessagesDataSource> chatMessagesDataSources;

    private KUSRequestManager requestManager;
    private KUSPushClient pushClient;
    private KUSDelegateProxy delegateProxy;
    private KUSClientActivityManager activityManager;
    private KUSStatsManager statsManager;

    private KUSSharedPreferences sharedPreferences;

    boolean shouldCaptureEmail;
    //endregion

    //region LifeCycle
    public KUSUserSession(String orgName, String orgId, boolean reset){
        this.orgName = orgName;
        this.orgId = orgId;

        if(this.orgName != null && this.orgName.length() > 0) {
            String firstLetter = this.orgName.substring(0,1).toUpperCase();
            this.organizationName = firstLetter.concat(this.orgName.substring(1));
        }

        if(reset){
            getTrackingTokenDataSource().reset();
            getSharedPreferences().reset();
        }

        getChatSettingsDataSource().fetch();
        getScheduleDataSource().fetch();
        getPushClient();

        chatSessionsDataSource.addListener(this);
        chatSessionsDataSource.fetchLatest();
    }

    public KUSUserSession(String orgName, String orgId){
        this(orgName,orgId,false);
    }

    //endregion


    //region public methods
    public void removeAllListeners() {
        pushClient.removeAllListeners();

        if(chatSessionsDataSource != null)
            chatSessionsDataSource.removeAllListeners();

        if(chatSettingsDataSource != null)
            chatSettingsDataSource.removeAllListeners();

        if(trackingTokenDataSource != null)
            trackingTokenDataSource.removeAllListeners();

        if(formDataSource != null)
            formDataSource.removeAllListeners();

        if(userDataSources != null && userDataSources.keySet().size() > 0)
            for(String key : userDataSources.keySet()){
                KUSUserDataSource dataSource = userDataSources.get(key);

                if(dataSource != null)
                    dataSource.removeAllListeners();
            }

        if(chatMessagesDataSources != null && chatMessagesDataSources.keySet().size() > 0)
            for(String key : chatMessagesDataSources.keySet()) {
                KUSChatMessagesDataSource dataSource = chatMessagesDataSources.get(key);

                if(dataSource != null)
                    dataSource.removeAllListeners();
            }
    }

    public KUSChatMessagesDataSource chatMessageDataSourceForSessionId(String sessionId){
        if(sessionId == null || sessionId.isEmpty())
            return null;

        KUSChatMessagesDataSource chatMessagesDataSource = getChatMessagesDataSources().get(sessionId);
        if (chatMessagesDataSource == null) {
            chatMessagesDataSource = new KUSChatMessagesDataSource(this, sessionId);
            chatMessagesDataSources.put(sessionId, chatMessagesDataSource);
        }
        return chatMessagesDataSource;
    }

    public KUSUserDataSource userDataSourceForUserId(String userId) {
        if(userId == null || userId.length() == 0 || userId.equals("__team"))
            return null;

        KUSUserDataSource userDataSource = getUserDataSources().get(userId);
        if(userDataSource == null){
            userDataSource = new KUSUserDataSource(this,userId);
            getUserDataSources().put(userId,userDataSource);
        }

        return userDataSource;
    }

    public void submitEmail(String emailAddress){

        getSharedPreferences().setDidCaptureEmail(true);
        final WeakReference<KUSUserSession> weakReference = new WeakReference<>(this);
        KUSCustomerDescription customerDescription = new KUSCustomerDescription();
        customerDescription.setEmail(emailAddress);

        describeCustomer(customerDescription, new KUSCustomerCompletionListener() {
            @Override
            public void onComplete(boolean success, Error error) {
                if(error != null || !success){
                    KUSLog.KUSLogError(String.format("Error submitting email: %s",
                            error != null ? error.toString() : ""));
                    return;
                }

                weakReference.get().trackingTokenDataSource.fetch();
            }
        });
    }

    public void describeCustomer(KUSCustomerDescription customerDescription, final KUSCustomerCompletionListener listener){

        HashMap<String, Object> formData = customerDescription.formData();

        if(formData.size() == 0)
            throw new AssertionError("Attempted to describe a customer with no attributes se");

        // If email is passed in custom description
        if (customerDescription.getEmail() != null) {
            getSharedPreferences().setDidCaptureEmail(true);
        }

        getRequestManager().performRequestType(
                KUSRequestType.KUS_REQUEST_TYPE_PATCH,
                KUSConstants.URL.CURRENT_CUSTOMER_ENDPOINT,
                formData,
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, JSONObject response) {
                        if(listener != null)
                            listener.onComplete(error == null, error);
                    }
                }
        );
    }


    //endregion

    //region Getters

    public KUSChatSessionsDataSource getChatSessionsDataSource() {
        if (chatSessionsDataSource == null)
            chatSessionsDataSource = new KUSChatSessionsDataSource(this);
        return chatSessionsDataSource;
    }

    public KUSChatSettingsDataSource getChatSettingsDataSource() {
        if (chatSettingsDataSource == null)
            chatSettingsDataSource = new KUSChatSettingsDataSource(this);
        return chatSettingsDataSource;
    }

    public KUSTrackingTokenDataSource getTrackingTokenDataSource() {
        if (trackingTokenDataSource == null)
            trackingTokenDataSource = new KUSTrackingTokenDataSource(this);
        return trackingTokenDataSource;
    }

    public KUSFormDataSource getFormDataSource() {
        if (formDataSource == null)
            formDataSource = new KUSFormDataSource(this);
        return formDataSource;
    }

    public KUSRequestManager getRequestManager() {
        if (requestManager == null)
            requestManager = new KUSRequestManager(this);
        return requestManager;
    }

    public KUSPushClient getPushClient() {
        if (pushClient == null)
            pushClient = new KUSPushClient(this);
        return pushClient;
    }


    public KUSSharedPreferences getSharedPreferences() {
        if (sharedPreferences == null) {
            try {
                sharedPreferences = new KUSSharedPreferences(Kustomer.getContext(), this);
            }catch (NullPointerException ignore){}
        }
        return sharedPreferences;
    }

    public HashMap<String, KUSUserDataSource> getUserDataSources() {
        if (userDataSources == null)
            userDataSources = new HashMap<>();
        return userDataSources;
    }

    public HashMap<String, KUSChatMessagesDataSource> getChatMessagesDataSources() {
        if (chatMessagesDataSources == null)
            chatMessagesDataSources = new HashMap<>();
        return chatMessagesDataSources;
    }

    public KUSClientActivityManager getActivityManager(){
        if(activityManager == null)
            activityManager = new KUSClientActivityManager(this);

        return activityManager;
    }

    public KUSScheduleDataSource getScheduleDataSource() {
        if(scheduleDataSource == null)
            scheduleDataSource = new KUSScheduleDataSource(this);

        return scheduleDataSource;
    }

    @NonNull
    public KUSStatsManager getStatsManager() {
        if(statsManager == null)
            statsManager = new KUSStatsManager(this);

        return statsManager;
    }
    //endregion

    //region Accessors

    public KUSDelegateProxy getDelegateProxy() {
        if(delegateProxy == null)
            delegateProxy = new KUSDelegateProxy();

        return delegateProxy;
    }

    public void setDelegateProxy(KUSDelegateProxy delegateProxy) {
        this.delegateProxy = delegateProxy;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public boolean isShouldCaptureEmail() {

        KUSTrackingToken trackingToken = (KUSTrackingToken) trackingTokenDataSource.getObject();
        if(trackingToken != null){
            if(trackingToken.getVerified()){
                return false;
            }

            return !getSharedPreferences().getDidCaptureEmail();
        }
        return false;
    }

    //endregion

    //region Callback
    @Override
    public void onLoad(KUSPaginatedDataSource dataSource) {
        if(dataSource == chatSessionsDataSource){
            chatSessionsDataSource.removeListener(this);

            for(KUSModel model : chatSessionsDataSource.getList()){
                KUSChatSession chatSession = (KUSChatSession) model;
                //Fetch any messages that might contribute to unread count
                KUSChatMessagesDataSource messagesDataSource = chatMessageDataSourceForSessionId(chatSession.getId());
                boolean hasUnseen = chatSession.getLastSeenAt() == null || (chatSession.getLastMessageAt() != null && chatSession.getLastMessageAt().after(chatSession.getLastSeenAt()));
                if(hasUnseen && !messagesDataSource.isFetched())
                    messagesDataSource.fetchLatest();
            }
        }
    }

    @Override
    public void onError(KUSPaginatedDataSource dataSource, Error error) {

    }

    @Override
    public void onContentChange(KUSPaginatedDataSource dataSource) {

    }

    //endregion

    //region Interface
    public interface KUSCustomerCompletionListener{
        void onComplete(boolean success, Error error);
    }
    //endregion
}
