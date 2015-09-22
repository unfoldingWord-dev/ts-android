package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
import com.door43.translationstudio.core.FrameTranslation;
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


/**
 * Created by joel on 9/9/2015.
 */
public class ReadModeAdapter extends ViewModeAdapter<ReadModeAdapter.ViewHolder> {

    private final CharSequence[] mRenderedTargetBody;
    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
    private boolean[] mTargetStateOpen;
    private CharSequence[] mRenderedSourceBody;
    private final Activity mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;
    private final TargetTranslation mTargetTranslation;
    private SourceTranslation mSourceTranslation;
    private final Library mLibrary;
    private final Translator mTranslator;
    private Chapter[] mChapters;
    private int mLayoutBuildNumber = 0;

    public ReadModeAdapter(Activity context, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId) {
        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());

        mChapters = mLibrary.getChapters(mSourceTranslation);
        if(chapterId != null) {
            // identify starting selection
            for (int i = 0; i < mChapters.length; i ++) {
                Chapter c = mChapters[i];
                if (c.getId().equals(chapterId)) {
                    setListStartPosition(i);
                    break;
                }
            }
        }
        mTargetStateOpen = new boolean[mChapters.length];
        mRenderedSourceBody = new CharSequence[mChapters.length];
        mRenderedTargetBody = new CharSequence[mChapters.length];
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
        mRenderedSourceBody = new CharSequence[mChapters.length];

        notifyDataSetChanged();
    }

    @Override
    void onCoordinate(ViewHolder holder) {

    }

    @Override
    public String getFocusedFrameId(int position) {
        return null;
    }

    @Override
    public String getFocusedChapterId(int position) {
        if(position >= 0 && position < mChapters.length) {
            return mChapters[position].getId();
        } else {
            return null;
        }
    }


    @Override
    public ViewHolder onCreateManagedViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_read_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int cardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.card_margin);
        int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);
        if(mTargetStateOpen[position]) {
            // target on top
            // elevation takes precedence for API 21+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.mSourceCard.setElevation(BOTTOM_ELEVATION);
                holder.mTargetCard.setElevation(TOP_ELEVATION);
            }
            holder.mTargetCard.bringToFront();
            CardView.LayoutParams targetParams = (CardView.LayoutParams)holder.mTargetCard.getLayoutParams();
            targetParams.setMargins(cardMargin, cardMargin, stackedCardMargin, stackedCardMargin);
            holder.mTargetCard.setLayoutParams(targetParams);
            CardView.LayoutParams sourceParams = (CardView.LayoutParams)holder.mSourceCard.getLayoutParams();
            sourceParams.setMargins(stackedCardMargin, stackedCardMargin, cardMargin, cardMargin);
            holder.mSourceCard.setLayoutParams(sourceParams);
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
            CardView.LayoutParams sourceParams = (CardView.LayoutParams)holder.mSourceCard.getLayoutParams();
            sourceParams.setMargins(cardMargin, cardMargin, stackedCardMargin, stackedCardMargin);
            holder.mSourceCard.setLayoutParams(sourceParams);
            CardView.LayoutParams targetParams = (CardView.LayoutParams)holder.mTargetCard.getLayoutParams();
            targetParams.setMargins(stackedCardMargin, stackedCardMargin, cardMargin, cardMargin);
            holder.mTargetCard.setLayoutParams(targetParams);
            ((View) holder.mSourceCard.getParent()).requestLayout();
            ((View) holder.mSourceCard.getParent()).invalidate();
        }

        holder.mTargetCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = true;
                    ViewUtil.animateSwapCards(holder.mSourceCard, holder.mTargetCard, TOP_ELEVATION, BOTTOM_ELEVATION, false);
                }
            }
        });
        holder.mSourceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = false;
                    ViewUtil.animateSwapCards(holder.mSourceCard, holder.mTargetCard, TOP_ELEVATION, BOTTOM_ELEVATION, true);
                }
            }
        });

        Chapter chapter = mChapters[position];

        // render the source chapter body
        if(mRenderedSourceBody[position] == null) {
            Frame[] frames = mLibrary.getFrames(mSourceTranslation, chapter.getId());
            String chapterBody = "";
            TranslationFormat bodyFormat = TranslationFormat.DEFAULT;
            if(frames.length > 0) {
                bodyFormat = frames[0].getFormat();
            }
            for (Frame frame : frames) {
                chapterBody += " " + frame.body;
            }
            RenderingGroup sourceRendering = new RenderingGroup();
            // TODO: identify key terms
            if (bodyFormat == TranslationFormat.USX) {
                // TODO: add click listeners
                sourceRendering.addEngine(new USXRenderer(null, null));
            } else {
                sourceRendering.addEngine(new DefaultRenderer());
            }
            sourceRendering.init(chapterBody);
            mRenderedSourceBody[position] = sourceRendering.start();
        }

        holder.mSourceBody.setText(mRenderedSourceBody[position]);
        String chapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            chapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        holder.mSourceTitle.setText(chapterTitle);

        // render the target chapter body
        if(mRenderedTargetBody[position] == null) {
            Frame[] frames = mLibrary.getFrames(mSourceTranslation, chapter.getId());
            String chapterBody = "";
            TranslationFormat bodyFormat = TranslationFormat.DEFAULT;
            if(frames.length > 0) {
                bodyFormat = frames[0].getFormat();
            }
            for (Frame frame : frames) {
                FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
                chapterBody += " " + frameTranslation.body;
            }
            RenderingGroup targetRendering = new RenderingGroup();
            // TODO: identify key terms
            if (bodyFormat == TranslationFormat.USX) {
                // TODO: add click listeners
                targetRendering.addEngine(new USXRenderer(null, null));
            } else {
                targetRendering.addEngine(new DefaultRenderer());
            }
            targetRendering.init(chapterBody);
            mRenderedTargetBody[position] = targetRendering.start();
        }

        // TODO: get the translations of the title
        holder.mTargetBody.setText(mRenderedTargetBody[position]);
        String targetChapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            targetChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
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
                if(getListener() != null) {
                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            getListener().onTabClick(sourceTranslationId);
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
                if(getListener() != null) {
                    getListener().onNewTabClick();
                }
            }
        });

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.formatTitle(mContext, holder.mSourceTitle, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatTitle(mContext, holder.mTargetTitle, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
        }
    }

    @Override
    public int getItemCount() {
        return mChapters.length;
    }

    public void rebuild() {
        mLayoutBuildNumber ++;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTargetTitle;
        private final TextView mTargetBody;
        private final CardView mTargetCard;
        private final CardView mSourceCard;
        private final ImageButton mNewTabButton;
        public TextView mSourceTitle;
        public TextView mSourceBody;
        public TabLayout mTabLayout;
        public int mLayoutBuildNumber = -1;

        public ViewHolder(View v) {
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
        }
    }
}
