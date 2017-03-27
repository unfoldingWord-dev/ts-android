package com.door43.translationstudio.ui.translate;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Selection;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.Link;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.FileHistory;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.MergeConflictsHandler;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.tasks.MergeConflictsParseTask;
import com.door43.translationstudio.tasks.CheckForMergeConflictsTask;
import com.door43.translationstudio.ui.translate.review.OnResourceClickListener;
import com.door43.translationstudio.ui.translate.review.OnSourceClickListener;
import com.door43.translationstudio.ui.translate.review.RenderHelpsTask;
import com.door43.translationstudio.ui.translate.review.RenderSourceTask;
import com.door43.translationstudio.ui.translate.review.ReviewHolder;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.ClickableRenderingEngine;
import com.door43.translationstudio.ui.spannables.NoteSpan;
import com.door43.translationstudio.ui.spannables.USFMNoteSpan;
import com.door43.translationstudio.ui.spannables.Span;
import com.door43.translationstudio.ui.spannables.USFMVerseSpan;
import com.door43.translationstudio.ui.spannables.VerseSpan;

import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;
import org.unfoldingword.tools.taskmanager.ThreadableUI;

import com.door43.translationstudio.ui.translate.review.ReviewListItem;
import com.door43.translationstudio.ui.translate.review.SearchSubject;
import com.door43.widget.ViewUtil;

import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unfoldingword.door43client.models.TargetLanguage;

public class ReviewModeAdapter extends ViewModeAdapter<ReviewHolder> implements ManagedTask.OnFinishedListener, OnResourceClickListener, OnSourceClickListener {
    private static final String TAG = ReviewModeAdapter.class.getSimpleName();

    private static final int TAB_NOTES = 0;
    private static final int TAB_WORDS = 1;
    private static final int TAB_QUESTIONS = 2;
    public static final int HIGHLIGHT_COLOR = Color.YELLOW;
    private static final String RENDER_GROUP = "review_mode_render_group";
    private final Door43Client mLibrary;
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_CONFLICT = 1;
    private final Translator mTranslator;
    private final Activity mContext;
    private final TargetTranslation mTargetTranslation;
    private ResourceContainer mSourceContainer;
    private final TargetLanguage mTargetLanguage;
    private List<ListItem> mItems = new ArrayList<>();
    private List<ListItem> mFilteredItems = new ArrayList<>();
    private int mLayoutBuildNumber = 0;
    private boolean mResourcesOpened = false;
    private ContentValues[] mTabs = new ContentValues[0];
    private int[] mOpenResourceTab = new int[0];

    private List<String> mChapters = new ArrayList<>();
    private List<String> mFilteredChapters = new ArrayList<>();
    private CharSequence mSearchText = null;
    private SearchSubject searchSubject = null;

    private Map<String, String[]> mSortedChunks = new HashMap<>();
    private boolean mHaveMergeConflict = false;
    private boolean mMergeConflictFilterEnabled = false;
    private boolean mMergeConflictFilterOn = false;
    private int mChunkSearchMatchesCounter = 0;
    private int mSearchPosition = 0;
    private int mSearchSubPositionItems = 0;
    private boolean mSearchingTarget = true;
    private boolean mLastSearchDirectionForward = true;
    private int mNumberOfChunkMatches = -1;
    private boolean mAtSearchEnd = true;
    private boolean mAtSearchStart = true;
    private int mStringSearchTaskID = -1;
    private HashSet<Integer> visiblePositions = new HashSet<>();

    @Override
    public void onNoteClick(TranslationHelp note, int resourceCardWidth) {
        if(getListener() != null) {
            getListener().onTranslationNoteClick(note, resourceCardWidth);
        }
    }

    @Override
    public void onWordClick(String resourceContainerSlug, Link word, int resourceCardWidth) {
        if(getListener() != null) {
            getListener().onTranslationWordClick(resourceContainerSlug, word.chapter, resourceCardWidth);
        }
    }

    @Override
    public void onQuestionClick(TranslationHelp question, int resourceCardWidth) {
        if(getListener() != null) {
            getListener().onTranslationQuestionClick(question, resourceCardWidth);
        }
    }

    @Override
    public void onResourceTabNotesSelected(ReviewHolder holder, ReviewListItem item) {
        int position = mFilteredItems.indexOf(item);
        mOpenResourceTab[position] = TAB_NOTES;
        holder.showNotes(mSourceContainer.language);
    }

    @Override
    public void onResourceTabWordsSelected(ReviewHolder holder, ReviewListItem item) {
        int position = mFilteredItems.indexOf(item);
        mOpenResourceTab[position] = TAB_WORDS;
        holder.showWords(mSourceContainer.language);
    }

    @Override
    public void onResourceTabQuestionsSelected(ReviewHolder holder, ReviewListItem item) {
        int position = mFilteredItems.indexOf(item);
        mOpenResourceTab[position] = TAB_QUESTIONS;
        holder.showQuestions(mSourceContainer.language);
    }

    @Override
    public void onSourceTabSelected(String sourceTranslationId) {
        if(getListener() != null) {
            getListener().onSourceTranslationTabClick(sourceTranslationId);
        }
    }

    @Override
    public void onChooseSourceButtonSelected() {
        if(getListener() != null) {
            getListener().onNewSourceTranslationTabClick();
        }
    }

    @Override
    public void onTapResourceCard() {
        if(!mResourcesOpened) openResources();
    }


