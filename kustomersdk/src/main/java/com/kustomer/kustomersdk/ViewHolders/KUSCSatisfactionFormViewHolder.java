package com.kustomer.kustomersdk.ViewHolders;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Adapters.KUSMessageListAdapter;
import com.kustomer.kustomersdk.Models.KUSCSatisfactionForm;
import com.kustomer.kustomersdk.Models.KUSCSatisfactionResponse;
import com.kustomer.kustomersdk.R2;
import com.kustomer.kustomersdk.Utils.KUSUtils;
import com.kustomer.kustomersdk.Views.KUSAvatarImageView;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.kustomer.kustomersdk.Enums.KUSCSatisfactionFormResponseStatus.KUS_C_SATISFACTION_RESPONSE_STATUS_RATED;
import static com.kustomer.kustomersdk.Enums.KUSCSatisfactionScaleType.*;

public class KUSCSatisfactionFormViewHolder extends RecyclerView.ViewHolder {

    //region Properties
    @BindView(R2.id.flAvatar)
    FrameLayout imageLayout;
    @BindView(R2.id.tvCSatRatingPrompt)
    TextView tvCSatRatingPrompt;
    @BindView(R2.id.tvLowRating)
    TextView tvLowRating;
    @BindView(R2.id.tvHighRating)
    TextView tvHighRating;

    @BindView(R2.id.tvCSatCommentQuestion)
    TextView tvCSatCommentQuestion;
    @BindView(R2.id.etCSatComment)
    EditText etCSatComment;
    @BindView(R2.id.btnCSatSubmit)
    Button btnCSatSubmit;

    @BindView(R2.id.ivRating1)
    ImageView ivRating1;
    @BindView(R2.id.ivRating2)
    ImageView ivRating2;
    @BindView(R2.id.ivRating3)
    ImageView ivRating3;
    @BindView(R2.id.ivRating4)
    ImageView ivRating4;
    @BindView(R2.id.ivRating5)
    ImageView ivRating5;

    @BindView(R2.id.feedbackLayout)
    LinearLayout feedbackLayout;
    @BindView(R2.id.satisfactionFormLayout)
    LinearLayout satisfactionFormLayout;
    @BindView(R2.id.tvCSatEdit)
    TextView tvCSatEdit;

    @BindView(R2.id.ratingConstraintLayout)
    ConstraintLayout ratingConstraintLayout;

    @NonNull
    private ImageView[] ratingViewsList;
    @NonNull
    private KUSMessageListAdapter.ChatMessageItemListener mListener;

    //endregion

    //region Initializer
    @SuppressLint("ClickableViewAccessibility")
    public KUSCSatisfactionFormViewHolder(View itemView,
                                          @NonNull KUSMessageListAdapter.ChatMessageItemListener listener) {
        super(itemView);
        ButterKnife.bind(this, itemView);

        mListener = listener;
        ratingViewsList = new ImageView[]{ivRating1, ivRating2, ivRating3, ivRating4, ivRating5};

        etCSatComment.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (etCSatComment.hasFocus()) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_SCROLL) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    //endregion

    //region Public Methods
    public void onBind(@NonNull KUSCSatisfactionResponse satisfactionResponse,
                       @NonNull KUSUserSession userSession,
                       boolean isEditing) {
        setViewHolderAvatar(userSession);

        boolean shouldShowFeedbackView = satisfactionResponse.getSubmittedAt() != null
                && !isEditing;

        if (shouldShowFeedbackView) {
            satisfactionFormLayout.setVisibility(View.GONE);
            feedbackLayout.setVisibility(View.VISIBLE);

            boolean shouldShowEditButton = satisfactionResponse.getLockedAt() == null
                    || satisfactionResponse.getLockedAt().after(Calendar.getInstance().getTime());

            tvCSatEdit.setVisibility(shouldShowEditButton ? View.VISIBLE : View.GONE);

        } else {
            satisfactionFormLayout.setVisibility(View.VISIBLE);
            feedbackLayout.setVisibility(View.GONE);

            setSatisfactionFormLayout(satisfactionResponse, isEditing);

        }
    }

    //endregion

    //region Private Methods

