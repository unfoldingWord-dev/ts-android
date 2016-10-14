package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.LinedEditText;
import com.door43.translationstudio.core.ProjectTranslation;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.ClickableRenderingEngine;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.MergeConflictHandler;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.widget.ViewUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joel on 9/9/2015.
 */
public class ChunkModeAdapter extends ViewModeAdapter<ChunkModeAdapter.ViewHolder> {
    public static final int HIGHLIGHT_COLOR = Color.YELLOW;
    private SourceLanguage mSourceLanguage;
    private TargetLanguage mTargetLanguage;
    private final Activity mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;
    private final TargetTranslation mTargetTranslation;
    private SourceTranslation mSourceTranslation;
    private final Library mLibrary;
    private final Translator mTranslator;
    private ListItem[] mUnfilteredItems;
    private ListItem[] mFilteredItems;
    private Map<String, Chapter> mChapters = new HashMap<>();
    private int mLayoutBuildNumber = 0;
    private ContentValues[] mTabs;
    private TranslationFormat mTargetFormat;
    private SearchFilter mSearchFilter;
    private CharSequence mSearchString;

    public ChunkModeAdapter(Activity context, String targetTranslationId, String sourceTranslationId, String startingChapterSlug, String startingFrameSlug, boolean openSelectedTarget) {
        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation);

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<ListItem> listItems = new ArrayList<>();

        // add project title card
        ListItem projectTitleItem = new ListItem(null, null);
        projectTitleItem.isProjectTitle = true;
        listItems.add(projectTitleItem);

        for(Chapter c:chapters) {
            // add title and reference cards for chapter
            if(!c.title.isEmpty()) {
                ListItem item = new ListItem(null, c.getId());
                item.isChapterTitle = true;
                listItems.add(item);
            }
            if(!c.reference.isEmpty()) {
                ListItem item = new ListItem(null, c.getId());
                item.isChapterReference = true;
                listItems.add(item);
            }
            // put in map for easier retrieval
            mChapters.put(c.getId(), c);

            String[] chapterFrameSlugs = mLibrary.getFrameSlugs(mSourceTranslation, c.getId());
            boolean setStartPosition = startingChapterSlug != null && c.getId().equals(startingChapterSlug) && chapterFrameSlugs.length > 0;
            // identify starting selection
            if(setStartPosition) {
                setListStartPosition(listItems.size());
            }
            for(String frameSlug:chapterFrameSlugs) {
                if(setStartPosition && startingFrameSlug != null && frameSlug.equals(startingFrameSlug)) {
                    setListStartPosition(listItems.size());
                }
                listItems.add(new ListItem(frameSlug, c.getId()));
            }
        }
        mUnfilteredItems = listItems.toArray(new ListItem[listItems.size()]);
        mUnfilteredItems[getListStartPosition()].isTargetCardOpen = openSelectedTarget;
        mFilteredItems = mUnfilteredItems;

