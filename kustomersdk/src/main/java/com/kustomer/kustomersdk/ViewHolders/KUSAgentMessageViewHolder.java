package com.kustomer.kustomersdk.ViewHolders;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Adapters.KUSMessageListAdapter;
import com.kustomer.kustomersdk.Enums.KUSChatMessageType;
import com.kustomer.kustomersdk.Helpers.GlideApp;
import com.kustomer.kustomersdk.Helpers.KUSDate;
import com.kustomer.kustomersdk.Helpers.KUSText;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Views.KUSAvatarImageView;
import com.kustomer.kustomersdk.Views.KUSSquareFrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Junaid on 1/19/2018.
 */

public class KUSAgentMessageViewHolder extends RecyclerView.ViewHolder {

    //region Properties
    @BindView(R2.id.tvMessage)
    TextView tvMessage;
    @BindView(R2.id.flAvatar)
    FrameLayout imageLayout;
    @BindView(R2.id.tvDate)
    TextView tvDate;
    @BindView(R2.id.ivAttachmentImage)
    ImageView ivAttachmentImage;
    @BindView(R2.id.attachmentLayout)
    KUSSquareFrameLayout attachmentLayout;
    @BindView(R2.id.progressBarImage)
    ProgressBar progressBarImage;

    private boolean imageLoadedSuccessfully = false;
    //endregion

    //region Initializer
    public KUSAgentMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
    //endregion

    //region Methods
    public void onBind(@Nullable final KUSChatMessage chatMessage,
                       @NonNull KUSUserSession userSession,
                       boolean showAvatar,
                       boolean showDate,
                       @NonNull final KUSMessageListAdapter.ChatMessageItemListener mListener) {

        if (chatMessage == null || chatMessage.getType() == KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_TEXT) {
            tvMessage.setVisibility(View.VISIBLE);
            attachmentLayout.setVisibility(View.GONE);
            KUSText.setMarkDownText(tvMessage,
                    chatMessage != null ? chatMessage.getBody().trim() : null);
        } else if (chatMessage.getType() == KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_IMAGE) {
            tvMessage.setVisibility(View.GONE);
            attachmentLayout.setVisibility(View.VISIBLE);

            updateImageForMessage(chatMessage, mListener);
        }

        imageLayout.removeAllViews();
        if (showAvatar) {
            KUSAvatarImageView avatarImageView = new KUSAvatarImageView(itemView.getContext());

            avatarImageView.setFontSize(16);
            avatarImageView.setDrawableSize(40);
            avatarImageView.initWithUserSession(userSession);
            avatarImageView.setUserId(chatMessage != null ? chatMessage.getSentById() : null);

            FrameLayout.LayoutParams avatarLayoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            avatarImageView.setLayoutParams(avatarLayoutParams);

            imageLayout.addView(avatarImageView);
        }

        if (chatMessage != null && showDate) {
            tvDate.setVisibility(View.VISIBLE);
            tvDate.setText(KUSDate.messageTimeStampTextFromDate(chatMessage.getCreatedAt()));
        } else {
            tvDate.setText("");
            tvDate.setVisibility(View.GONE);
        }

    }

    private void updateImageForMessage(@Nullable final KUSChatMessage chatMessage,
                                       @NonNull final KUSMessageListAdapter.ChatMessageItemListener mListener) {

        progressBarImage.setVisibility(View.VISIBLE);

        String imageUrl = chatMessage != null && chatMessage.getImageUrl() != null
                ? chatMessage.getImageUrl().toString()
                : null;

        GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                .addHeader(KUSConstants.Keys.K_KUSTOMER_TRACKING_TOKEN_HEADER_KEY, Kustomer.getSharedInstance().getUserSession().getTrackingTokenDataSource().getCurrentTrackingToken())
                .build());

        GlideApp.with(itemView)
                .load(glideUrl)
                .error(R.drawable.kus_ic_error_outline_red_33dp)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        imageLoadedSuccessfully = false;
                        ivAttachmentImage.setScaleType(ImageView.ScaleType.CENTER);
                        progressBarImage.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        imageLoadedSuccessfully = true;
                        ivAttachmentImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        progressBarImage.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(ivAttachmentImage);

        ivAttachmentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!imageLoadedSuccessfully) {
                    updateImageForMessage(chatMessage, mListener);
                } else {
                    mListener.onChatMessageImageClicked(chatMessage);
                }
            }
        });
    }
    //endregion
}
