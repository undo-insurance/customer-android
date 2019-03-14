package com.kustomer.kustomersdk.API;

import android.os.Handler;
import android.os.Looper;

import com.kustomer.kustomersdk.DataSources.KUSChatMessagesDataSource;
import com.kustomer.kustomersdk.DataSources.KUSObjectDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.Helpers.KUSAudio;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSPaginatedDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSPusherShouldConnectListener;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSTrackingToken;
import com.kustomer.kustomersdk.Utils.JsonHelper;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Views.KUSNotificationWindow;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PresenceChannel;
import com.pusher.client.channel.PresenceChannelEventListener;
import com.pusher.client.channel.User;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.HttpAuthorizer;

import org.json.JSONObject;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSPushClient implements Serializable, KUSObjectDataSourceListener, KUSPaginatedDataSourceListener {

    //region Properties
    private static final long KUS_SHOULD_CONNECT_TO_PUSHER_RECENCY_THRESHOLD = 60000;
    private static final long LAZY_POLLING_TIMER_INTERVAL = 30000;
    private static final long ACTIVE_POLLING_TIMER_INTERVAL = 7500;
    private long currentPollingTimerInterval = 0;

    private Pusher pusherClient;
    private PresenceChannel pusherChannel;
    private ConcurrentHashMap<String, KUSChatSession> previousChatSessions;

    private WeakReference<KUSUserSession> userSession;
    private boolean isSupportScreenShown = false;
    private Timer pollingTimer;
    private String pendingNotificationSessionId;
    private Handler handler;

    private boolean isPusherTrackingStarted;
    private boolean sessionUpdated;
    private boolean didPusherLostPackets;
    private Date lastActivity;
    //endregion

    //region LifeCycle
    KUSPushClient(KUSUserSession userSession) {
        this.userSession = new WeakReference<>(userSession);
        isPusherTrackingStarted = false;

        userSession.getChatSessionsDataSource().addListener(this);
        userSession.getChatSettingsDataSource().addListener(this);
        userSession.getTrackingTokenDataSource().addListener(this);

        connectToChannelsIfNecessary();
    }
    //endregion

    //region Public Methods
    public void onClientActivityTick() {
        // We only need to poll for client activity changes if we are not connected to the socket
        if (pusherClient.getConnection().getState() != ConnectionState.CONNECTED || !pusherChannel.isSubscribed()) {
            onPollTick();
        }
    }

    public void removeAllListeners() {
        if (pusherClient != null) {
            String pusherChannelName = getPusherChannelName();
            if (pusherChannelName != null)
                pusherClient.unsubscribe(pusherChannelName);
            pusherClient.disconnect();
        }

        if (pollingTimer != null) {
            pollingTimer.cancel();
            pollingTimer = null;
        }

        if (handler != null) {
            handler.removeCallbacks(null);
            handler = null;
        }

    }
    //endregion

    //region Private Methods
    private URL getPusherAuthURL() {
        return userSession.get().getRequestManager().urlForEndpoint(KUSConstants.URL.PUSHER_AUTH);
    }

    private String getPusherChannelName() {
        if (userSession.get() == null)
            return null;

        KUSTrackingToken trackingTokenObj = (KUSTrackingToken) userSession.get()
                .getTrackingTokenDataSource().getObject();

        if (trackingTokenObj != null)
            return String.format("presence-external-%s-tracking-%s", userSession.get().getOrgId(),
                    trackingTokenObj.getTrackingId());
        return null;
    }

    private void connectToChannelsIfNecessary() {
        KUSChatSettings chatSettings = null;

        if (userSession.get() != null)
            chatSettings = (KUSChatSettings) userSession.get().getChatSettingsDataSource().getObject();

        if (pusherClient == null && chatSettings != null && chatSettings.getPusherAccessKey() != null) {
            HashMap<String, String> headers = new HashMap<>();
            headers.put(KUSConstants.Keys.K_KUSTOMER_TRACKING_TOKEN_HEADER_KEY,
                    userSession.get().getTrackingTokenDataSource().getCurrentTrackingToken());
            headers.putAll(userSession.get().getRequestManager().genericHTTPHeaderValues);

            HttpAuthorizer authorizer = new HttpAuthorizer(getPusherAuthURL().toString());
            authorizer.setHeaders(headers);

            PusherOptions options = new PusherOptions().setEncrypted(true).setAuthorizer(authorizer);
            pusherClient = new Pusher(chatSettings.getPusherAccessKey(), options);
        }

        boolean shouldConnectToPusher = pusherClient != null &&
                pusherClient.getConnection().getState() != ConnectionState.CONNECTED;

        if (shouldConnectToPusher) {
            pusherClient.connect(pusherConnectionListener);
        }

        if (!isPusherTrackingStarted) {
            isPusherTrackingStarted = true;

            handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (userSession.get() == null)
                        return;

                    isPusherTrackingStarted = false;

                    updateStats(new KUSPusherShouldConnectListener() {
                        @Override
                        public void onCompletion() {
                            // Get latest session on update to avoid packet loss during socket connection
                            if (sessionUpdated) {
                                didPusherLostPackets = true;
                                userSession.get().getChatSessionsDataSource().fetchLatest();
                            }
                            connectToChannelsIfNecessary();
                        }
                    });
                }
            };
            handler.postDelayed(runnable, KUS_SHOULD_CONNECT_TO_PUSHER_RECENCY_THRESHOLD);
        }

        String pusherChannelName = getPusherChannelName();
        if (pusherClient != null && pusherChannelName != null && pusherChannel == null) {

            try {
                pusherChannel = pusherClient.subscribePresence(pusherChannelName);

                pusherChannel.bind(KUSConstants.PusherEventNames.SEND_MESSAGE_EVENT, eventListener);
                pusherChannel.bind(KUSConstants.PusherEventNames.END_SESSION_EVENT, eventListener);
            } catch (IllegalArgumentException ignore) {
            }
        }
        updatePollingTimer();
    }

    private void updatePollingTimer() {
        boolean isPusherConnected = pusherClient != null &&
                pusherClient.getConnection().getState() == ConnectionState.CONNECTED;
        boolean isPusherSubscribed = pusherChannel != null && pusherChannel.isSubscribed();
        if (isPusherConnected && isPusherSubscribed) {
            //Stop Polling
            if (pollingTimer != null) {
                pollingTimer.cancel();
                pollingTimer = null;
            }
        } else {
            if (isSupportScreenShown()) {
                // We are not yet connected to pusher, setup an active polling pollingTimer
                // (in the event that connecting to pusher fails)
                if (pollingTimer == null || currentPollingTimerInterval != ACTIVE_POLLING_TIMER_INTERVAL) {
                    if (pollingTimer != null)
                        pollingTimer.cancel();

                    startTimer(ACTIVE_POLLING_TIMER_INTERVAL);
                }
            } else {
                // Make sure we're polling lazily
                if (pollingTimer == null || currentPollingTimerInterval != LAZY_POLLING_TIMER_INTERVAL) {
                    if (pollingTimer != null)
                        pollingTimer.cancel();

                    startTimer(LAZY_POLLING_TIMER_INTERVAL);
                }
            }
        }
    }

    private void startTimer(long time) {
        try {
            if (pollingTimer != null)
                pollingTimer.cancel();

            currentPollingTimerInterval = time;
            final Handler handler = new Handler();
            pollingTimer = new Timer();
            TimerTask doAsynchronousTask = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            onPollTick();
                        }
                    });
                }
            };
            pollingTimer.schedule(doAsynchronousTask, time, time);
        } catch (Exception ignore) {
        }
    }

    private void onPollTick() {
        updateStats(new KUSPusherShouldConnectListener() {
            @Override
            public void onCompletion() {
                if (userSession.get() != null && sessionUpdated)
                    userSession.get().getChatSessionsDataSource().fetchLatest();
            }
        });
    }

    private void notifyForUpdatedChatSession(String sessionId) {

        if (isSupportScreenShown()) {
            KUSAudio.playMessageReceivedSound();
        } else {
            KUSChatMessagesDataSource chatMessagesDataSource = userSession.get().chatMessageDataSourceForSessionId(sessionId);

            if (chatMessagesDataSource == null)
                return;

            KUSChatMessage latestMessage = chatMessagesDataSource.getLatestMessage();
            KUSChatSession chatSession = (KUSChatSession) userSession.get().getChatSessionsDataSource().findById(sessionId);
            if (chatSession == null && latestMessage != null) {
                try {
                    chatSession = KUSChatSession.tempSessionFromChatMessage(latestMessage);
                } catch (KUSInvalidJsonException e) {
                    e.printStackTrace();
                }
                userSession.get().getChatSessionsDataSource().fetchLatest();
            }

            if (userSession.get().getDelegateProxy().shouldDisplayInAppNotification()
                    && chatSession != null && latestMessage != null) {
                boolean shouldAutoDismiss = latestMessage.getCampaignId() == null
                        || latestMessage.getCampaignId().length() == 0;

                //Sound is played by the notification itself
                KUSNotificationWindow.getSharedInstance().showNotification(chatSession, Kustomer.getContext(), shouldAutoDismiss);
            }
        }
    }

    private void updateStats(final KUSPusherShouldConnectListener listener) {
        if (userSession.get() == null)
            return;

        // Fetch last activity time of the client
        userSession.get().getRequestManager().getEndpoint(KUSConstants.URL.CUSTOMER_STATS_ENDPOINT,
                true,
                new KUSRequestCompletionListener() {
                    @Override
                    public void onCompletion(Error error, final JSONObject response) {
                        if (userSession.get() == null)
                            return;

                        JSONObject jsonObject = JsonHelper.jsonObjectFromKeyPath(response,
                                "data");
                        Date lastActivity = JsonHelper.dateFromKeyPath(jsonObject,
                                "attributes.lastActivity");


                        boolean sessionUpdated = (KUSPushClient.this.lastActivity == null && lastActivity != null)
                                || (KUSPushClient.this.lastActivity != null
                                && !KUSPushClient.this.lastActivity.equals(lastActivity));

                        KUSPushClient.this.sessionUpdated = sessionUpdated;
                        KUSPushClient.this.lastActivity = lastActivity;

                        Handler handler = new Handler(Looper.getMainLooper());
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                listener.onCompletion();
                            }
                        };
                        handler.post(runnable);
                    }
                });
    }

    private void updatePreviousChatSessions() {
        if (userSession.get() == null)
            return;

        previousChatSessions = new ConcurrentHashMap<>();
        for (KUSModel model : userSession.get().getChatSessionsDataSource().getList()) {
            KUSChatSession chatSession = (KUSChatSession) model;
            previousChatSessions.put(chatSession.getId(), chatSession);
        }
    }

    private void onPusherChatMessageSend(String data){
        JSONObject jsonObject = JsonHelper.stringToJson(data);
        final List<KUSModel> chatMessages = JsonHelper.kusChatModelsFromJSON(
                Kustomer.getContext(),
                JsonHelper.jsonObjectFromKeyPath(jsonObject, "data"));
        if (chatMessages == null || chatMessages.isEmpty())
            return;

        final KUSChatMessage chatMessage = (KUSChatMessage) chatMessages.get(0);
        final KUSChatMessagesDataSource messagesDataSource = userSession.get()
                .chatMessageDataSourceForSessionId(chatMessage.getSessionId());

        final boolean doesNotAlreadyContainMessage = messagesDataSource == null ||
                messagesDataSource.findById(chatMessage.getId()) == null;

        if (messagesDataSource != null)
            messagesDataSource.upsertNewMessages(chatMessages);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (doesNotAlreadyContainMessage)
                    notifyForUpdatedChatSession(chatMessage.getSessionId());
            }
        });
    }

    private void onPusherChatSessionEnd(String data) {
        JSONObject jsonObject = JsonHelper.stringToJson(data);

        List<KUSModel> chatSessions = userSession.get().getChatSessionsDataSource()
                .objectsFromJSON(JsonHelper.jsonObjectFromKeyPath(jsonObject, "data"));

        userSession.get().getChatSessionsDataSource().upsertNewSessions(chatSessions);

        if (chatSessions != null && chatSessions.size() > 0) {
            KUSChatSettings settings = (KUSChatSettings) userSession.get()
                    .getChatSettingsDataSource().getObject();
            if (settings != null && settings.getSingleSessionChat()) {

                for (KUSModel model : userSession.get().getChatSessionsDataSource().getList()) {
                    KUSChatSession session = (KUSChatSession) model;
                    KUSChatMessagesDataSource messagesDataSource = userSession.get()
                            .chatMessageDataSourceForSessionId(session.getId());

                    if (messagesDataSource != null)
                        messagesDataSource.fetchLatest();
                }
            } else {

                KUSChatSession chatSession = (KUSChatSession) chatSessions.get(0);
                KUSChatMessagesDataSource messagesDataSource = userSession.get()
                        .chatMessageDataSourceForSessionId(chatSession.getId());

                if (messagesDataSource != null)
                    messagesDataSource.fetchLatest();
            }
        }
    }

    //endregion

    //region Accessors

    private boolean isSupportScreenShown() {
        return isSupportScreenShown;
    }

    public void setSupportScreenShown(boolean supportScreenShown) {
        isSupportScreenShown = supportScreenShown;
        connectToChannelsIfNecessary();
    }
    //endregion

    //region Callbacks
    @Override
    public void objectDataSourceOnLoad(KUSObjectDataSource dataSource) {
        if (!userSession.get().getChatSessionsDataSource().isFetched())
            userSession.get().getChatSessionsDataSource().fetchLatest();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                connectToChannelsIfNecessary();
            }
        };
        handler.post(runnable);

    }

    @Override
    public void objectDataSourceOnError(KUSObjectDataSource dataSource, Error error) {

    }

    @Override
    public void onLoad(KUSPaginatedDataSource dataSource) {
        updatePreviousChatSessions();

        if (dataSource instanceof KUSChatMessagesDataSource) {
            KUSChatMessagesDataSource chatMessagesDataSource = (KUSChatMessagesDataSource) dataSource;
            if (pendingNotificationSessionId != null && !pendingNotificationSessionId.isEmpty()
                    && chatMessagesDataSource.getSessionId().equals(pendingNotificationSessionId)) {
                Handler handler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        notifyForUpdatedChatSession(pendingNotificationSessionId);
                        pendingNotificationSessionId = null;
                    }
                };
                handler.post(runnable);
            }
        }

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                connectToChannelsIfNecessary();
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onError(KUSPaginatedDataSource dataSource, Error error) {
        if (dataSource instanceof KUSChatMessagesDataSource) {
            KUSChatMessagesDataSource chatMessagesDataSource = (KUSChatMessagesDataSource) dataSource;
            if (chatMessagesDataSource.getSessionId().equals(pendingNotificationSessionId)) {
                Handler handler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        notifyForUpdatedChatSession(pendingNotificationSessionId);
                        pendingNotificationSessionId = null;
                    }
                };
                handler.post(runnable);
            }
        }
    }

    @Override
    public void onContentChange(final KUSPaginatedDataSource dataSource) {
        if (userSession.get() == null || dataSource != userSession.get().getChatSessionsDataSource())
            return;

        // Only consider new messages here if we're actively polling
        if (pollingTimer == null && !didPusherLostPackets) {
            //But update the state of previousChatSessions
            updatePreviousChatSessions();
            return;
        }

        didPusherLostPackets = false;
        String updatedSessionId = null;
        List<KUSModel> newChatSessions = userSession.get().getChatSessionsDataSource().getList();
        for (KUSModel model : newChatSessions) {
            KUSChatSession chatSession = (KUSChatSession) model;

            KUSChatSession previousChatSession = null;

            if (previousChatSessions != null)
                previousChatSession = previousChatSessions.get(chatSession.getId());

            KUSChatMessagesDataSource messagesDataSource = userSession.get().chatMessageDataSourceForSessionId(chatSession.getId());
            if (previousChatSession != null) {

                try {
                    KUSChatMessage latestChatMessage = null;

                    if (messagesDataSource.getList().size() > 0)
                        latestChatMessage = (KUSChatMessage) messagesDataSource.getList().get(0);

                    boolean isUpdatedSession = chatSession.getLastMessageAt().after(previousChatSession.getLastMessageAt());
                    Date sessionLastSeenAt = userSession.get().getChatSessionsDataSource().lastSeenAtForSessionId(chatSession.getId());
                    boolean lastSeenBeforeMessage = sessionLastSeenAt == null || chatSession.getLastMessageAt().after(sessionLastSeenAt);
                    boolean lastMessageAtNewerThanLocalLastMessage = latestChatMessage == null
                            || chatSession.getLastMessageAt().after(latestChatMessage.getCreatedAt());

                    boolean chatSessionSetToLock = chatSession.getLockedAt() != null
                            && !chatSession.getLockedAt().equals(previousChatSession.getLockedAt());

                    // Check that new message arrived or not
                    if (isUpdatedSession && lastSeenBeforeMessage && lastMessageAtNewerThanLocalLastMessage) {
                        updatedSessionId = chatSession.getId();
                        messagesDataSource.addListener(KUSPushClient.this);
                        messagesDataSource.fetchLatest();

                    } else if (chatSessionSetToLock) { // Check that session lock state changed
                        messagesDataSource.fetchLatest();
                    }
                } catch (Exception ignore) {
                }

            } else if (previousChatSessions != null) {
                updatedSessionId = chatSession.getId();
                messagesDataSource.addListener(KUSPushClient.this);
                messagesDataSource.fetchLatest();
            }
        }

        updatePreviousChatSessions();

        if (updatedSessionId != null) {
            pendingNotificationSessionId = updatedSessionId;
        }
    }

    private PresenceChannelEventListener eventListener = new PresenceChannelEventListener() {
        @Override
        public void onUsersInformationReceived(String channelName, Set<User> users) {
        }

        @Override
        public void userSubscribed(String channelName, User user) {
        }

        @Override
        public void userUnsubscribed(String channelName, User user) {
        }

        @Override
        public void onAuthenticationFailure(String message, Exception e) {
            updatePollingTimer();
        }

        @Override
        public void onSubscriptionSucceeded(String channelName) {
            updatePollingTimer();
        }

        @Override
        public void onEvent(String channelName, String eventName, String data) {
            if (eventName != null && eventName.equals(KUSConstants.PusherEventNames.SEND_MESSAGE_EVENT)) {
                onPusherChatMessageSend(data);

            } else if (eventName != null && eventName.equals(KUSConstants.PusherEventNames.END_SESSION_EVENT)) {
                if (userSession.get() == null)
                    return;

                onPusherChatSessionEnd(data);
            }
        }
    };

    private ConnectionEventListener pusherConnectionListener = new ConnectionEventListener() {
        @Override
        public void onConnectionStateChange(ConnectionStateChange change) {
            updatePollingTimer();
        }

        @Override
        public void onError(String message, String code, Exception e) {
            updatePollingTimer();
        }
    };

    //endregion
}