package com.kustomer.kustomersdk.Activities;

import android.Manifest;
import android.animation.LayoutTransition;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Adapters.MessageListAdapter;
import com.kustomer.kustomersdk.BaseClasses.BaseActivity;
import com.kustomer.kustomersdk.DataSources.KUSChatMessagesDataSource;
import com.kustomer.kustomersdk.DataSources.KUSObjectDataSource;
import com.kustomer.kustomersdk.DataSources.KUSPaginatedDataSource;
import com.kustomer.kustomersdk.DataSources.KUSTeamsDataSource;
import com.kustomer.kustomersdk.Enums.KUSChatMessageType;
import com.kustomer.kustomersdk.Enums.KUSFormQuestionProperty;
import com.kustomer.kustomersdk.Helpers.KUSLocalization;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Helpers.KUSPermission;
import com.kustomer.kustomersdk.Helpers.KUSText;
import com.kustomer.kustomersdk.Interfaces.KUSChatMessagesDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSEmailInputViewListener;
import com.kustomer.kustomersdk.Interfaces.KUSInputBarViewListener;
import com.kustomer.kustomersdk.Interfaces.KUSMLFormValuesPickerViewListener;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Interfaces.KUSOptionPickerViewListener;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSBitmap;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.Models.KUSChatSession;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSFormQuestion;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.Models.KUSTeam;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Utils.KUSUtils;
import com.kustomer.kustomersdk.Views.KUSEmailInputView;
import com.kustomer.kustomersdk.Views.KUSInputBarView;
import com.kustomer.kustomersdk.Views.KUSLargeImageViewer;
import com.kustomer.kustomersdk.Views.KUSMLFormValuesPickerView;
import com.kustomer.kustomersdk.Views.KUSOptionsPickerView;
import com.kustomer.kustomersdk.Views.KUSToolbar;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;

public class KUSChatActivity extends BaseActivity implements KUSChatMessagesDataSourceListener, KUSToolbar.OnToolbarItemClickListener, KUSEmailInputViewListener, KUSInputBarViewListener, KUSOptionPickerViewListener, MessageListAdapter.ChatMessageItemListener, KUSMLFormValuesPickerViewListener, KUSObjectDataSourceListener {

    //region Properties
    private static final int REQUEST_IMAGE_CAPTURE = 1122;
    private static final int GALLERY_INTENT = 1123;
    private static final int REQUEST_CAMERA_PERMISSION = 1133;
    private static final int REQUEST_STORAGE_PERMISSION = 1134;

    @BindView(R2.id.rvMessages)
    RecyclerView rvMessages;
    @BindView(R2.id.emailInputView)
    KUSEmailInputView emailInputView;
    @BindView(R2.id.app_bar_layout)
    AppBarLayout appBarLayout;
    @BindView(R2.id.kusInputBarView)
    KUSInputBarView kusInputBarView;
    @BindView(R2.id.kusOptionPickerView)
    KUSOptionsPickerView kusOptionPickerView;
    @BindView(R2.id.tvStartANewConversation)
    TextView tvStartANewConversation;
    @BindView(R2.id.tvClosedChat)
    TextView tvClosedChat;
    @BindView(R2.id.btnEndChat)
    Button btnEndChat;
    @BindView(R2.id.ivNonBusinessHours)
    ImageView ivNonBusinessHours;
    @BindView(R2.id.mlFormValuesPicker)
    KUSMLFormValuesPickerView mlFormValuesPickerView;
    @BindView(R2.id.footerLayout)
    LinearLayout footerLayout;

    KUSChatSession kusChatSession;
    KUSUserSession userSession;
    KUSChatMessagesDataSource chatMessagesDataSource;
    KUSTeamsDataSource teamOptionsDatasource;
    String chatSessionId;
    MessageListAdapter adapter;
    KUSToolbar kusToolbar;
    boolean shouldShowBackButton = true;
    boolean backPressed = false;
    boolean shouldShowNonBusinessHoursImage = false;

    String message;

    String mCurrentPhotoPath;
    //endregion

    //region LifeCycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLayout(R.layout.kus_activity_kuschat, R.id.toolbar_main, null, false);
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        initData();
        initViews();
        setupAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        KUSChatSession session = (KUSChatSession) userSession.getChatSessionsDataSource().findById(chatSessionId);

