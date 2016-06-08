package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
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
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.rendering.ClickableRenderingEngine;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.widget.ViewUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joel on 9/9/2015.
 */
public class ChunkModeAdapter extends ViewModeAdapter<ChunkModeAdapter.ViewHolder> implements SectionIndexer {
    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
    private final Activity mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;
    private final TargetTranslation mTargetTranslation;
    private SourceTranslation mSourceTranslation;
    private final Library mLibrary;
    private final Translator mTranslator;
    private ListItem[] mListItems;
    private Map<String, Chapter> mChapters = new HashMap<>();
    private int mLayoutBuildNumber = 0;
    private ContentValues[] mTabs;
    private TranslationFormat mTargetFormat;
    private String[] mChapterMarkers;
    private Integer[] mStartPositionForSection;
    private Integer[] mSectionForPosition;

    public ChunkModeAdapter(Activity context, String targetTranslationId, String sourceTranslationId, String startingChapterSlug, String startingFrameSlug, boolean openSelectedTarget) {
        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());

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
        mListItems = listItems.toArray(new ListItem[listItems.size()]);
        mListItems[getListStartPosition()].isTargetCardOpen = openSelectedTarget;

        loadTabInfo();
    }

    /**
     * Rebuilds the card tabs
     */
    private void loadTabInfo() {
        List<ContentValues> tabContents = new ArrayList<>();
        String[] sourceTranslationIds = AppContext.getOpenSourceTranslationIds(mTargetTranslation.getId());
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
        mListItems = listItems.toArray(new ListItem[listItems.size()]);

        loadTabInfo();

        notifyDataSetChanged();
    }

    @Override
    void onCoordinate(ViewHolder holder) {

    }

    @Override
    public String getFocusedFrameId(int position) {
        if(position >= 0 && position < mListItems.length) {
            return mListItems[position].frameSlug;
        }
        return null;
    }

    @Override
    public String getFocusedChapterId(int position) {
        if(position >= 0 && position < mListItems.length) {
            return mListItems[position].chapterSlug;
        }
        return null;
    }

    @Override
    public int getItemPosition(String chapterSlug, String frameSlug) {
        for(int i = 0; i < mListItems.length; i ++) {
            ListItem item = mListItems[i];
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
     * get the chapter ID for the position
     * @param position
     */
    public String getNearestChapterID(int position) {
        if(position < 0) {
            position = 0;
        } else if(position >= mListItems.length) {
            position = mListItems.length - 1;
        }

        ListItem item;
        for(int i = position ; i < mListItems.length; i++) { // check later frames to get frame with valid chapter ID
            item = mListItems[i];
            if(item.isFrame()) {
                String chapterSlug = item.chapterSlug;
                if(chapterSlug != null) {
                    return chapterSlug;
                }
            }
        }
        for(int i = position - 1; i >= 0; i--) { // check current frame and previous to get frame with valid chapter ID
            item = mListItems[i];
            if(item.isFrame()) {
                String chapterSlug = item.chapterSlug;
                if(chapterSlug != null) {
                    return chapterSlug;
                }
            }
        }

        String chapterID = Integer.toString(position + 1);
        return chapterID;
    }

    /**
     * get the chapter ID for the position
     * @param position
     */
    @Override
    public String getChapterID(int position) {
        int section = getSectionForPosition( position);
        if(section > 0) {
            return mChapterMarkers[section];
        }
        return "";
    }

    @Override
    public Object[] getSections() {
        makeSureChapterMarkersInitialized();
        return mChapterMarkers;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        makeSureChapterMarkersInitialized();

        if( sectionIndex < 0 ) { // limit input range
            sectionIndex = 0;
        } else if( sectionIndex >= mStartPositionForSection.length ) {
            sectionIndex = mStartPositionForSection.length - 1;
        }

        return mStartPositionForSection[sectionIndex];
    }

    /**
     * if not yet cached, determine and cache the chapter boundaries
     */
    private void makeSureChapterMarkersInitialized() {
        if(null == mChapterMarkers) {
            List<String> chapterLists = new ArrayList<>();
            List<Integer> sectionForPosition = new ArrayList<>();
            List<Integer> startPositionForSection = new ArrayList<>();
            int length = getItemCount();
            String lastChapter = "<null>";
            int currentSection = -1;
            for (int i = 0; i < length; i++) {
                String chapter = getNearestChapterID(i);
                if(!lastChapter.equals(chapter)) {
                    chapterLists.add(chapter);
                    startPositionForSection.add(i);
                    lastChapter = chapter;
                    currentSection = Integer.valueOf(chapter) - 1;
                }
                sectionForPosition.add(currentSection);
            }
            mChapterMarkers = chapterLists.toArray(new String[chapterLists.size()]);
            mStartPositionForSection = startPositionForSection.toArray(new Integer[chapterLists.size()]);
            mSectionForPosition = sectionForPosition.toArray(new Integer[chapterLists.size()]);
        }
    }

    @Override
    public int getSectionForPosition(int position) {
        makeSureChapterMarkersInitialized();

        if( position < 0 ) { // limit input range
            position = 0;
        } else if( position >= mSectionForPosition.length ) {
            position = mSectionForPosition.length - 1;
        }

        int section = mSectionForPosition[position];

        if( section < 0 ) { // sanity check - limit output range
            section = 0;
        } else if( section >= mChapterMarkers.length ) {
            section = mChapterMarkers.length - 1;
        }
        return section;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int cardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.card_margin);
        int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);
        ListItem item = mListItems[position];
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

                    return checkForPromptToEditDoneTargetCard( holder, mListItems[position]);
                }
                return false;
            }
        });

        holder.mTargetBody.setOnTouchListener(new View.OnTouchListener() { //for touches on edit area
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_UP == event.getAction()) {

                    return checkForPromptToEditDoneTargetCard( holder, mListItems[position]);
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
                    editTarget( holder.mTargetBody, mListItems[position]);
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
            Typography.formatSub(mContext, holder.mSourceTitle, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatSub(mContext, holder.mTargetTitle, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
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
    }

    /**
     * Renders the project title card
     * @param holder
     * @param position
     */
    private void renderProjectTitle(final ViewHolder holder, final int position) {
        holder.mSourceTitle.setText("");
        holder.mSourceBody.setText(mSourceTranslation.getProjectTitle());
        holder.mTargetTitle.setText(mTargetTranslation.getTargetLanguageName());
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
        CustomAlertDialog.Create(mContext)
                .setTitle(R.string.chunk_done_title)
//                                .setIcon(R.drawable.ic_local_library_black_24dp)
                .setMessage(R.string.chunk_done_prompt)
                .setPositiveButton(R.string.edit, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.mTargetBody.setEnabled(true);
                        holder.mTargetBody.setFocusable(true);
                        holder.mTargetBody.setFocusableInTouchMode(true);
                        holder.mTargetBody.setEnableLines(true);
                        editTarget(holder.mTargetBody, item);
                    }
                })
                .setNegativeButton(R.string.dismiss, null)
                .show("ChunkDone");
    }

    /**
         * Renders the chapter title card
         * @param holder
         * @param position
         * @param chapterId
         */
    private void renderChapterTitle(final ViewHolder holder, final int position, String chapterId) {
        final ListItem item = mListItems[position];
        Chapter chapter = mChapters.get(chapterId);
        if(chapter != null) {
            // source title
            String sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
            holder.mSourceTitle.setText(sourceChapterTitle);

            // source chapter title
            if(item.renderedSourceBody == null) {
                item.renderedSourceBody = chapter.title;
            }
            holder.mSourceBody.setText(item.renderedSourceBody);

            // target chapter reference
            final ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
            if(item.renderedTargetBody == null) {
                item.renderedTargetBody = chapterTranslation.title;
            }
            if(holder.mTextWatcher != null) {
                holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
            }
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
        final ListItem item = mListItems[position];
        Chapter chapter = mChapters.get(chapterId);
        if(chapter != null) {
            // source title
            String sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
            holder.mSourceTitle.setText(sourceChapterTitle);

            // source chapter reference
            if(item.renderedSourceBody == null) {
                item.renderedSourceBody = chapter.reference;
            }
            holder.mSourceBody.setText(item.renderedSourceBody);

            // target chapter reference
            final ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
            if(item.renderedTargetBody == null) {
                item.renderedTargetBody = chapterTranslation.reference;
            }
            if(holder.mTextWatcher != null) {
                holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
            }
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
        final ListItem item = mListItems[position];
        Frame frame = mLibrary.getFrame(mSourceTranslation, item.chapterSlug, item.frameSlug);

        // render the source frame body
        if(item.renderedSourceBody == null) {
            item.renderedSourceBody = renderText(frame.body, frame.getFormat());
        }

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
        if(item.renderedTargetBody == null) {
            item.renderedTargetBody = renderText(frameTranslation.body, mTargetFormat);
        }
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
                item.renderedTargetBody = renderText(translation, mTargetFormat);
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

    private CharSequence renderText(String text, TranslationFormat format) {
        RenderingGroup renderingGroup = new RenderingGroup();
        if (Clickables.isClickableFormat(format)) {
            // TODO: add click listeners for verses and notes
            Span.OnClickListener noteClickListener = new Span.OnClickListener() {
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
            };
            ClickableRenderingEngine renderer = Clickables.setupRenderingGroup(format, renderingGroup, null, noteClickListener, true);
            renderer.setVersesEnabled(false);

        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
        }
        renderingGroup.init(text);
        return renderingGroup.start();
    }

    @Override
    public int getItemCount() {
        return mListItems.length;
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
        final ListItem item = mListItems[position];
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
        final ListItem item = mListItems[position];
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
        final ListItem item = mListItems[position];
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
    }
}
