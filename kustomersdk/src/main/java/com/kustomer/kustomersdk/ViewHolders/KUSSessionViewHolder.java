package com.kustomer.kustomersdk.ViewHolders;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Adapters.KUSSessionListAdapter;
import com.kustomer.kustomersdk.DataSources.KUSChatMessagesDataSource;
import com.kustomer.kustomersdk.DataSources.KUSObjectDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.DataSources.KUSUserDataSource;
import com.kustomer.kustomersdk.Enums.KUSChatMessageType;
import com.kustomer.kustomersdk.Helpers.KUSDate;
import com.kustomer.kustomersdk.Helpers.KUSText;
import com.kustomer.kustomersdk.Interfaces.KUSChatMessagesDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSTypingIndicator;
import com.kustomer.kustomersdk.Models.KUSUser;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Views.KUSAvatarImageView;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Junaid on 1/19/2018.
 */

public class KUSSessionViewHolder extends RecyclerView.ViewHolder implements KUSObjectDataSourceListener,
        KUSChatMessagesDataSourceListener {

    //region Properties
    @BindView(R2.id.tvSessionTitle)
    TextView tvSessionTitle;
    @BindView(R2.id.flAvatar)
    FrameLayout imageLayout;
    @BindView(R2.id.tvSessionDate)
    TextView tvSessionDate;
    @BindView(R2.id.tvSessionSubtitle)
    TextView tvSessionSubtitle;
    @BindView(R2.id.tvUnreadCount)
    TextView tvUnreadCount;
    @BindView(R2.id.closedView)
    View closedView;

    private KUSUserSession mUserSession;
    private KUSChatMessagesDataSource chatMessagesDataSource;
    private KUSUserDataSource userDataSource;
    private KUSChatSession mChatSession;
    private Date sessionDate = null;
    //endregion

    public KUSSessionViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void onBind(final KUSChatSession chatSession, KUSUserSession userSession,
                       final KUSSessionListAdapter.onItemClickListener listener) {
        mUserSession = userSession;
        mChatSession = chatSession;

        clearListeners();

        if(!mUserSession.getChatSettingsDataSource().isFetched())
            mUserSession.getChatSettingsDataSource().addListener(this);

        chatMessagesDataSource = userSession.chatMessageDataSourceForSessionId(chatSession.getId());
        chatMessagesDataSource.addListener(this);
        if (!chatMessagesDataSource.isFetched() && !chatMessagesDataSource.isFetching())
            chatMessagesDataSource.fetchLatest();

        updateAvatar();
        updateLabels();

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onSessionItemClicked(chatSession);
            }
        });
    }

    public void onDetached() {
        clearListeners();
    }

    //region Private Methods
    private void clearListeners(){
        if (mUserSession != null && mUserSession.getChatSettingsDataSource() != null)
            mUserSession.getChatSettingsDataSource().removeListener(this);

        if (chatMessagesDataSource != null)
            chatMessagesDataSource.removeListener(this);

        if (userDataSource != null)
            userDataSource.removeListener(this);
    }

    private void updateAvatar() {
        imageLayout.removeAllViews();

        KUSAvatarImageView avatarImageView = new KUSAvatarImageView(itemView.getContext());
        avatarImageView.setFontSize(16);
        avatarImageView.setDrawableSize(40);

        avatarImageView.initWithUserSession(mUserSession);
        avatarImageView.setUserId(chatMessagesDataSource.getFirstOtherUserId());

        FrameLayout.LayoutParams avatarLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        avatarImageView.setLayoutParams(avatarLayoutParams);


        imageLayout.addView(avatarImageView);
    }

    private void updateLabels() {
        if (userDataSource != null)
            userDataSource.removeListener(this);

        userDataSource = mUserSession.userDataSourceForUserId(chatMessagesDataSource.getFirstOtherUserId());

        KUSUser firstOtherUser = null;
        if (userDataSource != null) {
            firstOtherUser = (KUSUser) userDataSource.getObject();
            if (firstOtherUser == null) {
                userDataSource.addListener(this);
                userDataSource.fetch();
            }
        }

        String responderName = firstOtherUser != null ? firstOtherUser.getDisplayName() : null;

        if (responderName == null || responderName.length() == 0) {
            KUSChatSettings chatSettings = (KUSChatSettings) mUserSession.getChatSettingsDataSource().getObject();
            responderName = chatSettings != null && chatSettings.getTeamName().length() > 0 ?
                    chatSettings.getTeamName() : mUserSession.getOrganizationName();
        }

        tvSessionTitle.setText(itemView.getContext().getString(R.string.com_kustomer_chat_with_param,
                responderName));

        //Subtitle text (from last message, or preview text)
        KUSChatMessage latestTextMessage = null;
        for (KUSModel model : chatMessagesDataSource.getList()) {
            KUSChatMessage message = (KUSChatMessage) model;
            if (message.getType() == KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_TEXT) {
                latestTextMessage = message;
                break;
            }
        }

        String subtitleText = null;
        if (latestTextMessage != null) {
            subtitleText = latestTextMessage.getBody() != null ?
                    latestTextMessage.getBody() : mChatSession.getPreview();
        }

        if (subtitleText != null) {
            KUSText.setMarkDownText(tvSessionSubtitle, subtitleText.trim());
            tvSessionSubtitle.setMovementMethod(null);
        }

        //Date text (from last message date, or session created at)

        if (latestTextMessage != null) {
            sessionDate = latestTextMessage.getCreatedAt() != null ?
                    latestTextMessage.getCreatedAt() : mChatSession.getCreatedAt();
        }
        tvSessionDate.setText(KUSDate.humanReadableTextFromDate(Kustomer.getContext(),sessionDate));

        //Unread count (number of messages > the lastSeenAt)
        Date sessionLastSeenAt = mUserSession.getChatSessionsDataSource().lastSeenAtForSessionId(mChatSession.getId());

        int unreadCount = 0;
        unreadCount = chatMessagesDataSource.unreadCountAfterDate(sessionLastSeenAt);

        if (unreadCount > 0) {
            tvUnreadCount.setText(String.valueOf(unreadCount));
            tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            tvUnreadCount.setVisibility(View.INVISIBLE);
        }
        updateClosedChatView();
    }

    private void updateClosedChatView() {
        if (mChatSession.getLockedAt() != null) {
            tvSessionDate.setVisibility(View.GONE);
            closedView.setVisibility(View.VISIBLE);
            tvSessionTitle.setAlpha(0.5f);
            tvSessionSubtitle.setAlpha(0.5f);
            imageLayout.setAlpha(0.5f);
        } else {
            tvSessionDate.setVisibility(View.VISIBLE);
            closedView.setVisibility(View.GONE);
            tvSessionTitle.setAlpha(1f);
            tvSessionSubtitle.setAlpha(1f);
            imageLayout.setAlpha(1f);
        }
    }
    //endregion

    //region Listener
    @Override
    public void objectDataSourceOnLoad(KUSObjectDataSource dataSource) {
        dataSource.removeListener(this);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateLabels();
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
    public void onContentChange(KUSPaginatedDataSource dataSource) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateLabels();
                updateAvatar();
                updateClosedChatView();
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onCreateSessionId(KUSChatMessagesDataSource source, String sessionId) {

    }

    @Override
    public void onChatSessionEnded(@NonNull KUSChatMessagesDataSource dataSource) {

    }

    @Override
    public void onReceiveTypingUpdate(@NonNull KUSChatMessagesDataSource source, @Nullable KUSTypingIndicator typingIndicator) {
        //No need to do anything here
    }

    @Override
    public void onSatisfactionResponseLoaded(@NonNull KUSChatMessagesDataSource dataSource) {
        //No need to do anything here
    }

    //endregion
}
