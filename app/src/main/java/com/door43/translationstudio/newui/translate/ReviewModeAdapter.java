package com.door43.translationstudio.newui.translate;

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
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.FileHistory;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LinedEditText;
import com.door43.translationstudio.core.ProjectTranslation;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.ClickableRenderingEngine;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.USFMNoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.USFMVerseSpan;
import com.door43.translationstudio.spannables.VerseSpan;

import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;
import org.unfoldingword.tools.taskmanager.ThreadableUI;
import com.door43.widget.ViewUtil;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 9/18/2015.
 */
public class ReviewModeAdapter extends ViewModeAdapter<ReviewModeAdapter.ViewHolder> {
    private static final String TAG = ReviewModeAdapter.class.getSimpleName();

    private static final int TAB_NOTES = 0;
    private static final int TAB_WORDS = 1;
    private static final int TAB_QUESTIONS = 2;
    public static final String UNDO = "Undo";
    public static final String REDO = "Redo";
    public static final String OPTIONS = "Options";
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm";
    public static final int HIGHLIGHT_COLOR = Color.YELLOW;
    private final Library mLibrary;
    private final Translator mTranslator;
    private final Activity mContext;
    private final TargetTranslation mTargetTranslation;
    private HashMap<String, Chapter> mChapters;
    private HashMap<String, Frame> mFrames;
    private SourceTranslation mSourceTranslation;
    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
    private ListItem[] mUnfilteredItems;
    private ListItem[] mFilteredItems;
    private int mLayoutBuildNumber = 0;
    private boolean mResourcesOpened = false;
    private ContentValues[] mTabs;
    private int[] mOpenResourceTab;
    private boolean mAllowFootnote = true;
    private SearchFilter mSearchFilter;
    private CharSequence mSearchString;


//    private boolean onBind = false;

    public ReviewModeAdapter(Activity context, String targetTranslationId, String sourceTranslationId, String startingChapterSlug, String startingFrameSlug, boolean resourcesOpened) {

        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        boolean usfm = mTargetTranslation.getFormat() == TranslationFormat.USFM;
        mAllowFootnote = usfm;
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation);
        mResourcesOpened = resourcesOpened;

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        mFrames = new HashMap<>();
        mChapters = new HashMap<>();
        List<ListItem> listItems = new ArrayList<>();

        // add project title card
        ListItem projectTitleItem = new ListItem(null, null);
        projectTitleItem.isProjectTitle = true;
        listItems.add(projectTitleItem);

        for(Chapter c:chapters) {
            // put in map for easier retrieval
            mChapters.put(c.getId(), c);

            String[] chapterFrameSlugs = mLibrary.getFrameSlugs(mSourceTranslation, c.getId());
            boolean setStartPosition = startingChapterSlug != null && c.getId().equals(startingChapterSlug) && chapterFrameSlugs.length > 0;

            // default starting selection is first item in chapter
            if(setStartPosition) {
                setListStartPosition(listItems.size());
            }

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

            // identify starting frame selection
            for(String frameSlug:chapterFrameSlugs) {
                if(setStartPosition && startingFrameSlug != null && frameSlug.equals(startingFrameSlug)) {
                    setListStartPosition(listItems.size());
                }
                listItems.add(new ListItem(frameSlug, c.getId()));
            }
        }
        mUnfilteredItems = listItems.toArray(new ListItem[listItems.size()]);
        mFilteredItems = mUnfilteredItems;
        mOpenResourceTab = new int[listItems.size()];

