package com.kustomer.kustomersdk.Managers;

import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.DataSources.KUSChatMessagesDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.Interfaces.KUSCustomerStatsListener;
import com.kustomer.kustomersdk.Interfaces.KUSPaginatedDataSourceListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Receivers.NetworkStateReceiver;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class KUSNetworkStateManager implements NetworkStateReceiver.NetworkStateReceiverListener,
        KUSPaginatedDataSourceListener {
    //region properties

    private static final long KUS_RETRY_DELAY = 1000;
    private static KUSNetworkStateManager kusNetworkStateManager;

    @NonNull
    private NetworkStateReceiver networkStateReceiver;
    @NonNull
    private ConcurrentHashMap<String, KUSChatSession> previousChatSessions;

    //endregion

    //region Lifecycle

    private KUSNetworkStateManager() {
        networkStateReceiver = new NetworkStateReceiver();
        previousChatSessions = new ConcurrentHashMap<>();
        networkStateReceiver.addListener(this);
    }

    public static KUSNetworkStateManager getSharedInstance() {
        if (kusNetworkStateManager == null) {
            kusNetworkStateManager = new KUSNetworkStateManager();
        }
        return kusNetworkStateManager;
    }

    //endregion

    //region Public methods

    public void startObservingNetworkState() {
        Kustomer.getContext().registerReceiver(kusNetworkStateManager.networkStateReceiver,
                new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public boolean isConnected() {
        return networkStateReceiver.isConnected();
    }

    public void stopObservingNetworkState() {
        networkStateReceiver.removeListener(this);
        Kustomer.getContext().unregisterReceiver(kusNetworkStateManager.networkStateReceiver);
    }

    //endregion

    //region Private Methods

    private void updatePreviousChatSessions() {
        KUSUserSession userSession = Kustomer.getSharedInstance().getUserSession();
        if (userSession == null)
            return;

        previousChatSessions.clear();
        for (KUSModel model : userSession.getChatSessionsDataSource().getList()) {
            KUSChatSession chatSession = (KUSChatSession) model;
            previousChatSessions.put(chatSession.getId(), chatSession);
        }
    }

    //endregion

    //region Listener

    @Override
    public void networkAvailable() {
        try {
            final KUSUserSession userSession = Kustomer.getSharedInstance().getUserSession();

            userSession.getStatsManager().updateStats(new KUSCustomerStatsListener() {
                @Override
                public void onCompletion(boolean sessionUpdated) {
                    if (sessionUpdated) {
                        userSession.getChatSessionsDataSource().addListener(KUSNetworkStateManager.this);
                        updatePreviousChatSessions();
                        userSession.getChatSessionsDataSource().fetchLatest();
                    } else {
                        KUSVolumeControlTimerManager.getSharedInstance().resumeVcTimers();
                    }
                }
            });
        } catch (AssertionError ignored) {

        }
    }

    @Override
    public void networkUnavailable() {
        KUSVolumeControlTimerManager.getSharedInstance().pauseVcTimers();
    }

    @Override
    public void onLoad(KUSPaginatedDataSource dataSource) {
        KUSUserSession userSession = Kustomer.getSharedInstance().getUserSession();
        if (dataSource != userSession.getChatSessionsDataSource())
            return;

        userSession.getChatSessionsDataSource().removeListener(KUSNetworkStateManager.this);
        KUSVolumeControlTimerManager.getSharedInstance().resumeVcTimers();
    }

    @Override
    public void onError(final KUSPaginatedDataSource dataSource, Error error) {
        final KUSUserSession userSession = Kustomer.getSharedInstance().getUserSession();
        if (dataSource != userSession.getChatSessionsDataSource())
            return;
        userSession.getChatSessionsDataSource().removeListener(KUSNetworkStateManager.this);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                userSession.getChatSessionsDataSource().addListener(KUSNetworkStateManager.this);
                userSession.getChatSessionsDataSource().fetchLatest();
            }
        }, KUS_RETRY_DELAY);
    }

    @Override
    public void onContentChange(KUSPaginatedDataSource dataSource) {
        KUSUserSession userSession = Kustomer.getSharedInstance().getUserSession();
        if (dataSource != userSession.getChatSessionsDataSource())
            return;

        userSession.getChatSessionsDataSource().removeListener(KUSNetworkStateManager.this);

        List<KUSModel> newChatSessions = userSession.getChatSessionsDataSource().getList();
        for (KUSModel model : newChatSessions) {
            KUSChatSession chatSession = (KUSChatSession) model;

            KUSChatSession prevChatSession = previousChatSessions.get(chatSession.getId());

            KUSChatMessagesDataSource messagesDataSource = userSession
                    .chatMessageDataSourceForSessionId(chatSession.getId());
            if (prevChatSession != null) {

                KUSChatMessage latestChatMessage = null;

                if (messagesDataSource != null && !messagesDataSource.getList().isEmpty())
                    latestChatMessage = (KUSChatMessage) messagesDataSource.getList().get(0);

                Date sessionLastSeenAt = userSession.getChatSessionsDataSource()
                        .lastSeenAtForSessionId(chatSession.getId());

                boolean isUpdatedSession = prevChatSession.getLastMessageAt() == null
                        || (chatSession.getLastMessageAt() != null
                        && chatSession.getLastMessageAt().after(prevChatSession.getLastMessageAt()));

                boolean lastSeenBeforeMessage = sessionLastSeenAt == null
                        || (chatSession.getLastMessageAt() != null
                        && chatSession.getLastMessageAt().after(sessionLastSeenAt));

                boolean lastMessageAtNewerThanLocalLastMessage = latestChatMessage == null
                        || latestChatMessage.getCreatedAt() == null
                        || (chatSession.getLastMessageAt() != null
                        && chatSession.getLastMessageAt().after(latestChatMessage.getCreatedAt()));

                boolean chatSessionSetToLock = chatSession.getLockedAt() != null
                        && !chatSession.getLockedAt().equals(prevChatSession.getLockedAt());

                // Check that new message arrived or not
                if (isUpdatedSession && lastSeenBeforeMessage && lastMessageAtNewerThanLocalLastMessage) {
                    if (messagesDataSource != null)
                        messagesDataSource.fetchLatest();

                } else if (chatSessionSetToLock) { // Check that session lock state changed
                    if (messagesDataSource != null)
                        messagesDataSource.fetchLatest();

                } else {
                    if (messagesDataSource != null)
                        KUSVolumeControlTimerManager.getSharedInstance()
                                .resumeVcTimer(messagesDataSource.getSessionId());
                }

            } else {
                if (messagesDataSource != null)
                    messagesDataSource.fetchLatest();
            }
        }
    }

    //endregion
}
