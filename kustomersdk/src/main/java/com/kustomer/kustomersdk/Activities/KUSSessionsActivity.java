package com.kustomer.kustomersdk.Activities;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Adapters.SessionListAdapter;
import com.kustomer.kustomersdk.BaseClasses.BaseActivity;
import com.kustomer.kustomersdk.DataSources.KUSChatSessionsDataSource;
import com.kustomer.kustomersdk.DataSources.KUSObjectDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.Helpers.KUSLocalization;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSPaginatedDataSourceListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Views.KUSToolbar;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;

public class KUSSessionsActivity extends BaseActivity implements KUSPaginatedDataSourceListener, SessionListAdapter.onItemClickListener, KUSToolbar.OnToolbarItemClickListener, KUSObjectDataSourceListener {

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
    private SessionListAdapter adapter;
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

        boolean shouldCreateNewSessionWithMessage = chatSessionsDataSource.getMessageToCreateNewChatSession() != null;

        if (chatSessionsDataSource.isFetched() || shouldCreateNewSessionWithMessage) {
            handleFirstLoadIfNecessary();
        } else {
            rvSessions.setVisibility(View.INVISIBLE);
            btnNewConversation.setVisibility(View.INVISIBLE);
            showProgressBar();
        }

        showKustomerBrandingFooterIfNeeded();
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
        if (chatSessionsDataSource != null)
            chatSessionsDataSource.removeListener(this);

        userSession.getPushClient().setSupportScreenShown(false);
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.kus_stay, R.anim.kus_slide_down);
    }
    //endregion

    //region Initializer

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
        adapter = new SessionListAdapter(rvSessions, chatSessionsDataSource, userSession, this);
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
        if (didHandleFirstLoad)
            return;
        didHandleFirstLoad = true;

        Intent intent = new Intent(this, KUSChatActivity.class);
        boolean shouldOpenChatActivity = true;

        if (chatSessionsDataSource.getMessageToCreateNewChatSession() != null) {
            intent.putExtra(KUSConstants.BundleName.CHAT_SCREEN_MESSAGE,
                    chatSessionsDataSource.getMessageToCreateNewChatSession());

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
    //endregion

    //region Listeners
    @Optional
    @OnClick(R2.id.btnRetry)
    void userTappedRetry() {
        chatSessionsDataSource.fetchLatest();
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
        if (dataSource != userSession.getChatSettingsDataSource())
            return;

        userSession.getChatSettingsDataSource().removeListener(this);

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
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

    }

    //endregion
}
