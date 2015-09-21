package com.door43.translationstudio.newui.publish;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.ViewUtil;

/**
 * Created by joel on 9/20/2015.
 */
public class ValidationAdapter extends RecyclerView.Adapter<ValidationAdapter.ViewHolder> {

    private final Library mLibrary;
    private final Translator mTranslator;
    private final Activity mContext;
//    private final TargetTranslation mTargetTranslation;
//    private final SourceTranslation mSourceTranslation;
//    private final SourceLanguage mSourceLanguage;
//    private final TargetLanguage mTargetLanguage;
    private ValidationItem[] mValidations;

    public ValidationAdapter(Activity context) {
        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
//        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
//        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
//        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);
//        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());

        mValidations = new ValidationItem[0];
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_publish_validation_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ValidationItem item = mValidations[position];

        // margin
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.mContainer.getLayoutParams();
        int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);
        if(item.isFrame()) {
            p.setMargins(stackedCardMargin, 0, 0, 0);
        } else {
            p.setMargins(0, 0, 0, 0);
        }
        holder.mContainer.requestLayout();

        // title
        holder.mTitle.setText(item.getTitle());

        // icon
        if(item.isValid()) {
            holder.mIcon.setBackgroundResource(R.drawable.ic_done_black_24dp);
            ViewUtil.tintViewDrawable(holder.mIcon, mContext.getResources().getColor(R.color.green));
        } else {
            holder.mIcon.setBackgroundResource(R.drawable.ic_report_black_24dp);
            ViewUtil.tintViewDrawable(holder.mIcon, mContext.getResources().getColor(R.color.warning));
        }

        // stack
        if(item.isRange()) {
            holder.mStackedCard.setVisibility(View.VISIBLE);
        } else {
            holder.mStackedCard.setVisibility(View.GONE);
        }

        // body
        if(item.isFrame() && !item.isValid()) {
            holder.mIcon.setVisibility(View.GONE);
            holder.mReviewButton.setVisibility(View.VISIBLE);
            holder.mBody.setVisibility(View.VISIBLE);
            holder.mBody.setText(item.getBody());
        } else {
            holder.mBody.setVisibility(View.GONE);
            holder.mReviewButton.setVisibility(View.GONE);
            holder.mIcon.setVisibility(View.VISIBLE);
        }

        holder.mReviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: handle review click
                Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), "Take me to back to review!", Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValidations.length;
    }

    public void setValidations(ValidationItem[] validations) {
        mValidations = validations;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView mStackedCard;
        private final CardView mCard;
        private final TextView mTitle;
        private final Button mReviewButton;
        private final ImageView mIcon;
        private final TextView mBody;
        private final FrameLayout mContainer;

        public ViewHolder(View v) {
            super(v);

            mStackedCard = (CardView)v.findViewById(R.id.stacked_card);
            mCard = (CardView)v.findViewById(R.id.card);
            mTitle = (TextView)v.findViewById(R.id.title);
            mReviewButton = (Button)v.findViewById(R.id.review_button);
            mIcon = (ImageView)v.findViewById(R.id.icon);
            mBody = (TextView)v.findViewById(R.id.body);
            mContainer = (FrameLayout)v.findViewById(R.id.card_container);
        }
    }
}
