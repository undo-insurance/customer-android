package com.kustomer.kustomersdk.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.DataSources.KUSObjectDataSource;
import com.kustomer.kustomersdk.DataSources.KUSUserDataSource;
import com.kustomer.kustomersdk.Helpers.KSize;
import com.kustomer.kustomersdk.Helpers.KUSImage;
import com.kustomer.kustomersdk.Interfaces.KUSObjectDataSourceListener;
import com.kustomer.kustomersdk.Models.KUSChatSettings;
import com.kustomer.kustomersdk.Models.KUSUser;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.Utils.KUSUtils;

import java.net.URL;

/**
 * Created by Junaid on 1/30/2018.
 */

public class KUSAvatarImageView extends FrameLayout implements KUSObjectDataSourceListener {

    //region Properties
    private String userId;

    private KUSUserSession userSession;
    private KUSUserDataSource userDataSource;

    ImageView staticImageView;
    ImageView remoteImageView;

    int strokeWidth = 0;
    int fontSize = 10;
    int drawableSize = 30;

    //endregion

    //region Initializer
    public KUSAvatarImageView(@NonNull Context context) {
        super(context);
    }

    public KUSAvatarImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public KUSAvatarImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public KUSAvatarImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if(userSession != null && userSession.getChatSettingsDataSource() != null)
            userSession.getChatSettingsDataSource().removeListener(this);

        if(userDataSource != null)
            userDataSource.removeListener(this);
    }

    //endregion

    //region Public Methods
    public void initWithUserSession(KUSUserSession userSession){
        this.userSession = userSession;
        userSession.getChatSettingsDataSource().addListener(this);

        staticImageView = new ImageView(getContext());
        staticImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams params1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);

        //default Margin in case there is no stroke around the image.
        if(strokeWidth == 0)
            params1.setMargins(1,1,1,1);

        staticImageView.setLayoutParams(params1);
        staticImageView.setVisibility(INVISIBLE);
        addView(staticImageView);

        remoteImageView = new ImageView(getContext());
        remoteImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams params2 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);

        if(strokeWidth > 0)
            //noinspection SuspiciousNameCombination
            params2.setMargins(strokeWidth, strokeWidth, strokeWidth, strokeWidth);

        remoteImageView.setLayoutParams(params2);
        addView(remoteImageView);

        updateAvatarImage();
    }

    public void setUserId(String userId){
        if(this.userId != null && this.userId.equals(userId))
            return;

        if(userDataSource != null)
            userDataSource.removeListener(this);

        this.userId = userId;
        this.userDataSource = userSession.userDataSourceForUserId(userId);

        if(userDataSource != null)
            userDataSource.addListener(this);

        updateAvatarImage();
    }

    public void setStrokeWidth(int dp){
        strokeWidth = (int) KUSUtils.dipToPixels(getContext(),dp);
    }

    public void setFontSize (int fontSize){
        this.fontSize = fontSize;
    }

    public void setDrawableSize (int dp){
        drawableSize = dp;
    }
    //endregion

    //region Private Methods
    private void updateAvatarImage(){
        try {
            TypedValue typedValue = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.kus_company_image, typedValue, true);
            int drawableRes = typedValue.resourceId;

            Drawable companyAvatarImage = null;

            companyAvatarImage = getContext().getResources().getDrawable(drawableRes);
            if(this.userId == null && companyAvatarImage != null){
                staticImageView.setImageDrawable(companyAvatarImage);
                return;
            }

        } catch (Exception e) {}

        KUSUser user = null;
        if(userDataSource != null) {
            user = (KUSUser) userDataSource.getObject();
            if (user == null && !userDataSource.isFetching()) {
                userDataSource.fetch();
            }
        }

        KUSChatSettings chatSettings = (KUSChatSettings) userSession.getChatSettingsDataSource().getObject();
        if(userSession.getChatSettingsDataSource() != null && chatSettings == null && !this.userSession.getChatSettingsDataSource().isFetching()){
            userSession.getChatSettingsDataSource().fetch();
        }


        String name = "";
        if (user != null && user.getDisplayName() != null)
            name = user.getDisplayName();
        else if (chatSettings != null && chatSettings.getTeamName() != null)
            name = chatSettings.getTeamName();
        else if (userSession.getOrganizationName() != null)
            name = userSession.getOrganizationName();

        Bitmap placeHolderImage = KUSImage.defaultAvatarBitmapForName(getContext(),
                new KSize((int)KUSUtils.dipToPixels(getContext(),drawableSize),
                        (int)KUSUtils.dipToPixels(getContext(),drawableSize)),
                name,
                strokeWidth,
                fontSize);

        staticImageView.setImageBitmap(placeHolderImage);

        if(chatSettings != null) {
            URL iconURL = user != null && user.getAvatarURL() != null ? user.getAvatarURL() : chatSettings.getTeamIconURL();

            if(iconURL != null) {
                try {
                    Glide.with(getContext())
                            .load(iconURL.toString())
                            .apply(RequestOptions.circleCropTransform())
                            .apply(RequestOptions.noAnimation())
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    staticImageView.setVisibility(VISIBLE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    return false;
                                }
                            })
                            .into(new SimpleTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    staticImageView.setVisibility(VISIBLE);
                                    remoteImageView.setImageDrawable(resource);
                                }
                            });

                } catch (IllegalArgumentException ignore) {
                    staticImageView.setVisibility(VISIBLE);
                }
            }
            else{
                staticImageView.setVisibility(VISIBLE);
            }

        }
    }
    //endregion

    //region Listener
    @Override
    public void objectDataSourceOnLoad(final KUSObjectDataSource dataSource) {

        if(dataSource == userSession.getChatSettingsDataSource()){
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateAvatarImage();
                }
            };
            handler.post(runnable);
        }else if(dataSource == userDataSource){
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateAvatarImage();
                }
            };
            handler.post(runnable);
        }


    }

    @Override
    public void objectDataSourceOnError(KUSObjectDataSource dataSource, Error error) {

    }
    //endregion

}
