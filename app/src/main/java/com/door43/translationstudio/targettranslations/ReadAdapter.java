package com.door43.translationstudio.targettranslations;

import android.content.Context;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.AnimationUtilities;

/**
 * Created by joel on 9/9/2015.
 */
public class ReadAdapter extends RecyclerView.Adapter<ReadAdapter.ViewHolder> {

    private final String[] mDataset;
    private final Context mContext;

    public ReadAdapter(Context context, String[] dataset) {
        mContext = context;
        mDataset = dataset;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_read_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // TODO: set correct card on top and set click listener on the back card
        holder.mTargetCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: only animate if bottom card
//                holder.mTargetCard.getParent().bringChildToFront(holder.mTargetCard);
//                holder.mTargetCard.bringToFront();
//                ((View)holder.mTargetCard.getParent()).requestLayout();
//                ((View)holder.mTargetCard.getParent()).invalidate();
                animateCards(holder);
            }
        });
        holder.mSourceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: only animate if bottom card
            }
        });

        // TODO: populate the rest of the card info
        // TODO: set up tabs
        holder.mSourceBody.setText(mDataset[position]);
    }

    private void animateCards(final ViewHolder holder) {
        // TODO: apply animations to the correct cards
        long duration = 2000;
        final float scale = 0.5f;
        // animate bottom card up
        AnimationSet bottomSet = new AnimationSet(true);
        Animation bottomShrink = new ScaleAnimation(1, scale, 1, scale, Animation.RELATIVE_TO_SELF, 1, Animation.RELATIVE_TO_SELF, 1);
        bottomShrink.setDuration(duration);
        bottomSet.setFillEnabled(true);
        bottomSet.setFillAfter(true);
        bottomSet.addAnimation(bottomShrink);

//        Animation swap_up = AnimationUtils.loadAnimation(mContext, R.anim.swap_card_up);
        bottomSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                int originalWidth = holder.mTargetCard.getWidth();
//                int originalHeight = holder.mTargetCard.getHeight();
//                float originalX = holder.mTargetCard.getX();
//                float originalY = holder.mTargetCard.getY();
//                holder.mTargetCard.setScaleX(scale);
//                holder.mTargetCard.setScaleY(scale);
//                holder.mTargetCard.setX(originalX + originalWidth - (scale * originalWidth));
//                holder.mTargetCard.setY(originalY + originalHeight - (scale * originalHeight));
//                ((View) holder.mTargetCard.getParent()).requestLayout();
//                ((View) holder.mTargetCard.getParent()).invalidate();

                // elevation takes precedence for API 21+
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    holder.mSourceCard.setElevation(2);
//                    holder.mTargetCard.setElevation(3);
//                }
//
//                holder.mTargetCard.setX(holder.mTargetCard.getX() - 48);
//                holder.mTargetCard.setY(holder.mTargetCard.getY() - 48);
//                holder.mTargetCard.bringToFront();
//                ((View) holder.mTargetCard.getParent()).requestLayout();
//                ((View) holder.mTargetCard.getParent()).invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        holder.mTargetCard.startAnimation(bottomSet);

        // animate top card down
        AnimationSet topSet = new AnimationSet(true);
        Animation topShrink = new ScaleAnimation(1, 0.5f, 1, 0.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topSet.setFillEnabled(true);
        topSet.setFillAfter(true);
        topShrink.setDuration(duration);
        topSet.addAnimation(topShrink);
//        Animation swap_down = AnimationUtils.loadAnimation(mContext, R.anim.swap_card_down);
        topSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                holder.mSourceCard.setScaleX(scale);
//                holder.mSourceCard.setScaleY(scale);
//
//                ((View) holder.mSourceCard.getParent()).requestLayout();
//                ((View) holder.mSourceCard.getParent()).invalidate();

//                holder.mSourceCard.setX(0);
//                holder.mSourceCard.setY(0);
//                holder.mSourceCard.setX(holder.mSourceCard.getX() + 48);
//                holder.mSourceCard.setY(holder.mSourceCard.getY() + 48);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        holder.mSourceCard.startAnimation(topSet);
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
