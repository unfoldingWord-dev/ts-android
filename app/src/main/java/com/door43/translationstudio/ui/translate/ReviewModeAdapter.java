package com.door43.translationstudio.ui.translate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.Link;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.core.FileHistory;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.TranslationType;
import com.door43.widget.LinedEditText;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.MergeConflictHandler;
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
import com.door43.widget.ViewUtil;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unfoldingword.door43client.models.TargetLanguage;

/**
 * Created by joel on 9/18/2015.
 */
public class ReviewModeAdapter extends ViewModeAdapter<ReviewModeAdapter.ViewHolder> {
    private static final String TAG = ReviewModeAdapter.class.getSimpleName();

    private static final int TAB_NOTES = 0;
    private static final int TAB_WORDS = 1;
    private static final int TAB_QUESTIONS = 2;
    public static final int HIGHLIGHT_COLOR = Color.YELLOW;
    private final Door43Client mLibrary;
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_CONFLICT = 1;
    public static final int CONFLICT_COLOR = R.color.warning;
    private static final boolean GET_HEAD = true;
    private static final boolean GET_TAIL = false;
    private final Translator mTranslator;
    private final Activity mContext;
    private final TargetTranslation mTargetTranslation;
    private final String startingChapterSlug;
    private final String startingChunkSlug;
    private ResourceContainer mSourceContainer;
    private final TargetLanguage mTargetLanguage;
    private List<ListItem> mItems = new ArrayList<>();
    private List<ListItem> mFilteredItems = new ArrayList<>();
    private int mLayoutBuildNumber = 0;
    private boolean mResourcesOpened = false;
    private ContentValues[] mTabs = new ContentValues[0];
    private int[] mOpenResourceTab = new int[0];
    private boolean mAllowFootnote = true;

    private List<String> mChapters = new ArrayList<>();
    private List<String> mFilteredChapters = new ArrayList<>();
    private CharSequence filterConstraint = null;
    private TranslationFilter.FilterSubject filterSubject = null;

    private boolean mMergeHeadSelected = false;
    private boolean mMergeTailSelected = false;
    private float mInitialTextSize = 0;
    private int mMarginInitialLeft = 0;
    private boolean mHaveMergeConflict = false;
    private boolean mMergeConflictFilterMode = false;

    @Deprecated
    public void setHelpContainers(List<ResourceContainer> helpfulContainers) {
        // TODO: 10/11/16 load the containers into a map so we can retrieve them
        triggerNotifyDataSetChanged();
    }

    enum DisplayState {
        NORMAL,
        SELECTED,
        DESELECTED
    }

