package com.door43.translationstudio.newui.publish;

import android.app.Activity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.widget.ViewUtil;

/**
 * Created by joel on 9/20/2015.
 */
public class ValidationAdapter extends RecyclerView.Adapter<ValidationAdapter.ViewHolder> {
    private final Activity mContext;
    private ValidationItem[] mValidations;
    private OnClickListener mListener;

    public ValidationAdapter(Activity context) {
        mContext = context;
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
        if(position == getItemCount() - 1) {
            holder.mNextLayout.setVisibility(View.VISIBLE);
            holder.mContainer.setVisibility(View.GONE);
            // next button

            holder.mNextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener != null) {
                        mListener.onClickNext();
                    }
                }
            });
        } else {
            holder.mNextLayout.setVisibility(View.GONE);
            holder.mContainer.setVisibility(View.VISIBLE);

            // validation item
            final ValidationItem item = mValidations[position];

            // margin
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.mContainer.getLayoutParams();
            int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);
            if (item.isFrame()) {
                p.setMargins(stackedCardMargin, 0, 0, 0);
            } else {
                p.setMargins(0, 0, 0, 0);
            }
            holder.mContainer.requestLayout();

            // title
            holder.mTitle.setText(item.getTitle());

            // icon
            if (item.isValid()) {
                if(item.isRange()) {
                    holder.mIcon.setBackgroundResource(R.drawable.ic_done_all_black_24dp);
                } else {
                    holder.mIcon.setBackgroundResource(R.drawable.ic_done_black_24dp);
                }
                ViewUtil.tintViewDrawable(holder.mIcon, mContext.getResources().getColor(R.color.green));
            } else {
                holder.mIcon.setBackgroundResource(R.drawable.ic_report_black_24dp);
                ViewUtil.tintViewDrawable(holder.mIcon, mContext.getResources().getColor(R.color.warning));
            }

            // stack
            if (item.isRange()) {
                holder.mStackedCard.setVisibility(View.VISIBLE);
            } else {
                holder.mStackedCard.setVisibility(View.GONE);
            }

            // body
            if (item.isFrame() && !item.isValid()) {
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
                    if (mListener != null) {
                        mListener.onClickReview(item.getTargetTranslationId(), item.getChapterId(), item.getFrameId());
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if(mValidations != null && mValidations.length > 0) {
            // leave room for the next button
            return mValidations.length + 1;
        } else {
            return 0;
        }
    }

    public void setValidations(ValidationItem[] validations) {
        mValidations = validations;
        notifyDataSetChanged();
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView mStackedCard;
        private final CardView mCard;
        private final TextView mTitle;
        private final Button mReviewButton;
        private final ImageView mIcon;
        private final TextView mBody;
        private final FrameLayout mContainer;
        private final LinearLayout mNextLayout;
        private final Button mNextButton;

        public ViewHolder(View v) {
            super(v);

            mStackedCard = (CardView)v.findViewById(R.id.stacked_card);
            mCard = (CardView)v.findViewById(R.id.card);
            mTitle = (TextView)v.findViewById(R.id.title);
            mReviewButton = (Button)v.findViewById(R.id.review_button);
            mIcon = (ImageView)v.findViewById(R.id.icon);
            mBody = (TextView)v.findViewById(R.id.body);
            mContainer = (FrameLayout)v.findViewById(R.id.card_container);
            mNextLayout = (LinearLayout)v.findViewById(R.id.next_layout);
            mNextButton = (Button)v.findViewById(R.id.next_button);
        }
    }

    public interface OnClickListener {
        void onClickReview(String targetTranslationId, String chapterId, String frameId);
        void onClickNext();
    }
}
