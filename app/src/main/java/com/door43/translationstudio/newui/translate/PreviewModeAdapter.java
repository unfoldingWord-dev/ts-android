package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.content.ContentValues;
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

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by blm on 1/10/2016.
 */
public class PreviewModeAdapter extends ViewModeAdapter<PreviewModeAdapter.ViewHolder> {

    private SourceLanguage mSourceLanguage;
    private CharSequence[] mRenderedSourceBody;
    private final Activity mContext;

    private SourceTranslation mSourceTranslation;
    private final Library mLibrary;
    private Chapter[] mChapters;
    private int mLayoutBuildNumber = 0;
    private ContentValues[] mTabs;

    public PreviewModeAdapter(Activity context, String sourceTranslationId, String chapterId, String frameId) {
        mLibrary = AppContext.getLibrary();
        mContext = context;
        mSourceTranslation = mLibrary.getDraftTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug);

        mChapters = mLibrary.getChapters(mSourceTranslation);
        mRenderedSourceBody = new CharSequence[mChapters.length];

        loadTabInfo();
    }

    /**
     * Updates the source translation displayed
     * @param sourceTranslationId
     */
    public void setSourceTranslation(String sourceTranslationId) {
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug);

        mChapters = mLibrary.getChapters(mSourceTranslation);
        mRenderedSourceBody = new CharSequence[mChapters.length];

        loadTabInfo();

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
    public int getItemPosition(String chapterId, String frameId) {
        for(int i = 0; i < mChapters.length; i ++) {
            Chapter chapter = mChapters[i];
            if(chapter.getId().equals(chapterId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void reload() {
        setSourceTranslation(mSourceTranslation.getId());
    }


    @Override
    public ViewHolder onCreateManagedViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_preview_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    /**
     * Rebuilds the card tabs
     */
    private void loadTabInfo() {
        List<ContentValues> tabContents = new ArrayList<>();

        if(mSourceTranslation != null) {
            ContentValues values = new ContentValues();
            // include the resource id if there are more than one
            if(mLibrary.getResources(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug).length > 1) {
                values.put("title", mSourceTranslation.getSourceLanguageTitle() + " " + mSourceTranslation.resourceSlug.toUpperCase());
            } else {
                values.put("title", mSourceTranslation.getSourceLanguageTitle());
            }
            values.put("tag", mSourceTranslation.getId());
            tabContents.add(values);
        }

        mTabs = tabContents.toArray(new ContentValues[tabContents.size()]);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int cardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.card_margin);
        int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);

        CardView.LayoutParams sourceParams = (CardView.LayoutParams)holder.mSourceCard.getLayoutParams();
        sourceParams.setMargins(cardMargin, cardMargin, stackedCardMargin, stackedCardMargin);
        holder.mSourceCard.setLayoutParams(sourceParams);
        ((View) holder.mSourceCard.getParent()).requestLayout();
        ((View) holder.mSourceCard.getParent()).invalidate();

        // re-enable new tab button
        holder.mNewTabButton.setEnabled(true);

        holder.mNewTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() != null) {
                    getListener().onNewSourceTranslationTabClick();
                }
            }
        });

        final Chapter chapter = mChapters[position];

        // render the source chapter body
        if(mRenderedSourceBody[position] == null) {
            String chapterBody = mLibrary.getChapterBody(mSourceTranslation, chapter.getId());
            TranslationFormat bodyFormat = mLibrary.getChapterBodyFormat(mSourceTranslation, chapter.getId());
            RenderingGroup sourceRendering = new RenderingGroup();
            if (bodyFormat == TranslationFormat.USX) {
                // TODO: add click listeners
                USXRenderer renderer = new USXRenderer(null, new Span.OnClickListener() {
                    @Override
                    public void onClick(View view, Span span, int start, int end) {
                        if(span instanceof NoteSpan) {
                            CustomAlertDialog.Create(mContext)
                                    .setTitle(R.string.title_note)
                                    .setMessage(((NoteSpan)span).getNotes())
                                    .setPositiveButton(R.string.dismiss, null)
                                    .show("note");
                        }
                    }

                    @Override
                    public void onLongClick(View view, Span span, int start, int end) {

                    }
                });
                sourceRendering.addEngine(renderer);

                // In read mode (and only in read mode), pull leading major section headings out for
                // display above chapter headings.
                renderer.setSuppressLeadingMajorSectionHeadings(true);
                CharSequence heading = renderer.getLeadingMajorSectionHeading(chapterBody);
                holder.mSourceHeading.setText(heading);
                holder.mSourceHeading.setVisibility(
                        heading.length() > 0 ? View.VISIBLE : View.GONE);
            } else {
                sourceRendering.addEngine(new DefaultRenderer());
            }
            sourceRendering.init(chapterBody);
            mRenderedSourceBody[position] = sourceRendering.start();
        }

        holder.mSourceBody.setText(mRenderedSourceBody[position]);
        ViewUtil.makeLinksClickable(holder.mSourceBody);
        String chapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            chapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        holder.mSourceTitle.setText(chapterTitle);

//        ChapterTranslation getChapterTranslation(String chapterSlug);

         // load tabs
        holder.mTabLayout.setOnTabSelectedListener(null);
        holder.mTabLayout.removeAllTabs();
        for(ContentValues values:mTabs) {
            TabLayout.Tab tab = holder.mTabLayout.newTab();
            tab.setText(values.getAsString("title"));
            tab.setTag(values.getAsString("tag"));
            holder.mTabLayout.addTab(tab);
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
                final String sourceTranslationId = (String) tab.getTag();
                if (getListener() != null) {
                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            getListener().onSourceTranslationTabClick(sourceTranslationId);
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

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.formatTitle(mContext, holder.mSourceHeading, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatTitle(mContext, holder.mSourceTitle, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
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
        private final CardView mSourceCard;
        private final ImageButton mNewTabButton;
        public TextView mSourceHeading;
        public TextView mSourceTitle;
        public TextView mSourceBody;
        public TabLayout mTabLayout;
        public int mLayoutBuildNumber = -1;

        public ViewHolder(View v) {
            super(v);
            mSourceCard = (CardView)v.findViewById(R.id.source_translation_card);
            mSourceHeading = (TextView)v.findViewById(R.id.source_translation_heading);
            mSourceTitle = (TextView)v.findViewById(R.id.source_translation_title);
            mSourceBody = (TextView)v.findViewById(R.id.source_translation_body);
            mTabLayout = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mTabLayout.setTabTextColors(R.color.dark_disabled_text, R.color.dark_secondary_text);
            mNewTabButton = (ImageButton) v.findViewById(R.id.new_tab_button);
        }
    }
}
