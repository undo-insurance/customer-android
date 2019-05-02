package com.kustomer.kustomersdk.Views;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kustomer.kustomersdk.API.KUSSessionQueuePollingManager;
import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.DataSources.KUSChatMessagesDataSource;
import com.kustomer.kustomersdk.DataSources.KUSObjectDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.DataSources.KUSUserDataSource;
import com.kustomer.kustomersdk.Helpers.KUSDate;
import com.kustomer.kustomersdk.Interfaces.KUSChatMessagesDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSSessionQueuePollingListener;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSSessionQueue;
import com.kustomer.kustomersdk.Models.KUSTypingIndicator;
import com.kustomer.kustomersdk.Models.KUSUser;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.Utils.KUSUtils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Junaid on 1/30/2018.
 */

public class KUSToolbar extends Toolbar implements KUSObjectDataSourceListener, KUSChatMessagesDataSourceListener, KUSSessionQueuePollingListener {
    //region Properties
    private String sessionId;
    private boolean showLabel;
    private boolean showBackButton;
    private boolean showDismissButton;
    private boolean extraLargeSize;
    private String waitingMessage;

    KUSUserSession userSession;
    KUSChatMessagesDataSource chatMessagesDataSource;
    KUSUserDataSource userDataSource;

    TextView tvName;
    TextView tvWaiting;
    TextView tvGreetingMessage;
    TextView tvToolbarUnreadCount;
    KUSMultipleAvatarsView kusMultipleAvatarsView;
    View ivBack;
    View ivClose;
    ViewGroup toolbarInnerLayout;
    OnToolbarItemClickListener listener;
    //endregion

    //region Initializer
    public KUSToolbar(Context context) {
        super(context);
    }

    public KUSToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public KUSToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
        setListeners();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (userSession != null && userSession.getChatSettingsDataSource() != null)
            userSession.getChatSettingsDataSource().removeListener(this);

        if (userSession != null && userSession.getChatSessionsDataSource() != null)
            userSession.getChatSessionsDataSource().removeListener(this);

        if (userDataSource != null)
            userDataSource.removeListener(this);

