package com.kustomer.kustomersdk.Adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.DataSources.KUSChatMessagesDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.Models.KUSCSatisfactionResponse;
import com.kustomer.kustomersdk.Enums.KUSTypingStatus;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSTypingIndicator;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.ViewHolders.AgentMessageViewHolder;
import com.kustomer.kustomersdk.ViewHolders.CSatisfactionFormViewHolder;
import com.kustomer.kustomersdk.ViewHolders.AgentTypingViewHolder;
import com.kustomer.kustomersdk.ViewHolders.DummyViewHolder;
import com.kustomer.kustomersdk.ViewHolders.UserMessageViewHolder;

/**
 * Created by Junaid on 1/19/2018.
 */

public class MessageListAdapter extends RecyclerView.Adapter {

    //region Properties
    private static final int K_PREFETCH_PADDING = 20;
    private static final int K_5_MINUTE = 5 * 60 * 1000;

    private static final int AGENT_VIEW = 0;
    private static final int USER_VIEW = 1;
    private static final int END_VIEW = 2;
    private static final int TYPING_VIEW = 3;
    private static final int SATISFACTION_FORM_VIEW = 4;

    private KUSPaginatedDataSource mPaginatedDataSource;
    private KUSUserSession mUserSession;
    private KUSChatMessagesDataSource mChatMessagesDataSource;
    private ChatMessageItemListener mListener;
    private KUSTypingIndicator typingIndicator;

    private boolean isSatisfactionFormEditing;
    //endregion

    //region LifeCycle
    public MessageListAdapter(KUSPaginatedDataSource paginatedDataSource,
                              KUSUserSession userSession,
                              KUSChatMessagesDataSource chatMessagesDataSource,
                              ChatMessageItemListener listener) {
        mPaginatedDataSource = paginatedDataSource;
        mUserSession = userSession;
        mChatMessagesDataSource = chatMessagesDataSource;
        mListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == USER_VIEW)
            return new UserMessageViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.kus_item_user_view_holder, parent, false));

        else if (viewType == AGENT_VIEW)
            return new AgentMessageViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.kus_item_agent_view_holder, parent, false));

        else if (viewType == END_VIEW)
            return new DummyViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.kus_item_closed_chat_layout, parent, false));

        else if (viewType == TYPING_VIEW)
            return new AgentTypingViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.kus_item_agent_typing_view_holder, parent, false));

        else
            return new CSatisfactionFormViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.kus_item_csat_view_holder, parent, false),
                    mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == END_VIEW)
            return;

        if (holder.getItemViewType() == TYPING_VIEW) {
            ((AgentTypingViewHolder) holder).onBind(typingIndicator, mUserSession);
            return;
        }

        if (holder.getItemViewType() == SATISFACTION_FORM_VIEW) {
            KUSCSatisfactionResponse response = (KUSCSatisfactionResponse) mChatMessagesDataSource
                    .getSatisfactionResponseDataSource().getObject();

            if (response != null)
                ((CSatisfactionFormViewHolder) holder)
                        .onBind(response, mUserSession, isSatisfactionFormEditing);
            return;
        }

        int mPosition = position - (getItemCount() - mPaginatedDataSource.getSize());

        KUSChatMessage chatMessage = messageForPosition(mPosition);
        KUSChatMessage previousChatMessage = previousMessage(mPosition);
        KUSChatMessage nextChatMessage = nextMessage(mPosition);

        if (!mChatMessagesDataSource.isFetchedAll() &&
                mPosition >= mChatMessagesDataSource.getSize() - 1 - K_PREFETCH_PADDING)
            mChatMessagesDataSource.fetchNext();

        boolean nextMessageOlderThan5Min = nextChatMessage == null ||
                nextChatMessage.getCreatedAt().getTime() - chatMessage.getCreatedAt().getTime() > K_5_MINUTE;

        if (holder.getItemViewType() == USER_VIEW) {
            ((UserMessageViewHolder) holder).onBind(chatMessage, nextMessageOlderThan5Min, mListener);

        } else if (holder.getItemViewType() == AGENT_VIEW) {
            boolean previousMessageDiffSender =
                    !KUSChatMessage.KUSMessagesSameSender(previousChatMessage, chatMessage);
            ((AgentMessageViewHolder) holder).onBind(chatMessage, mUserSession,
                    previousMessageDiffSender, nextMessageOlderThan5Min, mListener);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (getItemCount() > mPaginatedDataSource.getSize()) {
            if (mChatMessagesDataSource.shouldShowSatisfactionForm()) {
                if (position == 0)
                    return SATISFACTION_FORM_VIEW;
                else if (position == 1)
                    return END_VIEW;

            } else if (position == 0) {
                KUSChatSession session = (KUSChatSession) mUserSession
                        .getChatSessionsDataSource().findById(mChatMessagesDataSource.getSessionId());

                if (session != null && session.getLockedAt() != null)
                    return END_VIEW;
                else
                    return TYPING_VIEW;
            }
        }

        int mPosition = position - (getItemCount() - mPaginatedDataSource.getSize());

        KUSChatMessage chatMessage = messageForPosition(mPosition);

        boolean currentUser = KUSChatMessage.KUSChatMessageSentByUser(chatMessage);
        if (currentUser)
            return USER_VIEW;
        else
            return AGENT_VIEW;
    }

    @Override
    public int getItemCount() {
        KUSChatSession session = (KUSChatSession) mUserSession
                .getChatSessionsDataSource().findById(mChatMessagesDataSource.getSessionId());

        if (session != null && session.getLockedAt() != null) {

            if (mChatMessagesDataSource.shouldShowSatisfactionForm())
                return mPaginatedDataSource.getSize() + 2;
            else
                return mPaginatedDataSource.getSize() + 1;
        }

        if (typingIndicator != null && typingIndicator.getStatus() == KUSTypingStatus.KUS_TYPING)
            return mPaginatedDataSource.getSize() + 1;

        return mPaginatedDataSource.getSize();
    }

    //endregion

    //region public Methods

    public void setTypingIndicator(KUSTypingIndicator typingIndicator) {
        this.typingIndicator = typingIndicator;
    }

    public KUSTypingIndicator getTypingIndicator() {
        return typingIndicator;
    }

    //endregion

    //region Private Methods
    private KUSChatMessage messageForPosition(int position) {
        return (KUSChatMessage) mPaginatedDataSource.get(position);
    }

    private KUSChatMessage previousMessage(int position) {
        if (position < mChatMessagesDataSource.getSize() - 1 && position >= 0) {
            return messageForPosition(position + 1);
        } else {
            return null;
        }
    }

    private KUSChatMessage nextMessage(int position) {
        if (position > 0 && position < mChatMessagesDataSource.getSize()) {
            return messageForPosition(position - 1);
        } else {
            return null;
        }
    }

    public void isSatisfactionFormEditing(boolean editing) {
        this.isSatisfactionFormEditing = editing;
    }

    //endregion

    //region Interface
    public interface ChatMessageItemListener {
        void onChatMessageImageClicked(KUSChatMessage chatMessage);

        void onChatMessageErrorClicked(KUSChatMessage chatMessage);

        void onSatisfactionFormRated(int rating);

        void onSatisfactionFormCommented(@NonNull String comment);

        void onSatisfactionFormEditPressed();
    }
    //endregion
}