        loadTabInfo();
    }

    @Override
    void rebuild() {
        mLayoutBuildNumber ++;
        notifyDataSetChanged();
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

    @Override
    void setSourceTranslation(String sourceTranslationId) {
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug);

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<ListItem> listItems = new ArrayList<>();

        // add project title card
        ListItem projectTitleItem = new ListItem(null, null);
        projectTitleItem.isProjectTitle = true;
        listItems.add(projectTitleItem);

        mFrames = new HashMap<>();
        mChapters = new HashMap<>();
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
        mOpenResourceTab = new int[listItems.size()];

        loadTabInfo();

        notifyDataSetChanged();

        clearScreenAndStartNewSearch(mSearchString, isTargetSearch());
    }

    @Override
    void onCoordinate(final ViewHolder holder) {
        int durration = 400;
        float openWeight = 1f;
        float closedWeight = 0.765f;
        ObjectAnimator anim;
        if(mResourcesOpened) {
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
    public int getItemPosition(String chapterId, String frameId) {
        for(int i = 0; i < mFilteredItems.length; i ++) {
            ListItem item = mFilteredItems[i];
            if(item.isFrame() && item.chapterSlug.equals(chapterId) && item.frameSlug.equals(frameId)) {
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_review_list_item, parent, false);
        ViewHolder vh = new ViewHolder(parent.getContext(), v);
        return vh;
    }

    /**
     * Loads a frame from the index and caches it
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    private Frame loadFrame(String chapterSlug, String frameSlug) {
        String complexSlug = chapterSlug + "-" + frameSlug;
        if(mFrames.containsKey(complexSlug)) {
            return mFrames.get(complexSlug);
        } else {
            Frame frame = mLibrary.getFrame(mSourceTranslation, chapterSlug, frameSlug);
            mFrames.put(complexSlug, frame);
            return frame;
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.currentPosition = position;
        final ListItem item = mFilteredItems[position];

        // open/close resources
        if(mResourcesOpened) {
            holder.mMainContent.setWeightSum(.765f);
        } else {
            holder.mMainContent.setWeightSum(1f);
        }

        // fetch translation from disk
        item.loadTranslations(mSourceTranslation, mTargetTranslation, mChapters.get(item.chapterSlug), loadFrame(item.chapterSlug, item.frameSlug));

        ViewUtil.makeLinksClickable(holder.mSourceBody);

        // render the cards
        renderSourceCard(position, item, holder);
        renderTargetCard(position, item, holder);
        renderResourceCard(position, item, holder);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatSub(mContext, holder.mTargetTitle, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetEditableBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
        }
//        this.onBind = false;
    }

    /**
     * Returns the preferred translation notes.
     * if none exist in the source language it will return the english version
     * @param frame
     * @return
     */
    private static TranslationNote[] getPreferredNotes(SourceTranslation sourceTranslation, Frame frame) {
        Library library = App.getLibrary();
        TranslationNote[] notes = library.getTranslationNotes(sourceTranslation, frame.getChapterId(), frame.getId());
        if(notes.length == 0 && !sourceTranslation.sourceLanguageSlug.equals("en")) {
            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
            notes = library.getTranslationNotes(defaultSourceTranslation, frame.getChapterId(), frame.getId());
        }
        return notes;
    }

    /**
     * Returns the preferred translation words.
     * if none exist in the source language it will return the english version
     * @param sourceTranslation
     * @param frame
     * @return
     */
    private static TranslationWord[] getPreferredWords(SourceTranslation sourceTranslation, Frame frame) {
        Library library = App.getLibrary();
        TranslationWord[] words = library.getTranslationWords(sourceTranslation, frame.getChapterId(), frame.getId());
        if(words.length == 0 && !sourceTranslation.sourceLanguageSlug.equals("en")) {
            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
            words = library.getTranslationWords(defaultSourceTranslation, frame.getChapterId(), frame.getId());
        }
        return words;
    }

    /**
     * Returns the preferred checking questions.
     * if none exist in the source language it will return the english version
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    private static CheckingQuestion[] getPreferredQuestions(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        Library library = App.getLibrary();
        CheckingQuestion[] questions = library.getCheckingQuestions(sourceTranslation, chapterId, frameId);
        if(questions.length == 0 && !sourceTranslation.sourceLanguageSlug.equals("en")) {
            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
            questions = library.getCheckingQuestions(defaultSourceTranslation, chapterId, frameId);
        }
        return questions;
    }

    private void renderSourceCard(final int position, final ListItem item, final ViewHolder holder) {
        // render
        ManagedTask oldTask = TaskManager.getTask(holder.currentSourceTaskId);
        TaskManager.cancelTask(oldTask);
        if(item.renderedSourceBody == null) {
            holder.mSourceBody.setText("");
            ManagedTask task = new ManagedTask() {
                @Override
                public void start() {
                    if(interrupted()) return;
                    CharSequence text = renderSourceText(item.bodySource, mSourceTranslation.getFormat(), holder, item, false);
                    if(interrupted()) return;
                    setResult(text);
                }
            };
            task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
                @Override
                public void onTaskFinished(ManagedTask task) {
                    CharSequence data = (CharSequence)task.getResult();
                    if(!task.isCanceled() && data != null && position == holder.currentPosition) {
                        item.renderedSourceBody = data;
                        Handler hand = new Handler(Looper.getMainLooper());
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                holder.mSourceBody.setText(item.renderedSourceBody);
                            }
                        });
                    } else if (data != null && position == holder.currentPosition){
                        item.renderedSourceBody = data;
                    }
                }
            });
            holder.currentSourceTaskId = TaskManager.addTask(task);
        } else {
            holder.mSourceBody.setText(item.renderedSourceBody);
        }
    }

    private void renderTargetCard(int position, final ListItem item, final ViewHolder holder) {
        final Frame frame;
        if(item.isFrame()) {
            frame  = loadFrame(item.chapterSlug, item.frameSlug);
        } else {
            frame = null;
        }
        final Chapter chapter;
        if(item.isFrame() || item.isChapter()) {
            chapter = mChapters.get(item.chapterSlug);
        } else {
            chapter = null;
        }

        // remove old text watcher
        if(holder.mEditableTextWatcher != null) {
            holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
        }

        if(item.renderedTargetBody == null) {
            renderTargetBody(item, holder, frame);
        }

        // insert rendered text
        if(item.isEditing) {
            // editing mode
            holder.mTargetEditableBody.setText(item.renderedTargetBody);
        } else {
            // verse marker mode
            holder.mTargetBody.setText(item.renderedTargetBody);
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

        // render title
        String targetTitle = "";
        if(item.isChapter()) {
            targetTitle = mSourceTranslation.getProjectTitle()
                    + " " + Integer.parseInt(chapter.getId())
                    + " - " + mTargetLanguage.name;
        } else if(item.isFrame()) {
            ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(mChapters.get(item.chapterSlug));
            targetTitle = chapterTranslation.title;
            if(targetTitle.isEmpty()) {
                targetTitle = chapter.title;
                if (targetTitle.isEmpty()) {
                    targetTitle = mSourceTranslation.getProjectTitle()
                            + " " + Integer.parseInt(chapter.getId());
                }

            }
            targetTitle += ":" + frame.getTitle() + " - " + mTargetLanguage.name;
        } else if(item.isProjectTitle) {
            targetTitle = mTargetTranslation.getTargetLanguageName();
        }
        holder.mTargetTitle.setText(targetTitle);

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
                if(!history.isAtHead()) {
                    history.reset();
                    prepareTranslationUI(holder, item);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        if(item.isEditing) {
            holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
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
                prepareTranslationUI(holder, item);

                if(item.isEditing) {
                    holder.mTargetEditableBody.requestFocus();
                    InputMethodManager mgr = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(holder.mTargetEditableBody, InputMethodManager.SHOW_IMPLICIT);

                    // TRICKY: there may be changes to translation
                     item.loadTranslations(mSourceTranslation, mTargetTranslation, chapter, frame);

                    // re-render for editing mode
                    item.renderedTargetBody = renderSourceText(item.bodyTranslation, item.translationFormat, holder, item, true);
                    holder.mTargetEditableBody.setText(item.renderedTargetBody);
                    holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                } else {
                    if(holder.mEditableTextWatcher != null) {
                        holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                    }
                    holder.mTargetBody.requestFocus();
                    getListener().closeKeyboard();

                    // TRICKY: there may be changes to translation
                    item.loadTranslations(mSourceTranslation, mTargetTranslation, chapter, frame);

                    // re-render for verse mode
                    item.renderedTargetBody = renderTargetText(item.bodyTranslation, item.translationFormat, frame, item.frameTranslation, holder, item);
                    holder.mTargetBody.setText(item.renderedTargetBody);
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

        prepareTranslationUI(holder, item);

        // disable listener
        holder.mDoneSwitch.setOnCheckedChangeListener(null);

        // display as finished
        if(item.isTranslationFinished) {
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

        // display source language tabs
        renderTabs(holder);

        // done buttons
        holder.mDoneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // make sure to capture verse marker changes changes before dialog is displayed
                    Editable changes = holder.mTargetEditableBody.getText();
                    item.renderedTargetBody = changes;
                    String newBody = Translator.compileTranslation(changes);
                    item.bodyTranslation = newBody;

                    new AlertDialog.Builder(mContext,R.style.AppTheme_Dialog)
                            .setTitle(R.string.chunk_checklist_title)
                            .setMessage(Html.fromHtml(mContext.getString(R.string.chunk_checklist_body)))
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                        boolean success = onConfirmChunk(item, chapter, frame, mTargetTranslation.getFormat());
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

                    boolean opened;
                    if (item.isChapterReference) {
                        opened = mTargetTranslation.reopenChapterReference(chapter);
                    } else if (item.isChapterTitle) {
                        opened = mTargetTranslation.reopenChapterTitle(chapter);
                    } else if (item.isProjectTitle) {
                        opened = mTargetTranslation.openProjectTitle();
                    } else {
                        opened = mTargetTranslation.reopenFrame(frame);
                    }
                    if (opened) {
                        item.renderedTargetBody = null;
                        notifyDataSetChanged();
                    } else {
                        // TODO: 10/27/2015 notify user the frame could not be completed.
                    }
                }
            }
        });
    }

    private void prepareTranslationUI(final ViewHolder holder, ListItem item) {
        if(item.isEditing) {
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

            boolean allowFootnote = mAllowFootnote && item.isFrame();
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

    private void renderTargetBody(ListItem item, ViewHolder holder, Frame frame) {
        // render body
        if(item.isTranslationFinished || item.isEditing) {
            item.renderedTargetBody = renderSourceText(item.bodyTranslation, item.translationFormat, holder, item, true);
        } else {
            item.renderedTargetBody = renderTargetText(item.bodyTranslation, item.translationFormat, frame, item.frameTranslation, holder, item);
        }
    }

    /**
     * create a new footnote at selected position in target text.  Displays an edit dialog to enter footnote data.
     * @param holder
     * @param item
     */
    private void createFootnoteAtSelection(final ViewHolder holder, final ListItem item) {
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
     * @param initialNote
     * @param holder
     * @param item
     * @param footnotePos
     * @param footnoteEndPos
     */
    private void editFootnote(CharSequence initialNote, final ViewHolder holder, final ListItem item, final int footnotePos, final int footnoteEndPos ) {
        final EditText editText = getEditText(holder, item);
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
    private boolean verifyAndReplaceFootnote(CharSequence footnote, CharSequence original, int insertPos, final int insertEndPos, final ViewHolder holder, final ListItem item, EditText editText) {
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
    private void placeFootnote(CharSequence footnote, CharSequence original, int start, final int end, final ViewHolder holder, final ListItem item, EditText editText) {
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

        item.renderedTargetBody = newText;
        item.bodyTranslation = Translator.compileTranslation(editText.getText()); // get XML for footnote
        mTargetTranslation.applyFrameTranslation(item.frameTranslation, item.bodyTranslation); // save change

        Frame frame = null;
        if(item.isFrame()) {
            frame  = loadFrame(item.chapterSlug, item.frameSlug);
        }

        renderTargetBody(item, holder, frame); // generate spannable again adding
        editText.setText(item.renderedTargetBody);
        editText.setSelection(editText.length(), editText.length());
    }

    /**
     * save changed text
     * @param s A string or editable
     * @param item
     * @return
     */
    private String applyChangedText(CharSequence s, ViewHolder holder, ListItem item) {
        String translation;
        if(s instanceof Editable) {
            translation = Translator.compileTranslation((Editable) s);
        } else {
            translation = s.toString();
        }

        if (item.isChapterReference) {
            mTargetTranslation.applyChapterReferenceTranslation(item.chapterTranslation, translation);
        } else if (item.isChapterTitle) {
            mTargetTranslation.applyChapterTitleTranslation(item.chapterTranslation, translation);
        } else if (item.isProjectTitle) {
            try {
                mTargetTranslation.applyProjectTitleTranslation(s.toString());
            } catch (IOException e) {
                Logger.e(ReviewModeAdapter.class.getName(), "Failed to save the project title translation", e);
            }
        } else if (item.isFrame()) {
            mTargetTranslation.applyFrameTranslation(item.frameTranslation, translation);
        }
        item.renderedTargetBody = renderSourceText(translation, item.translationFormat, holder, item, true);
        return translation;
    }

    /**
     * restore the text from previous commit for fragment
     * @param holder
     * @param item
     */
    private void undoTextInTarget(final ViewHolder holder, final ListItem item) {
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
                try {
                    if(commit != null) {
                        String text = history.read(commit);
                        // save and update ui
                        if (text != null) {
                            // TRICKY: prevent history from getting rolled back soon after the user views it
                            restartAutoCommitTimer();
                            applyChangedText(text, holder, item);
                            holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                            holder.mTargetEditableBody.setText(item.renderedTargetBody);
                            holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
    private void redoTextInTarget(final ViewHolder holder, final ListItem item) {
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
                    if(commit != null) {
                        String text = history.read(commit);
                        // save and update ui
                        if (text != null) {
                            // TRICKY: prevent history from getting rolled back soon after the user views it
                            restartAutoCommitTimer();
                            applyChangedText(text, holder, item);
                            holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                            holder.mTargetEditableBody.setText(item.renderedTargetBody);
                            holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
    private boolean onConfirmChunk(final ListItem item, final Chapter chapter, final Frame frame, TranslationFormat format) {
        boolean success = true; // So far, so good.

        // Check for empty translation.
        if (item.bodyTranslation.isEmpty()) {
            Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.translate_first, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
            snack.show();
            success = false;
        }

        if(frame != null) {
            Matcher matcher;
            int lowVerse = -1;
            int highVerse = 999999999;
            int[] range = frame.getVerseRange();
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
                    matcher = USFM_CONSECUTIVE_VERSE_MARKERS.matcher(item.bodyTranslation);
                } else {
                    matcher = CONSECUTIVE_VERSE_MARKERS.matcher(item.bodyTranslation);
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
                    matcher = USFM_VERSE_MARKER.matcher(item.bodyTranslation);
                } else {
                    matcher = VERSE_MARKER.matcher(item.bodyTranslation);
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
        }

        // Everything looks good so far. Try and commit.
        if (success) {
            if (item.isChapterReference) {
                success = mTargetTranslation.finishChapterReference(chapter);
            } else if (item.isChapterTitle) {
                success = mTargetTranslation.finishChapterTitle(chapter);
            } else if (item.isProjectTitle) {
                success = mTargetTranslation.closeProjectTitle();
            } else if(frame != null){
                success = mTargetTranslation.finishFrame(frame);
            } else {
                success = false;
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
                String frameComplexId = frame == null ? "" : ":" + frame.getComplexId();
                Logger.e(TAG, "Failed to commit translation of " + mTargetTranslation.getId() + frameComplexId, e);
            }
            item.isEditing = false;
            item.renderedTargetBody = null;
            notifyDataSetChanged();
        }

        return success;
    }


    /**
     * Renders the source language tabs on the target card
     * @param holder
     */
    private void renderTabs(ViewHolder holder) {
        holder.mTranslationTabs.setOnTabSelectedListener(null);
        holder.mTranslationTabs.removeAllTabs();
        for(ContentValues values:mTabs) {
            TabLayout.Tab tab = holder.mTranslationTabs.newTab();
            tab.setText(values.getAsString("title"));
            tab.setTag(values.getAsString("tag"));
            holder.mTranslationTabs.addTab(tab);
        }

        // open selected tab
        for(int i = 0; i < holder.mTranslationTabs.getTabCount(); i ++) {
            TabLayout.Tab tab = holder.mTranslationTabs.getTabAt(i);
            if(tab.getTag().equals(mSourceTranslation.getId())) {
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

    private void renderResourceCard(final int position, ListItem item, final ViewHolder holder) {
        // clean up view
        if(holder.mResourceList.getChildCount() > 0) {
            holder.mResourceList.removeAllViews();
        }
        holder.mResourceTabs.setOnTabSelectedListener(null);
        holder.mResourceTabs.removeAllTabs();

        // skip if chapter title/reference
        if(!item.isFrame()) {
            return;
        }

        final Frame  frame = loadFrame(item.chapterSlug, item.frameSlug);

        // clear resource card
        renderResources(holder, position, new TranslationNote[0], new TranslationWord[0], new CheckingQuestion[0]);

        // prepare task to load resources
        ManagedTask oldTask = TaskManager.getTask(holder.currentResourceTaskId);
        TaskManager.cancelTask(oldTask);
        ManagedTask task = new ManagedTask() {
            @Override
            public void start() {
                if(interrupted()) return;
                TranslationNote[] notes = getPreferredNotes(mSourceTranslation, frame);
                if(interrupted()) return;
                TranslationWord[] words = getPreferredWords(mSourceTranslation, frame);
                if(interrupted()) return;
                CheckingQuestion[] questions = getPreferredQuestions(mSourceTranslation, frame.getChapterId(), frame.getId());
                if(interrupted()) return;
                Map<String, Object> result = new HashMap<>();
                result.put("notes", notes);
                result.put("words", words);
                result.put("questions", questions);
                setResult(result);
            }
        };
        task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
            @Override
            public void onTaskFinished(ManagedTask task) {
                Map<String, Object> data = (Map<String, Object>)task.getResult();
                if(!task.isCanceled() && data != null && position == holder.currentPosition) {
                    final TranslationNote[] notes = (TranslationNote[]) data.get("notes");
                    final TranslationWord[] words = (TranslationWord[]) data.get("words");
                    final CheckingQuestion[] questions = (CheckingQuestion[]) data.get("questions");

                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            holder.mResourceTabs.setOnTabSelectedListener(null);
                            holder.mResourceTabs.removeAllTabs();
                            if(notes.length > 0) {
                                TabLayout.Tab tab = holder.mResourceTabs.newTab();
                                tab.setText(R.string.label_translation_notes);
                                tab.setTag(TAB_NOTES);
                                holder.mResourceTabs.addTab(tab);
                                if(mOpenResourceTab[position] == TAB_NOTES) {
                                    tab.select();
                                }
                            }
                            if(words.length > 0) {
                                TabLayout.Tab tab = holder.mResourceTabs.newTab();
                                tab.setText(R.string.translation_words);
                                tab.setTag(TAB_WORDS);
                                holder.mResourceTabs.addTab(tab);
                                if(mOpenResourceTab[position] == TAB_WORDS) {
                                    tab.select();
                                }
                            }
                            if(questions.length > 0) {
                                TabLayout.Tab tab = holder.mResourceTabs.newTab();
                                tab.setText(R.string.questions);
                                tab.setTag(TAB_QUESTIONS);
                                holder.mResourceTabs.addTab(tab);
                                if(mOpenResourceTab[position] == TAB_QUESTIONS) {
                                    tab.select();
                                }
                            }

                            // select default tab. first notes, then words, then questions
                            if(mOpenResourceTab[position] == TAB_NOTES && notes.length == 0) {
                                mOpenResourceTab[position] = TAB_WORDS;
                            }
                            if(mOpenResourceTab[position] == TAB_WORDS && words.length == 0) {
                                mOpenResourceTab[position] = TAB_QUESTIONS;
                            }

                            // resource list
                            if(notes.length > 0 || words.length > 0 || questions.length > 0) {
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
        if(!mResourcesOpened) {
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
     * @param holder
     * @param position
     * @param notes
     * @param words
     * @param questions
     */
    private void renderResources(final ViewHolder holder, int position, TranslationNote[] notes, TranslationWord[] words, CheckingQuestion[] questions) {
        if(holder.mResourceList.getChildCount() > 0) {
            holder.mResourceList.removeAllViews();
        }
        if(mOpenResourceTab[position] == TAB_NOTES) {
            // render notes
            for(final TranslationNote note:notes) {
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
                Typography.formatSub(mContext, noteView, mSourceLanguage.getId(), mSourceLanguage.getDirection());
                holder.mResourceList.addView(noteView);
            }
        } else if(mOpenResourceTab[position] == TAB_WORDS) {
            // render words
            for(final TranslationWord word:words) {
                TextView wordView = (TextView) mContext.getLayoutInflater().inflate(R.layout.fragment_resources_list_item, null);
                wordView.setText(word.getTerm());
                wordView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getListener() != null) {
                            getListener().onTranslationWordClick(word.getId(), holder.getResourceCardWidth());
                        }
                    }
                });
                Typography.formatSub(mContext, wordView, mSourceLanguage.getId(), mSourceLanguage.getDirection());
                holder.mResourceList.addView(wordView);
            }
        } else if(mOpenResourceTab[position] == TAB_QUESTIONS) {
            // render questions
            for(final CheckingQuestion question:questions) {
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
                Typography.formatSub(mContext, questionView, mSourceLanguage.getId(), mSourceLanguage.getDirection());
                holder.mResourceList.addView(questionView);
            }
        }
    }

    /**
     * generate spannable for target text.  Will add click listener for notes and verses if they are supported
     * @param text
     * @param format
     * @param frame
     * @param frameTranslation
     * @param holder
     * @param item
     * @return
     */
    private CharSequence renderTargetText(String text, TranslationFormat format, final Frame frame, final FrameTranslation frameTranslation, final ViewHolder holder, final ListItem item) {
        RenderingGroup renderingGroup = new RenderingGroup();
        if(Clickables.isClickableFormat(format) && frame != null) {
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
                    ClipData dragData = ClipData.newPlainText(frame.getComplexId(), span.getMachineReadable());
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
                                item.renderedTargetBody = text;
                                editText.setText(text);
                                String translation = Translator.compileTranslation((Editable)editText.getText());
                                mTargetTranslation.applyFrameTranslation(frameTranslation, translation);

                                // Reload, so that bodyTranslation and other data are kept in sync.
                                item.loadTranslations(mSourceTranslation, mTargetTranslation, null, frame);
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                                view.setOnDragListener(null);
                                editText.setSelection(editText.getSelectionEnd());
                                // reset verse if dragged off the view
                                // TODO: 10/5/2015 perhaps we should confirm with the user?
                                if(!hasEntered) {
                                    // place the verse back at the beginning
                                    CharSequence text = editText.getText();
                                    text = TextUtils.concat(pin.toCharSequence(), text);
                                    item.renderedTargetBody = text;
                                    editText.setText(text);
                                    String translation = Translator.compileTranslation((Editable)editText.getText());
                                    mTargetTranslation.applyFrameTranslation(frameTranslation, translation);

                                    // Reload, so that bodyTranslation and other data are kept in sync.
                                    item.loadTranslations(mSourceTranslation, mTargetTranslation, null, frame);
                                }
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
            renderer.setPopulateVerseMarkers(frame.getVerseRange());
            if( isTargetSearch() ) {
                renderingGroup.setSearchString(mSearchString, HIGHLIGHT_COLOR);
            }

        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
            if( isTargetSearch() ) {
                renderingGroup.setSearchString(mSearchString, HIGHLIGHT_COLOR);
            }
        }
        if(!text.trim().isEmpty()) {
            renderingGroup.init(text);
            return renderingGroup.start();
        } else {
            return "";
        }
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
    private void showFootnote(final ViewHolder holder, final ListItem item, final NoteSpan span, final int start, final int end, boolean editable) {
        CharSequence marker = span.getPassage();
        CharSequence title = mContext.getResources().getText(R.string.title_note);
        if(!marker.toString().isEmpty()) {
            title = title + ": " + marker;
        }
        CharSequence message = span.getNotes();

        if(editable && !item.isTranslationFinished) {

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
    private void deleteFootnote(CharSequence note, final ViewHolder holder, final ListItem item, final int start, final int end ) {
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
     * @param holder
     * @param item
     * @return
     */
    private EditText getEditText(final ViewHolder holder, final ListItem item) {
        if (!item.isEditing) {
            return holder.mTargetBody;
        } else {
            return holder.mTargetEditableBody;
        }
    }

    /**
     * generate spannable for source text.  Will add click listener for notes if supported
     * @param text
     * @param format
     * @param holder
     * @param item
     * @param editable
     * @return
     */
    private CharSequence renderSourceText(String text, TranslationFormat format, final ViewHolder holder, final ListItem item, final boolean editable) {
        RenderingGroup renderingGroup = new RenderingGroup();
        boolean enableSearch = (isTargetSearch() && editable)  || (!isTargetSearch() && !editable);
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
                if(!item.isTranslationFinished) {
                    renderingGroup.setVersesEnabled(false);
                }
                renderingGroup.setLinebreaksEnabled(true);
            }

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

    private static class ListItem {
        private final String frameSlug;
        private final String chapterSlug;
        private boolean isChapterReference = false;
        private boolean isChapterTitle = false;
        private boolean isProjectTitle = false;
        private boolean isEditing = false;
        private CharSequence renderedSourceBody;
        private CharSequence renderedTargetBody;
        private TranslationFormat translationFormat;
        private String bodyTranslation;
        private boolean isTranslationFinished;
        private String bodySource;
        private FrameTranslation frameTranslation;
        private ChapterTranslation chapterTranslation;
        private ProjectTranslation projectTranslation;
        private FileHistory fileHistory = null;
        private boolean mHighlightSource = false;
        private boolean mHighlightTarget = false;

        public ListItem(String frameSlug, String chapterSlug) {
            this.frameSlug = frameSlug;
            this.chapterSlug = chapterSlug;
        }

        public boolean isFrame() {
            return this.frameSlug != null;
        }

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
         * Loads the file history or returns it from the cache
         * @param targetTranslation
         * @return
         */
        public FileHistory getFileHistory(TargetTranslation targetTranslation) {
            if(this.fileHistory != null) {
                return this.fileHistory;
            }

            FileHistory history = null;
            if(this.isChapterReference) {
                history = targetTranslation.getChapterReferenceHistory(this.chapterTranslation);
            } else if(this.isChapterTitle) {
                history = targetTranslation.getChapterTitleHistory(this.chapterTranslation);
            } else if(this.isProjectTitle) {
                history = targetTranslation.getProjectTitleHistory();
            } else if(this.isFrame()) {
                history = targetTranslation.getFrameHistory(this.frameTranslation);
            }
            this.fileHistory = history;
            return history;
        }

        /**
         * Loads the correct translation information into the item
         * @param targetTranslation
         * @param chapter
         * @param frame
         */
        public void loadTranslations(SourceTranslation sourceTranslation, TargetTranslation targetTranslation, Chapter chapter, Frame frame) {
            if(isChapterReference || isChapterTitle) {
                frameTranslation = null;
                chapterTranslation = targetTranslation.getChapterTranslation(chapter);
                translationFormat = targetTranslation.getFormat();
                if (isChapterTitle) {
                    bodyTranslation = chapterTranslation.title;
                    bodySource = chapter.title;
                    isTranslationFinished = chapterTranslation.isTitleFinished();
                } else {
                    bodyTranslation = chapterTranslation.reference;
                    bodySource = chapter.reference;
                    isTranslationFinished = chapterTranslation.isReferenceFinished();
                }
            } else if(isProjectTitle) {
                projectTranslation = targetTranslation.getProjectTranslation();
                bodyTranslation = projectTranslation.getTitle();
                bodySource = sourceTranslation.getProjectTitle();
                isTranslationFinished = projectTranslation.isTitleFinished();
            } else {
                chapterTranslation = null;
                frameTranslation = targetTranslation.getFrameTranslation(frame);
                translationFormat = targetTranslation.getFormat();
                bodyTranslation = frameTranslation.body;
                bodySource = frame.body;
                isTranslationFinished = frameTranslation.isFinished();
            }
        }
    }

    /**
     * remove displayed cards
     * @param searchString
     * @param searchTarget
     */
    public void clearScreenAndStartNewSearch(final CharSequence searchString, final boolean searchTarget) {

        // clear the cards displayed since we have new search string
        mFilteredItems = new ListItem[0];
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
                        if (item.renderedSourceBody != null) { // if source has already been rendered, search that
                            match = item.renderedSourceBody.toString().toLowerCase().contains(matchString);

                        } else { // next best we search source
                            if (item.bodySource == null) { // if source hasn't been loaded
                                item.loadTranslations(mSourceTranslation, mTargetTranslation, mChapters.get(item.chapterSlug), loadFrame(item.chapterSlug, item.frameSlug));
                            }
                            if (item.bodySource != null) {
                                match = item.bodySource.toLowerCase().contains(matchString);
                            }
                        }
                    } else { // search the target
                        if (item.renderedTargetBody != null) { // if target has already been rendered, search that
                            match = item.renderedTargetBody.toString().toLowerCase().contains(matchString);

                        } else { // next best we search source
                            if (item.bodyTranslation == null) { // if source hasn't been loaded
                                item.loadTranslations(mSourceTranslation, mTargetTranslation, mChapters.get(item.chapterSlug), loadFrame(item.chapterSlug, item.frameSlug));
                            }
                            if (item.bodyTranslation != null) {
                                match = item.bodyTranslation.toLowerCase().contains(matchString);
                            }
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
            notifyDataSetChanged();
            getListener().onSetBusyIndicator(false);
        }
    }
}