        if (chatMessagesDataSource != null)
        {
            if(chatMessagesDataSource.getSessionQueuePollingManager() != null)
                chatMessagesDataSource.getSessionQueuePollingManager().removeListener(this);

            chatMessagesDataSource.removeListener(this);
        }
    }

    //endregion

    //region Public Methods
    public void initWithUserSession(KUSUserSession userSession) {
        this.userSession = userSession;
        kusMultipleAvatarsView.initWithUserSession(userSession);

        this.userSession.getChatSettingsDataSource().addListener(this);
        this.userSession.getChatSessionsDataSource().addListener(this);
        this.userSession.getScheduleDataSource().addListener(this);

        updateTextLabel();
        updateBackButtonBadge();

    }
    //endregion

    //region Private Methods
    private void initViews() {
        tvName = findViewById(R.id.tvName);
        tvWaiting = findViewById(R.id.tvWaiting);
        tvGreetingMessage = findViewById(R.id.tvGreetingMessage);
        kusMultipleAvatarsView = findViewById(R.id.multipleAvatarViews);
        ivBack = findViewById(R.id.ivBack);
        ivClose = findViewById(R.id.ivClose);
        tvToolbarUnreadCount = findViewById(R.id.tvToolbarUnreadCount);
        toolbarInnerLayout = findViewById(R.id.toolbarInnerLayout);

        if (showLabel) {
            tvName.setVisibility(VISIBLE);
            tvGreetingMessage.setVisibility(VISIBLE);
            tvWaiting.setVisibility(VISIBLE);
        } else {
            tvName.setVisibility(GONE);
            tvGreetingMessage.setVisibility(GONE);
            tvWaiting.setVisibility(GONE);
        }

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout.LayoutParams avatarlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        avatarlp.gravity = Gravity.CENTER;

        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        vlp.gravity = Gravity.CENTER;

        if (extraLargeSize) {
            tvName.setTextSize(15f);
            tvGreetingMessage.setTextSize(13f);
            tvWaiting.setTextSize(13f);

            lp.setMargins(0, (int) KUSUtils.dipToPixels(getContext(), 40),
                    0, (int) KUSUtils.dipToPixels(getContext(), 40));

            avatarlp.setMargins(0, (int) KUSUtils.dipToPixels(getContext(), 4),
                    0, (int) KUSUtils.dipToPixels(getContext(), 4));

            vlp.setMargins((int) KUSUtils.dipToPixels(getContext(), 4),
                    (int) KUSUtils.dipToPixels(getContext(), 4),
                    (int) KUSUtils.dipToPixels(getContext(), 4),
                    (int) KUSUtils.dipToPixels(getContext(), 4));

            kusMultipleAvatarsView.setLayoutParams(avatarlp);
            tvName.setLayoutParams(vlp);
            tvGreetingMessage.setLayoutParams(vlp);
            tvWaiting.setLayoutParams(vlp);
            toolbarInnerLayout.setLayoutParams(lp);

            tvGreetingMessage.setVisibility(VISIBLE);

            if (userSession != null) {
                KUSChatSettings chatSettings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
                int isVCGreetingVisible = chatSettings != null && chatSettings.isVolumeControlEnabled() ? VISIBLE : GONE;
                tvWaiting.setVisibility(isVCGreetingVisible);
            }

        } else {
            tvName.setTextSize(13f);
            tvGreetingMessage.setTextSize(11f);
            tvWaiting.setTextSize(11f);

            lp.setMargins(0, (int) KUSUtils.dipToPixels(getContext(), 10),
                    0, (int) KUSUtils.dipToPixels(getContext(), 5));

            avatarlp.setMargins(0, (int) KUSUtils.dipToPixels(getContext(), 2),
                    0, (int) KUSUtils.dipToPixels(getContext(), 2));

            vlp.setMargins((int) KUSUtils.dipToPixels(getContext(), 4), 0,
                    (int) KUSUtils.dipToPixels(getContext(), 4), 0);

            kusMultipleAvatarsView.setLayoutParams(avatarlp);
            tvName.setLayoutParams(vlp);
            tvGreetingMessage.setLayoutParams(vlp);
            tvWaiting.setLayoutParams(vlp);
            toolbarInnerLayout.setLayoutParams(lp);

            tvGreetingMessage.setVisibility(VISIBLE);
            tvWaiting.setVisibility(GONE);
        }

        updateTextLabel();
    }

    private void setListeners() {
        ivBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onToolbarBackPressed();
            }
        });

        ivClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onToolbarClosePressed();
            }
        });
    }

    private void updateTextLabel() {
        if (userDataSource != null) {
            userDataSource.removeListener(this);
        }

        if (chatMessagesDataSource != null)
            userDataSource = userSession.userDataSourceForUserId(chatMessagesDataSource.getFirstOtherUserId());

        KUSUser firstOtherUser = null;
        if (userDataSource != null) {
            userDataSource.addListener(this);
            firstOtherUser = (KUSUser) userDataSource.getObject();
        }

        String responderName = "";
        if (firstOtherUser != null) {
            responderName = firstOtherUser.getDisplayName();
        }


        if(userSession == null)
            return;

        KUSChatSettings chatSettings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();

        if (chatSettings == null) {
            return;
        }

        if (responderName == null || responderName.length() == 0) {
            if (chatSettings.getTeamName() != null && chatSettings.getTeamName().length() != 0)
                responderName = chatSettings.getTeamName();
            else
                responderName = userSession.getOrganizationName();
        }
        tvName.setText(responderName);


        String waitMessage;

        if(waitingMessage != null){
            waitMessage = waitingMessage;
        }else if(chatSettings.getUseDynamicWaitMessage()){
            waitMessage = chatSettings.getWaitMessage();
        }else {
            waitMessage = chatSettings.getCustomWaitMessage();
        }

        if(extraLargeSize){
            if(userSession.getScheduleDataSource().isActiveBusinessHours()){
                tvGreetingMessage.setText(chatSettings.getGreeting());
            }else{
                tvGreetingMessage.setText(chatSettings.getOffHoursMessage());
            }

            tvWaiting.setText(waitMessage);

        }else{
            if(!userSession.getScheduleDataSource().isActiveBusinessHours()){
                tvGreetingMessage.setText(chatSettings.getOffHoursMessage());
            }else if(chatSettings.getVolumeControlEnabled()){
                tvGreetingMessage.setText(waitMessage);
            }else {
                tvGreetingMessage.setText(chatSettings.getGreeting());
            }
        }

    }

    private void updateBackButtonBadge() {
        int unreadCount = userSession.getChatSessionsDataSource().totalUnreadCountExcludingSessionId(sessionId);
        if (unreadCount > 0) {
            tvToolbarUnreadCount.setText(String.valueOf(unreadCount));
            tvToolbarUnreadCount.setVisibility(VISIBLE);
        } else {
            tvToolbarUnreadCount.setVisibility(INVISIBLE);
        }
    }
    //endregion

    //region Accessors & Mutators
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        if (this.sessionId != null && this.sessionId.equals(sessionId))
            return;

        if (sessionId == null)
            return;

        this.sessionId = sessionId;


        if (chatMessagesDataSource != null) {

            if(chatMessagesDataSource.getSessionQueuePollingManager() != null)
                chatMessagesDataSource.getSessionQueuePollingManager().removeListener(this);

            chatMessagesDataSource.removeListener(this);
        }

        chatMessagesDataSource = userSession.getChatMessagesDataSources().get(sessionId);
        chatMessagesDataSource.addListener(this);

        if(chatMessagesDataSource.getSessionQueuePollingManager() != null)
            chatMessagesDataSource.getSessionQueuePollingManager().addListener(this);

        kusMultipleAvatarsView.setUserIds(chatMessagesDataSource.getOtherUserIds());

        boolean isVolumeControlPollingActive = chatMessagesDataSource.getSessionQueuePollingManager() != null
                && chatMessagesDataSource.getSessionQueuePollingManager().getPollingStarted()
                && !chatMessagesDataSource.getSessionQueuePollingManager().getPollingCanceled();

        if(isVolumeControlPollingActive){
            KUSSessionQueue sessionQueue = chatMessagesDataSource.getSessionQueuePollingManager().getSessionQueue();

            if(sessionQueue != null)
                waitingMessage = KUSDate.humanReadableUpfrontVolumeControlWaitingTimeFromSeconds(
                        getContext(),sessionQueue.getEstimatedWaitTimeSeconds());
        }

        updateTextLabel();
        updateBackButtonBadge();
    }

    public boolean isShowLabel() {
        return showLabel;
    }

    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
        initViews();
    }

    public boolean isShowBackButton() {
        return showBackButton;
    }

    public void setShowBackButton(boolean showBackButton) {
        this.showBackButton = showBackButton;

        if (showBackButton)
            ivBack.setVisibility(VISIBLE);
        else
            ivBack.setVisibility(INVISIBLE);
    }

    public boolean isShowDismissButton() {
        return showDismissButton;
    }

    public void setShowDismissButton(boolean showDismissButton) {
        this.showDismissButton = showDismissButton;
    }

    public boolean isExtraLargeSize() {
        return extraLargeSize;
    }

    public void setExtraLargeSize(boolean extraLargeSize) {
        this.extraLargeSize = extraLargeSize;

        initViews();
    }

    public void setListener(OnToolbarItemClickListener listener) {
        this.listener = listener;
    }

    //endregion

    //region Listeners
    @Override
    public void objectDataSourceOnLoad(KUSObjectDataSource dataSource) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateTextLabel();
            }
        };
        handler.post(runnable);
    }

    @Override
    public void objectDataSourceOnError(KUSObjectDataSource dataSource, Error error) {

    }

    @Override
    public void onLoad(KUSPaginatedDataSource dataSource) {

    }

    @Override
    public void onError(KUSPaginatedDataSource dataSource, Error error) {

    }

    @Override
    public void onContentChange(final KUSPaginatedDataSource dataSource) {

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (dataSource == chatMessagesDataSource) {
                    kusMultipleAvatarsView.setUserIds((ArrayList<String>) chatMessagesDataSource.getOtherUserIds());
                    updateTextLabel();
                } else if (dataSource == userSession.getChatSessionsDataSource()) {
                    updateBackButtonBadge();
                }
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onCreateSessionId(KUSChatMessagesDataSource source, final String mSessionId) {


        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (sessionId.equals(mSessionId))
                    return;

                sessionId = mSessionId;
                chatMessagesDataSource.removeListener(KUSToolbar.this);
                chatMessagesDataSource = userSession.chatMessageDataSourceForSessionId(sessionId);
                chatMessagesDataSource.addListener(KUSToolbar.this);
                kusMultipleAvatarsView.setUserIds(chatMessagesDataSource.getOtherUserIds());

                updateTextLabel();
                updateBackButtonBadge();

            }
        };
        handler.post(runnable);


    }

    @Override
    public void onReceiveTypingUpdate(@NonNull KUSChatMessagesDataSource source, @Nullable KUSTypingIndicator typingIndicator) {
        //No need to do anything here
    }

    @Override
    public void onPollingStarted(KUSSessionQueuePollingManager manager) {

    }

    @Override
    public void onSessionQueueUpdated(KUSSessionQueuePollingManager manager, KUSSessionQueue sessionQueue) {
        waitingMessage = KUSDate.humanReadableUpfrontVolumeControlWaitingTimeFromSeconds(
                getContext(),sessionQueue.getEstimatedWaitTimeSeconds());

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateTextLabel();

            }
        };
        handler.post(runnable);
    }

    @Override
    public void onPollingEnd(KUSSessionQueuePollingManager manager) {
    }

    @Override
    public void onPollingCanceled(KUSSessionQueuePollingManager manager) {
        waitingMessage = null;

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateTextLabel();

            }
        };
        handler.post(runnable);
    }

    @Override
    public void onFailure(Error error, KUSSessionQueuePollingManager manager) {

    }
    //endregion

    //region Interface
    public interface OnToolbarItemClickListener {
        void onToolbarBackPressed();

        void onToolbarClosePressed();
    }
    //endregion


}
