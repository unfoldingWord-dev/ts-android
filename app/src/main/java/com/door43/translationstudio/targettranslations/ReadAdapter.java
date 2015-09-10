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
import android.view.animation.AnimationUtils;
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

        // animate bottom card up
        Animation swap_up = AnimationUtils.loadAnimation(mContext, R.anim.swap_card_up);
        swap_up.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // elevation takes precedence for API 21+
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.mSourceCard.setElevation(2);
                    holder.mTargetCard.setElevation(3);
                }

                holder.mTargetCard.setX(holder.mTargetCard.getX() - 48);
                holder.mTargetCard.setY(holder.mTargetCard.getY() - 48);
                holder.mTargetCard.bringToFront();
                ((View)holder.mTargetCard.getParent()).requestLayout();
                ((View)holder.mTargetCard.getParent()).invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        holder.mTargetCard.startAnimation(swap_up);

        // animate top card down
        Animation swap_down = AnimationUtils.loadAnimation(mContext, R.anim.swap_card_down);
        swap_down.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                holder.mSourceCard.setX(holder.mSourceCard.getX() + 48);
                holder.mSourceCard.setY(holder.mSourceCard.getY() + 48);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        holder.mSourceCard.startAnimation(swap_down);
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
