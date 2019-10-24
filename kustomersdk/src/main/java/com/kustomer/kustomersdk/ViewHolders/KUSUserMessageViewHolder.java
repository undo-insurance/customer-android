package com.kustomer.kustomersdk.ViewHolders;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.kustomer.kustomersdk.Adapters.KUSMessageListAdapter;
import com.kustomer.kustomersdk.Enums.KUSChatMessageState;
import com.kustomer.kustomersdk.Enums.KUSChatMessageType;
import com.kustomer.kustomersdk.Helpers.KUSCache;
import com.kustomer.kustomersdk.Helpers.KUSDate;
import com.kustomer.kustomersdk.Helpers.KUSText;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Views.KUSSquareFrameLayout;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Junaid on 1/19/2018.
 */

public class KUSUserMessageViewHolder extends RecyclerView.ViewHolder {

    //region Properties
    private static final long OPTIMISTIC_SEND_LOADING_DELAY = 750;

    @BindView(R2.id.tvMessage)
    TextView tvMessage;
    @BindView(R2.id.tvDate)
    TextView tvDate;
    @BindView(R2.id.ivAttachmentImage)
    ImageView ivAttachmentImage;
    @BindView(R2.id.attachmentLayout)
    KUSSquareFrameLayout attachmentLayout;
    @BindView(R2.id.progressBarImage)
    ProgressBar progressBarImage;
    private Timer sendingFadingTimer;
    @BindView(R2.id.retry)
    ImageView retry;

    private boolean imageLoadedSuccessfully = false;
    private KUSChatMessage chatMessage;
    private KUSMessageListAdapter.ChatMessageItemListener mListener;
    //endregion

    //region LifeCycle
    public KUSUserMessageViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this,itemView);
    }

    public void onBind(final KUSChatMessage chatMessage, boolean showDate, KUSMessageListAdapter.ChatMessageItemListener listener){
        this.chatMessage = chatMessage;
        this.mListener = listener;

        if(chatMessage.getType() == KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_TEXT){
            tvMessage.setVisibility(View.VISIBLE);
            attachmentLayout.setVisibility(View.GONE);
            KUSText.setMarkDownText(tvMessage,chatMessage.getBody().trim());
        }else if(chatMessage.getType() == KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_IMAGE){
            tvMessage.setVisibility(View.GONE);
            attachmentLayout.setVisibility(View.VISIBLE);

            updateImageForMessage();
        }

        if(chatMessage.getState() == KUSChatMessageState.KUS_CHAT_MESSAGE_STATE_FAILED){
            retry.setVisibility(View.VISIBLE);
            retry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onChatMessageErrorClicked(chatMessage);
                }
            });
        }else{
            retry.setVisibility(View.INVISIBLE);
        }

        if(showDate){
            tvDate.setVisibility(View.VISIBLE);
            tvDate.setText(KUSDate.messageTimeStampTextFromDate(chatMessage.getCreatedAt()));
        }else {
            tvDate.setText("");
            tvDate.setVisibility(View.GONE);
        }

        updateAlphaForState();
    }

    //endregion

    //region Private Methods
    private void updateImageForMessage(){

        progressBarImage.setVisibility(View.VISIBLE);

        Bitmap cachedImage = new KUSCache().getBitmapFromMemCache(chatMessage.getImageUrl().toString());
        if(cachedImage != null){
            ivAttachmentImage.setImageBitmap(cachedImage);
            progressBarImage.setVisibility(View.GONE);
            imageLoadedSuccessfully = true;
        }else {
            GlideUrl glideUrl = new GlideUrl(chatMessage.getImageUrl().toString(), new LazyHeaders.Builder()
                    .addHeader(KUSConstants.Keys.K_KUSTOMER_TRACKING_TOKEN_HEADER_KEY, Kustomer.getSharedInstance().getUserSession().getTrackingTokenDataSource().getCurrentTrackingToken())
                    .build());

            Glide.with(itemView)
                    .load(glideUrl)
                    .error(R.drawable.kus_ic_error_outline_red_33dp)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            imageLoadedSuccessfully = false;
                            ivAttachmentImage.setScaleType(ImageView.ScaleType.CENTER);
                            progressBarImage.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            imageLoadedSuccessfully = true;
                            ivAttachmentImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            progressBarImage.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(ivAttachmentImage);
        }

        ivAttachmentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!imageLoadedSuccessfully)
                    updateImageForMessage();
                else
                    mListener.onChatMessageImageClicked(chatMessage);
            }
        });
    }

    private void updateAlphaForState(){
        switch (chatMessage.getState()){
            case KUS_CHAT_MESSAGE_STATE_SENT:
                tvMessage.setAlpha(1.0f);
                attachmentLayout.setAlpha(1.0f);
                stopTimer();
                break;
            case KUS_CHAT_MESSAGE_STATE_SENDING:{
                long timeElapsed = Calendar.getInstance().getTimeInMillis() - chatMessage.getCreatedAt().getTime();
                if(timeElapsed >= OPTIMISTIC_SEND_LOADING_DELAY){
                    tvMessage.setAlpha(0.5f);
                    attachmentLayout.setAlpha(0.5f);
                    stopTimer();
                }else{
                    tvMessage.setAlpha(1.0f);
                    attachmentLayout.setAlpha(1.0f);

                    long timeInterval = OPTIMISTIC_SEND_LOADING_DELAY - timeElapsed;
                    startTimer(timeInterval);

                }

            }   break;
            case KUS_CHAT_MESSAGE_STATE_FAILED:
                tvMessage.setAlpha(0.5f);
                attachmentLayout.setAlpha(0.5f);
                stopTimer();
                break;
        }
    }

    private void stopTimer(){
        if(sendingFadingTimer != null) {
            sendingFadingTimer.cancel();
            sendingFadingTimer = null;
        }
    }

    private void startTimer(long time) {
        try {
            stopTimer();

            final Handler handler = new Handler();
            sendingFadingTimer = new Timer();
            TimerTask doAsynchronousTask = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            updateAlphaForState();
                        }
                    });
                }
            };
            sendingFadingTimer.schedule(doAsynchronousTask, time);
        }catch (Exception ignore){}
    }
    //endregion

}
