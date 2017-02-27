package com.door43.translationstudio.ui.translate;

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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.tasks.CheckForMergeConflictsTask;
import com.door43.widget.LinedEditText;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.ClickableRenderingEngine;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.ui.spannables.NoteSpan;
import com.door43.translationstudio.ui.spannables.Span;
import com.door43.widget.ViewUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

/**
 * Created by joel on 9/9/2015.
 */
public class ChunkModeAdapter extends ViewModeAdapter<ChunkModeAdapter.ViewHolder> {
    public static final int HIGHLIGHT_COLOR = Color.YELLOW;
    private TargetLanguage mTargetLanguage;
    private final Activity mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;
    private final TargetTranslation mTargetTranslation;
    private ResourceContainer mSourceContainer;
    private final Door43Client mLibrary;
    private final Translator mTranslator;
    private List<ListItem> mItems = new ArrayList<>();
    private List<ListItem> mFilteredItems = new ArrayList<>();
    private int mLayoutBuildNumber = 0;
    private ContentValues[] mTabs = new ContentValues[0];
    private List<String> mChapters = new ArrayList();
    private List<String> mFilteredChapters = new ArrayList<>();
    private CharSequence filterConstraint = null;
    private TranslationFilter.FilterSubject filterSubject = null;

    public ChunkModeAdapter(Activity context, String targetTranslationId, String startingChapterSlug, String startingChunkSlug, boolean openSelectedTarget) {
        this.startingChapterSlug = startingChapterSlug;
        this.startingChunkSlug = startingChunkSlug;

        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mTargetLanguage = App.languageFromTargetTranslation(mTargetTranslation);
    }

    @Override
    public void setSourceContainer(ResourceContainer sourceContainer) {
        mSourceContainer = sourceContainer;
        mLayoutBuildNumber++; // force resetting of fonts

        mChapters = new ArrayList();
        mItems = new ArrayList<>();
        initializeListItems(mItems, mChapters, mSourceContainer);

        mFilteredItems = mItems;
        mFilteredChapters = mChapters;

        loadTabInfo();

        filter(filterConstraint, filterSubject, 0);

        triggerNotifyDataSetChanged();
        updateMergeConflict();
    }

    @Override
    public ListItem createListItem(String chapterSlug, String chunkSlug) {
        return new ChunkListItem(chapterSlug, chunkSlug);
    }

    /**
     * check all cards for merge conflicts to see if we should show warning.  Runs as background task.
     */
    private void updateMergeConflict() {
        doCheckForMergeConflictTask(mItems, mSourceContainer, mTargetTranslation);
    }

