package com.kustomer.kustomersdk.Activities;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.FontRequestEmojiCompatConfig;
import android.support.v4.provider.FontRequest;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Adapters.KUSSessionListAdapter;
import com.kustomer.kustomersdk.BaseClasses.KUSBaseActivity;
import com.kustomer.kustomersdk.DataSources.KUSChatSessionsDataSource;
import com.kustomer.kustomersdk.DataSources.KUSObjectDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.Helpers.KUSLocalization;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSPaginatedDataSourceListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Utils.KUSJsonHelper;
import com.kustomer.kustomersdk.Views.KUSToolbar;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;

public class KUSSessionsActivity extends KUSBaseActivity implements KUSPaginatedDataSourceListener, KUSSessionListAdapter.onItemClickListener, KUSToolbar.OnToolbarItemClickListener, KUSObjectDataSourceListener {

    //region Properties
    @BindView(R2.id.rvSessions)
    RecyclerView rvSessions;
    @BindView(R2.id.btnNewConversation)
    Button btnNewConversation;
    @BindView(R2.id.footerLayout)
    LinearLayout footerLayout;

    private KUSUserSession userSession;
    private KUSChatSessionsDataSource chatSessionsDataSource;

    private boolean didHandleFirstLoad = false;
    private KUSSessionListAdapter adapter;
    private boolean shouldAnimateChatScreen = false;
    //endregion

    //region LifeCycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLayout(R.layout.kus_activity_kussessions, R.id.toolbar_main, null, false);
        super.onCreate(savedInstanceState);

        userSession = Kustomer.getSharedInstance().getUserSession();
        userSession.getPushClient().setSupportScreenShown(true);

        chatSessionsDataSource = userSession.getChatSessionsDataSource();
        chatSessionsDataSource.addListener(this);
        chatSessionsDataSource.fetchLatest();

        shouldAnimateChatScreen = chatSessionsDataSource.isFetched();

        setupAdapter();
        setupToolbar();