        loadTabInfo();
    }

    /**
     * Rebuilds the card tabs
     */
    private void loadTabInfo() {
        List<ContentValues> tabContents = new ArrayList<>();
        String[] sourceTranslationIds = App.getOpenSourceTranslationIds(mTargetTranslation.getId());
        for(String id:sourceTranslationIds) {
            SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(id);
            if(sourceTranslation != null) {
                ContentValues values = new ContentValues();
                // include the resource id if there are more than one
                if(mLibrary.getResources(sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug).length > 1) {
                    values.put("title", sourceTranslation.getSourceLanguageTitle() + " " + sourceTranslation.resourceSlug.toUpperCase());
                } else {
                    values.put("title", sourceTranslation.getSourceLanguageTitle());
                }
                values.put("tag", sourceTranslation.getId());
                tabContents.add(values);
            }
        }
        mTabs = tabContents.toArray(new ContentValues[tabContents.size()]);
    }

    /**
     * Updates the source translation displayed
     * @param sourceTranslationId
     */
    public void setSourceTranslation(String sourceTranslationId) {
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug);
        mChapters = new HashMap<>();

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<ListItem> listItems = new ArrayList<>();

        // add project title card
        ListItem projectTitleItem = new ListItem(null, null);
        projectTitleItem.isProjectTitle = true;
        listItems.add(projectTitleItem);

        for(Chapter c:chapters) {
            // add title and reference cards for chapter
            if(!c.title.isEmpty()) {
                ListItem item = new ListItem(null, c.getId());
                item.isChapterTitle = true;
                listItems.add(item);
            }
            if(!c.reference.isEmpty()) {
                ListItem item = new ListItem(null, c.getId());
                item.isChapterReference = true;
                listItems.add(item);
            }
            // put in map for easier retrieval
            mChapters.put(c.getId(), c);

            String[] chapterFrameSlugs = mLibrary.getFrameSlugs(mSourceTranslation, c.getId());
            for(String frameSlug:chapterFrameSlugs) {
                listItems.add(new ListItem(frameSlug, c.getId()));
            }
        }
        mUnfilteredItems = listItems.toArray(new ListItem[listItems.size()]);
        mFilteredItems = mUnfilteredItems;

        loadTabInfo();

        notifyDataSetChanged();

        clearScreenAndStartNewSearch(mSearchString, isTargetSearch());
    }

    @Override
    void onCoordinate(ViewHolder holder) {

    }

    @Override
    public String getFocusedFrameId(int position) {
        if(position >= 0 && position < mFilteredItems.length) {
            return mFilteredItems[position].frameSlug;
        }
        return null;
    }

    @Override
    public String getFocusedChapterId(int position) {
        if(position >= 0 && position < mFilteredItems.length) {
            return mFilteredItems[position].chapterSlug;
        }
        return null;
    }

    @Override
    public int getItemPosition(String chapterSlug, String frameSlug) {
        for(int i = 0; i < mFilteredItems.length; i ++) {
            ListItem item = mFilteredItems[i];
            if(item.isFrame() && item.chapterSlug.equals(chapterSlug) && item.frameSlug.equals(frameSlug)) {
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chunk_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    /**
     * get the chapter for the position, or null if not found
     * @param position
     * @return
     */
    public String getChapterForPosition(int position) {
        if( (position < 0) || (position >= mFilteredItems.length)) {
            return null;
        }

        ListItem item = mFilteredItems[position];
        if(item != null) {
            return item.chapterSlug;
        }

        return null;
    }

     @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int cardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.card_margin);
        int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);
        final ListItem item = mFilteredItems[position];
        if(item.isTargetCardOpen) {
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

            // disable new tab button so we don't accidently open it
            holder.mNewTabButton.setEnabled(false);
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

            // re-enable new tab button
            holder.mNewTabButton.setEnabled(true);
        }

        holder.mTargetCard.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) { // for touches on card other than edit area
                if(MotionEvent.ACTION_UP == event.getAction()) {

                    return checkForPromptToEditDoneTargetCard( holder, mFilteredItems[position]);
                }
                return false;
            }
        });

        holder.mTargetBody.setOnTouchListener(new View.OnTouchListener() { //for touches on edit area
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_UP == event.getAction()) {

                    return checkForPromptToEditDoneTargetCard( holder, mFilteredItems[position]);
                }
                return false;
            }
        });

        holder.mTargetCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean targetCardOpened = openTargetTranslationCard(holder, position);

                // Accept clicks anywhere on card as if they were on the text box --
                // but only if the text is actually editable (i.e., not yet done).

                if(!targetCardOpened && holder.mTargetBody.isEnabled()) {
                    editTarget( holder.mTargetBody, mFilteredItems[position]);
                }

                // if marked as done (disabled for edit), enable to allow capture of click events, but do not make it focusable so they can't edit

                else  {
                    enableClicksIfChunkIsDone(holder);

                }
            }
        });
        holder.mSourceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeTargetTranslationCard(holder, position);
            }
        });

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

        holder.mNewTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() != null) {
                    getListener().onNewSourceTranslationTabClick();
                }
            }
        });

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.formatSub(mContext, Typography.TranslationType.SOURCE, holder.mSourceTitle, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.format(mContext, Typography.TranslationType.SOURCE, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatSub(mContext, Typography.TranslationType.TRANSLATION, holder.mTargetTitle, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, Typography.TranslationType.TRANSLATION, holder.mTargetBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
        }

        ViewUtil.makeLinksClickable(holder.mSourceBody);

        // render the content
        if(item.isChapterReference) {
            renderChapterReference(holder, position, item.chapterSlug);
        } else if(item.isChapterTitle) {
            renderChapterTitle(holder, position, item.chapterSlug);
        } else if(item.isProjectTitle) {
            renderProjectTitle(holder, position);
        } else {
            renderFrame(holder, position);
        }

        //////
        // set up card UI for merge conflicts

        if(item.renderedTargetBody == null) {
            item.loadTranslations(mSourceTranslation, mTargetTranslation, mChapters.get(item.chapterSlug), loadFrame(item.chapterSlug, item.frameSlug), this);
        }

        Button conflictButton = (Button)holder.mTargetCard.findViewById(R.id.conflict_button);
        FrameLayout conflictButtonFrame = (FrameLayout)holder.mTargetCard.findViewById(R.id.conflict_frame);
        if((conflictButton != null) &&(conflictButtonFrame != null)) {
            if (item.isTranslationMergeConflicted) {
                conflictButton.setVisibility(View.VISIBLE);
                conflictButtonFrame.setVisibility(View.VISIBLE);
                holder.mTargetBody.setVisibility(View.GONE);
                conflictButton.setOnClickListener(new View.OnClickListener() {
                                                      @Override
                                                      public void onClick(View v) {
                                                          Bundle args = new Bundle();
                                                          args.putBoolean(ChunkModeFragment.EXTRA_TARGET_OPEN, true);
                                                          args.putString(App.EXTRA_CHAPTER_ID, item.chapterSlug);
                                                          args.putString(App.EXTRA_FRAME_ID, item.frameSlug);
                                                          getListener().openTranslationMode(TranslationViewMode.REVIEW, args);
                                                      }
                                                  }
                );
            } else {
                conflictButtonFrame.setVisibility(View.GONE);
                holder.mTargetBody.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Renders the project title card
     * @param holder
     * @param position
     */
    private void renderProjectTitle(final ViewHolder holder, final int position) {
        holder.mSourceTitle.setText("");
        boolean targetSearch = isTargetSearch();
        CharSequence projectTitle = renderText(mSourceTranslation.getProjectTitle(), TranslationFormat.DEFAULT, !targetSearch);
        CharSequence targetTitle = renderText(mTargetTranslation.getTargetLanguageName(), mTargetFormat, targetSearch);
        holder.mSourceBody.setText(projectTitle);
        holder.mTargetTitle.setText(targetTitle);
        if(holder.mTextWatcher != null) {
            holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
        }
        final ProjectTranslation projectTranslation = mTargetTranslation.getProjectTranslation();
        holder.mTargetBody.setText(projectTranslation.getTitle());

        indicateCardCompleted(projectTranslation.isTitleFinished(), holder);

        // editing
        holder.mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    mTargetTranslation.applyProjectTitleTranslation(s.toString());
                } catch (IOException e) {
                    Logger.e(ChunkModeFragment.class.getName(), "Failed to save the project title translation", e);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);
    }

    /**
     * Renders the chapter title card
     * begin edit of target card
     * @param target
     */
    public void editTarget(final EditText target, final ListItem item) {

        // flag that chunk is open for edit

        if (item.isChapterReference) {
            Chapter chapter = mLibrary.getChapter(mSourceTranslation, item.chapterSlug);
            if (null != chapter) {
                mTargetTranslation.reopenChapterReference(chapter);
            }
        } else if (item.isChapterTitle) {
            Chapter chapter = mLibrary.getChapter(mSourceTranslation, item.chapterSlug);
            if (null != chapter) {
                mTargetTranslation.reopenChapterTitle(chapter);
            }
        } else if(item.isProjectTitle) {
            mTargetTranslation.openProjectTitle();
        } else {
            Frame frame = mLibrary.getFrame(mSourceTranslation, item.chapterSlug, item.frameSlug);
            if(null != frame) {
                mTargetTranslation.reopenFrame(frame);
            }
        }

        // set focus on edit text
        boolean gotFocus = target.requestFocus();
        InputMethodManager mgr = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * if chunk that is marked done, then enable click event
     * @param holder
     */
    public void enableClicksIfChunkIsDone(final ViewHolder holder) {

        if (!holder.mTargetBody.isEnabled()) {
            holder.mTargetBody.setEnabled(true);
            holder.mTargetBody.setFocusable(false);
        }
    }

    /**
     * prompt to edit chunk that is marked done
     * @param holder
     * @param item
     */
    public boolean checkForPromptToEditDoneTargetCard(final ViewHolder holder, final ListItem item) {

        if (item.isTargetCardOpen) { // if page is already in front and they are tapping on it, then see if they want to open for edit

            boolean enabled = holder.mTargetBody.isEnabled();
            boolean focusable = holder.mTargetBody.isFocusable();

            if (enabled && !focusable) { //if we have enabled for touch events but not focusable for edit then prompt to enable editing
                promptToEditDoneChunk( holder, item);
                return true;
            }
        }

        return false;
    }

    /**
     * prompt to edit chunk that is marked done
     * @param holder
     */
    public void promptToEditDoneChunk(final ViewHolder holder, final ListItem item) {
        new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                .setTitle(R.string.chunk_done_title)
//                                .setIcon(R.drawable.ic_local_library_black_24dp)
                .setMessage(R.string.chunk_done_prompt)
                .setPositiveButton(R.string.edit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        holder.mTargetBody.setEnabled(true);
                        holder.mTargetBody.setFocusable(true);
                        holder.mTargetBody.setFocusableInTouchMode(true);
                        holder.mTargetBody.setEnableLines(true);
                        editTarget(holder.mTargetBody, item);
                    }
                })
                .setNegativeButton(R.string.dismiss, null)
                .show();
    }

    /**
         * Renders the chapter title card
         * @param holder
         * @param position
         * @param chapterId
         */
    private void renderChapterTitle(final ViewHolder holder, final int position, String chapterId) {
        final ListItem item = mFilteredItems[position];
        Chapter chapter = mChapters.get(chapterId);
        if(chapter != null) {
            // source title
            String sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
            holder.mSourceTitle.setText(sourceChapterTitle);

            // source chapter title
            if(item.renderedSourceBody == null) {
                item.renderedSourceBody = chapter.title;
            }

            boolean targetSearch = isTargetSearch();
            item.renderedSourceBody = renderText(item.renderedSourceBody.toString(), TranslationFormat.DEFAULT, !targetSearch);
            holder.mSourceBody.setText(item.renderedSourceBody);

            // target chapter reference
            final ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
            if(item.renderedTargetBody == null) {
                item.renderedTargetBody = chapterTranslation.title;
            }
            if(holder.mTextWatcher != null) {
                holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
            }

            item.testForMergeConflict(item.renderedTargetBody.toString());
            item.renderedTargetBody = renderText(item.renderedTargetBody.toString(), mTargetFormat, targetSearch);
            holder.mTargetBody.setText(item.renderedTargetBody);

            // target title
            holder.mTargetTitle.setText(sourceChapterTitle + " - " + mTargetLanguage.name);

            // indicate completed frame translations
            indicateCardCompleted(chapterTranslation.isTitleFinished(), holder);

            // editing
            holder.mTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // save
                    //                TODO: do this so we don't have to wait for compiling
                    //                Translator.applyFrameTranslation(frameTranslation, (Editable)s);

                    String translation = Translator.compileTranslation((Editable)s);
                    mTargetTranslation.applyChapterTitleTranslation(chapterTranslation, translation);
                    item.renderedTargetBody = translation;
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            };
            holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);
        }
    }

    /**
     * Renders the chapter reference card
     * @param holder
     * @param position
     * @param chapterId
     */
    private void renderChapterReference(final ViewHolder holder, final int position, String chapterId) {
        final ListItem item = mFilteredItems[position];
        Chapter chapter = mChapters.get(chapterId);
        if(chapter != null) {
            // source title
            String sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
            holder.mSourceTitle.setText(sourceChapterTitle);

            // source chapter reference
            if(item.renderedSourceBody == null) {
                item.renderedSourceBody = chapter.reference;
            }
            boolean targetSearch = isTargetSearch();
            item.renderedSourceBody = renderText(item.renderedSourceBody.toString(), TranslationFormat.DEFAULT, !targetSearch);
            holder.mSourceBody.setText(item.renderedSourceBody);

            // target chapter reference
            final ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
            if(item.renderedTargetBody == null) {
                item.renderedTargetBody = chapterTranslation.reference;
            }
            if(holder.mTextWatcher != null) {
                holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
            }
            item.testForMergeConflict(item.renderedTargetBody.toString());
            item.renderedTargetBody = renderText(item.renderedTargetBody.toString(), mTargetFormat, targetSearch);
            holder.mTargetBody.setText(item.renderedTargetBody);

            // target title
            holder.mTargetTitle.setText(sourceChapterTitle + " - " + mTargetLanguage.name);

            // indicate completed frame translations
            indicateCardCompleted(chapterTranslation.isReferenceFinished(), holder);

            // editing
            holder.mTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // save
    //                TODO: do this so we don't have to wait for compiling
    //                Translator.applyFrameTranslation(frameTranslation, (Editable)s);

                    String translation = Translator.compileTranslation((Editable)s);
                    mTargetTranslation.applyChapterReferenceTranslation(chapterTranslation, translation);
                    item.renderedTargetBody = translation;
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            };
            holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);
        }
    }

    /**
     * Renders the frame cards
     * @param holder
     * @param position
     */
    private void renderFrame(final ViewHolder holder, final int position) {
        final ListItem item = mFilteredItems[position];
        Frame frame = mLibrary.getFrame(mSourceTranslation, item.chapterSlug, item.frameSlug);

        // render the source frame body
        item.renderedSourceBody = renderText(frame.body, frame.getFormat(), !isTargetSearch());

        holder.mSourceBody.setText(item.renderedSourceBody);

        // render source frame title
        Chapter chapter = mChapters.get(frame.getChapterId());
        String sourceChapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        sourceChapterTitle += ":" + frame.getTitle();
        holder.mSourceTitle.setText(sourceChapterTitle);

        // render the target frame body
        mTargetFormat =  mTargetTranslation.getFormat();
        final FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
        item.testForMergeConflict(frameTranslation.body);
        item.renderedTargetBody = renderText(frameTranslation.body, mTargetFormat, isTargetSearch());

        if(holder.mTextWatcher != null) {
            holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
        }
        holder.mTargetBody.setText(TextUtils.concat(item.renderedTargetBody, "\n"));

        // render target frame title
        ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
        String targetChapterTitle = chapterTranslation.title;
        if(!targetChapterTitle.isEmpty()) {
            targetChapterTitle += ":" + frameTranslation.getTitle();
        } else {
            targetChapterTitle = sourceChapterTitle;
        }
        holder.mTargetTitle.setText(targetChapterTitle + " - " + mTargetLanguage.name);

        // indicate completed frame translations
        indicateCardCompleted(frameTranslation.isFinished(), holder);

        holder.mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // save
//                TODO: do this so we don't have to wait for compiling
//                Translator.applyFrameTranslation(frameTranslation, (Editable)s);

                String translation = Translator.compileTranslation((Editable)s);
                mTargetTranslation.applyFrameTranslation(frameTranslation, translation);
                item.renderedTargetBody = renderText(translation, mTargetFormat, isTargetSearch());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);
    }

    private void indicateCardCompleted(boolean finished, ViewHolder holder) {
        if(finished) {
            holder.mTargetBody.setEnabled(false);
            holder.mTargetBody.setEnableLines(false);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        } else {
            holder.mTargetBody.setEnabled(true);
            holder.mTargetBody.setEnableLines(true);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        }
    }

    /**
     * Trigers some aspects of the children views to be rebuilt
     */
    public void rebuild() {
        mLayoutBuildNumber ++;
        notifyDataSetChanged();
    }

    private CharSequence renderText(String text, TranslationFormat format, boolean enableSearch) {
        RenderingGroup renderingGroup = new RenderingGroup();
        if (Clickables.isClickableFormat(format)) {
            // TODO: add click listeners for verses and notes
            Span.OnClickListener noteClickListener = new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    if(span instanceof NoteSpan) {
                        new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                                .setTitle(R.string.title_note)
                                .setMessage(((NoteSpan)span).getNotes())
                                .setPositiveButton(R.string.dismiss, null)
                                .show();
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            };
            ClickableRenderingEngine renderer = Clickables.setupRenderingGroup(format, renderingGroup, null, noteClickListener, true);
            renderer.setVersesEnabled(false);
            if( enableSearch ) {
                renderingGroup.setSearchString(mSearchString, HIGHLIGHT_COLOR);
            }
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
            if( enableSearch ) {
                renderingGroup.setSearchString(mSearchString, HIGHLIGHT_COLOR);
            }
        }
        renderingGroup.init(text);
        return renderingGroup.start();
    }

    @Override
    public int getItemCount() {
        return mFilteredItems.length;
    }

    /**
     * removes text selection from the target card
     * @param holder
     */
    public void clearSelectionFromTarget(ViewHolder holder) {
        EditText translationEditText = (EditText) holder.mTargetCard.findViewById(R.id.target_translation_body);
        if (translationEditText != null) {
            translationEditText.clearFocus();
        }
    }

    /**
     * Toggle the target translation card between front and back
     * @param holder
     * @param position
     * @param swipeLeft
     * @return true if action was taken, else false
     */
    public boolean toggleTargetTranslationCard(final ViewHolder holder, final int position, final boolean swipeLeft) {
        final ListItem item = mFilteredItems[position];
        if (item.isTargetCardOpen) {
            return closeTargetTranslationCard( holder, position, !swipeLeft);
        }

        boolean success = openTargetTranslationCard( holder, position, !swipeLeft);
        enableClicksIfChunkIsDone(holder);
        return success;
    }

    /**
     * Moves the target translation card to the back
     * @param holder
     * @param position
     * @param leftToRight
     * @return true if action was taken, else false
     */
    public boolean closeTargetTranslationCard(final ViewHolder holder, final int position, final boolean leftToRight) {
        final ListItem item = mFilteredItems[position];
        if(item.isTargetCardOpen) {

            clearSelectionFromTarget(holder);

            ViewUtil.animateSwapCards(holder.mTargetCard, holder.mSourceCard, TOP_ELEVATION, BOTTOM_ELEVATION, leftToRight, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    item.isTargetCardOpen = false;
                    if (getListener() != null) {
                        getListener().closeKeyboard();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            if (getListener() != null) {
                getListener().closeKeyboard();
            }
            // re-enable new tab button
            holder.mNewTabButton.setEnabled(true);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Moves the target translation card to the back - left to right
     * @param holder
     * @param position
     * @return true if action was taken, else false
     */
    public boolean closeTargetTranslationCard(final ViewHolder holder, final int position) {
        return closeTargetTranslationCard ( holder, position, true);
    }

    /**
     * Moves the target translation to the top
     * @param holder
     * @param position
     * @param leftToRight
     * @return true if action was taken, else false
     */
    public boolean openTargetTranslationCard(final ViewHolder holder, final int position, final boolean leftToRight) {
        final ListItem item = mFilteredItems[position];
        if(!item.isTargetCardOpen) {
            ViewUtil.animateSwapCards(holder.mSourceCard, holder.mTargetCard, TOP_ELEVATION, BOTTOM_ELEVATION, leftToRight, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    item.isTargetCardOpen = true;
                    if (getListener() != null) {
                        getListener().closeKeyboard();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            if (getListener() != null) {
                getListener().closeKeyboard();
            }
            // disable new tab button so we don't accidently open it
            holder.mNewTabButton.setEnabled(false);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Moves the target translation to the top
     * @param holder
     * @param position
     * @return true if action was taken, else false
     */
    public boolean openTargetTranslationCard(final ViewHolder holder, final int position) {
        return openTargetTranslationCard( holder, position, false);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public int mLayoutBuildNumber = -1;
        public TextWatcher mTextWatcher;
        public final TextView mTargetTitle;
        public final LinedEditText mTargetBody;
        public final CardView mTargetCard;
        public final CardView mSourceCard;
        public final TabLayout mTabLayout;
        public final ImageButton mNewTabButton;
        public TextView mSourceTitle;
        public TextView mSourceBody;
        public LinearLayout mTargetInnerCard;

        public ViewHolder(View v) {
            super(v);
            mSourceCard = (CardView)v.findViewById(R.id.source_translation_card);
            mSourceTitle = (TextView)v.findViewById(R.id.source_translation_title);
            mSourceBody = (TextView)v.findViewById(R.id.source_translation_body);
            mTargetCard = (CardView)v.findViewById(R.id.target_translation_card);
            mTargetInnerCard = (LinearLayout)v.findViewById(R.id.target_translation_inner_card);
            mTargetTitle = (TextView)v.findViewById(R.id.target_translation_title);
            mTargetBody = (LinedEditText)v.findViewById(R.id.target_translation_body);
            mTabLayout = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mTabLayout.setTabTextColors(R.color.dark_disabled_text, R.color.dark_secondary_text);
            mNewTabButton = (ImageButton) v.findViewById(R.id.new_tab_button);
        }
    }

    /**
     * A simple container for list items
     */
    private static class ListItem {
        private final String frameSlug;
        private final String chapterSlug;
        private boolean isChapterReference = false;
        private boolean isChapterTitle = false;
        public boolean isProjectTitle = false;
        private boolean isTargetCardOpen = false;
        private CharSequence renderedSourceBody;
        private CharSequence renderedTargetBody;
        private boolean mHighlightSource = false;
        private boolean mHighlightTarget = false;
        private boolean isTranslationMergeConflicted = false;

        public ListItem(String frameSlug, String chapterSlug) {
            this.frameSlug = frameSlug;
            this.chapterSlug = chapterSlug;
        }

        /**
         * Checks if this item is a frame
         * @return
         */
        public boolean isFrame() {
            return this.frameSlug != null;
        }

        /**
         * Checks if this item is a chapter (either title or reference)
         * @return
         */
        public boolean isChapter() {
            return this.frameSlug == null && this.chapterSlug != null;
        }

        /**
         * clear all the previous highlighting states for the item
         */
        public void clearAllHighLighting() {
            setHighLighting(false, true);
            setHighLighting(false, false);
        }

        /**
         * set the highlighting state for the item and clearing any old highlighting
         * @param enable
         * @param target
         */
        public void setHighLighting(boolean enable, boolean target) {
            if(target) {
                if(!enable) { // disable highlighting
                    if(mHighlightTarget) {
                        renderedTargetBody = null; // remove rendered text so will be re-rendered without highlighting
                    }
                } else { // enable highlighting
                    renderedTargetBody = null; // remove rendered text so will be re-rendered with new highlighting
                }
                mHighlightTarget = enable;
            } else { // source
                if(!enable) { // disable highlighting
                    if(mHighlightSource) {
                        renderedSourceBody = null; // remove rendered text so will be re-rendered without highlighting
                    }
                } else { // enable highlighting
                    renderedSourceBody = null; // remove rendered text so will be re-rendered with new highlighting
                }
                mHighlightSource = enable;
            }
        }

        /**
         * Loads the correct translation information into the item
         * @param targetTranslation
         * @param chapter
         * @param frame
         */
        public void loadTranslations(SourceTranslation sourceTranslation, TargetTranslation targetTranslation, Chapter chapter, Frame frame, ChunkModeAdapter adapter) {
            if(isChapterReference || isChapterTitle) {
                ChapterTranslation chapterTranslation = targetTranslation.getChapterTranslation(chapter);
                if (isChapterTitle) {
                    renderedTargetBody = chapterTranslation.title;
                    renderedSourceBody = chapter.title;
                    updateMergeConflict(chapterTranslation.title);
                } else {
                    renderedTargetBody = chapterTranslation.reference;
                    renderedSourceBody = chapter.reference;
                    updateMergeConflict(chapterTranslation.reference);
                }
            } else if(isProjectTitle) {
                ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();
                renderedTargetBody = projectTranslation.getTitle();
                renderedSourceBody = sourceTranslation.getProjectTitle();
                updateMergeConflict(projectTranslation.getTitle());
            } else {
                FrameTranslation frameTranslation = targetTranslation.getFrameTranslation(frame);
                TranslationFormat targetFormat =  targetTranslation.getFormat();
                boolean targetSearch = adapter.isTargetSearch();
                renderedSourceBody = adapter.renderText(frame.body, frame.getFormat(),!targetSearch);
                renderedTargetBody = adapter.renderText(frameTranslation.body, targetFormat, targetSearch);
                updateMergeConflict(frameTranslation.body);
            }
        }

        /**
         * recheck if merge is conflict
         */
        private void updateMergeConflict(String bodyTranslation) {
            if (bodyTranslation != null) {
                isTranslationMergeConflicted = MergeConflictHandler.isMergeConflicted(bodyTranslation);
            } else {
                isTranslationMergeConflicted = false;
            }
        }

        public void testForMergeConflict(String text) {
            updateMergeConflict(text);
        }
    }

    /**
     * Loads a frame from the index and caches it
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    private Frame loadFrame(String chapterSlug, String frameSlug) {
        Frame frame = mLibrary.getFrame(mSourceTranslation, chapterSlug, frameSlug);
        return frame;
    }

    /**
     * remove displayed cards
     * @param searchString
     * @param searchTarget
     */
    public void clearScreenAndStartNewSearch(final CharSequence searchString, final boolean searchTarget) {

        // clear the cards displayed since we have new search string
        mFilteredItems = new ListItem[0];
        resetSectionMarkers();
        notifyDataSetChanged();

        if( (searchString != null) && (searchString.length() > 0)) {
            getListener().onSetBusyIndicator(true);
        }

        //start search on delay so cards will clear first
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                ((TranslationSearchFilter) getFilter()).setTargetSearch(searchTarget).filter(searchString);
            }
        });
    }


    /**
     * check the filter to see what the last search type was
     * @return
     */
    private boolean isTargetSearch() {
        if(mSearchFilter != null) {
            return mSearchFilter.isTargetSearch();
        }
        return false;
    }

    /**
     * Returns the target language filter
     * @return
     */
    public Filter getFilter() {
        if(mSearchFilter == null) {
            mSearchFilter = new SearchFilter();
        }
        return mSearchFilter;
    }

    /**
     * class for searching text
     */
    private class SearchFilter extends TranslationSearchFilter {

        private boolean searchTarget = false;
        CharSequence oldSearch = null;

        public SearchFilter setTargetSearch(boolean searchTarget) { // chainable
            this.searchTarget = searchTarget;
            return this;
        }

        public boolean isTargetSearch() {
            return searchTarget;
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();
            mSearchString = charSequence;
            if(charSequence == null || charSequence.length() == 0) {
                // no filter
                results.values = Arrays.asList(mUnfilteredItems);
                results.count = mUnfilteredItems.length;
                for (ListItem unfilteredItem : mUnfilteredItems) {
                    unfilteredItem.clearAllHighLighting();
                }
            } else {
                // perform filter
                String matchString = charSequence.toString().toLowerCase();
                List<ListItem> filteredCategories = new ArrayList<>();
                for(ListItem item: mUnfilteredItems) {
                    boolean match = false;

                    if(!searchTarget) { // search the source
                        if (item.renderedSourceBody == null) { // if source hasn't been rendered
                            item.loadTranslations(mSourceTranslation, mTargetTranslation, mChapters.get(item.chapterSlug), loadFrame(item.chapterSlug, item.frameSlug), ChunkModeAdapter.this);
                        }
                        if (item.renderedSourceBody != null) {
                            match = item.renderedSourceBody.toString().toLowerCase().contains(matchString);
                        }
                    } else { // search the target
                        if (item.renderedTargetBody == null) { // if target hasn't been rendered
                            item.loadTranslations(mSourceTranslation, mTargetTranslation, mChapters.get(item.chapterSlug), loadFrame(item.chapterSlug, item.frameSlug), ChunkModeAdapter.this);
                        }
                        if (item.renderedTargetBody != null) {
                            match = item.renderedTargetBody.toString().toLowerCase().contains(matchString);
                        }
                    }

                    if(match) {
                        filteredCategories.add(item);
                        item.setHighLighting(true, searchTarget);
                        item.setHighLighting(false, !searchTarget); // remove searching from opposite pane
                    } else {
                        item.clearAllHighLighting(); // if not matched item, remove previous highlighting
                    }
                }
                results.values = filteredCategories;
                results.count = filteredCategories.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            List<ListItem> filteredLanguages = (List<ListItem>)filterResults.values;
            mFilteredItems = filteredLanguages.toArray(new ListItem[filteredLanguages.size()]);
            updateChapterMarkers(oldSearch);
            oldSearch = mSearchString;
            notifyDataSetChanged();
            getListener().onSetBusyIndicator(false);
        }
    }

    /**
     * check to see if search string has changed and therefor chapter markers will need updating.  Note null string and empty string are treated as the same
     * @param oldSearch
     */
    private void updateChapterMarkers(CharSequence oldSearch) {
        boolean searchChanged = false;
        if (oldSearch == null) {
            if( (mSearchString != null) && (mSearchString.length() > 0) ) {
                searchChanged = true;
            }
        } else  if (mSearchString == null) {
            if( (oldSearch != null)  && (oldSearch.length() > 0) ) {
                searchChanged = true;
            }
        } else if(!mSearchString.equals(oldSearch)) {
            searchChanged = true;
        }

        if(searchChanged) {
            resetSectionMarkers();
        }
    }
}
