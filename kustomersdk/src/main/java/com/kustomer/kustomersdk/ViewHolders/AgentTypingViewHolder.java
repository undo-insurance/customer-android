package com.kustomer.kustomersdk.ViewHolders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Models.KUSTypingIndicator;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Views.KUSAvatarImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Junaid on 1/19/2018.
 */

public class AgentTypingViewHolder extends RecyclerView.ViewHolder {

    //region Properties

    @BindView(R2.id.flAvatar)
    FrameLayout imageLayout;

    //endregion

    //region Initializer
    public AgentTypingViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
    //endregion

    //region Methods
    public void onBind(KUSTypingIndicator typingIndicator, KUSUserSession userSession) {

        imageLayout.removeAllViews();

        KUSAvatarImageView avatarImageView = new KUSAvatarImageView(itemView.getContext());
        avatarImageView.setFontSize(16);
        avatarImageView.setDrawableSize(40);

        avatarImageView.initWithUserSession(userSession);
        avatarImageView.setUserId(typingIndicator.getUserId());

        FrameLayout.LayoutParams avatarLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        avatarImageView.setLayoutParams(avatarLayoutParams);

        imageLayout.addView(avatarImageView);

    }

    //endregion
}