    @Override
    public void onTaskFinished(ManagedTask task) {
        TaskManager.clearTask(task);

        if (task instanceof CheckForMergeConflictsTask) {
            CheckForMergeConflictsTask mergeConflictsTask = (CheckForMergeConflictsTask) task;

            final boolean mergeConflictFound = mergeConflictsTask.hasMergeConflict();
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    OnEventListener listener = getListener();
                    if(listener != null) {
                        listener.onEnableMergeConflict(mergeConflictFound, false);
                    }
                }
            });
        }
    }

    /**
     * Rebuilds the card tabs
     */
    private void loadTabInfo() {
        List<ContentValues> tabContents = new ArrayList<>();
        String[] sourceTranslationIds = App.getSelectedSourceTranslations(mTargetTranslation.getId());
        for(String slug:sourceTranslationIds) {
            Translation st = mLibrary.index().getTranslation(slug);
            if(st != null) {
                ContentValues values = new ContentValues();
                // include the resource id if there are more than one
                if(mLibrary.index().getResources(st.language.slug, st.project.slug).size() > 1) {
                    values.put("title", st.language.name + " " + st.resource.slug.toUpperCase());
                } else {
                    values.put("title", st.language.name);
                }
                values.put("tag", st.resourceContainerSlug);
                tabContents.add(values);
            }
        }
        mTabs = tabContents.toArray(new ContentValues[tabContents.size()]);
    }

    @Override
    void onCoordinate(ViewHolder holder) {

    }

    @Override
    public String getFocusedChunkSlug(int position) {
        if(position >= 0 && position < mFilteredItems.size()) {
            return mFilteredItems.get(position).chunkSlug;
        }
        return null;
    }

    @Override
    public String getFocusedChapterSlug(int position) {
        if(position >= 0 && position < mFilteredItems.size()) {
            return mFilteredItems.get(position).chapterSlug;
        }
        return null;
    }

    @Override
    public int getItemPosition(String chapterSlug, String chunkSlug) {
        for(int i = 0; i < mFilteredItems.size(); i ++) {
            ListItem item = mFilteredItems.get(i);
            if(item.isChunk() && item.chapterSlug.equals(chapterSlug) && item.chunkSlug.equals(chunkSlug)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ViewHolder onCreateManagedViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chunk_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

     @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int cardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.card_margin);
        int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);
        final ListItem item = mFilteredItems.get(position);
        if(((ChunkListItem)item).isTargetCardOpen) {
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

                    return checkForPromptToEditDoneTargetCard( holder, mFilteredItems.get(position));
                }
                return false;
            }
        });

        holder.mTargetBody.setOnTouchListener(new View.OnTouchListener() { //for touches on edit area
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_UP == event.getAction()) {

                    return checkForPromptToEditDoneTargetCard( holder, mFilteredItems.get(position));
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
                    editTarget( holder.mTargetBody, mFilteredItems.get(position));
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
            if(tab.getTag().equals(mSourceContainer.slug)) {
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

         item.load(mSourceContainer, mTargetTranslation);

         renderChunk(holder, position);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;

            Typography.formatSub(mContext, TranslationType.SOURCE, holder.mSourceTitle, mSourceContainer.language.slug, mSourceContainer.language.direction);
            Typography.format(mContext, TranslationType.SOURCE, holder.mSourceBody, mSourceContainer.language.slug, mSourceContainer.language.direction);
            Typography.formatSub(mContext, TranslationType.TARGET, holder.mTargetTitle, mTargetLanguage.slug, mTargetLanguage.direction);
            Typography.format(mContext, TranslationType.TARGET, holder.mTargetBody, mTargetLanguage.slug, mTargetLanguage.direction);
        }

        //////
        // set up card UI for merge conflicts

        Button conflictButton = (Button)holder.mTargetCard.findViewById(R.id.conflict_button);
        FrameLayout conflictButtonFrame = (FrameLayout)holder.mTargetCard.findViewById(R.id.conflict_frame);
        if((conflictButton != null) &&(conflictButtonFrame != null)) {
            if (item.hasMergeConflicts) {
                conflictButton.setVisibility(View.VISIBLE);
                conflictButtonFrame.setVisibility(View.VISIBLE);
                holder.mTargetBody.setVisibility(View.GONE);
                conflictButton.setOnClickListener(new View.OnClickListener() {
                                                      @Override
                                                      public void onClick(View v) {
                                                          Bundle args = new Bundle();
                                                          args.putBoolean(ChunkModeFragment.EXTRA_TARGET_OPEN, true);
                                                          args.putString(App.EXTRA_CHAPTER_ID, item.chapterSlug);
                                                          args.putString(App.EXTRA_FRAME_ID, item.chunkSlug);
                                                          getListener().openTranslationMode(TranslationViewMode.REVIEW, args);
                                                      }
                                                  }
                );
            } else {
                conflictButtonFrame.setVisibility(View.GONE);
                holder.mTargetBody.setVisibility(View.VISIBLE);
            }
        }

        ViewUtil.makeLinksClickable(holder.mSourceBody);


    }

    /**
     * Renders the chapter title card
     * begin edit of target card
     * @param target
     */
    public void editTarget(final EditText target, final ListItem item) {

        // flag that chunk is open for edit

        if (item.isChapterReference()) {
            mTargetTranslation.reopenChapterReference(item.chapterSlug);
        } else if (item.isChapterTitle()) {
            mTargetTranslation.reopenChapterTitle(item.chapterSlug);
        } else if(item.isProjectTitle()) {
            mTargetTranslation.openProjectTitle();
        } else {
            mTargetTranslation.reopenFrame(item.chapterSlug, item.chunkSlug);
        }

        // set focus on edit text
        target.requestFocus();
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

        if (((ChunkListItem)item).isTargetCardOpen) { // if page is already in front and they are tapping on it, then see if they want to open for edit

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
     * Renders the frame cards
     * @param holder
     * @param position
     */
    private void renderChunk(final ViewHolder holder, final int position) {
        final ListItem item = mFilteredItems.get(position);

        // render source text
        if(item.renderedSourceText == null) {
            boolean enableSearch = filterConstraint != null && filterSubject != null && filterSubject == TranslationFilter.FilterSubject.SOURCE;
            item.renderedSourceText = renderText(item.sourceText, item.sourceTranslationFormat, enableSearch);
        }
        holder.mSourceBody.setText(item.renderedSourceText);

        // render target text
        if(item.renderedTargetText == null) {
            boolean enableSearch = filterConstraint != null && filterSubject != null && filterSubject == TranslationFilter.FilterSubject.TARGET;
            item.renderedTargetText = renderText(item.targetText, item.targetTranslationFormat, enableSearch);
        }
        if(holder.mTextWatcher != null) holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
        holder.mTargetBody.setText(TextUtils.concat(item.renderedTargetText, "\n"));

        // render source title
        if(item.isProjectTitle()) {
            holder.mSourceTitle.setText("");
        } else if(item.isChapter()) {
            holder.mSourceTitle.setText(mSourceContainer.project.name.trim());
        } else {
            // TODO: we should read the title from a cache instead of doing file io again
            String title = mSourceContainer.readChunk(item.chapterSlug, "title").trim();
            if(title.isEmpty()) {
                title = mSourceContainer.project.name.trim() + " " + Integer.parseInt(item.chapterSlug);
            }
            String verseSpan = Frame.parseVerseTitle(item.sourceText, item.sourceTranslationFormat);
            if(verseSpan.isEmpty()) {
                title += ":" + Integer.parseInt(item.chunkSlug);
            } else {
                title += ":" + verseSpan;
            }
            holder.mSourceTitle.setText(title);
        }

        // render target title
        holder.mTargetTitle.setText(item.getTargetTitle());

        // indicate complete
        indicateCardCompleted(item.isComplete, holder);

        holder.mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // save
                String translation = Translator.compileTranslation((Editable)s);
                if(item.isProjectTitle()){
                    try {
                        mTargetTranslation.applyProjectTitleTranslation(translation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if(item.isChapterTitle()) {
                    mTargetTranslation.applyChapterTitleTranslation(mTargetTranslation.getChapterTranslation(item.chapterSlug), translation);
                } else if(item.isChapterReference()) {
                    mTargetTranslation.applyChapterReferenceTranslation(mTargetTranslation.getChapterTranslation(item.chapterSlug), translation);
                } else {
                    mTargetTranslation.applyFrameTranslation(mTargetTranslation.getFrameTranslation(item.chapterSlug, item.chunkSlug, item.targetTranslationFormat), translation);
                }

                boolean enableSearch = filterConstraint != null && filterSubject != null && filterSubject == TranslationFilter.FilterSubject.TARGET;
                item.renderedTargetText = renderText(translation, item.targetTranslationFormat, enableSearch);
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
                renderingGroup.setSearchString(filterConstraint, HIGHLIGHT_COLOR);
            }
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
            if( enableSearch ) {
                renderingGroup.setSearchString(filterConstraint, HIGHLIGHT_COLOR);
            }
        }
        renderingGroup.init(text);
        return renderingGroup.start();
    }

    @Override
    public int getItemCount() {
        return mFilteredItems.size();
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
        final ListItem item = mFilteredItems.get(position);
        if (((ChunkListItem)item).isTargetCardOpen) {
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
        final ListItem item = mFilteredItems.get(position);
        if(((ChunkListItem)item).isTargetCardOpen) {

            clearSelectionFromTarget(holder);

            ViewUtil.animateSwapCards(holder.mTargetCard, holder.mSourceCard, TOP_ELEVATION, BOTTOM_ELEVATION, leftToRight, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    ((ChunkListItem)item).isTargetCardOpen = false;
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
        final ListItem item = mFilteredItems.get(position);
        if(!((ChunkListItem)item).isTargetCardOpen) {
            ViewUtil.animateSwapCards(holder.mSourceCard, holder.mTargetCard, TOP_ELEVATION, BOTTOM_ELEVATION, leftToRight, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    ((ChunkListItem)item).isTargetCardOpen = true;
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

    @Override
    public Object[] getSections() {
        return mFilteredChapters.toArray();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        // not used
        return sectionIndex;
    }

    @Override
    public int getSectionForPosition(int position) {
        if(position >= 0 && position < mFilteredItems.size()) {
            ListItem item = mFilteredItems.get(position);
            return mFilteredChapters.indexOf(item.chapterSlug);
        } else {
            return -1;
        }
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
    private static class ChunkListItem extends ListItem {
        private boolean isTargetCardOpen = false;
        public ChunkListItem(String chapterSlug, String chunkSlug) {
            super(chapterSlug, chunkSlug);
        }
    }
}
