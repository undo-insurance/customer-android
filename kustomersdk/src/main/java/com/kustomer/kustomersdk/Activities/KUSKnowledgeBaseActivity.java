package com.kustomer.kustomersdk.Activities;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.BaseClasses.BaseActivity;
import com.kustomer.kustomersdk.Helpers.KUSLocalization;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Utils.KUSConstants;

import java.lang.reflect.Method;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;

public class KUSKnowledgeBaseActivity extends BaseActivity {

    //region Properties
    @BindView(R2.id.wvKnowledge)
    WebView wvKnowledge;
    @BindView(R2.id.progressBar)
    ProgressBar progressBar;

    @BindView(R2.id.flWebBack)
    View backButton;
    @BindView(R2.id.flWebForward)
    View forwardButton;
    @BindView(R2.id.flWebRefresh)
    View refreshButton;
    //endregion

    //region LifeCycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLayout(R.layout.kus_activity_knowledge_base, R.id.toolbar_main, null, true);
        super.onCreate(savedInstanceState);

        enableMixedContentModeCompat(wvKnowledge.getSettings());

        wvKnowledge.getSettings().setDomStorageEnabled(true);
        wvKnowledge.getSettings().setJavaScriptEnabled(true);
        wvKnowledge.getSettings().setLoadsImagesAutomatically(true);
        wvKnowledge.getSettings().setBlockNetworkImage(false);
        wvKnowledge.setWebViewClient(new WebViewController());
        wvKnowledge.loadUrl(getUrl());

        updateButtons();
    }

    private void enableMixedContentModeCompat(WebSettings settings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wvKnowledge.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        } else {
            //setMixedContentMode() was made public in API 21. Use reflection to get it for older SDK
            try {
                Method m = WebSettings.class.getMethod("setMixedContentMode", int.class);
                if (m == null) {
                    Log.e("WebSettings", "Error getting setMixedContentMode method");
                } else {
                    m.invoke(settings, 0); // 0 = MIXED_CONTENT_ALWAYS_ALLOW
                    Log.i("WebSettings", "Successfully set MIXED_CONTENT_ALWAYS_ALLOW");
                }
            } catch (Exception ex) {
                Log.e("WebSettings", "Error calling setMixedContentMode: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void finish() {
        super.finish();

        if (KUSLocalization.getSharedInstance().isLTR())
            overridePendingTransition(R.anim.kus_stay, R.anim.kus_slide_right);
        else
            overridePendingTransition(R.anim.kus_stay, R.anim.kus_slide_right_rtl);
    }
    //endregion

    //region Intializer
    private void updateButtons(){
        if(wvKnowledge.canGoBack()) {
            backButton.setAlpha(1.0f);
            backButton.setClickable(true);
        }
        else {
            backButton.setAlpha(0.3f);
            backButton.setClickable(false);
        }

        if(wvKnowledge.canGoForward()) {
            forwardButton.setAlpha(1.0f);
            forwardButton.setClickable(true);
        }
        else {
            forwardButton.setAlpha(0.3f);
            forwardButton.setClickable(false);
        }
    }
    //endregion

    //region private method
    private String getUrl() {
        String url = getIntent().getStringExtra(KUSConstants.Keys.K_KUSTOMER_URL_KEY);
        if (url != null) {
            return url;
        }else {
            KUSUserSession userSession = Kustomer.getSharedInstance().getUserSession();
            return String.format(Locale.getDefault(), "https://%s.kustomer.help/", userSession.getOrgName());
        }
    }
    //endregion

    //region Listener
    @OnClick(R2.id.flWebBack)
    void onWebBackPressed(){
        wvKnowledge.goBack();
    }

    @OnClick(R2.id.flWebForward)
    void onWebForwardPressed(){
        wvKnowledge.goForward();
    }

    @OnClick(R2.id.flWebRefresh)
    void onWebRefreshPressed(){
        wvKnowledge.reload();
    }
    //endregion

    //region WebView Client
    public class WebViewController extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            getSupportActionBar().setSubtitle(url);
            updateButtons();
        }
    }
    //endregion
}