    public ReviewModeAdapter(Activity context, String targetTranslationSlug, String startingChapterSlug, String startingChunkSlug, boolean openResources) {
        this.startingChapterSlug = startingChapterSlug;
        this.startingChunkSlug = startingChunkSlug;

        TaskManager.killGroup(RENDER_GROUP);

        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationSlug);
        mTargetLanguage = App.languageFromTargetTranslation(mTargetTranslation);
        mResourcesOpened = openResources;
    }

    @Override
    void setSourceContainer(ResourceContainer sourceContainer) {
        // TRICKY: if there is no change don't do anything
        if(sourceContainer == null && mSourceContainer == null) return;

        TaskManager.killGroup(RENDER_GROUP);

        mSourceContainer = sourceContainer;
        mLayoutBuildNumber++; // force resetting of fonts

        mChapters = new ArrayList<>();
        mItems = new ArrayList<>();
        initializeListItems(mItems, mChapters, mSourceContainer);

        // Prompt for different source if this one is empty
        if(mSourceContainer != null && mItems.size() == 0) {
            getListener().onNewSourceTranslationTabClick();
        }

        mFilteredItems = mItems;
        mFilteredChapters = mChapters;
        mOpenResourceTab = new int[mItems.size()];

        loadTabInfo();

        filter(mSearchText, searchSubject, mSearchPosition);

        triggerNotifyDataSetChanged();
        updateMergeConflict();
    }

    @Override
    public ListItem createListItem(String chapterSlug, String chunkSlug) {
        return new ReviewListItem(chapterSlug, chunkSlug);
    }

    /**
     * check all cards for merge conflicts to see if we should show warning.  Runs as background task.
     */
    private void updateMergeConflict() {
        doCheckForMergeConflictTask(mItems, mSourceContainer, mTargetTranslation);
    }

    /**
     * Rebuilds the card tabs
     */
    private void loadTabInfo() {
        List<ContentValues> tabContents = new ArrayList<>();
        String[] sourceTranslationIds = App.getOpenSourceTranslations(mTargetTranslation.getId());
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
    void onCoordinate(final ReviewHolder holder) {
        holder.showResourceCard(mResourcesOpened, true);
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
            ReviewListItem item = (ReviewListItem) mFilteredItems.get(i);
            if(item.chapterSlug.equals(chapterSlug) && item.chunkSlug.equals(chunkSlug)) {
                return i;
            }
        }
        return -1;
    }

    public ListItem getItem(int position) {
        if(position >= 0 && position < mFilteredItems.size()) {
            return mFilteredItems.get(position);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        ListItem item = getItem( position );
        if(item != null) {
            // fetch translation from disk
            item.load(mSourceContainer, mTargetTranslation);
            boolean conflicted = item.hasMergeConflicts;
            if(conflicted) {
                showMergeConflictIcon(true, mMergeConflictFilterEnabled);
                return VIEW_TYPE_CONFLICT;
            }
        }
        return VIEW_TYPE_NORMAL;
    }

    @Override
    public ReviewHolder onCreateManagedViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case VIEW_TYPE_CONFLICT:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_review_list_item_merge_conflict, parent, false);
                break;
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_review_list_item, parent, false);
                break;
        }
        ReviewHolder vh = new ReviewHolder(parent.getContext(), v);
        vh.setOnClickListener(this);
        return vh;
    }

    /**
     * Perform task garbage collection
     * @param range the position that left the screen
     */
    @Override
    protected void onVisiblePositionsChanged(int[] range) {
        // constrain the upper bound
        if(range[1] >= mFilteredItems.size()) range[1] = mFilteredItems.size() - 1;
        if(range[0] >= mFilteredItems.size()) range[0] = mFilteredItems.size() - 1;

        HashSet<Integer> visible = new HashSet<>();
        // record visible positions;
        for(int i=range[0]; i<range[1]; i++) {
            visible.add(i);
        }
        // notify not-visible
        this.visiblePositions.removeAll(visible);
        for (Integer i:this.visiblePositions) {
            runTaskGarbageCollection(i);
        }

        this.visiblePositions = visible;
    }

    /**
     * Performs garbage collection on tasks performed from the position.
     * @param position the position that will be garbage collected
     */
    private void runTaskGarbageCollection(int position) {
        if(position >=0 && position < mFilteredItems.size()) {
            ListItem item = mFilteredItems.get(position);

            // source
            String sourceTag = RenderSourceTask.makeTag(item.chapterSlug, item.chunkSlug);
            ManagedTask sourceTask = TaskManager.getTask(sourceTag);
            if (sourceTask != null) {
                Logger.i(TAG, "Garbage collecting task: " + sourceTag);
                TaskManager.cancelTask(sourceTask);
                sourceTask.destroy();
                TaskManager.clearTask(sourceTask);
            }

            // helps
            String helpsTag = RenderHelpsTask.makeTag(item.chapterSlug, item.chunkSlug);
            ManagedTask helpsTask = TaskManager.getTask(helpsTag);
            if (helpsTask != null) {
                Logger.i(TAG, "Garbage collecting task: " + helpsTag);
                TaskManager.cancelTask(helpsTask);
                helpsTask.destroy();
                TaskManager.clearTask(helpsTask);
            }
        }
    }

     @Override
    public void onBindManagedViewHolder(final ReviewHolder holder, final int position) {
         final ReviewListItem item = (ReviewListItem) mFilteredItems.get(position);
         holder.currentItem = item;
         holder.showResourceCard(mResourcesOpened);

        // fetch translation from disk
        item.load(mSourceContainer, mTargetTranslation);

        ViewUtil.makeLinksClickable(holder.mSourceBody);

        // render the cards
        renderSourceCard(item, holder);
         if(getItemViewType(position) == VIEW_TYPE_CONFLICT) {
             renderConflictingTargetCard(position, item, holder);
         } else {
             renderTargetCard(position, item, holder);
         }
        renderResourceCard(item, holder);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.format(mContext, TranslationType.SOURCE, holder.mSourceBody, mSourceContainer.language.slug, mSourceContainer.language.direction);
            if(!item.hasMergeConflicts) {
                Typography.format(mContext, TranslationType.TARGET, holder.mTargetBody, mTargetLanguage.slug, mTargetLanguage.direction);
                Typography.format(mContext, TranslationType.TARGET, holder.mTargetEditableBody, mTargetLanguage.slug, mTargetLanguage.direction);
            } else {
                Typography.formatSub(mContext, TranslationType.TARGET, holder.mConflictText, mTargetLanguage.slug, mTargetLanguage.direction);
            }
        }
    }

    private void renderSourceCard(final ReviewListItem item, final ReviewHolder holder) {
        String tag = RenderSourceTask.makeTag(item.chapterSlug, item.chunkSlug);
        RenderSourceTask task = (RenderSourceTask) TaskManager.getTask(tag);

        // re-purpose task
        if(task != null && task.interrupted()) {
            task.destroy();
            TaskManager.clearTask(task);
            Logger.i(TAG, "Re-starting task: " + task.getTaskId());
            task = null;
        }

        // schedule rendering
        if(task == null && item.renderedSourceText == null) {
            holder.showLoadingSource();
            task = new RenderSourceTask(item, this, mSearchText, searchSubject);
            task.addOnFinishedListener(this);
            TaskManager.addTask(task, tag);
            TaskManager.groupTask(task, RENDER_GROUP);
        } else if(item.renderedSourceText != null) {
            // show cached render
            holder.setSource(item.renderedSourceText);
        }

        holder.renderSourceTabs(mTabs);
    }

    /**
     * Renders a target card that has merge conflicts
     * @param position
     * @param item
     * @param holder
     */
    private void renderConflictingTargetCard(int position, final ReviewListItem item, final ReviewHolder holder) {
        // render title
        holder.mTargetTitle.setText(item.getTargetTitle());

        if(holder.mMergeConflictLayout == null) { // sanity check
            return;
        }

        item.mergeItemSelected = -1;

        MergeConflictsParseTask parseTask = new MergeConflictsParseTask(item.targetText);
        parseTask.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
            @Override
            public void onTaskFinished(final ManagedTask task) {
                TaskManager.clearTask(task);

                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        holder.displayMergeConflictsOnTargetCard(mSourceContainer.language, (MergeConflictsParseTask) task, item);
                    }
                });
            }
        });
        TaskManager.addTask(parseTask);

        holder.mConflictText.setVisibility(View.VISIBLE);
        holder.mButtonBar.setVisibility(View.GONE);

        holder.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.mergeItemSelected = -1;
                holder.displayMergeConflictSelectionState(item);
            }
        });

        holder.mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if((item.mergeItemSelected >= 0) && (item.mergeItemSelected < item.mergeItems.size()) ) {
                    CharSequence selectedText = item.mergeItems.get(item.mergeItemSelected);
                    applyNewCompiledText(selectedText.toString(), holder, item);
                    reOpenItem(item);
                    item.hasMergeConflicts = MergeConflictsHandler.isMergeConflicted(selectedText);
                    item.mergeItemSelected = -1;
                    item.isEditing = false;
                    updateMergeConflict();
                }
            }
        });

        holder.mUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoTextInTarget(holder, item);
            }
        });
        holder.mRedoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redoTextInTarget(holder, item);
            }
        });

        holder.rebuildControls();
        holder.mUndoButton.setVisibility(View.GONE);
        holder.mRedoButton.setVisibility(View.GONE);
    }

    /**
     * Renders a normal target card
     * @param position
     * @param item
     * @param holder
     */
    private void renderTargetCard(final int position, final ReviewListItem item, final ReviewHolder holder) {
        // remove old text watcher
        if(holder.mEditableTextWatcher != null) {
            holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
        }

        // insert rendered text
        if(item.isEditing) {
            // editing mode
            holder.mTargetEditableBody.setText(item.renderedTargetText);
        } else {
            // verse marker mode
            holder.mTargetBody.setText(item.renderedTargetText);
            holder.mTargetBody.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    v.onTouchEvent(event);
                    v.clearFocus();
                    return true;
                }
            });
            ViewUtil.makeLinksClickable(holder.mTargetBody);
        }

        // title
        holder.mTargetTitle.setText(item.getTargetTitle());

        // set up text watcher
        holder.mEditableTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String translation = applyChangedText(s, holder, item);

                // commit immediately if editing history
                FileHistory history = item.getFileHistory();
                if(!history.isAtHead()) {
                    history.reset();
                    holder.rebuildControls();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        // render target body
        ManagedTask oldtask = TaskManager.getTask(item.currentTargetTaskId);
//        Log.i(TAG, "renderTargetCard(): Position " + position + ": Cancelling ID: " + item.currentTargetTaskId);
        TaskManager.cancelTask(oldtask);
        TaskManager.clearTask(oldtask);
        if(item.renderedTargetText == null) {
            holder.mTargetEditableBody.setText(item.targetText);
            holder.mTargetEditableBody.setVisibility(View.INVISIBLE);
            holder.mTargetBody.setText(item.targetText);
            holder.mTargetBody.setVisibility(View.INVISIBLE);
            ManagedTask task = new ManagedTask() {
                @Override
                public void start() {
                    setThreadPriority(Thread.MIN_PRIORITY);
                    if(isCanceled()) {
//                        Log.i(TAG, "renderTargetCard(): Position " + position + ": Render cancelled ID: " + item.currentTargetTaskId);
                        return;
                    }
//                    Log.i(TAG, "renderTargetCard(): Position " + position + ": Render started ID: " + item.currentTargetTaskId);
                    CharSequence text;
                    if(item.isComplete || item.isEditing) {
                        text = renderSourceText(item.targetText, item.targetTranslationFormat, holder, item, true);
                    } else {
                        text = renderTargetText(item.targetText, item.targetTranslationFormat, item.ft, holder, item);
                    }
                    setResult(text);
                }
            };
            task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
                @Override
                public void onTaskFinished(final ManagedTask task) {
                    TaskManager.clearTask(task);
                    final CharSequence data = (CharSequence)task.getResult();
                    if(!task.isCanceled() && data != null && item == holder.currentItem) {

                        Handler hand = new Handler(Looper.getMainLooper());
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
//                                Log.i(TAG, "renderTargetCard(): Position " + position + ": Render finished ID: " + item.currentTargetTaskId);
                                if (!task.isCanceled() && data != null && item == holder.currentItem) {
                                    item.renderedTargetText = data;

                                    int selectPosition = checkForSelectedSearchItem(item, position, true);
                                    if (item.isEditing) {
                                        // edit mode
                                        holder.mTargetEditableBody.setText(item.renderedTargetText);
                                        selectCurrentSearchItem(position, selectPosition, holder.mTargetEditableBody);
                                        holder.mTargetEditableBody.setVisibility(View.VISIBLE);
                                        holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                                    } else {
                                        // verse marker mode
                                        holder.mTargetBody.setText(item.renderedTargetText);
                                        selectCurrentSearchItem(position, selectPosition, holder.mTargetBody);
                                        holder.mTargetBody.setVisibility(View.VISIBLE);
                                        holder.mTargetBody.setOnTouchListener(new View.OnTouchListener() {
                                            @Override
                                            public boolean onTouch(View v, MotionEvent event) {
                                                v.onTouchEvent(event);
                                                v.clearFocus();
                                                return true;
                                            }
                                        });
                                        setFinishedMode(item, holder);
                                        ViewUtil.makeLinksClickable(holder.mTargetBody);
                                    }
                                    addMissingVerses(item, holder);
                                } else {
//                                    Log.i(TAG, "renderTargetCard(): Position " + position + ": ID: " + item.currentTargetTaskId + ": Render failed after delay: task.isCanceled()=" + task.isCanceled() + ", (data==null)=" + (data == null) + ", (item!=holder.currentItem)=" + (item != holder.currentItem));
                                }
                            }
                        });
                    }  else {
//                        Log.i(TAG, "renderTargetCard(): Position " + position + ": ID: " + item.currentTargetTaskId + ": Render failed  no delay: task.isCanceled()=" + task.isCanceled() + ", (data==null)=" + (data == null) + ", (item!=holder.currentItem)=" + (item != holder.currentItem));
                    }
                }
            });
            item.currentTargetTaskId = TaskManager.addTask(task);