        if (!getResources().getBoolean(R.bool.kusNewSessionButtonHasShadow)) {
            ViewCompat.setElevation(btnNewConversation, 0);

            //This StateList is null, so no need to create a compat call for <21
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                btnNewConversation.setStateListAnimator(null);
            }
        }

        userSession.getScheduleDataSource().addListener(this);
        userSession.getScheduleDataSource().fetch();

        if (shouldHandleFirstLoad()) {
            handleFirstLoadIfNecessary();
        } else {
            rvSessions.setVisibility(View.INVISIBLE);
            btnNewConversation.setVisibility(View.INVISIBLE);
            showProgressBar();
        }

        showKustomerBrandingFooterIfNeeded();

        //Connecting to Presence channel when Kustomer support chat screen shown for existing user
        connectToCustomerPresenceChannel();

        //Initialize Emoji Compact Support Library
        initializeEmojiCompact(Kustomer.getSharedInstance().getEmojiCompactSupported());
    }

    private void connectToCustomerPresenceChannel() {
        String customerId = chatSessionsDataSource.getCustomerId();

        if(null!=customerId) {
            userSession.getPushClient().connectToCustomerPresenceChannel(customerId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (chatSessionsDataSource != null)
            chatSessionsDataSource.fetchLatest();
        setCreateSessionBackToChatButton();
    }

    @Override
    protected void onDestroy() {
        if (null != chatSessionsDataSource)
            chatSessionsDataSource.removeListener(this);

        if (null != userSession)
            userSession.getScheduleDataSource().removeListener(this);

        rvSessions.setAdapter(null);
        userSession.getPushClient().setSupportScreenShown(false);

        //Disconnecting from Presence channel when Kustomer support is exited
        userSession.getPushClient().disconnectFromCustomerPresenceChannel();
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.kus_stay, R.anim.kus_slide_down);
    }
    //endregion

    //region Initializer

    private boolean shouldHandleFirstLoad() {

        boolean shouldCreateNewSessionWithMessage = chatSessionsDataSource.getMessageToCreateNewChatSession() != null;
        boolean scheduleFetched = userSession.getScheduleDataSource().isFetched();
        boolean chatSessionFetched = chatSessionsDataSource.isFetched();

        return scheduleFetched && (chatSessionFetched || shouldCreateNewSessionWithMessage);
    }

    private void showKustomerBrandingFooterIfNeeded() {
        if (userSession.getChatSettingsDataSource().isFetched()) {
            KUSChatSettings chatSettings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
            footerLayout.setVisibility(chatSettings != null && chatSettings.shouldShowKustomerBranding() ?
                    View.VISIBLE : View.GONE);
        } else {
            userSession.getChatSettingsDataSource().addListener(this);
            userSession.getChatSettingsDataSource().fetch();
        }
    }

    private void setupToolbar() {
        KUSToolbar kusToolbar = (KUSToolbar) toolbar;
        kusToolbar.initWithUserSession(userSession);
        kusToolbar.setShowLabel(false);
        kusToolbar.setListener(this);
        kusToolbar.setShowDismissButton(true);
    }

    private void setupAdapter() {
        adapter = new KUSSessionListAdapter(rvSessions, chatSessionsDataSource, userSession, this);
        rvSessions.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        rvSessions.setLayoutManager(layoutManager);

        adapter.notifyDataSetChanged();
    }
    //endregion

    //region Private Methods
    private void setCreateSessionBackToChatButton() {
        if (isBackToChatButton()) {
            btnNewConversation.setText(R.string.com_kustomer_back_to_chat);
            btnNewConversation.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        } else {

            if (userSession.getScheduleDataSource().isActiveBusinessHours()) {
                btnNewConversation.setText(R.string.com_kustomer_new_conversation);
            } else {
                btnNewConversation.setText(R.string.com_kustomer_leave_a_message);
            }

            TypedArray a = getTheme().obtainStyledAttributes(R.style.KUSAppTheme,
                    new int[]{R.attr.kus_new_session_button_image});
            int attributeResourceId = a.getResourceId(0, 0);
            a.recycle();

            Drawable drawable = getResources().getDrawable(attributeResourceId);
            if (KUSLocalization.getSharedInstance().isLTR())
                btnNewConversation.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            else
                btnNewConversation.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
        }
    }

    private boolean isBackToChatButton() {
        KUSChatSettings settings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
        int openChats = userSession.getChatSessionsDataSource().getOpenChatSessionsCount();
        int proactiveChats = userSession.getChatSessionsDataSource().getOpenProactiveCampaignsCount();
        return (settings != null && settings.getSingleSessionChat() && (openChats - proactiveChats) >= 1);
    }

    private void handleFirstLoadIfNecessary() {
        if (didHandleFirstLoad || !userSession.getChatSettingsDataSource().isFetched())
            return;

        if (!chatSessionsDataSource.isFetched() &&
                chatSessionsDataSource.getMessageToCreateNewChatSession() == null)
            return;

        didHandleFirstLoad = true;

        Intent intent = new Intent(this, KUSChatActivity.class);
        boolean shouldOpenChatActivity = true;

        if (chatSessionsDataSource.getMessageToCreateNewChatSession() != null) {
            intent.putExtra(KUSConstants.BundleName.CHAT_SCREEN_MESSAGE,
                    chatSessionsDataSource.getMessageToCreateNewChatSession());

            intent.putExtra(KUSConstants.BundleName.CHAT_SCREEN_FORM_ID,
                    chatSessionsDataSource.getFormIdForConversationalForm());

        } else if (chatSessionsDataSource != null &&
                (chatSessionsDataSource.getSize() == 0 || chatSessionsDataSource.getOpenChatSessionsCount() == 0)) {
            intent.putExtra(KUSConstants.BundleName.CHAT_SCREEN_BACK_BUTTON_KEY, false);

        } else if (chatSessionsDataSource != null) {
            // Go directly to the most recent chat session
            KUSChatSession chatSession = chatSessionsDataSource.getMostRecentSession();

            intent.putExtra(KUSConstants.BundleName.CHAT_SESSION_BUNDLE_KEY, chatSession);
        } else {
            shouldOpenChatActivity = false;
        }

        if (shouldOpenChatActivity) {
            startActivity(intent);
            if (shouldAnimateChatScreen)
                overridePendingTransition(R.anim.kus_slide_up, R.anim.kus_stay);
            else
                overridePendingTransition(0, 0);
        }
    }

    private void handleSuccessfulDataLoad() {
        if (shouldHandleFirstLoad()) {
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    hideProgressBar();
                    handleFirstLoadIfNecessary();
                    rvSessions.setVisibility(View.VISIBLE);
                    btnNewConversation.setVisibility(View.VISIBLE);
                }

            };
            handler.post(runnable);
        }
    }

    private void initializeEmojiCompact(@NonNull Boolean emojiCompactSupported) {

        String emojiQuery = emojiCompactSupported ? KUSConstants.GoogleFonts.EMOJI_FONT_NAME : "";

        FontRequest fontRequest = new FontRequest(
                KUSConstants.GoogleFonts.FONT_PROVIDER_AUTHORITY_NAME,
                KUSConstants.GoogleFonts.FONT_PROVIDER_PACKAGE_NAME,
                emojiQuery
                ,R.array.com_google_android_gms_fonts_certs);

        EmojiCompat.Config emojiConfig = new FontRequestEmojiCompatConfig(this,fontRequest)
                .registerInitCallback(new EmojiCompat.InitCallback() {
                    @Override
                    public void onInitialized() {
                        super.onInitialized();
                        KUSLog.KUSLogDebug("EmojiCompat Initialized");
                    }
                    @Override
                    public void onFailed(@Nullable Throwable throwable) {
                        KUSLog.KUSLogDebug("EmojiCompat initialization failed : "+throwable.getMessage());
                    }
                })
                ;
        EmojiCompat.init(emojiConfig);
    }

    //endregion

    //region Listeners
    @Optional
    @OnClick(R2.id.btnRetry)
    void userTappedRetry() {
        chatSessionsDataSource.fetchLatest();
        userSession.getScheduleDataSource().fetch();
        showProgressBar();
    }

    @OnClick(R2.id.btnNewConversation)
    void newConversationClicked() {
        Intent intent = new Intent(this, KUSChatActivity.class);

        if (isBackToChatButton()) {
            KUSChatSession chatSession = userSession.getChatSessionsDataSource().mostRecentNonProactiveCampaignOpenSession();
            intent.putExtra(KUSConstants.BundleName.CHAT_SESSION_BUNDLE_KEY, chatSession);
        }

        startActivity(intent);
        if (KUSLocalization.getSharedInstance().isLTR())
            overridePendingTransition(R.anim.kus_slide_left, R.anim.kus_stay);
        else
            overridePendingTransition(R.anim.kus_slide_left_rtl, R.anim.kus_stay);
    }

    @Override
    public void onLoad(KUSPaginatedDataSource dataSource) {
        handleSuccessfulDataLoad();
    }

    @Override
    public void onError(KUSPaginatedDataSource dataSource, Error error) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                hideProgressBar();
                String errorText = getResources().getString(R.string.com_kustomer_something_went_wrong_please_try_again);
                showErrorWithText(errorText);
                rvSessions.setVisibility(View.INVISIBLE);
                btnNewConversation.setVisibility(View.INVISIBLE);
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onContentChange(final KUSPaginatedDataSource dataSource) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                setCreateSessionBackToChatButton();
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onSessionItemClicked(KUSChatSession chatSession) {
        Intent intent = new Intent(this, KUSChatActivity.class);
        intent.putExtra(KUSConstants.BundleName.CHAT_SESSION_BUNDLE_KEY, chatSession);
        startActivity(intent);

        if (KUSLocalization.getSharedInstance().isLTR())
            overridePendingTransition(R.anim.kus_slide_left, R.anim.kus_stay);
        else
            overridePendingTransition(R.anim.kus_slide_left_rtl, R.anim.kus_stay);
    }

    @Override
    public void onToolbarBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onToolbarClosePressed() {
        clearAllLibraryActivities();
    }

    @Override
    public void objectDataSourceOnLoad(KUSObjectDataSource dataSource) {
        if (dataSource == userSession.getScheduleDataSource()) {
            handleSuccessfulDataLoad();
            return;
        }

        if (dataSource != userSession.getChatSettingsDataSource())
            return;

        userSession.getChatSettingsDataSource().removeListener(this);

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                setCreateSessionBackToChatButton();
                handleFirstLoadIfNecessary();

                KUSChatSettings chatSettings = (KUSChatSettings) userSession
                        .getChatSettingsDataSource().getObject();
                footerLayout.setVisibility(chatSettings != null && chatSettings.shouldShowKustomerBranding() ?
                        View.VISIBLE : View.GONE);
            }
        };
        handler.post(runnable);
    }

    @Override
    public void objectDataSourceOnError(KUSObjectDataSource dataSource, Error error) {
        if (dataSource == userSession.getScheduleDataSource()) {
            int statusCode = KUSJsonHelper.getErrorStatus(error);

            boolean isNotFoundError = statusCode == KUSConstants.ApiStatusCodes.NOT_FOUND_CODE;

            if (isNotFoundError) {
                handleSuccessfulDataLoad();
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        String errorText = getString(R.string.com_kustomer_something_went_wrong_please_try_again);
                        showErrorWithText(errorText);
                        rvSessions.setVisibility(View.INVISIBLE);
                        btnNewConversation.setVisibility(View.INVISIBLE);
                    }
                };
                handler.post(runnable);
            }
        }
    }

    //endregion
}