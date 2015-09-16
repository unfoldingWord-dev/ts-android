package com.door43.translationstudio.targettranslations;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by joel on 9/9/2015.
 */
public class ChunkAdapter extends RecyclerView.Adapter<ChunkAdapter.ViewHolder> {

    private CharSequence[] mRenderedTargetBody;
    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
    private boolean[] mTargetStateOpen;
    private CharSequence[] mRenderedSourceBody;
    private final Context mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;
    private final TargetTranslation mTargetTranslation;
    private SourceTranslation mSourceTranslation;
    private final Library mLibrary;
    private final Translator mTranslator;
    private Frame[] mFrames;
    private OnClickListener mListener;

    public ChunkAdapter(Context context, String targetTranslationId, String sourceTranslationId) {
        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<Frame> frames = new ArrayList<>();
        for(Chapter c:chapters) {
            Frame[] chapterFrames = mLibrary.getFrames(mSourceTranslation, c.getId());
            frames.addAll(Arrays.asList(chapterFrames));
        }
        mFrames = frames.toArray(new Frame[frames.size()]);
        mTargetStateOpen = new boolean[mFrames.length];
        mRenderedSourceBody = new CharSequence[mFrames.length];
        mRenderedTargetBody = new CharSequence[mFrames.length];
    }

    /**
     * Updates the source translation displayed
     * @param sourceTranslationId
     */
    public void setSourceTranslation(String sourceTranslationId) {
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<Frame> frames = new ArrayList<>();
        for(Chapter c:chapters) {
            Frame[] chapterFrames = mLibrary.getFrames(mSourceTranslation, c.getId());
            frames.addAll(Arrays.asList(chapterFrames));
        }
        mFrames = frames.toArray(new Frame[frames.size()]);
        mTargetStateOpen = new boolean[mFrames.length];
        mRenderedSourceBody = new CharSequence[mFrames.length];
        mRenderedTargetBody = new CharSequence[mFrames.length];

        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chunk_list_item, parent, false);
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
                    ViewUtil.animateCards(holder.mSourceCard, holder.mTargetCard, TOP_ELEVATION, BOTTOM_ELEVATION, false);
                }
            }
        });
        holder.mSourceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = false;
                    ViewUtil.animateCards(holder.mSourceCard, holder.mTargetCard, TOP_ELEVATION, BOTTOM_ELEVATION, true);
                }
            }
        });

        Frame frame = mFrames[position];

        // render the source frame body
        if(mRenderedSourceBody[position] == null) {
            RenderingGroup sourceRendering = new RenderingGroup();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
//        Boolean highlightTerms = prefs.getBoolean(SettingsActivity.KEY_PREF_HIGHLIGHT_KEY_TERMS, Boolean.parseBoolean(mContext.getResources().getString(R.string.pref_default_highlight_key_terms)));
            // TODO: identify key terms
            if (frame.getFormat() == TranslationFormat.USX) {
                // TODO: add click listeners
                sourceRendering.addEngine(new USXRenderer(null, null));
            } else {
                sourceRendering.addEngine(new DefaultRenderer());
            }
            sourceRendering.init(frame.body);
            mRenderedSourceBody[position] = sourceRendering.start();
        }

        holder.mSourceBody.setText(mRenderedSourceBody[position]);

        Chapter chapter = mLibrary.getChapter(mSourceTranslation, frame.getChapterId());
        String sourceChapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        sourceChapterTitle += ":" + frame.getTitle();
        holder.mSourceTitle.setText(sourceChapterTitle);

        // render the target frame body
        if(mRenderedTargetBody[position] == null) {
            mRenderedTargetBody[position] = "";
            // TODO: load the frames from the target translation
        }

        // TODO: get the translations of the title
        holder.mTargetBody.setText(mRenderedTargetBody[position]);
        String targetChapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            targetChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        targetChapterTitle += ":" + frame.getTitle();
        holder.mTargetTitle.setText(targetChapterTitle + " - " + mTargetLanguage.name);

        // load tabs
        holder.mTabLayout.setOnTabSelectedListener(null);
        holder.mTabLayout.removeAllTabs();
        String[] sourceTranslationIds = mTranslator.getSourceTranslations(mTargetTranslation.getId());
        for(String id:sourceTranslationIds) {
            SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(id);
            if(sourceTranslation != null) {
                TabLayout.Tab tab = holder.mTabLayout.newTab();
                // include the resource id if there are more than one
                if(mLibrary.getResources(sourceTranslation.projectId, sourceTranslation.sourceLanguageId).length > 1) {
                    tab.setText(sourceTranslation.getSourceLanguageTitle() + " " + sourceTranslation.resourceId.toUpperCase());
                } else {
                    tab.setText(sourceTranslation.getSourceLanguageTitle());
                }
                tab.setTag(sourceTranslation.getId());
                holder.mTabLayout.addTab(tab);
            }
        }

        // select correct tab
        for(int i = 0; i < holder.mTabLayout.getTabCount(); i ++) {
            TabLayout.Tab tab = holder.mTabLayout.getTabAt(i);
            if(tab.getTag().equals(mSourceTranslation.getId())) {
                tab.select();
                break;
            }
        }

        // hook up listener
        holder.mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final String sourceTranslationId = (String)tab.getTag();
                if(mListener != null) {
                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onTabClick(sourceTranslationId);
                        }
                    });
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        holder.mNewTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onNewTabClick();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFrames.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTargetTitle;
        private final TextView mTargetBody;
        private final CardView mTargetCard;
        private final CardView mSourceCard;
        private final TabLayout mTabLayout;
        private final ImageButton mNewTabButton;
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
            mTabLayout = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mTabLayout.setTabTextColors(R.color.dark_disabled_text, R.color.dark_secondary_text);
            mNewTabButton = (ImageButton) v.findViewById(R.id.new_tab_button);

            // set up fonts
            Typography.formatSub(context, mSourceTitle, source.getId(), source.getDirection());
            Typography.format(context, mSourceBody, source.getId(), source.getDirection());
            Typography.formatSub(context, mTargetTitle, target.getId(), target.getDirection());
            Typography.format(context, mTargetBody, target.getId(), target.getDirection());
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    public interface OnClickListener {
        void onTabClick(String sourceTranslationId);
        void onNewTabClick();
    }
}
