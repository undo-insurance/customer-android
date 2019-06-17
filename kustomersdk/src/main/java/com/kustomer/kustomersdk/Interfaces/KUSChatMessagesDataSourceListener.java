package com.kustomer.kustomersdk.Interfaces;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.DataSources.KUSChatMessagesDataSource;
import com.kustomer.kustomersdk.Models.KUSTypingIndicator;

/**
 * Created by Junaid on 1/23/2018.
 */

public interface KUSChatMessagesDataSourceListener extends KUSPaginatedDataSourceListener {
    void onCreateSessionId(KUSChatMessagesDataSource source, String sessionId);

    void onChatSessionEnded(@NonNull KUSChatMessagesDataSource dataSource);

    void onSatisfactionResponseLoaded(@NonNull KUSChatMessagesDataSource dataSource);

    void onReceiveTypingUpdate(@NonNull KUSChatMessagesDataSource source,
                               @Nullable KUSTypingIndicator typingIndicator);
}