    private void setSatisfactionFormLayout(@NonNull KUSCSatisfactionResponse satisfactionResponse,
                                           boolean isEditing) {
        KUSCSatisfactionForm satisfactionForm = satisfactionResponse.getSatisfactionForm();

        if (satisfactionForm == null)
            return;

        tvCSatRatingPrompt.setText(satisfactionForm.getRatingPrompt());
        tvHighRating.setText(satisfactionForm.getScaleLabelHigh());
        tvLowRating.setText(satisfactionForm.getScaleLabelLow());

        if (satisfactionForm.getScaleOptionsCount() > 4) {
            setRatingLabelsBiases(0.0f, 1.0f);
            tvLowRating.setGravity(Gravity.START);
            tvHighRating.setGravity(Gravity.END);
        } else {
            setRatingLabelsBiases(1.0f, 0.0f);
            tvHighRating.setGravity(Gravity.START);
            tvLowRating.setGravity(Gravity.END);
        }

        if (satisfactionResponse.getStatus() == KUS_C_SATISFACTION_RESPONSE_STATUS_RATED
                && !satisfactionForm.getQuestions().isEmpty()) {

            setRatingsView(satisfactionForm, satisfactionResponse.getRating(), true);
            tvCSatCommentQuestion.setText(satisfactionForm.getQuestions().get(0).getPrompt());

            tvCSatCommentQuestion.setVisibility(View.VISIBLE);
            etCSatComment.setVisibility(View.VISIBLE);
            btnCSatSubmit.setVisibility(View.VISIBLE);
        } else {
            setRatingsView(satisfactionForm, satisfactionResponse.getRating(), !isEditing);

            tvCSatCommentQuestion.setVisibility(View.GONE);
            etCSatComment.setVisibility(View.GONE);
            btnCSatSubmit.setVisibility(View.GONE);
        }
    }

    private void setRatingLabelsBiases(float lowRatingLabelBias, float highRatingLabelBias) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(ratingConstraintLayout);
        constraintSet.setHorizontalBias(tvLowRating.getId(), lowRatingLabelBias);
        constraintSet.setHorizontalBias(tvHighRating.getId(), highRatingLabelBias);
        constraintSet.applyTo(ratingConstraintLayout);
    }

    private void setRatingsView(@NonNull KUSCSatisfactionForm satisfactionForm, int rating,
                                boolean showRating) {
        int ratingCount = satisfactionForm.getScaleOptionsCount();

        ivRating3.setVisibility(ratingCount > 2 ? View.VISIBLE : View.GONE);
        ivRating2.setVisibility(ratingCount > 4 ? View.VISIBLE : View.GONE);
        ivRating4.setVisibility(ratingCount > 4 ? View.VISIBLE : View.GONE);

        String type = null;
        if (satisfactionForm.getScaleType() == KUS_C_SATISFACTION_SCALE_TYPE_EMOJI) {
            type = "emoji";

        } else if (satisfactionForm.getScaleType() == KUS_C_SATISFACTION_SCALE_TYPE_NUMBER) {
            type = "number";

        } else if (satisfactionForm.getScaleType() == KUS_C_SATISFACTION_SCALE_TYPE_THUMB) {
            type = "thumb";
        }

        if (type != null) {
            setAllRatingIcons(type);

            if (showRating && rating > 0 && rating <= 5)
                setRatedIcon(type, rating);
        }
    }

    private void setAllRatingIcons(String type) {
        for (int i = 0; i < ratingViewsList.length; i++) {
            ratingViewsList[i].setImageDrawable(KUSUtils.getDrawableForKey(itemView.getContext(),
                    "kus_grey_" + type + "_" + (i + 1)));
        }
    }

    private void setRatedIcon(String type, int rating) {
        ratingViewsList[rating - 1].setImageDrawable(KUSUtils.getDrawableForKey(itemView.getContext(),
                "kus_color_" + type + "_" + rating));
    }

    private void setViewHolderAvatar(@NonNull KUSUserSession userSession) {
        imageLayout.removeAllViews();
        KUSAvatarImageView avatarImageView = new KUSAvatarImageView(itemView.getContext());
        avatarImageView.setFontSize(16);
        avatarImageView.setDrawableSize(40);

        avatarImageView.initWithUserSession(userSession);

        FrameLayout.LayoutParams avatarLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        avatarImageView.setLayoutParams(avatarLayoutParams);

        imageLayout.addView(avatarImageView);
    }

    //endregion

    //region Callbacks

    @OnClick(R2.id.tvCSatEdit)
    void editClicked() {
        mListener.onSatisfactionFormEditPressed();
    }

    @OnClick(R2.id.btnCSatSubmit)
    void submitClicked() {
        String comment = etCSatComment.getText().toString();
        mListener.onSatisfactionFormCommented(comment);
        etCSatComment.setText("");
    }

    @OnClick(R2.id.ivRating1)
    void rating1Clicked() {
        mListener.onSatisfactionFormRated(1);
    }

    @OnClick(R2.id.ivRating2)
    void rating2Clicked() {
        mListener.onSatisfactionFormRated(2);
    }

    @OnClick(R2.id.ivRating3)
    void rating3Clicked() {
        mListener.onSatisfactionFormRated(3);
    }

    @OnClick(R2.id.ivRating4)
    void rating4Clicked() {
        mListener.onSatisfactionFormRated(4);
    }

    @OnClick(R2.id.ivRating5)
    void rating5Clicked() {
        mListener.onSatisfactionFormRated(5);
    }

    //endregion

}
