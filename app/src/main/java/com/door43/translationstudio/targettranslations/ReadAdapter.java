package com.door43.translationstudio.targettranslations;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.widget.ScreenUtil;

/**
 * Created by joel on 9/9/2015.
 */
public class ReadAdapter extends RecyclerView.Adapter<ReadAdapter.ViewHolder> {

    private final String[] mDataset;
    private final boolean[] mTargetStateOpen;
    private final Context mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;

    public ReadAdapter(Context context, String[] dataset) {
        mContext = context;
        mDataset = dataset;
        mTargetStateOpen = new boolean[mDataset.length];
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_read_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int margin = ScreenUtil.dpToPx(mContext, mContext.getResources().getDimension(R.dimen.card_margin));
        int stackedMargin = ScreenUtil.dpToPx(mContext, mContext.getResources().getDimension(R.dimen.stacked_card_margin));
        if(mTargetStateOpen[position]) {
            // target on top
            // elevation takes precedence for API 21+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.mSourceCard.setElevation(BOTTOM_ELEVATION);
                holder.mTargetCard.setElevation(TOP_ELEVATION);
            }
            holder.mTargetCard.bringToFront();
            ((View) holder.mTargetCard.getParent()).requestLayout();
            ((View) holder.mTargetCard.getParent()).invalidate();

            CardView.LayoutParams layoutParamsBottom = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
            layoutParamsBottom.setMargins(margin, margin, stackedMargin, stackedMargin);
            holder.mTargetCard.setLayoutParams(layoutParamsBottom);

            CardView.LayoutParams layoutParamsTop = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
            layoutParamsTop.setMargins(stackedMargin, stackedMargin, margin, margin);
            holder.mSourceCard.setLayoutParams(layoutParamsTop);
        } else {
            // source on top
            // elevation takes precedence for API 21+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.mTargetCard.setElevation(BOTTOM_ELEVATION);
                holder.mSourceCard.setElevation(TOP_ELEVATION);
            }
            holder.mSourceCard.bringToFront();
            ((View) holder.mSourceCard.getParent()).requestLayout();
            ((View) holder.mSourceCard.getParent()).invalidate();

            CardView.LayoutParams layoutParamsBottom = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
            layoutParamsBottom.setMargins(margin, margin, stackedMargin, stackedMargin);
            holder.mSourceCard.setLayoutParams(layoutParamsBottom);

            CardView.LayoutParams layoutParamsTop = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
            layoutParamsTop.setMargins(stackedMargin, stackedMargin, margin, margin);
            holder.mTargetCard.setLayoutParams(layoutParamsTop);
        }

        holder.mTargetCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = true;
                    animateCards(holder.mSourceCard, holder.mTargetCard);
                }
            }
        });
        holder.mSourceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = false;
                    animateCards(holder.mTargetCard, holder.mSourceCard);
                }
            }
        });

        // TODO: populate the rest of the card info
        // TODO: set up tabs
        holder.mSourceBody.setText(mDataset[position]);
    }

    private void animateCards(final CardView topView, final CardView bottomView) {
        long duration = 400;
        final int stackedMargin = ScreenUtil.dpToPx(mContext, mContext.getResources().getDimension(R.dimen.stacked_card_margin));
        final int margin = ScreenUtil.dpToPx(mContext, mContext.getResources().getDimension(R.dimen.card_margin));
        final float scale = 0.4f;

        // animate bottom card up
        Animation bottomShrink = new ScaleAnimation(1, scale, 1, scale, Animation.RELATIVE_TO_SELF, .9f, Animation.RELATIVE_TO_SELF, .9f);
        bottomShrink.setDuration(duration);

        Animation bottomGrow = new ScaleAnimation(1, 1 / scale, 1, 1 / scale, Animation.RELATIVE_TO_SELF, .9f, Animation.RELATIVE_TO_SELF, .9f);
        bottomGrow.setDuration(duration);
        Animation bottomTranslate = new TranslateAnimation(0, margin-stackedMargin, 0, margin-stackedMargin);
        bottomTranslate.setDuration(duration);

        final AnimationSet bottomFinishSet = new AnimationSet(true);
        bottomFinishSet.setStartOffset(duration);
        bottomFinishSet.addAnimation(bottomGrow);
        bottomFinishSet.addAnimation(bottomTranslate);

        AnimationSet bottomSet = new AnimationSet(true);
        bottomSet.addAnimation(bottomShrink);
        bottomSet.addAnimation(bottomFinishSet);

        bottomShrink.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // elevation takes precedence for API 21+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    topView.setElevation(BOTTOM_ELEVATION);
                    bottomView.setElevation(TOP_ELEVATION);
                }
                bottomView.bringToFront();
                ((View) bottomView.getParent()).requestLayout();
                ((View) bottomView.getParent()).invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        bottomSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                bottomView.clearAnimation();
                CardView.LayoutParams layoutParamsBottom = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
                layoutParamsBottom.setMargins(margin, margin, stackedMargin, stackedMargin);
                bottomView.setLayoutParams(layoutParamsBottom);

                topView.clearAnimation();
                CardView.LayoutParams layoutParamsTop = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
                layoutParamsTop.setMargins(stackedMargin, stackedMargin, margin, margin);
                topView.setLayoutParams(layoutParamsTop);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        bottomView.startAnimation(bottomSet);

        // animate top card down
        Animation topShrink = new ScaleAnimation(1, scale, 1, scale, Animation.RELATIVE_TO_SELF, .1f, Animation.RELATIVE_TO_SELF, .1f);
        topShrink.setDuration(duration);

        Animation topGrow = new ScaleAnimation(1, 1 / scale, 1, 1 / scale, Animation.RELATIVE_TO_SELF, .1f, Animation.RELATIVE_TO_SELF, .1f);
        topGrow.setDuration(duration);
        Animation topTranslate = new TranslateAnimation(0, stackedMargin-margin, 0, stackedMargin-margin);
        topTranslate.setDuration(duration);

        AnimationSet topFinishSet = new AnimationSet(true);
        topFinishSet.setStartOffset(duration);
        topFinishSet.addAnimation(topGrow);
        topFinishSet.addAnimation(topTranslate);

        AnimationSet topSet = new AnimationSet(true);
        topSet.addAnimation(topShrink);
        topSet.addAnimation(topFinishSet);
        topView.startAnimation(topSet);
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTargetTitle;
        private final TextView mTargetBody;
        private final CardView mTargetCard;
        private final CardView mSourceCard;
        public TextView mSourceTitle;
        public TextView mSourceBody;
        public ViewHolder(View v) {
            super(v);
            mSourceCard = (CardView)v.findViewById(R.id.source_translation_card);
            mSourceTitle = (TextView)v.findViewById(R.id.source_translation_title);
            mSourceBody = (TextView)v.findViewById(R.id.source_translation_body);
            mTargetCard = (CardView)v.findViewById(R.id.target_translation_card);
            mTargetTitle = (TextView)v.findViewById(R.id.target_translation_title);
            mTargetBody = (TextView)v.findViewById(R.id.target_translation_body);
        }
    }
}