        if ((session == null || session.getLockedAt() == null)
                && tvClosedChat.getVisibility() == View.GONE
                && kusOptionPickerView.getVisibility() == View.GONE
                && !shouldShowNonBusinessHoursImage) {
            kusInputBarView.requestInputFocus();
        }

        if (userSession != null && chatSessionId != null)
            userSession.getChatSessionsDataSource().updateLastSeenAtForSessionId(chatSessionId, null);
    }

    @Override
    protected void onPause() {
        if (userSession != null && chatSessionId != null)
            userSession.getChatSessionsDataSource().updateLastSeenAtForSessionId(chatSessionId, null);

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (chatMessagesDataSource != null)
            chatMessagesDataSource.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (shouldShowBackButton) {
            backPressed = true;
            super.onBackPressed();
        } else {
            clearAllLibraryActivities();
        }
    }

    @Override
    public void finish() {
        super.finish();

        if (backPressed) {
            if (KUSLocalization.getSharedInstance().isLTR())
                overridePendingTransition(R.anim.kus_stay, R.anim.kus_slide_right);
            else
                overridePendingTransition(R.anim.kus_stay, R.anim.kus_slide_right_rtl);
        } else
            overridePendingTransition(R.anim.kus_stay, R.anim.kus_slide_down);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        checkShouldShowEmailInput();
        checkShouldShowInputView();
        checkShouldShowCloseChatButtonView();

        updateOptionPickerHeight();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK && mCurrentPhotoPath != null) {
                Uri photoUri = KUSUtils.getUriFromFile(this, new File(mCurrentPhotoPath));

                if (photoUri != null) {
                    String photoPath = photoUri.toString();
                    attachImage(photoPath);
                }
                mCurrentPhotoPath = null;
            } else {
                mCurrentPhotoPath = null;
            }
        } else if (requestCode == GALLERY_INTENT && resultCode == RESULT_OK) {
            if (data != null) {
                String photoUri = data.getDataString();
                attachImage(photoUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, R.string.com_kustomer_camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_STORAGE_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                } else {
                    Toast.makeText(this, R.string.com_kustomer_storage_permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //endregion

    //region Initializer
    private void initData() {
        userSession = Kustomer.getSharedInstance().getUserSession();
        kusChatSession = (KUSChatSession) getIntent().getSerializableExtra(KUSConstants.BundleName.CHAT_SESSION_BUNDLE_KEY);
        shouldShowBackButton = getIntent().getBooleanExtra(KUSConstants.BundleName.CHAT_SCREEN_BACK_BUTTON_KEY, true);
        message = getIntent().getStringExtra(KUSConstants.BundleName.CHAT_SCREEN_MESSAGE);

        shouldShowNonBusinessHoursImage = !userSession.getScheduleDataSource().isActiveBusinessHours();

        KUSChatSettings settings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
        if (settings != null && settings.getNoHistory())
            shouldShowBackButton = false;

        if (kusChatSession != null) {
            chatSessionId = kusChatSession.getId();
            chatMessagesDataSource = userSession.chatMessageDataSourceForSessionId(chatSessionId);
        } else {
            chatMessagesDataSource = new KUSChatMessagesDataSource(userSession, true);
        }

        if (message != null) {
            shouldShowBackButton = false;
            chatMessagesDataSource.sendMessageWithText(message, null);
            userSession.getChatSessionsDataSource().setMessageToCreateNewChatSession(null);
        }

        chatMessagesDataSource.addListener(this);
        chatMessagesDataSource.fetchLatest();
        if (!chatMessagesDataSource.isFetched()) {
            progressDialog.show();
        }
    }

    private void initViews() {
        kusInputBarView.initWithUserSession(userSession);
        kusInputBarView.setListener(this);
        kusInputBarView.setAllowsAttachment(chatMessagesDataSource != null
                && chatMessagesDataSource.shouldAllowAttachments());
        kusOptionPickerView.setListener(this);
        mlFormValuesPickerView.setListener(this);
        updateOptionPickerHeight();
        setupToolbar();
        checkShouldShowInputView();
        showNonBusinessHoursImageIfNeeded();
        showKustomerBrandingFooterIfNeeded();
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

    private void updateOptionPickerHeight() {
        kusOptionPickerView.setMaxHeight(KUSUtils.getWindowHeight(this) / 2);
        mlFormValuesPickerView.setOptionPickerMaxHeight(KUSUtils.getWindowHeight(this) / 3);
    }

    private void showNonBusinessHoursImageIfNeeded() {

        if (chatMessagesDataSource != null && chatMessagesDataSource.getSize() > 0) {
            shouldShowNonBusinessHoursImage = false;
            ivNonBusinessHours.setVisibility(View.GONE);
            return;
        }

        if (!shouldShowNonBusinessHoursImage) {
            ivNonBusinessHours.setVisibility(View.GONE);
            return;
        }

        KUSChatSettings chatSettings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
        if (chatSettings != null && chatSettings.getOffHoursImageUrl() != null
                && !chatSettings.getOffHoursImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(chatSettings.getOffHoursImageUrl())
                    .apply(RequestOptions.noAnimation())
                    .into(ivNonBusinessHours);
        } else {
            ivNonBusinessHours.setImageDrawable(getResources().getDrawable(R.drawable.kus_away_image));
        }
        ivNonBusinessHours.setVisibility(View.VISIBLE);
    }

    private void setupToolbar() {
        kusToolbar = (KUSToolbar) toolbar;
        kusToolbar.initWithUserSession(userSession);
        kusToolbar.setExtraLargeSize(chatMessagesDataSource.getSize() == 0);
        kusToolbar.setSessionId(chatSessionId);
        kusToolbar.setShowLabel(true);
        kusToolbar.setListener(this);
        kusToolbar.setShowBackButton(shouldShowBackButton);
        kusToolbar.setShowDismissButton(true);

        checkShouldShowEmailInput();
    }

    @Nullable
    private String getValidChatSessionId() {
        return chatSessionId == null ||
                chatSessionId.equals(KUSConstants.ChatSession.TEMP_SESSION_ID) ? null : chatSessionId;
    }

    private void checkShouldShowCloseChatButtonView() {
        KUSChatSettings settings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();

        if (settings != null && settings.getClosableChat() && getValidChatSessionId() != null) {
            KUSChatSession session = (KUSChatSession) userSession
                    .getChatSessionsDataSource().findById(getValidChatSessionId());

            if (session.getLockedAt() == null && chatMessagesDataSource.isAnyMessageByCurrentUser()) {
                if (btnEndChat.getVisibility() != View.VISIBLE) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            btnEndChat.setVisibility(View.VISIBLE);
                        }
                    };
                    handler.postDelayed(runnable, 500);
                }
                return;
            }
        }

        btnEndChat.setVisibility(View.GONE);
    }

    private void checkShouldShowEmailInput() {
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape && KUSUtils.isPhone(this)) {
            appBarLayout.setLayoutTransition(null);

            emailInputView.setVisibility(View.GONE);
        } else {
            KUSChatSettings settings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
            boolean isChatCloseable = settings != null && settings.getClosableChat();

            boolean shouldShowEmailInput = userSession.isShouldCaptureEmail()
                    && getValidChatSessionId() != null && !isChatCloseable;

            appBarLayout.setLayoutTransition(new LayoutTransition());

            if (shouldShowEmailInput) {
                emailInputView.setVisibility(View.VISIBLE);
                emailInputView.setListener(this);
            } else {
                emailInputView.setVisibility(View.GONE);
            }
        }
    }

    private void checkShouldShowOptionPicker() {
        KUSChatSession session = (KUSChatSession) userSession
                .getChatSessionsDataSource().findById(getValidChatSessionId());
        if (session != null && session.getLockedAt() != null) {
            return;
        }

        if (chatMessagesDataSource.isChatClosed() && chatMessagesDataSource.getOtherUserIds().size() == 0) {
            return;
        }

        KUSFormQuestion vcCurrentQuestion = chatMessagesDataSource.volumeControlCurrentQuestion();

        boolean wantsOptionPicker = (vcCurrentQuestion != null &&
                vcCurrentQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_FOLLOW_UP_CHANNEL);

        if (wantsOptionPicker) {
            kusInputBarView.setVisibility(View.GONE);
            kusInputBarView.clearInputFocus();
            kusInputBarView.setText("");

            if (vcCurrentQuestion.getValues() != null && vcCurrentQuestion.getValues().size() > 0) {
                kusOptionPickerView.setVisibility(View.VISIBLE);
                updateOptionsPickerOptions();
            }
            return;
        }

        KUSFormQuestion currentQuestion = chatMessagesDataSource.currentQuestion();

        boolean wantMultiLevelValuesPicker = (currentQuestion != null
                && currentQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_MLV);

        if (wantMultiLevelValuesPicker) {
            kusInputBarView.setVisibility(View.GONE);
            kusInputBarView.clearInputFocus();

            if (currentQuestion.getMlFormValues() != null
                    && currentQuestion.getMlFormValues().getMlNodes() != null) {
                if (currentQuestion.getMlFormValues().getMlNodes().size() > 0
                        && mlFormValuesPickerView.getVisibility() == View.GONE) {

                    mlFormValuesPickerView.setMlFormValues(currentQuestion.getMlFormValues().getMlNodes(),
                            currentQuestion.getMlFormValues().getLastNodeRequired());
                    mlFormValuesPickerView.setVisibility(View.VISIBLE);
                }
            }

            return;
        }

        wantsOptionPicker = (currentQuestion != null
                && currentQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_VALUES
                && currentQuestion.getValues() != null && currentQuestion.getValues().size() > 0);

        if (wantsOptionPicker) {
            kusInputBarView.setVisibility(View.GONE);
            kusInputBarView.clearInputFocus();
            kusInputBarView.setText("");
            kusOptionPickerView.setVisibility(View.VISIBLE);
            updateOptionsPickerOptions();
            return;
        }

        wantsOptionPicker = (currentQuestion != null
                && currentQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CONVERSATION_TEAM
                && currentQuestion.getValues() != null && currentQuestion.getValues().size() > 0);

        boolean teamOptionsDidFail = teamOptionsDatasource != null && (teamOptionsDatasource.getError() != null
                || (teamOptionsDatasource.isFetched() && teamOptionsDatasource.getSize() == 0));
        if (wantsOptionPicker && !teamOptionsDidFail) {
            kusInputBarView.setVisibility(View.GONE);
            kusInputBarView.clearInputFocus();
            kusInputBarView.setText("");

            List<String> teamIds = currentQuestion.getValues();
            if (teamOptionsDatasource == null || !teamOptionsDatasource.getTeamIds().equals(teamIds)) {
                teamOptionsDatasource = new KUSTeamsDataSource(userSession, teamIds);
                teamOptionsDatasource.addListener(this);
                teamOptionsDatasource.fetchLatest();
            }

            kusOptionPickerView.setVisibility(View.VISIBLE);
            updateOptionsPickerOptions();
        } else {
            teamOptionsDatasource = null;

            kusInputBarView.setVisibility(View.VISIBLE);
            kusOptionPickerView.setVisibility(View.GONE);
            tvClosedChat.setVisibility(View.GONE);
            tvStartANewConversation.setVisibility(View.GONE);
            mlFormValuesPickerView.setVisibility(View.GONE);
        }
    }

    private void checkShouldShowInputView() {
        KUSChatSession session = (KUSChatSession) userSession
                .getChatSessionsDataSource().findById(getValidChatSessionId());

        if (session != null && session.getLockedAt() != null) {
            kusInputBarView.setVisibility(View.GONE);
            kusInputBarView.clearInputFocus();
            kusInputBarView.setText("");
            kusInputBarView.removeAllAttachments();
            kusOptionPickerView.setVisibility(View.GONE);
            tvClosedChat.setVisibility(View.GONE);


            if (userSession.getSharedPreferences().getShouldHideConversationButton())
                tvStartANewConversation.setVisibility(View.GONE);
            else
                tvStartANewConversation.setVisibility(View.VISIBLE);

            if (isBackToChatButton()) {
                tvStartANewConversation.setText(R.string.com_kustomer_back_to_chat);
            } else {
                if (userSession.getScheduleDataSource().isActiveBusinessHours()) {
                    tvStartANewConversation.setText(R.string.com_kustomer_start_a_new_conversation);
                } else {
                    tvStartANewConversation.setText(R.string.com_kustomer_leave_a_message);
                }
            }

            return;
        }

        boolean wantsClosedView = chatMessagesDataSource != null &&
                chatMessagesDataSource.isChatClosed() &&
                chatMessagesDataSource.getOtherUserIds().size() == 0;
        if (wantsClosedView) {
            kusInputBarView.setVisibility(View.GONE);
            kusInputBarView.clearInputFocus();
            kusOptionPickerView.setVisibility(View.GONE);
            tvClosedChat.setVisibility(View.VISIBLE);

            return;
        }

        checkShouldShowOptionPicker();
    }

    private void updateOptionsPickerOptions() {
        KUSFormQuestion vcCurrentQuestion = chatMessagesDataSource.volumeControlCurrentQuestion();
        boolean wantsOptionPicker = (vcCurrentQuestion != null
                && vcCurrentQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_FOLLOW_UP_CHANNEL
                && vcCurrentQuestion.getValues() != null && vcCurrentQuestion.getValues().size() > 0);
        if (wantsOptionPicker) {
            kusOptionPickerView.setOptions(vcCurrentQuestion.getValues());
            return;
        }

        KUSFormQuestion currentQuestion = chatMessagesDataSource.currentQuestion();
        wantsOptionPicker = (currentQuestion != null
                && currentQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_VALUES
                && currentQuestion.getValues() != null && currentQuestion.getValues().size() > 0);
        if (wantsOptionPicker) {
            kusOptionPickerView.setOptions(currentQuestion.getValues());
            return;
        }

        if (teamOptionsDatasource != null) {
            List<String> options = new ArrayList<>();
            for (KUSModel model : teamOptionsDatasource.getList()) {
                KUSTeam team = (KUSTeam) model;
                options.add(team.fullDisplay());
            }
            kusOptionPickerView.setOptions(options);
        }
    }

    private void setupAdapter() {
        adapter = new MessageListAdapter(chatMessagesDataSource, userSession, chatMessagesDataSource, this);
        rvMessages.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        rvMessages.setLayoutManager(layoutManager);

        adapter.notifyDataSetChanged();
    }

    private void openCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!KUSPermission.isCameraPermissionAvailable(this)) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);

            } else {
                dispatchTakePictureIntent();
            }
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ignored) {
            }

            if (photoFile != null) {
                Uri photoURI = KUSUtils.getUriFromFile(this, photoFile);

                if (photoURI != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Toast.makeText(this, getString(R.string.com_kustomer_unable_to_open_camera),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!KUSPermission.isStoragePermissionAvailable(this)) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);

            } else {
                startGalleryIntent();
            }
        } else {
            startGalleryIntent();
        }
    }


    private void startGalleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_INTENT);
    }

    private boolean isBackToChatButton() {
        KUSChatSettings settings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
        int openChats = userSession.getChatSessionsDataSource().getOpenChatSessionsCount();
        int proactiveChats = userSession.getChatSessionsDataSource().getOpenProactiveCampaignsCount();
        return (settings != null && settings.getSingleSessionChat() && (openChats - proactiveChats) >= 1);
    }

    private void attachImage(String path) {
        kusInputBarView.attachImage(path, new KUSInputBarView.MemoryListener() {
            @Override
            public void onOutOfMemoryError(OutOfMemoryError error) {
                KUSLog.KUSLogError(error.getMessage());
                showMemoryError();
            }
        });
    }

    private void showMemoryError() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                KUSUtils.showShortToast(KUSChatActivity.this,
                        getString(R.string.com_kustomer_low_memory_error));
            }
        };
        handler.post(runnable);
    }
    //endregion

    //region Listeners
    @OnClick(R2.id.btnEndChat)
    void endChatClicked() {
        showProgressBar();
        chatMessagesDataSource.endChat("customer_ended", new KUSChatMessagesDataSource.OnEndChatListener() {
            @Override
            public void onComplete(boolean success) {
                Handler handler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        hideProgressBar();
                    }
                };
                handler.post(runnable);

            }
        });

    }

    @OnClick(R2.id.tvStartANewConversation)
    void startNewConversationClicked() {
        chatMessagesDataSource.removeListener(this);

        if (isBackToChatButton()) {
            KUSChatSession chatSession = userSession.getChatSessionsDataSource().mostRecentNonProactiveCampaignOpenSession();
            chatSessionId = chatSession.getId();
            chatMessagesDataSource = userSession.chatMessageDataSourceForSessionId(chatSessionId);

        } else {
            chatMessagesDataSource = new KUSChatMessagesDataSource(userSession, true);
            chatSessionId = null;
            kusInputBarView.setAllowsAttachment(false);

            shouldShowNonBusinessHoursImage = !userSession.getScheduleDataSource().isActiveBusinessHours();
            ivNonBusinessHours.setVisibility(shouldShowNonBusinessHoursImage ? View.VISIBLE : View.GONE);
        }

        chatMessagesDataSource.addListener(this);

        adapter = null;
        setupAdapter();
        kusInputBarView.setVisibility(View.VISIBLE);
        kusInputBarView.setText("");
        kusInputBarView.removeAllAttachments();
        tvStartANewConversation.setVisibility(View.GONE);
        kusToolbar.setSessionId(chatSessionId);

        checkShouldShowEmailInput();
        checkShouldShowCloseChatButtonView();
        checkShouldShowInputView();

        kusToolbar.setExtraLargeSize(chatMessagesDataSource.getSize() == 0);
    }

    @Override
    public void onLoad(final KUSPaginatedDataSource dataSource) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null)
                    progressDialog.dismiss();

                if (dataSource == chatMessagesDataSource) {
                    checkShouldShowCloseChatButtonView();
                    if (isBackToChatButton()) {
                        tvStartANewConversation.setText(R.string.com_kustomer_back_to_chat);
                    } else {
                        if (userSession.getScheduleDataSource().isActiveBusinessHours()) {
                            tvStartANewConversation.setText(R.string.com_kustomer_start_a_new_conversation);
                        } else {
                            tvStartANewConversation.setText(R.string.com_kustomer_leave_a_message);
                        }
                    }

                    shouldShowNonBusinessHoursImage = false;
                    ivNonBusinessHours.setVisibility(View.GONE);
                }
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onError(KUSPaginatedDataSource dataSource, Error error) {
        if (dataSource == chatMessagesDataSource && !chatMessagesDataSource.isFetched()) {
            final WeakReference<KUSChatActivity> weakReference = new WeakReference<>(this);
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    KUSChatActivity strongReference = weakReference.get();
                    if (strongReference != null)
                        strongReference.chatMessagesDataSource.fetchLatest();
                }
            };
            handler.postDelayed(runnable, 1000);
        } else if (dataSource == teamOptionsDatasource) {
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    checkShouldShowInputView();
                }
            };
            handler.post(runnable);
        }
    }

    @Override
    public void onContentChange(final KUSPaginatedDataSource dataSource) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (dataSource == chatMessagesDataSource) {
                    adapter.notifyDataSetChanged();
                    checkShouldShowInputView();
                    checkShouldShowCloseChatButtonView();

                    if (dataSource.getSize() >= 1)
                        setupToolbar();

                    boolean shouldAllowAttachments = chatMessagesDataSource.shouldAllowAttachments();
                    if (!shouldAllowAttachments) {
                        kusInputBarView.removeAllAttachments();
                    }
                    kusInputBarView.setAllowsAttachment(shouldAllowAttachments);

                    shouldShowNonBusinessHoursImage = false;
                    ivNonBusinessHours.setVisibility(View.GONE);
                } else if (dataSource == teamOptionsDatasource) {
                    checkShouldShowInputView();
                    updateOptionsPickerOptions();
                }
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onCreateSessionId(final KUSChatMessagesDataSource source, final String sessionId) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                chatSessionId = sessionId;
                kusInputBarView.setAllowsAttachment(getValidChatSessionId() != null);
                kusToolbar.setSessionId(chatSessionId);
                shouldShowBackButton = true;

                KUSChatSettings settings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
                shouldShowBackButton = !settings.getNoHistory();

                kusToolbar.setShowBackButton(shouldShowBackButton);
                setupToolbar();
                checkShouldShowEmailInput();
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onToolbarBackPressed() {
        onBackPressed();
    }

    @Override
    public void onToolbarClosePressed() {
        clearAllLibraryActivities();
    }


    @Override
    public void onSubmitEmail(String email) {
        userSession.submitEmail(email);
        checkShouldShowEmailInput();
    }

    @Override
    public void inputBarAttachmentClicked() {
        ArrayList<String> itemsList = new ArrayList<>();
        String[] items = null;

        if (KUSPermission.isCameraPermissionDeclared(this))
            itemsList.add(getString(R.string.com_kustomer_camera));
        if (KUSPermission.isReadPermissionDeclared(this))
            itemsList.add(getString(R.string.com_kustomer_gallery));

        if (itemsList.size() > 0) {
            items = new String[itemsList.size()];
            items = itemsList.toArray(items);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // camera
                        openCamera();
                        break;

                    case 1: // gallery
                        openGallery();
                        break;
                }
            }
        });
        builder.setNegativeButton(R.string.com_kustomer_cancel, null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void inputBarSendClicked() {
        if (chatMessagesDataSource.shouldPreventSendingMessage())
            return;

        final String text = kusInputBarView.getText();

        final List<Bitmap> bitmapList = new ArrayList<>();
        for (KUSBitmap kusBitmap : kusInputBarView.getKUSBitmapList()) {
            if (kusBitmap.getBitmap() != null)
                bitmapList.add(kusBitmap.getBitmap());
        }

        //Sending Data in background
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chatMessagesDataSource.sendMessageWithText(text, bitmapList);
                } catch (OutOfMemoryError e) {
                    KUSLog.KUSLogError(e.getMessage());
                    showMemoryError();
                }
            }
        }).start();

        kusInputBarView.setText("");
        kusInputBarView.removeAllAttachments();
    }

    @Override
    public boolean inputBarShouldEnableSend() {
        KUSFormQuestion currentVCQuestion = chatMessagesDataSource.volumeControlCurrentQuestion();
        if (currentVCQuestion != null) {

            if (currentVCQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_EMAIL) {
                return KUSText.isValidEmail(kusInputBarView.getText());
            } else if (currentVCQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_PHONE) {
                return KUSText.isValidPhone(kusInputBarView.getText());
            }
        }

        KUSFormQuestion currentQuestion = chatMessagesDataSource.currentQuestion();
        if (currentQuestion != null && currentQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CUSTOMER_EMAIL)
            return KUSText.isValidEmail(kusInputBarView.getText());

        return true;
    }

    @Override
    public void optionPickerOnOptionSelected(String option) {
        KUSTeam team = null;

        int optionIndex = kusOptionPickerView.getOptions().indexOf(option);
        KUSFormQuestion currentQuestion = chatMessagesDataSource.currentQuestion();

        if (optionIndex >= 0
                && currentQuestion != null
                && currentQuestion.getProperty() == KUSFormQuestionProperty.KUS_FORM_QUESTION_PROPERTY_CONVERSATION_TEAM
                && optionIndex < (teamOptionsDatasource != null ? teamOptionsDatasource.getSize() : 0))

            team = (KUSTeam) teamOptionsDatasource.get(optionIndex);

        chatMessagesDataSource.sendMessageWithText(
                team != null && team.displayName != null ? team.displayName : option,
                null,
                team != null ? team.getId() : null);
    }

    @Override
    public void onChatMessageImageClicked(KUSChatMessage chatMessage) {
        int startingIndex = 0;

        List<String> imageURIs = new ArrayList<>();

        for (int i = chatMessagesDataSource.getSize() - 1; i >= 0; i--) {
            KUSChatMessage kusChatMessage = (KUSChatMessage) chatMessagesDataSource.get(i);
            if (kusChatMessage.getType() == KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_IMAGE) {
                imageURIs.add(kusChatMessage.getImageUrl().toString());
            }
        }

        startingIndex = imageURIs.indexOf(chatMessage.getImageUrl().toString());

        new KUSLargeImageViewer(this).showImages(imageURIs, startingIndex);
    }

    @Override
    public void onChatMessageErrorClicked(final KUSChatMessage chatMessage) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                chatMessagesDataSource.resendMessage(chatMessage);
            }
        }).start();
    }

    @Override
    public void mlFormValueSelected(String option, String optionId) {
        chatMessagesDataSource.sendMessageWithText(option, null, optionId);
        kusInputBarView.setText("");
        kusInputBarView.removeAllAttachments();
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
