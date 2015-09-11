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
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.ScreenUtil;

/**
 * Created by joel on 9/9/2015.
 */
public class ReadAdapter extends RecyclerView.Adapter<ReadAdapter.ViewHolder> {

//    private final String[] mDataset;
    private boolean[] mTargetStateOpen;
    private CharSequence[] mRenderedBody;
    private final Context mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;
    private final TargetTranslation mTargetTranslation;
    private SourceTranslation mSourceTranslation;
    private final Library mLibrary;
    private final Translator mTranslator;
    private Chapter[] mChapters;

    public ReadAdapter(Context context, String targetTranslationId, String sourceTranslationId) {
        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mChapters = mLibrary.getChapters(mSourceTranslation);
        mTargetStateOpen = new boolean[mChapters.length];
        mRenderedBody = new CharSequence[mChapters.length];
    }

    /**
     * Updates the source translation displayed
     * @param sourceTranslationId
     */
    public void setSourceTranslation(String sourceTranslationId) {
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mChapters = mLibrary.getChapters(mSourceTranslation);
        mTargetStateOpen = new boolean[mChapters.length];
        mRenderedBody = new CharSequence[mChapters.length];
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_read_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
//        int margin = ScreenUtil.dpToPx(mContext, mContext.getResources().getDimension(R.dimen.card_margin));
//        int stackedMargin = ScreenUtil.dpToPx(mContext, mContext.getResources().getDimension(R.dimen.stacked_card_margin));
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

//            CardView.LayoutParams layoutParamsBottom = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
//            layoutParamsBottom.setMargins(margin, margin, stackedMargin, stackedMargin);
//            holder.mTargetCard.setLayoutParams(layoutParamsBottom);
//
//            CardView.LayoutParams layoutParamsTop = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
//            layoutParamsTop.setMargins(stackedMargin, stackedMargin, margin, margin);
//            holder.mSourceCard.setLayoutParams(layoutParamsTop);
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
//
//            CardView.LayoutParams layoutParamsBottom = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
//            layoutParamsBottom.setMargins(margin, margin, stackedMargin, stackedMargin);
//            holder.mSourceCard.setLayoutParams(layoutParamsBottom);
//
//            CardView.LayoutParams layoutParamsTop = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
//            layoutParamsTop.setMargins(stackedMargin, stackedMargin, margin, margin);
//            holder.mTargetCard.setLayoutParams(layoutParamsTop);
        }

        holder.mTargetCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = true;
                    animateCards(holder.mSourceCard, holder.mTargetCard, false);
                }
            }
        });
        holder.mSourceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = false;
                    animateCards(holder.mSourceCard, holder.mTargetCard, true);
                }
            }
        });

        Chapter chapter = mChapters[position];
        Frame[] frames = mLibrary.getFrames(mSourceTranslation, chapter.getId());
        String chapterBody = "";
        for(Frame frame:frames) {
            chapterBody +=  " " + frame.body;
        }
        // TODO: set up rendering engine
        // TODO: store rendered body
        holder.mSourceBody.setText(chapterBody);
        String chapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            chapterTitle = mSourceTranslation.getProjectTitle() + " " + chapter.getId();
        }
        holder.mSourceTitle.setText(chapterTitle);

        // TODO: load target translation
    }

    private void animateCards(final CardView leftCard, final CardView rightCard, final boolean bringLeftCardToFront) {
        long duration = 700;
//        final int stackedMargin = ScreenUtil.dpToPx(mContext, mContext.getResources().getDimension(R.dimen.stacked_card_margin));
//        final int margin = ScreenUtil.dpToPx(mContext, mContext.getResources().getDimension(R.dimen.card_margin));
//        final float scale = 0.4f;

        // animate bottom card up
        Animation bottomOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        bottomOut.setDuration(duration);

        Animation bottomIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        bottomIn.setDuration(duration);

        final AnimationSet bottomFinishSet = new AnimationSet(true);
        bottomFinishSet.setStartOffset(duration);
        bottomFinishSet.addAnimation(bottomIn);

        AnimationSet bottomSet = new AnimationSet(true);
        bottomSet.addAnimation(bottomOut);
        bottomSet.addAnimation(bottomFinishSet);

        bottomOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // elevation takes precedence for API 21+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if(bringLeftCardToFront) {
                        leftCard.setElevation(TOP_ELEVATION);
                        rightCard.setElevation(BOTTOM_ELEVATION);
                    } else {
                        leftCard.setElevation(BOTTOM_ELEVATION);
                        rightCard.setElevation(TOP_ELEVATION);
                    }
                }
                if(bringLeftCardToFront) {
                    leftCard.bringToFront();
                } else {
                    rightCard.bringToFront();
                }
                ((View) rightCard.getParent()).requestLayout();
                ((View) rightCard.getParent()).invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rightCard.startAnimation(bottomSet);

        // animate top card down
        Animation topOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -.5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topOut.setDuration(duration);

        Animation topIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        topIn.setDuration(duration);
        AnimationSet topFinishSet = new AnimationSet(true);
        topFinishSet.setStartOffset(duration);
        topFinishSet.addAnimation(topIn);

        AnimationSet topSet = new AnimationSet(true);
        topSet.addAnimation(topOut);
        topSet.addAnimation(topFinishSet);
        leftCard.startAnimation(topSet);
    }

    @Override
    public int getItemCount() {
        return mChapters.length;
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