//            Log.i(TAG, "renderTargetCard(): Position " + position + ": Adding task ID: " + item.currentTargetTaskId);

            ManagedTask verifiedTask = TaskManager.getTask(item.currentTargetTaskId); // verify task in queue
            if((verifiedTask == null) || (verifiedTask != task) || (verifiedTask.interrupted())) {
                if(verifiedTask == null) {
//                    Logger.e(TAG, "renderTargetCard(): Position " + position + ": Add task failed, verify task null,  ID: " + item.currentTargetTaskId);
                } else {
//                    Logger.e(TAG, "renderTargetCard(): Position " + position + ": Add task failed ID: " + item.currentTargetTaskId + ": (verifiedTask != task)=" + (verifiedTask != task) + ", verifiedTask.interrupted()=" + verifiedTask.interrupted());
                }
            }
        } else if(item.isEditing) {
            // editing mode
            holder.mTargetEditableBody.setText(item.renderedTargetText);
            holder.mTargetEditableBody.setVisibility(View.VISIBLE);

            if(item.refreshSearchHighlightTarget) {
                int selectPosition = checkForSelectedSearchItem(item, position, true);
                selectCurrentSearchItem(position, selectPosition, holder.mTargetEditableBody);
                item.refreshSearchHighlightTarget = false;
            }

            holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
        } else {
            // verse marker mode
            holder.mTargetBody.setText(item.renderedTargetText);
            holder.mTargetBody.setVisibility(View.VISIBLE);

            if(item.refreshSearchHighlightTarget) {
                int selectPosition = checkForSelectedSearchItem(item, position, true);
                selectCurrentSearchItem(position, selectPosition, holder.mTargetBody);
                item.refreshSearchHighlightTarget = false;
            }

            holder.mTargetBody.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    v.onTouchEvent(event);
                    v.clearFocus();
                    return true;
                }
            });
            ViewUtil.makeLinksClickable(holder.mTargetBody);
        }

        holder.mUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoTextInTarget(holder, item);
            }
        });
        holder.mRedoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redoTextInTarget(holder, item);
            }
        });

        // editing button
        final GestureDetector detector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                item.isEditing = !item.isEditing;
                holder.rebuildControls();

                if(item.isEditing) {
                    holder.mTargetEditableBody.requestFocus();
                    InputMethodManager mgr = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(holder.mTargetEditableBody, InputMethodManager.SHOW_IMPLICIT);

                    // TRICKY: there may be changes to translation
                    item.loadTarget(mTargetTranslation);

                    // re-render for editing mode
                    item.renderedTargetText = renderSourceText(item.targetText, item.targetTranslationFormat, holder, item, true);
                    holder.mTargetEditableBody.setText(item.renderedTargetText);
                    holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                    addMissingVerses(item, holder);
                } else {
                    if(holder.mEditableTextWatcher != null) {
                        holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                    }
                    holder.mTargetBody.requestFocus();
                    getListener().closeKeyboard();

                    // TODO: 2/16/17 save translation

                    // TRICKY: there may be changes to translation
                    item.loadTarget(mTargetTranslation);

                    // re-render for verse mode
                    item.renderedTargetText = renderTargetText(item.targetText, item.targetTranslationFormat, item.ft, holder, item);
                    holder.mTargetBody.setText(item.renderedTargetText);
                    addMissingVerses(item, holder);
                }
                return true;
            }
        });

        holder.mEditButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });

        holder.mAddNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFootnoteAtSelection(holder, item);
            }
        });

        holder.rebuildControls();

        // disable listener
        holder.mDoneSwitch.setOnCheckedChangeListener(null);

        // display as finished
        setFinishedMode(item, holder);

        // done buttons
        holder.mDoneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(item.isEditing) {
                        // make sure to capture verse marker changes changes before dialog is displayed
                        Editable changes = holder.mTargetEditableBody.getText();
                        item.renderedTargetText = changes;
                        String newBody = Translator.compileTranslation(changes);
                        item.targetText = newBody;
                    }

                    new AlertDialog.Builder(mContext,R.style.AppTheme_Dialog)
                            .setTitle(R.string.chunk_checklist_title)
                            .setMessage(Html.fromHtml(mContext.getString(R.string.chunk_checklist_body)))
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                        boolean success = onConfirmChunk(item, item.chapterSlug, item.chunkSlug, mTargetTranslation.getFormat());
                                        holder.mDoneSwitch.setChecked(success);
                                    }
                                }
                            )
                            .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    holder.mDoneSwitch.setChecked(false); // force back off if not accepted
                                }
                            })
                            .show();

                } else { // done button checked off
                    reOpenItem(item);
                }
            }
        });
    }

    /**
     * if missing verses were found during render, then add them
     * @param item
     * @param holder
     * @return - returns true if missing verses were applied
     */
    private boolean addMissingVerses(ReviewListItem item, ReviewHolder holder) {
        if(item.hasMissingVerses && !item.isComplete) {
            Log.i(TAG, "Adding Missing verses to: " + item.targetText);
            if ((item.targetText != null) && !item.targetText.isEmpty()) {
                String translation = applyChangedText(item.renderedTargetText, holder, item);
                Log.i(TAG, "Added Missing verses: " + translation);
                item.hasMissingVerses = false;
                item.renderedTargetText = null; // force rerendering of target text
                triggerNotifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * check if we have a selected search item in this chunk, returns position if found, -1 if not found
     * @param item
     * @param position
     * @param target
     * @return
     */
    private int checkForSelectedSearchItem(ReviewListItem item, int position, boolean target) {
        int selectPosition = -1;
        if(item.hasSearchText && (position == mSearchPosition)) {
            if (mSearchSubPositionItems < 0) { // if we haven't counted items yet
                findSearchItemInChunkAndPreselect(mLastSearchDirectionForward, item, target);
                Log.i(TAG, "Rerendering, Found search items in chunk " + position + ": " + mSearchSubPositionItems);
            } else if (mSearchSubPositionItems > 0) { // if we have counted items then find the number selected
                int searchSubPosition = 0;
                MatchResults results = getMatchItemN( item, mSearchText, searchSubPosition, target);
                if( (results != null) && (results.foundLocation >= 0)) {
                    Log.i(TAG, "Highlight at position: " + position + " : " + results.foundLocation);
                    selectPosition = results.foundLocation;
                } else {
                    Log.i(TAG, "Highlight failed for position: " + position + "; chunk position: " + searchSubPosition + "; chunk count: " + mSearchSubPositionItems);
                }
                checkIfAtSearchLimits();
            }
        }
        return selectPosition;
    }

    /**
     * highlight the current selected search text item at position
     * @param position - list item position
     * @param selectPosition
     * @param view
     */
    private void selectCurrentSearchItem(final int position, int selectPosition, TextView view) {
        if(selectPosition >= 0) {

            Layout layout = view.getLayout();
            if(layout != null) {
                int lineNumberForLocation = layout.getLineForOffset(selectPosition);
                int baseline = layout.getLineBaseline(lineNumberForLocation);
                int ascent = layout.getLineAscent(lineNumberForLocation);

                final int verticalOffset = baseline + ascent;
                Log.i(TAG, "set position for " + selectPosition + ", scroll to y=" + verticalOffset);

                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "selectCurrentSearchItem position= " + position + ", offset=" +(-verticalOffset));
                        onSetSelectedPosition(position, -verticalOffset);
                    }
                });
            } else {
                Logger.e(TAG, "cannot get layout for position: " + position);
            }
        }
    }

    /**
     * set the UI to reflect the finished mode
     * @param item
     * @param holder
     */
    private void setFinishedMode(ReviewListItem item, ReviewHolder holder) {
        if(item.isComplete) {
            holder.mEditButton.setVisibility(View.GONE);
            holder.mUndoButton.setVisibility(View.GONE);
            holder.mRedoButton.setVisibility(View.GONE);
            holder.mAddNoteButton.setVisibility(View.GONE);
            holder.mDoneSwitch.setChecked(true);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        } else {
            holder.mEditButton.setVisibility(View.VISIBLE);
            holder.mDoneSwitch.setChecked(false);
        }
    }

    /**
     * mark item as not done
     * @param item
     */
    private void reOpenItem(ListItem item) {
        boolean opened;
        if (item.isChapterReference()) {
            opened = mTargetTranslation.reopenChapterReference(item.chapterSlug);
        } else if (item.isChapterTitle()) {
            opened = mTargetTranslation.reopenChapterTitle(item.chapterSlug);
        } else if (item.isProjectTitle()) {
            opened = mTargetTranslation.openProjectTitle();
        } else {
            opened = mTargetTranslation.reopenFrame(item.chapterSlug, item.chunkSlug);
        }
        if (opened) {
            item.renderedTargetText = null;
            item.isComplete = false;
            triggerNotifyDataSetChanged();
        } else {
            // TODO: 10/27/2015 notify user the frame could not be completed.
        }
    }

    /**
     * create a new footnote at selected position in target text.  Displays an edit dialog to enter footnote data.
     * @param holder
     * @param item
     */
    private void createFootnoteAtSelection(final ReviewHolder holder, final ReviewListItem item) {
        final EditText editText = holder.getEditText();
        int endPos = editText.getSelectionEnd();
        if (endPos < 0) {
            endPos = 0;
        }
        final int insertPos = endPos;
        editFootnote("", holder, item, insertPos, insertPos);
    }

    /**
     * edit contents of footnote at specified position
     * @param initialNote
     * @param holder
     * @param item
     * @param footnotePos
     * @param footnoteEndPos
     */
    private void editFootnote(CharSequence initialNote, final ReviewHolder holder, final ReviewListItem item, final int footnotePos, final int footnoteEndPos ) {
        final EditText editText = holder.getEditText();
        final CharSequence original = editText.getText();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View footnoteFragment = inflater.inflate(R.layout.fragment_footnote_prompt, null);
        if(footnoteFragment != null) {
            final EditText footnoteText = (EditText) footnoteFragment.findViewById(R.id.footnote_text);
            if ((footnoteText != null)) {
                footnoteText.setText(initialNote);

                // pop up note prompt
                new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                        .setTitle(R.string.title_add_footnote)
                        .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                CharSequence footnote = footnoteText.getText();
                                boolean validated = verifyAndReplaceFootnote(footnote, original, footnotePos, footnoteEndPos, holder, item, editText);
                                if(validated) {
                                    dialog.dismiss();
                                }
                            }
                        })
                        .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                               dialog.dismiss();
                            }
                        })
                        .setView(footnoteFragment)
                        .show();

            }
        }
    }

    /**
     * insert footnote into EditText or remove footnote from EditText if both footnote and
     *      footnoteTitleText are null
     * @param footnote
     * @param original
     * @param insertPos
     * @param insertEndPos
     * @param item
     * @param editText
     */
    private boolean verifyAndReplaceFootnote(CharSequence footnote, CharSequence original, int insertPos, final int insertEndPos, final ReviewHolder holder, final ReviewListItem item, EditText editText) {
        // sanity checks
        if ((null == footnote) || (footnote.length() <= 0)) {
            warnDialog(R.string.title_footnote_invalid, R.string.footnote_message_empty);
            return false;
        }

        placeFootnote(footnote, original, insertPos, insertEndPos, holder, item, editText);
        return true;
    }

    /**
     * display warning dialog
     * @param titleID
     * @param messageID
     */
    private void warnDialog(int titleID, int messageID) {
        new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
            .setTitle(titleID)
            .setMessage(messageID)
            .setPositiveButton(R.string.dismiss, null)
            .show();
    }

    /**
     * insert footnote into EditText or remove footnote from EditText if both footnote and
     *      footnoteTitleText are null
     * @param footnote
     * @param original
     * @param start
     * @param end
     * @param item
     * @param editText
     */
    private void placeFootnote(CharSequence footnote, CharSequence original, int start, final int end, final ReviewHolder holder, final ReviewListItem item, EditText editText) {
        CharSequence footnotecode = "";
        if(footnote != null) {
            // sanity checks
            if ((null == footnote) || (footnote.length() <= 0)) {
                footnote = mContext.getResources().getString(R.string.footnote_label);
            }

            USFMNoteSpan footnoteSpannable = USFMNoteSpan.generateFootnote(footnote);
            footnotecode = footnoteSpannable.getMachineReadable();
        }

        CharSequence newText = TextUtils.concat(original.subSequence(0, start), footnotecode, original.subSequence(end, original.length()));
        editText.setText(newText);

        item.renderedTargetText = newText;
        item.targetText = Translator.compileTranslation(editText.getText()); // get XML for footnote
        mTargetTranslation.applyFrameTranslation(item.ft, item.targetText); // save change

//        String frame = null;
//        if(item.isFrame()) {
//            frame  = loadFrame(item.chapterSlug, item.chunkSlug);
//        }

        // generate spannable again adding
        if(item.isComplete || item.isEditing) {
            item.renderedTargetText = renderSourceText(item.targetText, item.targetTranslationFormat, holder, (ReviewListItem) item, true);
        } else {
            item.renderedTargetText = renderTargetText(item.targetText, item.targetTranslationFormat, item.ft, holder, (ReviewListItem) item);
        }
        editText.setText(item.renderedTargetText);
        editText.setSelection(editText.length(), editText.length());
    }

    /**
     * save changed text to item,  first see if it needs to be compiled
     * @param s A string or editable
     * @param item
     * @param holder
     * @param item
     * * @return
     */
    private String applyChangedText(CharSequence s, ReviewHolder holder, ReviewListItem item) {
        String translation;
        if (s == null) {
            return null;
        } else if(s instanceof Editable) {
            translation = Translator.compileTranslation((Editable) s);
        } else if(s instanceof SpannedString) {
            translation = Translator.compileTranslationSpanned((SpannedString) s);
        } else {
            translation = s.toString();
        }

        applyNewCompiledText(translation, holder, item);
        return translation;
    }

    /**
     *  save new text to item
     * @param translation
     * @param holder
     * @param item
     */
    private void applyNewCompiledText(String translation, ReviewHolder holder, ListItem item) {
        item.targetText = translation;
        if (item.isChapterReference()) {
            mTargetTranslation.applyChapterReferenceTranslation(item.ct, translation);
        } else if (item.isChapterTitle()) {
            mTargetTranslation.applyChapterTitleTranslation(item.ct, translation);
        } else if (item.isProjectTitle()) {
            try {
                mTargetTranslation.applyProjectTitleTranslation(translation);
            } catch (IOException e) {
                Logger.e(ReviewModeAdapter.class.getName(), "Failed to save the project title translation", e);
            }
        } else if (item.isChunk()) {
            mTargetTranslation.applyFrameTranslation(item.ft, translation);
        }

        item.renderedTargetText = renderSourceText(translation, item.targetTranslationFormat, holder, (ReviewListItem) item, true);
    }

    /**
     * restore the text from previous commit for fragment
     * @param holder
     * @param item
     */
    private void undoTextInTarget(final ReviewHolder holder, final ReviewListItem item) {
        holder.mUndoButton.setVisibility(View.INVISIBLE);
        holder.mRedoButton.setVisibility(View.INVISIBLE);

        final FileHistory history = item.getFileHistory();
        ThreadableUI thread = new ThreadableUI(mContext) {
            RevCommit commit = null;
            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                // commit changes before viewing history
                if(history.isAtHead()) {
                    if(!mTargetTranslation.isClean()) {
                        try {
                            mTargetTranslation.commitSync();
                            history.loadCommits();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                // get previous
                commit = history.previous();
            }

            @Override
            public void onPostExecute() {
                if(commit != null) {
                    String text = null;
                    try {
                        text = history.read(commit);
                    } catch (IllegalStateException e) {
                        Logger.w(TAG,"Undo is past end of history for specific file", e);
                        text = ""; // graceful recovery
                    }catch (Exception e) {
                        Logger.w(TAG,"Undo Read Exception", e);
                    }

                    // save and update ui
                    if (text != null) {
                        // TRICKY: prevent history from getting rolled back soon after the user views it
                        restartAutoCommitTimer();
                        applyChangedText(text, holder, item);

                        App.closeKeyboard(mContext);
                        item.hasMergeConflicts = MergeConflictsHandler.isMergeConflicted(text);
                        triggerNotifyDataSetChanged();
                        updateMergeConflict();

                        if(holder.mTargetEditableBody != null) {
                            holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                            holder.mTargetEditableBody.setText(item.renderedTargetText);
                            holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                        }
                    }
                }

                if(history.hasNext()) {
                    holder.mRedoButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mRedoButton.setVisibility(View.GONE);
                }
                if(history.hasPrevious()) {
                    holder.mUndoButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mUndoButton.setVisibility(View.GONE);
                }
            }
        };
        thread.start();
    }

    /**
     * restore the text from later commit for fragment
     * @param holder
     * @param item
     */
    private void redoTextInTarget(final ReviewHolder holder, final ReviewListItem item) {
        holder.mUndoButton.setVisibility(View.INVISIBLE);
        holder.mRedoButton.setVisibility(View.INVISIBLE);

        final FileHistory history = item.getFileHistory();
        ThreadableUI thread = new ThreadableUI(mContext) {
            RevCommit commit = null;
            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                commit = history.next();
            }

            @Override
            public void onPostExecute() {
                if(commit != null) {
                    String text = null;
                    try {
                        text = history.read(commit);
                    } catch (IllegalStateException e) {
                        Logger.w(TAG,"Redo is past end of history for specific file", e);
                        text = ""; // graceful recovery
                    }catch (Exception e) {
                        Logger.w(TAG,"Redo Read Exception", e);
                    }

                    // save and update ui
                    if (text != null) {
                        // TRICKY: prevent history from getting rolled back soon after the user views it
                        restartAutoCommitTimer();
                        applyChangedText(text, holder, item);

                        App.closeKeyboard(mContext);
                        item.hasMergeConflicts = MergeConflictsHandler.isMergeConflicted(text);
                        triggerNotifyDataSetChanged();
                        updateMergeConflict();

                        if(holder.mTargetEditableBody != null) {
                            holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                            holder.mTargetEditableBody.setText(item.renderedTargetText);
                            holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                        }
                    }
                }

                if(history.hasNext()) {
                    holder.mRedoButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mRedoButton.setVisibility(View.GONE);
                }
                if(history.hasPrevious()) {
                    holder.mUndoButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mUndoButton.setVisibility(View.GONE);
                }
            }
        };
        thread.start();
    }

    private static final Pattern USFM_CONSECUTIVE_VERSE_MARKERS =
            Pattern.compile("\\\\v\\s(\\d+(-\\d+)?)\\s*\\\\v\\s(\\d+(-\\d+)?)");

    private static final Pattern USFM_VERSE_MARKER =
            Pattern.compile(USFMVerseSpan.PATTERN);

    private static final Pattern CONSECUTIVE_VERSE_MARKERS =
            Pattern.compile("(<verse [^>]+/>\\s*){2}");

    private static final Pattern VERSE_MARKER =
            Pattern.compile("<verse\\s+number=\"(\\d+)\"[^>]*>");

    /**
     * Performs some validation, and commits changes if ready.
     * @return true if the section was successfully confirmed; otherwise false.
     */
    private boolean onConfirmChunk(final ReviewListItem item, final String chapter, final String frame, TranslationFormat format) {
        boolean success = true; // So far, so good.

        // Check for empty translation.
        if (item.targetText.isEmpty()) {
            Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.translate_first, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
            snack.show();
            success = false;
        }

//        if(frame != null) {
            Matcher matcher;
            int lowVerse = -1;
            int highVerse = 999999999;
            int[] range = Frame.getVerseRange(item.targetText, item.targetTranslationFormat);
            if ((range != null) && (range.length > 0)) {
                lowVerse = range[0];
                highVerse = lowVerse;
                if (range.length > 1) {
                    highVerse = range[1];
                }
            }

            // Check for contiguous verse numbers.
            if (success) {
                if (format == TranslationFormat.USFM) {
                    matcher = USFM_CONSECUTIVE_VERSE_MARKERS.matcher(item.targetText);
                } else {
                    matcher = CONSECUTIVE_VERSE_MARKERS.matcher(item.targetText);
                }
                if (matcher.find()) {
                    Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.consecutive_verse_markers, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                    snack.show();
                    success = false;
                }
            }

            // Check for out-of-order verse markers.
            if (success) {
                int error = 0;
                if (format == TranslationFormat.USFM) {
                    matcher = USFM_VERSE_MARKER.matcher(item.targetText);
                } else {
                    matcher = VERSE_MARKER.matcher(item.targetText);
                }
                int lastVerseSeen = 0;
                while (matcher.find()) {
                    int currentVerse = Integer.valueOf(matcher.group(1));
                    if (currentVerse <= lastVerseSeen) {
                        if (currentVerse == lastVerseSeen) {
                            error = R.string.duplicate_verse_marker;
                            success = false;
                            break;
                        } else {
                            error = R.string.outoforder_verse_markers;
                            success = false;
                            break;
                        }
                    } else if ((currentVerse < lowVerse) || (currentVerse > highVerse)) {
                        error = R.string.outofrange_verse_marker;
                        success = false;
                        break;
                    } else {
                        lastVerseSeen = currentVerse;
                    }
                }
                if (!success) {
                    Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), error, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
//        }

        // Everything looks good so far. Try and commit.
        if (success) {
            if (item.isChapterReference()) {
                success = mTargetTranslation.finishChapterReference(item.chapterSlug);
            } else if (item.isChapterTitle()) {
                success = mTargetTranslation.finishChapterTitle(item.chapterSlug);
            } else if (item.isProjectTitle()) {
                success = mTargetTranslation.closeProjectTitle();
            } else {
                success = mTargetTranslation.finishFrame(item.chapterSlug, item.chunkSlug);
            }

            if (!success) {
                // TODO: Use a more accurate (if potentially more opaque) error message.
                Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.failed_to_commit_chunk, Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                snack.show();
            } else {
                item.isComplete = true;
            }
        }

        // Wrap up.
        if (success) {
            try {
                mTargetTranslation.commit();
            } catch (Exception e) {
                String frameComplexId =  ":" + item.chapterSlug + "-" + item.chunkSlug;
                Logger.e(TAG, "Failed to commit translation of " + mTargetTranslation.getId() + frameComplexId, e);
            }
            item.isEditing = false;
            item.renderedTargetText = null;
            triggerNotifyDataSetChanged();
        }

        return success;
    }


    /**
     * Initiates rendering the resource card
     * @param item
     * @param holder
     */
    private void renderResourceCard(final ReviewListItem item, final ReviewHolder holder) {
        holder.clearResourceCard();

        // skip if chapter title/reference
        if(!item.isChunk() || mSourceContainer.resource.slug.equals("udb")) {
            return;
        }

        String tag = RenderHelpsTask.makeTag(item.chapterSlug, item.chunkSlug);
        RenderHelpsTask task = (RenderHelpsTask) TaskManager.getTask(tag);

        // re-purpose task
        if(task != null && task.interrupted()) {
            task.destroy();
            TaskManager.clearTask(task);
            Logger.i(TAG, "Re-starting task: " + task.getTaskId());
            task = null;
        }

        // schedule rendering
        if(task == null) {
            holder.showLoadingResources();
            task = new RenderHelpsTask(mLibrary, item, mSortedChunks);
            task.addOnFinishedListener(this);
            TaskManager.addTask(task, tag);
            TaskManager.groupTask(task, RENDER_GROUP);
        }
    }

    /**
     * generate spannable for target text.  Will add click listener for notes and verses if they are supported
     * @param text
     * @param format
     * @param frameTranslation
     * @param holder
     * @param item
     * @return
     */
    private CharSequence renderTargetText(String text, TranslationFormat format, final FrameTranslation frameTranslation, final ReviewHolder holder, final ReviewListItem item) {
        RenderingGroup renderingGroup = new RenderingGroup();
        boolean enableSearch = mSearchText != null && searchSubject != null && searchSubject == SearchSubject.TARGET;
        if(Clickables.isClickableFormat(format)) {
            Span.OnClickListener verseClickListener = new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.long_click_to_drag, Snackbar.LENGTH_SHORT);
                    ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                    snack.show();
                    ((EditText) view).setSelection(((EditText) view).getText().length());
                }

                @Override
                public void onLongClick(final View view, Span span, int start, int end) {
                    ClipData dragData = ClipData.newPlainText(item.chapterSlug + "-" + item.chunkSlug, span.getMachineReadable());
                    final VerseSpan pin = ((VerseSpan) span);

                    // create drag shadow
                    LayoutInflater inflater = (LayoutInflater) App.context().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    FrameLayout verseLayout = (FrameLayout)inflater.inflate(R.layout.fragment_verse_marker, null);
                    TextView verseTitle = (TextView)verseLayout.findViewById(R.id.verse);
                    if(pin.getEndVerseNumber() > 0) {
                        verseTitle.setText(pin.getStartVerseNumber() + "-" + pin.getEndVerseNumber());
                    } else {
                        verseTitle.setText(pin.getStartVerseNumber() + "");
                    }
                    Bitmap shadow = ViewUtil.convertToBitmap(verseLayout);
                    View.DragShadowBuilder myShadow = CustomDragShadowBuilder.fromBitmap(mContext, shadow);

                    int[] spanRange = {start, end};
                    view.startDrag(dragData,  // the data to be dragged
                            myShadow,  // the drag shadow builder
                            spanRange,      // no need to use local data
                            0          // flags (not currently used, set to 0)
                    );
                    view.setOnDragListener(new View.OnDragListener() {
                        private boolean hasEntered = false;
                        @Override
                        public boolean onDrag(View v, DragEvent event) {
                            EditText editText = ((EditText) view);
                            // TODO: highlight the drop site.
                            if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                                // delete old span
                                int[] spanRange = (int[])event.getLocalState();
                                if((spanRange != null) && (spanRange.length >= 2) ) {
                                    CharSequence in = editText.getText();
                                    if( (spanRange[0] < in.length()) && spanRange[1] < in.length()) {
                                        CharSequence out = TextUtils.concat(in.subSequence(0, spanRange[0]), in.subSequence(spanRange[1], in.length()));
                                        editText.setText(out);
                                    }
                                }
                            } else if(event.getAction() == DragEvent.ACTION_DROP) {
                                int offset = editText.getOffsetForPosition(event.getX(), event.getY());
                                CharSequence text = editText.getText();

                                offset = closestSpotForVerseMarker(offset, text);

                                if(offset >= 0) {
                                    // insert the verse at the offset
                                    text = TextUtils.concat(text.subSequence(0, offset), pin.toCharSequence(), text.subSequence(offset, text.length()));
                                } else {
                                    // place the verse back at the beginning
                                    text = TextUtils.concat(pin.toCharSequence(), text);
                                }
                                item.renderedTargetText = text;
                                editText.setText(text);
                                String translation = Translator.compileTranslation((Editable)editText.getText());
                                mTargetTranslation.applyFrameTranslation(frameTranslation, translation);

                                // Reload, so that targetText is kept in sync.
                                item.loadTarget(mTargetTranslation);
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                                view.setOnDragListener(null);
                                editText.setSelection(editText.getSelectionEnd());
                                // reset verse if dragged off the view
                                // TODO: 10/5/2015 perhaps we should confirm with the user?
                                if(!hasEntered) {
                                    // place the verse back at the beginning
                                    CharSequence text = editText.getText();
                                    text = TextUtils.concat(pin.toCharSequence(), text);
                                    item.renderedTargetText = text;
                                    editText.setText(text);
                                    String translation = Translator.compileTranslation((Editable)editText.getText());
                                    mTargetTranslation.applyFrameTranslation(frameTranslation, translation);

                                    // Reload, so that targetText is kept in sync.
                                    item.loadTarget(mTargetTranslation);
                                }
                                triggerNotifyDataSetChanged();
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                                hasEntered = true;
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                                hasEntered = false;
                                editText.setSelection(editText.getSelectionEnd());
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                                int offset = editText.getOffsetForPosition(event.getX(), event.getY());
                                if(offset >= 0) {
                                    Selection.setSelection(editText.getText(), offset);
                                } else {
                                    editText.setSelection(editText.getSelectionEnd());
                                }
                            }
                            return true;
                        }
                    });
                }
            };

            Span.OnClickListener noteClickListener = new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    if (span instanceof NoteSpan) {
                        showFootnote(holder, item, (NoteSpan) span, start, end, true);
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            };

            ClickableRenderingEngine renderer = Clickables.setupRenderingGroup(format, renderingGroup, verseClickListener, noteClickListener, true);
            renderer.setLinebreaksEnabled(true);
            renderer.setPopulateVerseMarkers(Frame.getVerseRange(item.sourceText, item.sourceTranslationFormat));
            if(enableSearch) {
                renderingGroup.setSearchString(mSearchText, HIGHLIGHT_COLOR);
            }

        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
            if(enableSearch) {
                renderingGroup.setSearchString(mSearchText, HIGHLIGHT_COLOR);
            }
        }
        if((text != null) && !text.trim().isEmpty()) {
            renderingGroup.init(text);
            CharSequence results = renderingGroup.start();
            item.hasMissingVerses = renderingGroup.isAddedMissingVerse();
            return results;
        } else {
            return "";
        }
    }

    @Override
    public String getVerseChunk(String chapter, String verse) {
        return mapVerseToChunk(chapter, verse, mSortedChunks, mSourceContainer);
    }

    /**
     * Maps a verse to a chunk.
     * The sorted chunks will be cached for better performance
     *
     * @param chapter
     * @param verse
     * @param sortedChunks the chunks that have already been sorted
     * @param source the source of sorted chunks
     * @return
     */
    public static String mapVerseToChunk(String chapter, String verse, Map<String, String[]> sortedChunks, ResourceContainer source) {
        // cache the sorted chunks
        if(!sortedChunks.containsKey(chapter)) {
            try {
                String[] chunks = source.chunks(chapter);
                Arrays.sort(chunks, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        Integer i1;
                        Integer i2;
                        // TRICKY: push strings to top
                        try {
                            i1 = Integer.valueOf(o1);
                        } catch (NumberFormatException e) {
                            return 1;
                        }
                        try {
                            i2 = Integer.valueOf(o2);
                        } catch (NumberFormatException e) {
                            return 1;
                        }
                        return i1.compareTo(i2);
                    }
                });
                sortedChunks.put(chapter, chunks);
            } catch (Exception e) {
                return verse;
            }
        }

        // interpolate the chunk
        return Util.verseToChunk(verse, sortedChunks.get(chapter));
    }

    /**
     * find closest place to drop verse marker.  Weighted toward beginning of word.
     * @param offset - initial drop position
     * @param text - edit text
     * @return
     */
    private int closestSpotForVerseMarker(int offset, CharSequence text) {
        int charsToWhiteSpace = 0;
        for (int j = offset; j >= 0; j--) {
            if(j >= text.length()) j = text.length() - 1;
            char c = text.charAt(j);
            boolean whitespace = isWhitespace(c);
            if(whitespace) {

                if((j == offset) ||  // if this is already a good spot, then done
                    (j == offset - 1)) {
                    return offset;
                }

                charsToWhiteSpace = j - offset + 1;
                break;
            }
        }

        int limit = offset - charsToWhiteSpace - 1;
        if(limit > text.length()) {
            limit = text.length();
        }

        for (int j = offset + 1; j < limit; j++) {
            char c = text.charAt(j);
            boolean whitespace = isWhitespace(c);
            if(whitespace) {
                charsToWhiteSpace = j - offset;
                break;
            }
        }

        if(charsToWhiteSpace != 0) {
            offset += charsToWhiteSpace;
        }
        return offset;
    }

    /**
     * test if character is whitespace
     * @param c
     * @return
     */
    private boolean isWhitespace(char c) {
        return (c ==' ') || (c == '\t') || (c == '\n') || (c == '\r');
    }

    /**
     * display selected footnote in dialog.  If editable, then it adds options to delete and edit
     *      the footnote
     * @param holder
     * @param item
     * @param span
     * @param editable
     */
    private void showFootnote(final ReviewHolder holder, final ReviewListItem item, final NoteSpan span, final int start, final int end, boolean editable) {
        CharSequence marker = span.getPassage();
        CharSequence title = mContext.getResources().getText(R.string.title_footnote);
        if(!marker.toString().isEmpty()) {
            title = title + ": " + marker;
        }
        CharSequence message = span.getNotes();

        if(editable && !item.isComplete) {

            new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.dismiss, null)
                    .setNeutralButton(R.string.edit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            editFootnote(span.getNotes(), holder, item, start, end);
                        }
                    })

                    .setNegativeButton(R.string.label_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteFootnote(span.getNotes(), holder, item, start, end);
                        }
                    })
                    .show();

        } else {

            new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.dismiss, null)
                    .show();
        }
    }

    /**
     * prompt to confirm removal of specific footnote at position
     * @param note
     * @param holder
     * @param item
     * @param start
     * @param end
     */
    private void deleteFootnote(CharSequence note, final ReviewHolder holder, final ReviewListItem item, final int start, final int end ) {
        final EditText editText = holder.getEditText();
        final CharSequence original = editText.getText();

        new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                .setTitle(R.string.footnote_confirm_delete)
                .setMessage(note)
                .setPositiveButton(R.string.label_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        placeFootnote(null, original, start, end, holder, item, editText);
                    }
                })
                .setNegativeButton(R.string.title_cancel, null)
                .show();
    }

    /**
     * generate spannable for source text.  Will add click listener for notes if supported
     *
     * Currently this is also used when rendering the target text when not editable.
     *
     *
     * @param text
     * @param format
     * @param holder
     * @param item
     * @param editable
     * @return
     */
    @Deprecated
    private CharSequence renderSourceText(String text, TranslationFormat format, final ReviewHolder holder, final ReviewListItem item, final boolean editable) {
        RenderingGroup renderingGroup = new RenderingGroup();
        boolean enableSearch = mSearchText != null && searchSubject != null;
        if(editable) { // if rendering for target card
            enableSearch &= searchSubject == SearchSubject.TARGET; // make sure we are searching target
        } else { // if rendering for source card
            enableSearch &= searchSubject == SearchSubject.SOURCE; // make sure we are searching source
        }
        if (Clickables.isClickableFormat(format)) {
            // TODO: add click listeners for verses
            Span.OnClickListener noteClickListener = new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    if(span instanceof NoteSpan) {
                        showFootnote(holder, item, (NoteSpan) span, start, end, editable);
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            };

            Clickables.setupRenderingGroup(format, renderingGroup, null, noteClickListener, false);
            if(editable) {
                if(!item.isComplete) {
                    renderingGroup.setVersesEnabled(false);
                }
                renderingGroup.setLinebreaksEnabled(true);
            }

            if( enableSearch ) {
                renderingGroup.setSearchString(mSearchText, HIGHLIGHT_COLOR);
            }
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
            if( enableSearch ) {
                renderingGroup.setSearchString(mSearchText, HIGHLIGHT_COLOR);
            }
        }
        renderingGroup.init(text);
        CharSequence results = renderingGroup.start();
        item.hasMissingVerses = renderingGroup.isAddedMissingVerse();
        return results;
    }

    @Override
    public int getItemCount() {
        return mFilteredItems.size();
    }

    /**
     * show or hide the merge conflict icon
     * @param showMergeConflict
     * @param mergeConflictFilterMode
     */
    private void showMergeConflictIcon(final boolean showMergeConflict, final boolean mergeConflictFilterMode) {
        if( (showMergeConflict != mHaveMergeConflict) || (mergeConflictFilterMode != mMergeConflictFilterEnabled) ) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    OnEventListener listener = getListener();
                    if(listener != null) {
                        listener.onEnableMergeConflict(showMergeConflict, mergeConflictFilterMode);
                    }
                }
            });
        }
        mHaveMergeConflict = showMergeConflict;
        mMergeConflictFilterEnabled = mHaveMergeConflict ? mergeConflictFilterMode : false;
    }

    /**
     * opens the resources view
     */
    public void openResources() {
        if(!mResourcesOpened) {
            mResourcesOpened = true;
            coordinateViewHolders();
        }
    }

    /**
     * closes the resources view
     */
    public void closeResources() {
        if(mResourcesOpened) {
            mResourcesOpened = false;
            coordinateViewHolders();
        }
    }

    /**
     * Checks if the resources are open
     * @return
     */
    public boolean isResourcesOpen() {
        return mResourcesOpened;
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

    @Override
    public void onSourceFootnoteClick(ReviewListItem item, NoteSpan span, int start, int end) {
        int position = mFilteredItems.indexOf(item);
        if(getListener() == null) return;
        ReviewHolder holder = (ReviewHolder) getListener().getVisibleViewHolder(position);
        if(holder == null) return;
        showFootnote(holder, item, span, start, end, false);
    }

    /**
     * for returning multiple values in the text search results
     */
    private static class MatchResults {
        final private int foundLocation;
        final private int numberFound;
        final private boolean needRender;

        public MatchResults(int foundLocation, int numberFound, boolean needRender) {
            this.foundLocation = foundLocation;
            this.numberFound = numberFound;
            this.needRender = needRender;
        }
    }

    /**
     * move to next (forward/previous) search item. If current position has matches, then it will first try to move to the next item within the chunk.  Otherwise it will find the next chunk with text.
     * @param forward if true then find next instance (moving down the page), otherwise will find previous (moving up the page)
     */
    @Override
    public void onMoveSearch(boolean forward) {
        mLastSearchDirectionForward = forward;
        Log.i(TAG, "onMoveSearch position " + mSearchPosition + " forward= " + forward);

        int foundPos = findNextMatchChunk(forward);
        if(foundPos >= 0) {
            Log.i(TAG, "onMoveSearch foundPos= " + foundPos);
            mSearchPosition = foundPos;
            mSearchSubPositionItems = -1;
            if(getListener() != null) {
                Log.i(TAG, "onMoveSearch position= " + foundPos);
                getListener().onSetSelectedPosition(foundPos, 0); // coarse scrolling
            }
            onSearching(false, mNumberOfChunkMatches, false, false);

            ReviewListItem item = (ReviewListItem) getItem(mSearchPosition);
            if(item != null) {
                findSearchItemInChunkAndPreselect(forward, item, mSearchingTarget);
            }
        } else { // not found, clear last selection
            Log.i(TAG, "onMoveSearch at limit = " + mSearchPosition);
            ReviewListItem item = (ReviewListItem) getItem(mSearchPosition);
            if(item != null) {
                forceSearchReRender(item);
            }

            showAtLimit(forward);
            if(forward) {
                mSearchPosition++;
            } else {
                mSearchPosition--;
            }
        }
    }

    /**
     * check if current highlight is at either limit (forward or back)
     */
    private void checkIfAtSearchLimits() {
        checkIfAtSearchLimit(true);
        checkIfAtSearchLimit(false);
    }

    /**
     * check if current highlight is at limit
     * @param forward
     */
    private void checkIfAtSearchLimit(boolean forward) {
        int nextPos = findNextMatchChunk(forward);
        if(nextPos < 0) {
            showAtLimit(forward);
        }
    }

    /**
     * indicate that we are at limit
     * @param forward
     */
    private void showAtLimit(boolean forward) {
        if(forward) {
            onSearching(false, mNumberOfChunkMatches, true, mNumberOfChunkMatches == 0);
        } else {
            onSearching(false, mNumberOfChunkMatches, mNumberOfChunkMatches == 0, true);
        }
    }

    /**
     * get next match item
     * @param forward
     * @return
     */
    private int findNextMatchChunk(boolean forward) {
        int foundPos = -1;
        if(forward) {
            int start = Math.max(mSearchPosition,-1);
            for(int i = start + 1; i < mFilteredItems.size(); i++) {
                ReviewListItem item = (ReviewListItem) getItem(i);
                if(item.hasSearchText) {
                    foundPos = i;
                    break;
                }
            }
        } else { // previous
            int start = Math.min(mSearchPosition, mFilteredItems.size());
            for(int i = start - 1; i >= 0; i--) {
                ReviewListItem item = (ReviewListItem) getItem(i);
                if(item.hasSearchText) {
                    foundPos = i;
                    break;
                }
            }
        }
        return foundPos;
    }

    /**
     * gets the number of string matches within chunk and selects next item if going forward, or the last item if going backward
     * @param forward
     * @param item
     * @param target - if true searching target card
     */
    private MatchResults findSearchItemInChunkAndPreselect(boolean forward, ReviewListItem item, boolean target) {
        MatchResults results = getMatchItemN( item, mSearchText, 1000, target); // get item count
        mSearchSubPositionItems = results.numberFound;
        int searchSubPosition = 0;
        if(results.needRender) {
            mSearchSubPositionItems = -1; // this will flag to get count after render completes
        } else {
            if(results.numberFound <= 0) {
                item.hasSearchText = false;
            }
            forceSearchReRender(item);
            checkIfAtSearchLimits();
        }
        item.selectItemNum = searchSubPosition;
        return results;
    }

    /**
     * cause search to be re-rendered to highlight search items
     * @param item
     */
    private void forceSearchReRender(ReviewListItem item) {
        if (mSearchingTarget) {
            item.renderedTargetText = null;
            item.refreshSearchHighlightTarget = false;
        } else {
            item.renderedSourceText = null;
            item.refreshSearchHighlightSource = false;
        }
        triggerNotifyDataSetChanged();
    }

    /**
     * search text to find the nth item (matchNumb) of the search string
     * @param item
     * @param match
     * @param matchNumb - number of item to locate (0 based)
     * @param target - if true searching target card
     * @return object containing position of match (-1 if not found), number of items actually found (if less), and a flag that indicates that text needs to be rendered
     */
    private MatchResults getMatchItemN(ReviewListItem item, CharSequence match, int matchNumb, boolean target) {
        String matcher = match.toString();
        int length = matcher.length();

        CharSequence text = mSearchingTarget ? item.renderedTargetText : item.renderedSourceText;
        boolean needRender = (text == null);

        boolean matcherEmpty = (matcher.length() == 0);
        if(matcherEmpty || needRender || (matchNumb < 0)
                || (target != mSearchingTarget)) {
            return new MatchResults(-1, -1, needRender);
        }

        Log.i(TAG, "getMatchItemN() Search started: " + matcher);

        int searchStartLocation = 0;
        int count = 0;
        int pos = -1;
        String textLowerCase = text.toString().toLowerCase();

        while (count <= matchNumb) {
            pos = textLowerCase.indexOf(matcher, searchStartLocation);
            if(pos < 0) { // not found
                break;
            }

            searchStartLocation = pos + length;
            if(++count > matchNumb) {
                return new MatchResults(pos, count, needRender);
            }
        }

        return new MatchResults(-1, count, needRender); // failed, return number of items actually found
    }

    @Override
    /**
     * technically no longer a filter but now a search that flags items containing search string
     */
    public void filter(CharSequence constraint, SearchSubject subject, final int initialPosition) {
        mSearchText = (constraint == null) ? "" : constraint.toString().toLowerCase().trim();
        searchSubject = subject;

        mSearchingTarget = subject == SearchSubject.TARGET || subject == SearchSubject.BOTH;

        ManagedTask oldTask = TaskManager.getTask(mStringSearchTaskID);
        TaskManager.cancelTask(oldTask);
        TaskManager.clearTask(oldTask);

        final String matcher = mSearchText.toString();
        final SearchSubject subjectFinal = subject;

        ManagedTask task = new ManagedTask() {
            @Override
            public void start() {
                if(isCanceled()) {
                    return;
                }

                boolean matcherEmpty = (matcher.isEmpty());

                Log.i(TAG, "filter(): Search started: " + matcher);

                mChunkSearchMatchesCounter = 0;
                for (int i = 0; i < mFilteredItems.size(); i++) {
                    if(isCanceled()) {
                        return;
                    }

                    ReviewListItem item = (ReviewListItem) mFilteredItems.get(i);
                    if(item == null) {
                        return;
                    }

                    boolean match = false;

                    if(!matcherEmpty) {
                        if (mSearchingTarget) {
                            boolean foundMatch = false;

                            if (item.targetText != null) {
                                foundMatch = item.targetText.toString().toLowerCase().contains(matcher);
                                if(foundMatch) { // if match, it could be in markup, so we double check by rendering and searching that
                                    CharSequence text = renderTargetText(item.targetText, item.targetTranslationFormat, item.ft, null, item);
                                    foundMatch = text.toString().toLowerCase().contains(matcher);
                                }
                            }
                            match = foundMatch || match;
                        }
                        if (!mSearchingTarget) {
                            boolean foundMatch = false;

                            if (item.renderedSourceText != null) {
                                foundMatch = item.renderedSourceText.toString().toLowerCase().contains(matcher);
                            } else
                            if (item.sourceText != null) {
                                foundMatch = item.sourceText.toString().toLowerCase().contains(matcher);
                                if(foundMatch) { // if match, it could be in markup, so we double check by rendering and searching that
                                    CharSequence text = renderSourceText(item.sourceText, item.sourceTranslationFormat, null, item, false);
                                    foundMatch = text.toString().toLowerCase().contains(matcher);
                                }
                            }
                            match = foundMatch || match;
                        }
                    }

                    if(item.hasSearchText && !match) { // check for search match cleared
                        item.renderedTargetText = null;  // re-render target
                        item.renderedSourceText = null;  // re-render source
                    }

                    item.hasSearchText = match;
                    if(match) {
                        item.renderedTargetText = null;  // re-render target
                        item.renderedSourceText = null;  // re-render source
                        mChunkSearchMatchesCounter++;
                    }
                }
            }
        };
        task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
            @Override
            public void onTaskFinished(final ManagedTask task) {
                if(!task.isCanceled()) {
                    Log.i(TAG, "filter(): Search finished: '" + matcher + "', count: " + mChunkSearchMatchesCounter);

                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            mSearchPosition = initialPosition;
                            mLayoutBuildNumber++; // force redraw of displayed cards
                            triggerNotifyDataSetChanged();
                            boolean zeroItemsFound = ReviewModeAdapter.this.mChunkSearchMatchesCounter <= 0;
                            onSearching(false, ReviewModeAdapter.this.mChunkSearchMatchesCounter, zeroItemsFound, zeroItemsFound);
                            if(!zeroItemsFound) {
                                checkIfAtSearchLimits();
                            }
                        }
                    });
                } else {
                    Log.i(TAG, "filter(): Search cancelled: '" + matcher + "'");
                    onSearching(false, 0, true, true);
                }
            }
        });
        if(matcher.isEmpty() && mChunkSearchMatchesCounter == 0) {
            // TRICKY: don't run search if query is empty and there are not already matches
            return;
        }
        onSearching(true, 0, true, true);
        mStringSearchTaskID = TaskManager.addTask(task);
    }

    /**
     * notify listener of search state changes
     * @param doingSearch - search is currently processing
     * @param numberOfChunkMatches - number of chunks that have the search string
     * @param atEnd - we are at last search item highlighted
     * @param atStart - we are at first search item highlighted
     */
    private void onSearching(boolean doingSearch, int numberOfChunkMatches, boolean atEnd, boolean atStart) {
        if(getListener() != null) {
            getListener().onSearching(doingSearch, numberOfChunkMatches, atEnd, atStart);
            this.mNumberOfChunkMatches = numberOfChunkMatches;
            this.mAtSearchEnd = atEnd;
            this.mAtSearchStart = atStart;
        }
    }

    /**
     * Sets the position where the list should start when first built
     * @param startPosition
     */
    @Override
    protected void setListStartPosition(int startPosition) {
        super.setListStartPosition(startPosition);
        mSearchPosition = startPosition;
    }

    @Override
    public boolean hasFilter() {
        return true;
    }

    /**
     * enable/disable merge conflict filter in adapter
     * @param enableFilter
     */
    @Override
    public final void setMergeConflictFilter(boolean enableFilter) {
        mMergeConflictFilterEnabled = enableFilter;

        if(!mHaveMergeConflict || !mMergeConflictFilterEnabled) { // if no merge conflict or filter off, then remove filter
            mFilteredItems = mItems;
            mFilteredChapters = mChapters;

            if(mMergeConflictFilterOn) {
                mMergeConflictFilterOn = false;
                triggerNotifyDataSetChanged();
            }
            return;
        }

        mMergeConflictFilterOn = true;

        CharSequence filterConstraint = enableFilter ? "true" : null; // will filter if string is not null
        showMergeConflictIcon(mHaveMergeConflict, true);

        // clear the cards displayed since we have new search string
        mFilteredItems = new ArrayList<>();

        MergeConflictFilter filter = new MergeConflictFilter(mSourceContainer, mTargetTranslation, mItems);
        filter.setListener(new MergeConflictFilter.OnMatchListener() {
            @Override
            public void onMatch(ListItem item) {
                if(!mFilteredChapters.contains(item.chapterSlug)) mFilteredChapters.add(item.chapterSlug);
            }

            @Override
            public void onFinished(CharSequence constraint, List<ListItem> results) {
                mFilteredItems = results;
                updateMergeConflict();
                triggerNotifyDataSetChanged();
                checkForConflictSummary(mFilteredItems.size());
            }
        });
        filter.filter(filterConstraint);
    }

    @Override
    public void onTaskFinished(final ManagedTask task) {
        TaskManager.clearTask(task);
        if(task.interrupted()) {
            Logger.i(TAG, "Task Dismissed: " + task.getTaskId());
            return;
        }

        if (task instanceof CheckForMergeConflictsTask) {
            CheckForMergeConflictsTask mergeConflictsTask = (CheckForMergeConflictsTask) task;

            final boolean mergeConflictFound = mergeConflictsTask.hasMergeConflict();
            boolean doMergeFiltering = mergeConflictFound && mMergeConflictFilterEnabled;
            final int conflictCount = mergeConflictsTask.getConflictCount();
            final boolean conflictCountChanged = conflictCount != mFilteredItems.size();
            final boolean needToUpdateFilter = (doMergeFiltering != mMergeConflictFilterOn) || conflictCountChanged;

            checkForConflictSummary(conflictCount);

            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    filter(mSearchText, searchSubject, mSearchPosition); // update search filter
                }
            });

            hand.post(new Runnable() {
                @Override
                public void run() {
                    showMergeConflictIcon(mergeConflictFound, mMergeConflictFilterEnabled);
                    if (needToUpdateFilter) {
                        setMergeConflictFilter(mMergeConflictFilterEnabled);
                    }
                }
            });
        } else if(task instanceof RenderHelpsTask) {
            final Map<String, Object> data = (Map<String, Object>)task.getResult();
            if(data == null) {
                Logger.i(TAG, "Task Data Missing: " + task.getTaskId());
                return;
            }
            final List<TranslationHelp> notes = (List<TranslationHelp>)data.get("notes");
            final List<Link> words = (List<Link>) data.get("words");
            final List<TranslationHelp> questions = (List<TranslationHelp>)data.get("questions");

            final int position = mFilteredItems.indexOf(((RenderHelpsTask) task).getItem());
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(getListener() != null) {
                        ReviewHolder holder = (ReviewHolder) getListener().getVisibleViewHolder(position);
                        if (holder != null) {
                            holder.setResources(mSourceContainer.language, notes, questions, words);
                            // TODO: 2/28/17 select the correct tab
                        } else {
                            Logger.i(TAG, "UI Miss: " + task.getTaskId());
                            notifyItemChanged(position);
                        }
                    }
                }
            });
        } else if(task instanceof RenderSourceTask) {
            final CharSequence data = (CharSequence)task.getResult();
            if(data == null) {
                Logger.i(TAG, "Task Data Missing: " + task.getTaskId());
                return;
            }
            final ReviewListItem item = ((RenderSourceTask)task).getItem();
            item.renderedSourceText = data;
            final int position = mFilteredItems.indexOf(item);
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(getListener() != null) {
                        ReviewHolder holder = (ReviewHolder) getListener().getVisibleViewHolder(position);
                        if(holder != null) {
                            holder.setSource(data);

                            // update the search
                            item.refreshSearchHighlightSource = false;
                            int selectPosition = checkForSelectedSearchItem(item, position, false);
                            selectCurrentSearchItem(position, selectPosition, holder.mSourceBody);
                        } else {
                            Logger.i(TAG, "UI Miss: " + task.getTaskId());
                            notifyItemChanged(position);
                        }
                    }
                }
            });
        }
    }

    /**
     * check if we are supposed to pop up summary
     * @param conflictCount
     */
    protected void checkForConflictSummary(final int conflictCount) {
        if(mShowMergeSummary) {
            mShowMergeSummary = false; // we just show the merge summary once

            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    String message = mContext.getString(R.string.merge_summary, conflictCount);

                    // pop up merge conflict summary
                    new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                            .setTitle(R.string.change_complete_title)
                            .setMessage(message)
                            .setPositiveButton(R.string.label_close, null)
                            .show();
                }

            });
        }
    }
}