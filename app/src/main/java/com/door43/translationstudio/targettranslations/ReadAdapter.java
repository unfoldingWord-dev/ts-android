package com.door43.translationstudio.targettranslations;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.util.AppContext;

/**
 * Created by joel on 9/9/2015.
 */
public class ReadAdapter extends RecyclerView.Adapter<ReadAdapter.ViewHolder> {

    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
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
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());

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
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);

        mChapters = mLibrary.getChapters(mSourceTranslation);
        mTargetStateOpen = new boolean[mChapters.length];
        mRenderedBody = new CharSequence[mChapters.length];

        notifyDataSetChanged();
        // TODO: make sure notifyDataSetChanged causes the ViewHolders to be regenerated.
        // otherwise we won't be passing the correct source language to the typeface methods
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_read_list_item, parent, false);
        ViewHolder vh = new ViewHolder(mContext, v, mSourceLanguage, mTargetLanguage);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
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

        // render the chapter body
        if(mRenderedBody[position] == null) {
            Frame[] frames = mLibrary.getFrames(mSourceTranslation, chapter.getId());
            String chapterBody = "";
            TranslationFormat bodyFormat = TranslationFormat.DEFAULT;
            if(frames.length > 0) {
                bodyFormat = frames[0].getFormat();
            }
            for (Frame frame : frames) {
                chapterBody += " " + frame.body;
            }
            // TODO: set up rendering engine
            RenderingGroup sourceRendering = new RenderingGroup();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
//        Boolean highlightTerms = prefs.getBoolean(SettingsActivity.KEY_PREF_HIGHLIGHT_KEY_TERMS, Boolean.parseBoolean(mContext.getResources().getString(R.string.pref_default_highlight_key_terms)));
            // TODO: identify key terms
            if (bodyFormat == TranslationFormat.USX) {
                // TODO: add click listeners
                sourceRendering.addEngine(new USXRenderer(null, null));
            } else {
                sourceRendering.addEngine(new DefaultRenderer());
            }
            sourceRendering.init(chapterBody);
            mRenderedBody[position] = sourceRendering.start();
        }

        holder.mSourceBody.setText(mRenderedBody[position]);
        String chapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            chapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        holder.mSourceTitle.setText(chapterTitle);

        // TODO: load target translation text
    }

    private void animateCards(final CardView leftCard, final CardView rightCard, final boolean bringLeftCardToFront) {
        long duration = 700;
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
        public ViewHolder(Context context, View v, SourceLanguage source, TargetLanguage target) {
            super(v);
            mSourceCard = (CardView)v.findViewById(R.id.source_translation_card);
            mSourceTitle = (TextView)v.findViewById(R.id.source_translation_title);
            mSourceBody = (TextView)v.findViewById(R.id.source_translation_body);
            mTargetCard = (CardView)v.findViewById(R.id.target_translation_card);
            mTargetTitle = (TextView)v.findViewById(R.id.target_translation_title);
            mTargetBody = (TextView)v.findViewById(R.id.target_translation_body);

            // set up fonts
            Typography.formatTitle(context, mSourceTitle, source.getId(), source.getDirection());
            Typography.format(context, mSourceBody, source.getId(), source.getDirection());
            Typography.formatTitle(context, mTargetTitle, target.getId(), target.getDirection());
            Typography.format(context, mTargetBody, target.getId(), target.getDirection());
        }
    }
}