    public ReviewModeAdapter(Activity context, String targetTranslationSlug, String startingChapterSlug, String startingChunkSlug, boolean openResources) {
        this.startingChapterSlug = startingChapterSlug;
        this.startingChunkSlug = startingChunkSlug;

        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationSlug);
        mAllowFootnote = mTargetTranslation.getFormat() == TranslationFormat.USFM;
        mTargetLanguage = App.languageFromTargetTranslation(mTargetTranslation);
        mResourcesOpened = openResources;
    }

    @Override
    void setSourceContainer(ResourceContainer sourceContainer) {
        mSourceContainer = sourceContainer;

        this.mChapters = new ArrayList();
        mItems = new ArrayList<>();

        // TODO: there is also a map form of the toc.
        setListStartPosition(0);

        if (mSourceContainer != null) {
            for (Map tocChapter : (List<Map>) mSourceContainer.toc) {
                String chapterSlug = (String) tocChapter.get("chapter");
                this.mChapters.add(chapterSlug);
                List<String> tocChunks = (List) tocChapter.get("chunks");
                for (String chunkSlug : tocChunks) {
                    if (chapterSlug.equals(startingChapterSlug) && chunkSlug.equals(startingChunkSlug)) {
                        setListStartPosition(mItems.size());
                    }
                    ReviewListItem item = new ReviewListItem(chapterSlug, chunkSlug);

                    // fetch translation from disk
                    item.load(mSourceContainer, mTargetTranslation);
                    if(item.hasMergeConflicts) {
                        setMergeConflict(true);
                    }

                    mItems.add(item);
                }
            }
        }

        mFilteredItems = mItems;
        mFilteredChapters = mChapters;
        mOpenResourceTab = new int[mItems.size()];

        loadTabInfo();

        filter(filterConstraint, filterSubject);

        triggerNotifyDataSetChanged();
    }

    /**
     * Rebuilds the card tabs
     */
    private void loadTabInfo() {
        List<ContentValues> tabContents = new ArrayList<>();
        String[] sourceTranslationIds = App.getSelectedSourceTranslations(mTargetTranslation.getId());
        for (String slug : sourceTranslationIds) {
            Translation st = mLibrary.index().getTranslation(slug);
            if (st != null) {
                ContentValues values = new ContentValues();
                // include the resource id if there are more than one
                if (mLibrary.index().getResources(st.language.slug, st.project.slug).size() > 1) {
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
    void onCoordinate(final ViewHolder holder) {
        int durration = 400;
        float openWeight = 1f;
        float closedWeight = 0.765f;
        ObjectAnimator anim;
        if (mResourcesOpened) {
            holder.mResourceLayout.setVisibility(View.VISIBLE);
            anim = ObjectAnimator.ofFloat(holder.mMainContent, "weightSum", openWeight, closedWeight);
        } else {
            holder.mResourceLayout.setVisibility(View.INVISIBLE);
            anim = ObjectAnimator.ofFloat(holder.mMainContent, "weightSum", closedWeight, openWeight);
        }
        anim.setDuration(durration);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                holder.mMainContent.requestLayout();
            }
        });
        anim.start();
    }

    @Override
    public String getFocusedChunkSlug(int position) {
        if (position >= 0 && position < mFilteredItems.size()) {
            return mFilteredItems.get(position).chunkSlug;
        }
        return null;
    }

    @Override
    public String getFocusedChapterSlug(int position) {
        if (position >= 0 && position < mFilteredItems.size()) {
            return mFilteredItems.get(position).chapterSlug;
        }
        return null;
    }

    @Override
    public int getItemPosition(String chapterId, String frameId) {
        for (int i = 0; i < mFilteredItems.size(); i++) {
            ReviewListItem item = (ReviewListItem) mFilteredItems.get(i);
            if (item.isChunk() && item.chapterSlug.equals(chapterId) && item.chunkSlug.equals(frameId)) {
                return i;
            }
        }
        return -1;
    }

    public ListItem getItem(int position) {
        if (position >= 0 && position < mFilteredItems.size()) {
            return mFilteredItems.get(position);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        ListItem item = getItem(position);
        if (item != null) {
            boolean conflicted = item.hasMergeConflicts;
            if (conflicted) {
                setMergeConflict(true);
                return VIEW_TYPE_CONFLICT;
            }
        }
        return VIEW_TYPE_NORMAL;
    }

    @Override
    public ViewHolder onCreateManagedViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case VIEW_TYPE_CONFLICT:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_review_list_item_merge_conflict, parent, false);
                break;
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_review_list_item, parent, false);
                break;
        }
        ViewHolder vh = new ViewHolder(parent.getContext(), v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.currentPosition = position;
        final ReviewListItem item = (ReviewListItem) mFilteredItems.get(position);

        // open/close resources
        if (mResourcesOpened) {
            holder.mMainContent.setWeightSum(.765f);
        } else {
            holder.mMainContent.setWeightSum(1f);
        }

        ViewUtil.makeLinksClickable(holder.mSourceBody);

        // render the cards
        renderSourceCard(position, item, holder);
        if (getItemViewType(position) == VIEW_TYPE_CONFLICT) {
            renderConflictingTargetCard(position, item, holder);
        } else {
            renderTargetCard(position, item, holder);
        }
        renderResourceCard(position, item, holder);

        // set up fonts
        if (holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.format(mContext, TranslationType.SOURCE, holder.mSourceBody, mSourceContainer.language.slug, mSourceContainer.language.direction);
            Typography.formatSub(mContext, TranslationType.TARGET, holder.mTargetTitle, mTargetLanguage.slug, mTargetLanguage.direction);
            if (!item.hasMergeConflicts) {
                Typography.format(mContext, TranslationType.TARGET, holder.mTargetBody, mTargetLanguage.slug, mTargetLanguage.direction);
                Typography.format(mContext, TranslationType.TARGET, holder.mTargetEditableBody, mTargetLanguage.slug, mTargetLanguage.direction);
            } else {
                Typography.format(mContext, TranslationType.TARGET, holder.mHeadText, mTargetLanguage.slug, mTargetLanguage.direction);
                Typography.format(mContext, TranslationType.TARGET, holder.mTailText, mTargetLanguage.slug, mTargetLanguage.direction);
                Typography.formatSub(mContext, TranslationType.TARGET, holder.mConflictText, mTargetLanguage.slug, mTargetLanguage.direction);
            }
        }
    }

    /**
     * Returns the preferred translation notes.
     * if none exist in the source language it will return the english version
     *
     * @param frame
     * @return
     */
    private static TranslationNote[] getPreferredNotes(ResourceContainer sourceTranslation, String frame) {
        Door43Client library = App.getLibrary();
//        TranslationNote[] notes = library.getTranslationNotes(sourceTranslation, frame.getChapterId(), frame.getId());
//        if(notes.length == 0 && !sourceTranslation.language.slug.equals("en")) {
//            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.project.slug, "en");
//            notes = library.getTranslationNotes(defaultSourceTranslation, frame.getChapterId(), frame.getId());
//        }
        return new TranslationNote[0];
    }

    /**
     * Returns the preferred translation words.
     * if none exist in the source language it will return the english version
     *
     * @param sourceTranslation
     * @param frame
     * @return
     */
    private static TranslationWord[] getPreferredWords(ResourceContainer sourceTranslation, String frame) {
        Door43Client library = App.getLibrary();
//        TranslationWord[] words = library.getTranslationWords(sourceTranslation, frame.getChapterId(), frame.getId());
//        if(words.length == 0 && !sourceTranslation.language.slug.equals("en")) {
//            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.project.slug, "en");
//            words = library.getTranslationWords(defaultSourceTranslation, frame.getChapterId(), frame.getId());
//        }
        return new TranslationWord[0];
    }

    /**
     * Returns the preferred checking questions.
     * if none exist in the source language it will return the english version
     *
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    private static CheckingQuestion[] getPreferredQuestions(ResourceContainer sourceTranslation, String chapterId, String frameId) {
        Door43Client library = App.getLibrary();
//        CheckingQuestion[] questions = library.getCheckingQuestions(sourceTranslation, chapterId, frameId);
//        if(questions.length == 0 && !sourceTranslation.language.slug.equals("en")) {
//            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.project.slug, "en");
//            questions = library.getCheckingQuestions(defaultSourceTranslation, chapterId, frameId);
//        }
        return new CheckingQuestion[0];
    }

    private void renderSourceCard(final int position, final ReviewListItem item, final ViewHolder holder) {
        ManagedTask oldTask = TaskManager.getTask(holder.currentSourceTaskId);
        TaskManager.cancelTask(oldTask);
        TaskManager.clearTask(oldTask);
        if (item.renderedSourceText == null) {
            holder.mSourceBody.setText("");
            ManagedTask task = new ManagedTask() {
                @Override
                public void start() {
                    if (interrupted()) return;
                    CharSequence text = renderSourceText(item.sourceText, TranslationFormat.parse(mSourceContainer.contentMimeType), holder, item, false);
                    setResult(text);
                }
            };
            task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
                @Override
                public void onTaskFinished(ManagedTask task) {
                    TaskManager.clearTask(task);
                    CharSequence data = (CharSequence) task.getResult();
                    item.renderedSourceText = data;
                    if (!task.isCanceled() && data != null && position == holder.currentPosition) {
                        Handler hand = new Handler(Looper.getMainLooper());
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                holder.mSourceBody.setText(item.renderedSourceText);
                            }
                        });
                    }
                }
            });
            holder.currentSourceTaskId = TaskManager.addTask(task);
        } else {
            holder.mSourceBody.setText(item.renderedSourceText);
        }

        renderTabs(holder);
    }

    /**
     * Renders a target card that has merge conflicts
     *
     * @param position
     * @param item
     * @param holder
     */
    private void renderConflictingTargetCard(int position, final ReviewListItem item, final ViewHolder holder) {
        // render title
        holder.mTargetTitle.setText(item.getTargetTitle());

        MergeConflictHandler renderer = new MergeConflictHandler();
        int mergeConflictColor = mContext.getResources().getColor(CONFLICT_COLOR);
        renderer.renderMergeConflict(item.targetText, mergeConflictColor);
        item.headText = renderer.getConflictPart(MergeConflictHandler.MERGE_HEAD_PART);
        item.tailText = renderer.getConflictPart(MergeConflictHandler.MERGE_TAIL_PART);
        item.isFullMergeConflict = renderer.isFullBlockMergeConflict();
        if (item.isFullMergeConflict) { // if whole string is different, no need to highlight differing lines, so change to regular text color
            SpannableStringBuilder span = new SpannableStringBuilder(item.headText);
            span.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.dark_primary_text)), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.headText = span;

            span = new SpannableStringBuilder(item.tailText);
            span.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.dark_primary_text)), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.tailText = span;
        }

        Typography.formatSub(mContext, TranslationType.SOURCE, holder.mHeadText, mSourceContainer.language.slug, mSourceContainer.language.direction);
        Typography.formatSub(mContext, TranslationType.SOURCE, holder.mTailText, mSourceContainer.language.slug, mSourceContainer.language.direction);

        if (mInitialTextSize == 0) { // see if we need to initialize values
            mMarginInitialLeft = leftMargin(holder.mHeadText);
            mInitialTextSize = holder.mHeadText.getTextSize();
        }
        displayMergeConflictSelectionState(false, false, holder, item);
        holder.mConflictText.setVisibility(View.VISIBLE);
        holder.mButtonBar.setVisibility(View.GONE);

        holder.mHeadText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayMergeConflictSelectionState(true, false, holder, item);
            }
        });

        holder.mTailText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayMergeConflictSelectionState(false, true, holder, item);
            }
        });

        holder.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayMergeConflictSelectionState(false, false, holder, item);
            }
        });

        holder.mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence selectedText = mMergeHeadSelected ? item.headText : item.tailText;
                applyNewCompiledText(selectedText.toString(), holder, item);
                item.hasMergeConflicts = MergeConflictHandler.isMergeConflicted(selectedText.toString());
                reOpenItem(item);
                updateMergeConflict();
                notifyDataSetChanged();
            }
        });

        holder.mUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoTextInTarget(holder, (ReviewListItem) item);
            }
        });
        holder.mRedoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redoTextInTarget(holder, (ReviewListItem) item);
            }
        });

        prepareUndoRedoUI(holder, item);
        holder.mUndoButton.setVisibility(View.GONE);
        holder.mRedoButton.setVisibility(View.GONE);
    }

    /**
     * Renders a normal target card
     *
     * @param position
     * @param item
     * @param holder
     */
    private void renderTargetCard(final int position, final ReviewListItem item, final ViewHolder holder) {
        // remove old text watcher
        if (holder.mEditableTextWatcher != null) {
            holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
        }

        // insert rendered text
        if (item.isEditing) {
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
                FileHistory history = item.getFileHistory(mTargetTranslation);
                if (!history.isAtHead()) {
                    history.reset();
                    loadControls(holder, item);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        // render target body
        if (item.renderedTargetText == null) {
            holder.mTargetEditableBody.setText("");
            holder.mTargetBody.setText("");
            ManagedTask task = new ManagedTask() {
                @Override
                public void start() {
                    if (interrupted()) return;
                    CharSequence text;
                    if (item.isComplete || item.isEditing) {
                        text = renderSourceText(item.targetText, item.translationFormat, holder, item, true);
                    } else {
                        text = renderTargetText(item.targetText, item.translationFormat, item.ft, holder, item);
                    }
                    setResult(text);
                }
            };
            task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
                @Override
                public void onTaskFinished(ManagedTask task) {
                    TaskManager.clearTask(task);
                    CharSequence data = (CharSequence) task.getResult();
                    item.renderedTargetText = data;
                    if (!task.isCanceled() && data != null && position == holder.currentPosition) {
                        Handler hand = new Handler(Looper.getMainLooper());
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                if (item.isEditing) {
                                    // edit mode
                                    holder.mTargetEditableBody.setText(item.renderedTargetText);
                                    holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
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
                            }
                        });
                    }
                }
            });
            holder.currentTargetTaskId = TaskManager.addTask(task);
        } else if (item.isEditing) {
            // editing mode
            holder.mTargetEditableBody.setText(item.renderedTargetText);
            holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
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
                loadControls(holder, item);

                if (item.isEditing) {
                    holder.mTargetEditableBody.requestFocus();
                    InputMethodManager mgr = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(holder.mTargetEditableBody, InputMethodManager.SHOW_IMPLICIT);

                    // TRICKY: there may be changes to translation
                    item.load(mSourceContainer, mTargetTranslation);

                    // re-render for editing mode
                    item.renderedTargetText = renderSourceText(item.targetText, item.translationFormat, holder, item, true);
                    holder.mTargetEditableBody.setText(item.renderedTargetText);
                    holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                } else {
                    if (holder.mEditableTextWatcher != null) {
                        holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                    }
                    holder.mTargetBody.requestFocus();
                    getListener().closeKeyboard();

                    // TRICKY: there may be changes to translation
                    item.load(mSourceContainer, mTargetTranslation);

                    // re-render for verse mode
                    item.renderedTargetText = renderTargetText(item.targetText, item.translationFormat, item.ft, holder, item);
                    holder.mTargetBody.setText(item.renderedTargetText);
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

        loadControls(holder, item);

        // disable listener
        holder.mDoneSwitch.setOnCheckedChangeListener(null);

        // display as finished
        if (item.isComplete) {
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

        // done buttons
        holder.mDoneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // make sure to capture verse marker changes changes before dialog is displayed
                    Editable changes = holder.mTargetEditableBody.getText();
                    item.renderedTargetText = changes;
                    String newBody = Translator.compileTranslation(changes);
                    item.targetText = newBody;

                    new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
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
     * mark item as not done
     *
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
            notifyDataSetChanged();
        } else {
            // TODO: 10/27/2015 notify user the frame could not be completed.
        }
    }

    /**
     * get either the head or tail merge text with highlighting
     *
     * @param text
     * @param getHead
     * @return
     */
    private CharSequence getMergeText(CharSequence text, boolean getHead) {
        MergeConflictHandler renderer = new MergeConflictHandler();
        int mergeConflictColor = mContext.getResources().getColor(CONFLICT_COLOR);
        renderer.renderMergeConflict(text, mergeConflictColor);
        if (getHead) {
            return renderer.getConflictPart(MergeConflictHandler.MERGE_HEAD_PART);
        } else {
            return renderer.getConflictPart(MergeConflictHandler.MERGE_TAIL_PART);
        }
    }

    /**
     * get the left margin for view
     *
     * @param v
     * @return
     */
    private int leftMargin(View v) {
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        int lm = p.leftMargin;
        return lm;
    }

    /**
     * set merge conflict selection state
     *
     * @param selectHead
     * @param selectTail
     * @param holder
     * @param item
     */
    private void displayMergeConflictSelectionState(boolean selectHead, boolean selectTail, ViewHolder holder, ReviewListItem item) {
        mMergeHeadSelected = selectHead;
        mMergeTailSelected = selectTail;

        if (mMergeHeadSelected || mMergeTailSelected) {
            if (mMergeHeadSelected) {
                displayMergeSelectionState(DisplayState.SELECTED, holder.mHeadText, item.headText);
                displayMergeSelectionState(DisplayState.DESELECTED, holder.mTailText, formatDeselected(item, GET_TAIL));
                holder.mConflictText.setVisibility(View.GONE);
                holder.mButtonBar.setVisibility(View.VISIBLE);
            } else {
                displayMergeSelectionState(DisplayState.DESELECTED, holder.mHeadText, formatDeselected(item, GET_HEAD));
                displayMergeSelectionState(DisplayState.SELECTED, holder.mTailText, item.tailText);
                holder.mConflictText.setVisibility(View.GONE);
                holder.mButtonBar.setVisibility(View.VISIBLE);
            }

        } else {
            displayMergeSelectionState(DisplayState.NORMAL, holder.mHeadText, item.headText);
            displayMergeSelectionState(DisplayState.NORMAL, holder.mTailText, item.tailText);
            holder.mConflictText.setVisibility(View.VISIBLE);
            holder.mButtonBar.setVisibility(View.GONE);
        }
    }

    private CharSequence formatDeselected(ReviewListItem item, boolean getHead) {
        if (item.isFullMergeConflict) { // gray out everyithing
            CharSequence text = getHead ? item.headText : item.tailText;
            SpannableStringBuilder span = new SpannableStringBuilder(text);
            span.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.dark_disabled_text)), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return span;
        } else { // partial merge conflict, so set text gray and then highlight differing merge text
            SpannableStringBuilder span = new SpannableStringBuilder(item.targetText);
            span.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.dark_disabled_text)), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return getMergeText(span, getHead);
        }
    }

    /**
     * display the selection state for card
     *
     * @param state
     */
    private void displayMergeSelectionState(DisplayState state, TextView view, CharSequence text) {

        SpannableStringBuilder span;

        switch (state) {
            case SELECTED:
                setLeftRightMargins(view, mMarginInitialLeft); // shrink margins to emphasize
                span = new SpannableStringBuilder(text);
                // bold text to emphasize
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, mInitialTextSize * 1.0f); // grow text to emphasize
                span.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                view.setText(span);
                break;

            case DESELECTED:
                setLeftRightMargins(view, 2 * mMarginInitialLeft); // grow margins to de-emphasize
                // contents of text has already been grayed out to de-emphasize
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, mInitialTextSize * 0.8f); // shrink text to de-emphasize
                view.setText(text);
                break;

            case NORMAL:
            default:
                setLeftRightMargins(view, mMarginInitialLeft); // restore original margins
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, mInitialTextSize * 1.0f); // restore initial test size
                view.setText(text); // remove text emphasis
                break;
        }
    }

    /**
     * change left and right margins to emphasize/de-emphasize
     *
     * @param view
     * @param newValue
     */
    private void setLeftRightMargins(TextView view, int newValue) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.leftMargin = newValue;
        params.rightMargin = newValue;
        view.requestLayout();
    }

    /**
     * Sets the correct ui state for translation controls
     *
     * @param holder
     * @param item
     */
    private void loadControls(final ViewHolder holder, ListItem item) {
        if (item.isEditing) {
            prepareUndoRedoUI(holder, item);

            boolean allowFootnote = mAllowFootnote && item.isChunk();
            holder.mEditButton.setImageResource(R.drawable.ic_done_black_24dp);
            holder.mAddNoteButton.setVisibility(allowFootnote ? View.VISIBLE : View.GONE);
            holder.mUndoButton.setVisibility(View.GONE);
            holder.mRedoButton.setVisibility(View.GONE);
            holder.mTargetBody.setVisibility(View.GONE);
            holder.mTargetEditableBody.setVisibility(View.VISIBLE);
            holder.mTargetEditableBody.setEnableLines(true);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        } else {
            holder.mEditButton.setImageResource(R.drawable.ic_mode_edit_black_24dp);
            holder.mUndoButton.setVisibility(View.GONE);
            holder.mRedoButton.setVisibility(View.GONE);
            holder.mAddNoteButton.setVisibility(View.GONE);
            holder.mTargetBody.setVisibility(View.VISIBLE);
            holder.mTargetEditableBody.setVisibility(View.GONE);
            holder.mTargetEditableBody.setEnableLines(false);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        }
    }

    /**
     * check history to see if we should show undo/redo buttons
     *
     * @param holder
     * @param item
     */
    private void prepareUndoRedoUI(final ViewHolder holder, ListItem item) {
        final FileHistory history = item.getFileHistory(mTargetTranslation);
        ThreadableUI thread = new ThreadableUI(mContext) {
            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                try {
                    history.loadCommits();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPostExecute() {
                if (history.hasNext()) {
                    holder.mRedoButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mRedoButton.setVisibility(View.GONE);
                }
                if (history.hasPrevious()) {
                    holder.mUndoButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mUndoButton.setVisibility(View.GONE);
                }
            }
        };
        thread.start();
    }

    /**
     * create a new footnote at selected position in target text.  Displays an edit dialog to enter footnote data.
     *
     * @param holder
     * @param item
     */
    private void createFootnoteAtSelection(final ViewHolder holder, final ReviewListItem item) {
        final EditText editText = getEditText(holder, item);
        int endPos = editText.getSelectionEnd();
        if (endPos < 0) {
            endPos = 0;
        }
        final int insertPos = endPos;
        editFootnote("", holder, item, insertPos, insertPos);
    }

    /**
     * edit contents of footnote at specified position
     *
     * @param initialNote
     * @param holder
     * @param item
     * @param footnotePos
     * @param footnoteEndPos
     */
    private void editFootnote(CharSequence initialNote, final ViewHolder holder, final ReviewListItem item, final int footnotePos, final int footnoteEndPos) {
        final EditText editText = getEditText(holder, item);
        final CharSequence original = editText.getText();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View footnoteFragment = inflater.inflate(R.layout.fragment_footnote_prompt, null);
        if (footnoteFragment != null) {
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
                                if (validated) {
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
     * footnoteTitleText are null
     *
     * @param footnote
     * @param original
     * @param insertPos
     * @param insertEndPos
     * @param item
     * @param editText
     */
    private boolean verifyAndReplaceFootnote(CharSequence footnote, CharSequence original, int insertPos, final int insertEndPos, final ViewHolder holder, final ReviewListItem item, EditText editText) {
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
     *
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
     * footnoteTitleText are null
     *
     * @param footnote
     * @param original
     * @param start
     * @param end
     * @param item
     * @param editText
     */
    private void placeFootnote(CharSequence footnote, CharSequence original, int start, final int end, final ViewHolder holder, final ReviewListItem item, EditText editText) {
        CharSequence footnotecode = "";
        if (footnote != null) {
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
        if (item.isComplete || item.isEditing) {
            item.renderedTargetText = renderSourceText(item.targetText, item.translationFormat, holder, (ReviewListItem) item, true);
        } else {
            item.renderedTargetText = renderTargetText(item.targetText, item.translationFormat, item.ft, holder, (ReviewListItem) item);
        }
        editText.setText(item.renderedTargetText);
        editText.setSelection(editText.length(), editText.length());
    }

    /**
     * save changed text to item,  first see if it needs to be compiled
     *
     * @param s      A string or editable
     * @param item
     * @param holder
     * @param item   * @return
     */
    private String applyChangedText(CharSequence s, ViewHolder holder, ReviewListItem item) {
        String translation;
        if (s instanceof Editable) {
            translation = Translator.compileTranslation((Editable) s);
        } else {
            translation = s.toString();
        }

        applyNewCompiledText(translation, holder, item);
        return translation;
    }

    /**
     * save new text to item
     *
     * @param translation
     * @param holder
     * @param item
     */
    private void applyNewCompiledText(String translation, ViewHolder holder, ListItem item) {
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

        item.renderedTargetText = renderSourceText(translation, item.translationFormat, holder, (ReviewListItem) item, true);
    }

    /**
     * restore the text from previous commit for fragment
     *
     * @param holder
     * @param item
     */
    private void undoTextInTarget(final ViewHolder holder, final ReviewListItem item) {
        holder.mUndoButton.setVisibility(View.INVISIBLE);
        holder.mRedoButton.setVisibility(View.INVISIBLE);

        final FileHistory history = item.getFileHistory(mTargetTranslation);
        ThreadableUI thread = new ThreadableUI(mContext) {
            RevCommit commit = null;

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                // commit changes before viewing history
                if (history.isAtHead()) {
                    if (!mTargetTranslation.isClean()) {
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
                try {
                    if (commit != null) {
                        String text = history.read(commit);
                        // save and update ui
                        if (text != null) {
                            // TRICKY: prevent history from getting rolled back soon after the user views it
                            restartAutoCommitTimer();
                            applyChangedText(text, holder, item);

                            App.closeKeyboard(mContext);
                            item.hasMergeConflicts = MergeConflictHandler.isMergeConflicted(text);
                            notifyDataSetChanged();

                            if (holder.mTargetEditableBody != null) {
                                holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                                holder.mTargetEditableBody.setText(item.renderedTargetText);
                                holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (history.hasNext()) {
                    holder.mRedoButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mRedoButton.setVisibility(View.GONE);
                }
                if (history.hasPrevious()) {
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
     *
     * @param holder
     * @param item
     */
    private void redoTextInTarget(final ViewHolder holder, final ReviewListItem item) {
        holder.mUndoButton.setVisibility(View.INVISIBLE);
        holder.mRedoButton.setVisibility(View.INVISIBLE);

        final FileHistory history = item.getFileHistory(mTargetTranslation);
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
                try {
                    if (commit != null) {
                        String text = history.read(commit);
                        // save and update ui
                        if (text != null) {
                            // TRICKY: prevent history from getting rolled back soon after the user views it
                            restartAutoCommitTimer();
                            applyChangedText(text, holder, item);

                            App.closeKeyboard(mContext);
                            item.hasMergeConflicts = MergeConflictHandler.isMergeConflicted(text);
                            notifyDataSetChanged();

                            if (holder.mTargetEditableBody != null) {
                                holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                                holder.mTargetEditableBody.setText(item.renderedTargetText);
                                holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (history.hasNext()) {
                    holder.mRedoButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mRedoButton.setVisibility(View.GONE);
                }
                if (history.hasPrevious()) {
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
     *
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
        int[] range = Frame.getVerseRange(item.targetText, item.translationFormat);
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
            }
        }

        // Wrap up.
        if (success) {
            try {
                mTargetTranslation.commit();
            } catch (Exception e) {
                String frameComplexId = ":" + item.chapterSlug + "-" + item.chunkSlug;
                Logger.e(TAG, "Failed to commit translation of " + mTargetTranslation.getId() + frameComplexId, e);
            }
            item.isEditing = false;
            item.renderedTargetText = null;
            triggerNotifyDataSetChanged();
        }

        return success;
    }


    /**
     * Renders the source language tabs on the target card
     *
     * @param holder
     */
    private void renderTabs(ViewHolder holder) {
        holder.mTranslationTabs.setOnTabSelectedListener(null);
        holder.mTranslationTabs.removeAllTabs();
        for (ContentValues values : mTabs) {
            TabLayout.Tab tab = holder.mTranslationTabs.newTab();
            tab.setText(values.getAsString("title"));
            tab.setTag(values.getAsString("tag"));
            holder.mTranslationTabs.addTab(tab);
        }

        // open selected tab
        for (int i = 0; i < holder.mTranslationTabs.getTabCount(); i++) {
            TabLayout.Tab tab = holder.mTranslationTabs.getTabAt(i);
            if (tab.getTag().equals(mSourceContainer.slug)) {
                tab.select();
                break;
            }
        }

        // tabs listener
        holder.mTranslationTabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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
        // change tabs listener
        holder.mNewTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() != null) {
                    getListener().onNewSourceTranslationTabClick();
                }
            }
        });
    }

    /**
     * Returns the config options for a chunk
     *
     * @param chapterSlug
     * @param chunkSlug
     * @return
     */
    private Map<String, List<String>> getChunkConfig(String chapterSlug, String chunkSlug) {
        if (mSourceContainer != null) {
            Map config = null;
            if (mSourceContainer.config == null || !mSourceContainer.config.containsKey("content") || !(mSourceContainer.config.get("content") instanceof Map)) {
                // default to english if no config is found
                ResourceContainer rc = ContainerCache.cacheClosest(mLibrary, "en", mSourceContainer.project.slug, mSourceContainer.resource.slug);
                if (rc != null) config = rc.config;
            } else {
                config = mSourceContainer.config;
            }

            // look up config for chunk
            if (config != null && config.containsKey("content") && config.get("content") instanceof Map) {
                Map contentConfig = (Map<String, Object>) config.get("content");
                if (contentConfig.containsKey(chapterSlug)) {
                    Map chapterConfig = (Map<String, Object>) contentConfig.get(chapterSlug);
                    if (chapterConfig.containsKey(chunkSlug)) {
                        return (Map<String, List<String>>) chapterConfig.get(chunkSlug);
                    }
                }
            }
        }
        return new HashMap();
    }

    private void renderResourceCard(final int position, final ReviewListItem item, final ViewHolder holder) {
        // clean up view
        if (holder.mResourceList.getChildCount() > 0) {
            holder.mResourceList.removeAllViews();
        }
        holder.mResourceTabs.setOnTabSelectedListener(null);
        holder.mResourceTabs.removeAllTabs();

        // skip if chapter title/reference
        if (!item.isChunk()) {
            return;
        }

//        final String  frame = loadFrame(item.chapterSlug, item.chunkSlug);

        // clear resource card
        renderResources(holder, position, new TranslationNote[0], new ArrayList<Link>() {
        }, new CheckingQuestion[0]);

        // prepare task to load resources
        ManagedTask oldTask = TaskManager.getTask(holder.currentResourceTaskId);
        TaskManager.cancelTask(oldTask);
        ManagedTask task = new ManagedTask() {
            @Override
            public void start() {
                Map<String, List<Link>> result = new HashMap<>();

                // add some default values
                result.put("words", new ArrayList<Link>());
                result.put("questions", new ArrayList<Link>());
                result.put("notes", new ArrayList<Link>());

                if (getListener() == null) return;

                if (interrupted()) return;
                Map<String, List<String>> config = getChunkConfig(item.chapterSlug, item.chunkSlug);

                if (config.containsKey("words")) {
                    List<Link> links = ContainerCache.cacheClosestFromLinks(mLibrary, config.get("words"));
                    Pattern titlePattern = Pattern.compile("#(.*)");
                    for (Link link : links) {
                        ResourceContainer rc = ContainerCache.cacheClosest(App.getLibrary(), link.language, link.project, link.resource);
                        // TODO: 10/12/16 the words need to have their title placed into a "title" file instead of being inline in the chunk
                        String word = rc.readChunk(link.chapter, "01");
                        Matcher match = titlePattern.matcher(word.trim());
                        if (match.find()) {
                            link.title = match.group(1);
                        }
                    }
                    result.put("words", links);
                }

                if (interrupted()) return;
                if (config.containsKey("questions")) {
                    List<Link> links = ContainerCache.cacheClosestFromLinks(mLibrary, config.get("questions"));
                    result.put("questions", links);
                }

                if (interrupted()) return;
                if (config.containsKey("notes")) {
                    List<Link> links = ContainerCache.cacheClosestFromLinks(mLibrary, config.get("notes"));
                    result.put("notes", links);
                }
                // TODO: 10/17/16 if there are no results then look in the english version of this container
                setResult(result);
            }
        };
        task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
            @Override
            public void onTaskFinished(ManagedTask task) {
                Map<String, Object> data = (Map<String, Object>) task.getResult();
                if (!task.isCanceled() && data != null && position == holder.currentPosition) {
                    final TranslationNote[] notes = new TranslationNote[0];//data.get("notes");
                    final List<Link> words = (List<Link>) data.get("words");
                    final CheckingQuestion[] questions = new CheckingQuestion[0]; // data.get("questions");

                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            holder.mResourceTabs.setOnTabSelectedListener(null);
                            holder.mResourceTabs.removeAllTabs();
                            if (notes.length > 0) {
                                TabLayout.Tab tab = holder.mResourceTabs.newTab();
                                tab.setText(R.string.label_translation_notes);
                                tab.setTag(TAB_NOTES);
                                holder.mResourceTabs.addTab(tab);
                                if (mOpenResourceTab[position] == TAB_NOTES) {
                                    tab.select();
                                }
                            }
                            if (words.size() > 0) {
                                TabLayout.Tab tab = holder.mResourceTabs.newTab();
                                tab.setText(R.string.translation_words);
                                tab.setTag(TAB_WORDS);
                                holder.mResourceTabs.addTab(tab);
                                if (mOpenResourceTab[position] == TAB_WORDS) {
                                    tab.select();
                                }
                            }
                            if (questions.length > 0) {
                                TabLayout.Tab tab = holder.mResourceTabs.newTab();
                                tab.setText(R.string.questions);
                                tab.setTag(TAB_QUESTIONS);
                                holder.mResourceTabs.addTab(tab);
                                if (mOpenResourceTab[position] == TAB_QUESTIONS) {
                                    tab.select();
                                }
                            }

                            // select default tab. first notes, then words, then questions
                            if (mOpenResourceTab[position] == TAB_NOTES && notes.length == 0) {
                                mOpenResourceTab[position] = TAB_WORDS;
                            }
                            if (mOpenResourceTab[position] == TAB_WORDS && words.size() == 0) {
                                mOpenResourceTab[position] = TAB_QUESTIONS;
                            }

                            // resource list
                            if (notes.length > 0 || words.size() > 0 || questions.length > 0) {
                                renderResources(holder, position, notes, words, questions);
                            }

                            holder.mResourceTabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                                @Override
                                public void onTabSelected(TabLayout.Tab tab) {
                                    if ((int) tab.getTag() == TAB_NOTES && mOpenResourceTab[position] != TAB_NOTES) {
                                        mOpenResourceTab[position] = TAB_NOTES;
                                        // render notes
                                        renderResources(holder, position, notes, words, questions);
                                    } else if ((int) tab.getTag() == TAB_WORDS && mOpenResourceTab[position] != TAB_WORDS) {
                                        mOpenResourceTab[position] = TAB_WORDS;
                                        // render words
                                        renderResources(holder, position, notes, words, questions);
                                    } else if ((int) tab.getTag() == TAB_QUESTIONS && mOpenResourceTab[position] != TAB_QUESTIONS) {
                                        mOpenResourceTab[position] = TAB_QUESTIONS;
                                        // render questions
                                        renderResources(holder, position, notes, words, questions);
                                    }
                                }

                                @Override
                                public void onTabUnselected(TabLayout.Tab tab) {

                                }

                                @Override
                                public void onTabReselected(TabLayout.Tab tab) {

                                }
                            });
                        }
                    });
                }
            }
        });
        holder.currentResourceTaskId = TaskManager.addTask(task);

        // tap to open resources
        if (!mResourcesOpened) {
            holder.mResourceLayout.setVisibility(View.INVISIBLE);
            // TRICKY: we have to detect a single tap so that swipes do not trigger this
            final GestureDetector resourceCardDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    if (!mResourcesOpened) {
                        openResources();
                    }
                    return true;
                }
            });
            holder.mResourceCard.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return resourceCardDetector.onTouchEvent(event);
                }
            });
        } else {
            holder.mResourceLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Renders the resources card
     *
     * @param holder
     * @param position
     * @param notes
     * @param words
     * @param questions
     */
    private void renderResources(final ViewHolder holder, int position, TranslationNote[] notes, List<Link> words, CheckingQuestion[] questions) {
        if (holder.mResourceList.getChildCount() > 0) {
            holder.mResourceList.removeAllViews();
        }
        if (mOpenResourceTab[position] == TAB_NOTES) {
            // render notes
            for (final TranslationNote note : notes) {
                TextView noteView = (TextView) mContext.getLayoutInflater().inflate(R.layout.fragment_resources_list_item, null);
                noteView.setText(note.getTitle());
                noteView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getListener() != null) {
                            getListener().onTranslationNoteClick(note.getChapterId(), note.getFrameId(), note.getId(), holder.getResourceCardWidth());
                        }
                    }
                });
                Typography.formatSub(mContext, TranslationType.SOURCE, noteView, mSourceContainer.language.slug, mSourceContainer.language.direction);
                holder.mResourceList.addView(noteView);
            }
        } else if (mOpenResourceTab[position] == TAB_WORDS) {
            // render words
            for (final Link word : words) {
                TextView wordView = (TextView) mContext.getLayoutInflater().inflate(R.layout.fragment_resources_list_item, null);
                wordView.setText(word.title);
                wordView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getListener() != null) {
                            ResourceContainer rc = ContainerCache.cacheClosest(App.getLibrary(), word.language, word.project, word.resource);
                            getListener().onTranslationWordClick(rc.slug, word.chapter, holder.getResourceCardWidth());
                        }
                    }
                });
                Typography.formatSub(mContext, TranslationType.SOURCE, wordView, mSourceContainer.language.slug, mSourceContainer.language.direction);
                holder.mResourceList.addView(wordView);
            }
        } else if (mOpenResourceTab[position] == TAB_QUESTIONS) {
            // render questions
            for (final CheckingQuestion question : questions) {
                TextView questionView = (TextView) mContext.getLayoutInflater().inflate(R.layout.fragment_resources_list_item, null);
                questionView.setText(question.getQuestion());
                questionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getListener() != null) {
                            getListener().onCheckingQuestionClick(question.getChapterId(), question.getFrameId(), question.getId(), holder.getResourceCardWidth());
                        }
                    }
                });
                Typography.formatSub(mContext, TranslationType.SOURCE, questionView, mSourceContainer.language.slug, mSourceContainer.language.direction);
                holder.mResourceList.addView(questionView);
            }
        }
    }

    /**
     * generate spannable for target text.  Will add click listener for notes and verses if they are supported
     *
     * @param text
     * @param format
     * @param frameTranslation
     * @param holder
     * @param item
     * @return
     */
    private CharSequence renderTargetText(String text, TranslationFormat format, final FrameTranslation frameTranslation, final ViewHolder holder, final ReviewListItem item) {
        RenderingGroup renderingGroup = new RenderingGroup();
        boolean enableSearch = filterConstraint != null && filterSubject != null && filterSubject == TranslationFilter.FilterSubject.TARGET;
        if (Clickables.isClickableFormat(format)) {
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
                    FrameLayout verseLayout = (FrameLayout) inflater.inflate(R.layout.fragment_verse_marker, null);
                    TextView verseTitle = (TextView) verseLayout.findViewById(R.id.verse);
                    if (pin.getEndVerseNumber() > 0) {
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
                                int[] spanRange = (int[]) event.getLocalState();
                                if ((spanRange != null) && (spanRange.length >= 2)) {
                                    CharSequence in = editText.getText();
                                    if ((spanRange[0] < in.length()) && spanRange[1] < in.length()) {
                                        CharSequence out = TextUtils.concat(in.subSequence(0, spanRange[0]), in.subSequence(spanRange[1], in.length()));
                                        editText.setText(out);
                                    }
                                }
                            } else if (event.getAction() == DragEvent.ACTION_DROP) {
                                int offset = editText.getOffsetForPosition(event.getX(), event.getY());
                                CharSequence text = editText.getText();

                                offset = closestSpotForVerseMarker(offset, text);

                                if (offset >= 0) {
                                    // insert the verse at the offset
                                    text = TextUtils.concat(text.subSequence(0, offset), pin.toCharSequence(), text.subSequence(offset, text.length()));
                                } else {
                                    // place the verse back at the beginning
                                    text = TextUtils.concat(pin.toCharSequence(), text);
                                }
                                item.renderedTargetText = text;
                                editText.setText(text);
                                String translation = Translator.compileTranslation((Editable) editText.getText());
                                mTargetTranslation.applyFrameTranslation(frameTranslation, translation);

                                // Reload, so that targetText and other data are kept in sync.
                                item.load(mSourceContainer, mTargetTranslation);
                            } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                                view.setOnDragListener(null);
                                editText.setSelection(editText.getSelectionEnd());
                                // reset verse if dragged off the view
                                // TODO: 10/5/2015 perhaps we should confirm with the user?
                                if (!hasEntered) {
                                    // place the verse back at the beginning
                                    CharSequence text = editText.getText();
                                    text = TextUtils.concat(pin.toCharSequence(), text);
                                    item.renderedTargetText = text;
                                    editText.setText(text);
                                    String translation = Translator.compileTranslation((Editable) editText.getText());
                                    mTargetTranslation.applyFrameTranslation(frameTranslation, translation);

                                    // Reload, so that targetText and other data are kept in sync.
                                    item.load(mSourceContainer, mTargetTranslation);
                                }
                            } else if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                                hasEntered = true;
                            } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                                hasEntered = false;
                                editText.setSelection(editText.getSelectionEnd());
                            } else if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                                int offset = editText.getOffsetForPosition(event.getX(), event.getY());
                                if (offset >= 0) {
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
            renderer.setPopulateVerseMarkers(Frame.getVerseRange(item.sourceText, item.translationFormat));
            if (enableSearch) {
                renderingGroup.setSearchString(filterConstraint, HIGHLIGHT_COLOR);
            }

        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
            if (enableSearch) {
                renderingGroup.setSearchString(filterConstraint, HIGHLIGHT_COLOR);
            }
        }
        if (!text.trim().isEmpty()) {
            renderingGroup.init(text);
            return renderingGroup.start();
        } else {
            return "";
        }
    }

    /**
     * find closest place to drop verse marker.  Weighted toward beginning of word.
     *
     * @param offset - initial drop position
     * @param text   - edit text
     * @return
     */
    private int closestSpotForVerseMarker(int offset, CharSequence text) {
        int charsToWhiteSpace = 0;
        for (int j = offset; j >= 0; j--) {
            char c = text.charAt(j);
            boolean whitespace = isWhitespace(c);
            if (whitespace) {

                if ((j == offset) ||  // if this is already a good spot, then done
                        (j == offset - 1)) {
                    return offset;
                }

                charsToWhiteSpace = j - offset + 1;
                break;
            }
        }

        int limit = offset - charsToWhiteSpace - 1;
        if (limit > text.length()) {
            limit = text.length();
        }

        for (int j = offset + 1; j < limit; j++) {
            char c = text.charAt(j);
            boolean whitespace = isWhitespace(c);
            if (whitespace) {
                charsToWhiteSpace = j - offset;
                break;
            }
        }

        if (charsToWhiteSpace != 0) {
            offset += charsToWhiteSpace;
        }
        return offset;
    }

    /**
     * test if character is whitespace
     *
     * @param c
     * @return
     */
    private boolean isWhitespace(char c) {
        return (c == ' ') || (c == '\t') || (c == '\n') || (c == '\r');
    }

    /**
     * display selected footnote in dialog.  If editable, then it adds options to delete and edit
     * the footnote
     *
     * @param holder
     * @param item
     * @param span
     * @param editable
     */
    private void showFootnote(final ViewHolder holder, final ReviewListItem item, final NoteSpan span, final int start, final int end, boolean editable) {
        CharSequence marker = span.getPassage();
        CharSequence title = mContext.getResources().getText(R.string.title_note);
        if (!marker.toString().isEmpty()) {
            title = title + ": " + marker;
        }
        CharSequence message = span.getNotes();

        if (editable && !item.isComplete) {

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
     *
     * @param note
     * @param holder
     * @param item
     * @param start
     * @param end
     */
    private void deleteFootnote(CharSequence note, final ViewHolder holder, final ReviewListItem item, final int start, final int end) {
        final EditText editText = getEditText(holder, item);
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
     * get appropriate edit text - it is different when editing versus viewing
     *
     * @param holder
     * @param item
     * @return
     */
    private EditText getEditText(final ViewHolder holder, final ReviewListItem item) {
        if (!item.isEditing) {
            return holder.mTargetBody;
        } else {
            return holder.mTargetEditableBody;
        }
    }

    /**
     * generate spannable for source text.  Will add click listener for notes if supported
     *
     * @param text
     * @param format
     * @param holder
     * @param item
     * @param editable
     * @return
     */
    private CharSequence renderSourceText(String text, TranslationFormat format, final ViewHolder holder, final ReviewListItem item, final boolean editable) {
        RenderingGroup renderingGroup = new RenderingGroup();
        boolean enableSearch = filterConstraint != null && filterSubject != null && filterSubject == TranslationFilter.FilterSubject.SOURCE;
        if (Clickables.isClickableFormat(format)) {
            // TODO: add click listeners for verses
            Span.OnClickListener noteClickListener = new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    if (span instanceof NoteSpan) {
                        showFootnote(holder, item, (NoteSpan) span, start, end, editable);
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            };

            Clickables.setupRenderingGroup(format, renderingGroup, null, noteClickListener, false);
            if (editable) {
                if (!item.isComplete) {
                    renderingGroup.setVersesEnabled(false);
                }
                renderingGroup.setLinebreaksEnabled(true);
            }

            if (enableSearch) {
                renderingGroup.setSearchString(filterConstraint, HIGHLIGHT_COLOR);
            }
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
            if (enableSearch) {
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
     * show or hide the merge conflict icon
     * @param showMergeConflict
     */
    private void setMergeConflict(boolean showMergeConflict) {
        if(showMergeConflict != mHaveMergeConflict) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    getListener().onEnableMergeConflict(mHaveMergeConflict);
                }
            });
        }
        mHaveMergeConflict = showMergeConflict;
    }

    /**
     * check all cards for merge conflicts to see if we should show warning
     */
    private void updateMergeConflict() {
        boolean mergeConflictFound = false;
        for (ListItem item : mItems) {
            if(item.hasMergeConflicts) {
                mergeConflictFound = true;
            }
        }
        setMergeConflict(mergeConflictFound);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public int currentPosition = -1;
        public final ImageButton mAddNoteButton;
        public final ImageButton mUndoButton;
        public final ImageButton mRedoButton;
        public final ImageButton mEditButton;
        public final CardView mResourceCard;
        public final LinearLayout mMainContent;
        public final LinearLayout mResourceLayout;
        public final Switch mDoneSwitch;
        private final LinearLayout mTargetInnerCard;
        private final TabLayout mResourceTabs;
        private final LinearLayout mResourceList;
        public final LinedEditText mTargetEditableBody;
        public int mLayoutBuildNumber = -1;
        public TextWatcher mEditableTextWatcher;
        public final TextView mTargetTitle;
        public final EditText mTargetBody;
        public final CardView mTargetCard;
        public final CardView mSourceCard;
        public final TabLayout mTranslationTabs;
        public final ImageButton mNewTabButton;
        public TextView mSourceBody;
        public int currentResourceTaskId = -1;
        public int currentSourceTaskId = -1;
        public final TextView mHeadText;
        public final TextView mTailText;
        public final TextView mConflictText;
        public final LinearLayout mButtonBar;
        public final Button mCancelButton;
        public final Button mConfirmButton;
        public int currentTargetTaskId = -1;

        public ViewHolder(Context context, View v) {
            super(v);
            mMainContent = (LinearLayout)v.findViewById(R.id.main_content);
            mSourceCard = (CardView)v.findViewById(R.id.source_translation_card);
            mSourceBody = (TextView)v.findViewById(R.id.source_translation_body);
            mResourceCard = (CardView)v.findViewById(R.id.resources_card);
            mResourceLayout = (LinearLayout)v.findViewById(R.id.resources_layout);
            mResourceTabs = (TabLayout)v.findViewById(R.id.resource_tabs);
            mResourceTabs.setTabTextColors(R.color.dark_disabled_text, R.color.dark_secondary_text);
            mResourceList = (LinearLayout)v.findViewById(R.id.resources_list);
            mTargetCard = (CardView)v.findViewById(R.id.target_translation_card);
            mTargetInnerCard = (LinearLayout)v.findViewById(R.id.target_translation_inner_card);
            mTargetTitle = (TextView)v.findViewById(R.id.target_translation_title);
            mTargetBody = (EditText)v.findViewById(R.id.target_translation_body);
            mTargetEditableBody = (LinedEditText)v.findViewById(R.id.target_translation_editable_body);
            mTranslationTabs = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mEditButton = (ImageButton)v.findViewById(R.id.edit_translation_button);
            mAddNoteButton = (ImageButton)v.findViewById(R.id.add_note_button);
            mUndoButton = (ImageButton)v.findViewById(R.id.undo_button);
            mRedoButton = (ImageButton)v.findViewById(R.id.redo_button);
            mDoneSwitch = (Switch)v.findViewById(R.id.done_button);
            mTranslationTabs.setTabTextColors(R.color.dark_disabled_text, R.color.dark_secondary_text);
            mNewTabButton = (ImageButton) v.findViewById(R.id.new_tab_button);
            mHeadText = (TextView)v.findViewById(R.id.merge_head);
            mTailText = (TextView)v.findViewById(R.id.merge_tail);
            mConflictText = (TextView)v.findViewById(R.id.conflict_label);
            mButtonBar = (LinearLayout)v.findViewById(R.id.button_bar);
            mCancelButton = (Button) v.findViewById(R.id.cancel_button);
            mConfirmButton = (Button)v.findViewById(R.id.confirm_button);
        }

        /**
         * Returns the full width of the resource card
         * @return
         */
        public int getResourceCardWidth() {
            if(mResourceCard != null) {
                int rightMargin = ((ViewGroup.MarginLayoutParams)mResourceCard.getLayoutParams()).rightMargin;
                return mResourceCard.getWidth() + rightMargin;
            } else {
                return 0;
            }
        }
    }

    private static class ReviewListItem extends ListItem {
        public CharSequence headText = null;
        public boolean isFullMergeConflict = false;
        public CharSequence tailText = null;

        public ReviewListItem(String chapterSlug, String chunkSlug) {
            super(chapterSlug, chunkSlug);
        }
    }

    @Override
    public void filter(CharSequence constraint, TranslationFilter.FilterSubject subject) {
        this.filterConstraint = constraint;
        this.filterSubject = subject;
        if(constraint == null || constraint.toString().trim().isEmpty()) {
            mFilteredItems = mItems;
            mFilteredChapters = mChapters;
            return;
        }

        // clear the cards displayed since we have new search string
        mFilteredItems = new ArrayList<>();

        getListener().onSearching(true);
        TranslationFilter filter = new TranslationFilter(mSourceContainer, mTargetTranslation, subject, mItems);
        filter.setListener(new TranslationFilter.OnMatchListener() {
            @Override
            public void onMatch(ListItem item) {
                if(!mFilteredChapters.contains(item.chapterSlug)) mFilteredChapters.add(item.chapterSlug);
            }

            @Override
            public void onFinished(CharSequence constraint, List<ListItem> results) {
                mFilteredItems = results;
                triggerNotifyDataSetChanged();
                getListener().onSearching(false);
            }
        });
        filter.filter(constraint);
    }

    @Override
    public boolean hasFilter() {
        return true;
    }

    @Override
    public void mergeConflictFilter() {
        this.filterConstraint = mHaveMergeConflict ? "true" : null;

        mMergeConflictFilterMode = !mMergeConflictFilterMode; // toggle filter mode

        if(!mHaveMergeConflict || !mMergeConflictFilterMode) {
            mFilteredItems = mItems;
            mFilteredChapters = mChapters;
            return;
        }

        // clear the cards displayed since we have new search string
        mFilteredItems = new ArrayList<>();

        getListener().onSearching(true);
        MergeConflictFilter filter = new MergeConflictFilter(mSourceContainer, mTargetTranslation, mItems);
        filter.setListener(new MergeConflictFilter.OnMatchListener() {
            @Override
            public void onMatch(ListItem item) {
                if(!mFilteredChapters.contains(item.chapterSlug)) mFilteredChapters.add(item.chapterSlug);
            }

            @Override
            public void onFinished(CharSequence constraint, List<ListItem> results) {
                mFilteredItems = results;
                triggerNotifyDataSetChanged();
                getListener().onSearching(false);
            }
        });
        filter.filter(this.filterConstraint);
    }
}
