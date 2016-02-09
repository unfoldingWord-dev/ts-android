package com.door43.translationstudio.newui.translate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.CheckingQuestion;
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
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.VersePinSpan;
import com.door43.widget.ViewUtil;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private final Library mLibrary;
    private final Translator mTranslator;
    private final Activity mContext;
    private final TargetTranslation mTargetTranslation;
    private HashMap<String, Chapter> mChapters;
    private HashMap<String, Frame> mFrames;
    private SourceTranslation mSourceTranslation;
    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
    private ListItem[] mListItems;
    private int mLayoutBuildNumber = 0;
    private boolean mResourcesOpened = false;
    private ContentValues[] mTabs;
    private int[] mOpenResourceTab;
//    private boolean onBind = false;

    public ReviewModeAdapter(Activity context, String targetTranslationId, String sourceTranslationId, String startingChapterSlug, String startingFrameSlug, boolean resourcesOpened) {

        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectSlug, mSourceTranslation.sourceLanguageSlug);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());
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
        mListItems = listItems.toArray(new ListItem[listItems.size()]);
        mOpenResourceTab = new int[listItems.size()];

        loadTabInfo();

        notifyDataSetChanged();
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
    public int getItemPosition(String chapterId, String frameId) {
        for(int i = 0; i < mListItems.length; i ++) {
            ListItem item = mListItems[i];
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
//        this.onBind = true;
        final ListItem item = mListItems[position];

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
        Library library = AppContext.getLibrary();
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
        Library library = AppContext.getLibrary();
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
        Library library = AppContext.getLibrary();
        CheckingQuestion[] questions = library.getCheckingQuestions(sourceTranslation, chapterId, frameId);
        if(questions.length == 0 && !sourceTranslation.sourceLanguageSlug.equals("en")) {
            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
            questions = library.getCheckingQuestions(defaultSourceTranslation, chapterId, frameId);
        }
        return questions;
    }

    private void renderSourceCard(int position, final ListItem item, ViewHolder holder) {
        // render
        if(item.renderedSourceBody == null) {
            item.renderedSourceBody = renderSourceText(item.bodySource, item.translationFormat);
        }
        holder.mSourceBody.setText(item.renderedSourceBody);
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

        // render body
        if(item.renderedTargetBody == null) {
            if(item.isTranslationFinished || item.isEditing) {
                item.renderedTargetBody = renderSourceText(item.bodyTranslation, item.translationFormat);
            } else {
                item.renderedTargetBody = renderTargetText(item.bodyTranslation, item.translationFormat, frame, item.frameTranslation, holder, item);
            }
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
                String translation = applyChangedText(s, item);
                clearCurrentCommit(holder, item);

                // update view if pasting text
                // TRICKY: anything worth rendering will need to change by at least 7 characters
                // <a></a> <-- at least 7 characters are required to create a tag for rendering.
                int minDeviation = 7;
                if(count - before > minDeviation) {
                    int scrollX = holder.mTargetEditableBody.getScrollX();
                    int scrollY = holder.mTargetEditableBody.getScrollX();
                    int selection = holder.mTargetEditableBody.getSelectionStart();

                    holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                    holder.mTargetEditableBody.setText(item.renderedTargetBody);
                    holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);

                    holder.mTargetEditableBody.scrollTo(scrollX, scrollY);
                    if (selection > holder.mTargetEditableBody.length()) {
                        selection = holder.mTargetEditableBody.length();
                    }
                    holder.mTargetEditableBody.setSelection(selection);
                    holder.mTargetEditableBody.clearFocus();
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
                doUndo(holder, item);
            }
        });
        holder.mRedoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRedo(holder, item);
            }
        });

        // editing button
        final GestureDetector detector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                item.isEditing = !item.isEditing;
                if(item.isEditing) {
                    // open editing mode
                    holder.mEditButton.setImageResource(R.drawable.ic_done_black_24dp);
                    holder.mUndoButton.setVisibility(View.VISIBLE);
                    holder.mRedoButton.setVisibility(View.VISIBLE);
                    holder.mTargetBody.setVisibility(View.GONE);
                    holder.mTargetEditableBody.setVisibility(View.VISIBLE);
                    holder.mTargetEditableBody.requestFocus();
                    holder.mTargetInnerCard.setBackgroundResource(R.color.white);
                    holder.mTargetEditableBody.setEnableLines(true);
                    InputMethodManager mgr = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(holder.mTargetEditableBody, InputMethodManager.SHOW_IMPLICIT);

                    // TRICKY: there may be changes to translation
                    item.loadTranslations(mSourceTranslation, mTargetTranslation, chapter, frame);
                    // re-render for editing mode
                    item.renderedTargetBody = renderSourceText(item.bodyTranslation, item.translationFormat);
                    holder.mTargetEditableBody.setText(item.renderedTargetBody);
                    holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                } else {
                    // close editing mode
                    saveRestoredText( holder, item);
                    holder.mEditButton.setImageResource(R.drawable.ic_mode_edit_black_24dp);
                    holder.mUndoButton.setVisibility(View.GONE);
                    holder.mRedoButton.setVisibility(View.GONE);
                    holder.mTargetBody.setVisibility(View.VISIBLE);
                    holder.mTargetEditableBody.setVisibility(View.GONE);
                    holder.mTargetInnerCard.setBackgroundResource(R.color.white);
                    holder.mTargetEditableBody.setEnableLines(false);
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


        // display verse/editing mode
        if(item.isEditing) {
            holder.mEditButton.setImageResource(R.drawable.ic_done_black_24dp);
            holder.mUndoButton.setVisibility(View.VISIBLE);
            holder.mRedoButton.setVisibility(View.VISIBLE);
            holder.mTargetBody.setVisibility(View.GONE);
            holder.mTargetEditableBody.setVisibility(View.VISIBLE);
            holder.mTargetEditableBody.setEnableLines(true);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        } else {
            holder.mEditButton.setImageResource(R.drawable.ic_mode_edit_black_24dp);
            holder.mUndoButton.setVisibility(View.GONE);
            holder.mRedoButton.setVisibility(View.GONE);
            holder.mTargetBody.setVisibility(View.VISIBLE);
            holder.mTargetEditableBody.setVisibility(View.GONE);
            holder.mTargetEditableBody.setEnableLines(false);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        }

        // disable listener
        holder.mDoneSwitch.setOnCheckedChangeListener(null);

        // display as finished
        if(item.isTranslationFinished) {
            holder.mEditButton.setVisibility(View.GONE);
            holder.mUndoButton.setVisibility(View.GONE);
            holder.mRedoButton.setVisibility(View.GONE);
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

                    CustomAlertDialog.Create(mContext)
                            .setTitle(R.string.chunk_checklist_title)
                            .setMessageHtml(R.string.chunk_checklist_body)
                            .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if(!item.isEditing) { // make sure to capture verse marker changes
                                                item.renderedTargetBody = holder.mTargetEditableBody.getText();
                                            }
                                            boolean success = onConfirmChunk(item, chapter, frame);
                                            if(success) {
                                                saveRestoredText( holder, item);
                                            }
                                            holder.mDoneSwitch.setChecked(success);
                                        }
                                    }
                            )
                            .setNegativeButton(R.string.title_cancel, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    holder.mDoneSwitch.setChecked(false); // force back off if not accepted
                                }
                            })
                            .show("Chunk2");

                } else { // done button checked off

                    boolean opened;
                    if (item.isChapterReference) {
                        opened = mTargetTranslation.reopenChapterReference(chapter);
                    } else if (item.isChapterTitle) {
                        opened = mTargetTranslation.reopenChapterTitle(chapter);
                    } else if (item.isProjectTitle) {
                        opened = mTargetTranslation.reopenProjectTitle();
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

    public void clearCurrentCommit(ViewHolder holder, ListItem item) {
        item.currentCommit = null; // clears undo position
        item.commits = null;
        holder.mCurrentCommitItem = null;
    }

    /**
     * check if user has restored old text.  If so then save it
     * @param holder
     * @param item
     */
    private void saveRestoredText(ViewHolder holder, ListItem item) {
        if(item.currentCommit != null) {
            CharSequence s = holder.mTargetEditableBody.getText();
            applyChangedText(s, item);
            clearCurrentCommit(holder, item);
        }
    }

    /**
     * check if user has restored old text.  If so then save it
     * @param holder
     */
    public void saveRestoredText(ViewHolder holder) {
        ListItem item = holder.mCurrentCommitItem;
        if(item != null) {
            saveRestoredText(holder, item);
        }
    }

    @Override
    public void onViewRecycled (ViewHolder holder) {
        saveRestoredText(holder);
    }

    @Override
    public void onViewDetachedFromWindow (ViewHolder holder) {
        saveRestoredText(holder);
    }

    /**
     * save changed text
     * @param s
     * @param item
     * @return
     */
    private String applyChangedText(CharSequence s, ListItem item) {
        String translation = Translator.compileTranslation((Editable) s);
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
        item.renderedTargetBody = renderSourceText(translation, item.translationFormat);
        return translation;
    }

    /**
     * restore the text from previous commit for fragment
     * @param holder
     * @param item
     */
    private void doUndo(final ViewHolder holder, final ListItem item) {
        holder.mUndoButton.setVisibility(View.INVISIBLE);
        holder.mRedoButton.setVisibility(View.INVISIBLE);

        showRestoreMessage(item, R.string.label_undo);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Git git = mTargetTranslation.getGit();
                    File file = getFileForItem(item);
                    fetchCommitList(git, file, item);
                    RevCommit commit = mTargetTranslation.getUndoCommit(item.commits, item.currentCommit);
                    restoreCommitText(holder, item, git, file, commit);
                } catch (Exception e) {
                    Logger.w(TAG, "error getting commit list", e);
                }
            }
        });
    }

    /**
     * restore the text from later commit for fragment
     * @param holder
     * @param item
     */
    private void doRedo(final ViewHolder holder, final ListItem item) {
        holder.mUndoButton.setVisibility(View.INVISIBLE);
        holder.mRedoButton.setVisibility(View.INVISIBLE);

        showRestoreMessage(item, R.string.label_redo);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Git git = mTargetTranslation.getGit();
                    File file = getFileForItem(item);
                    fetchCommitList(git, file, item);
                    RevCommit commit = mTargetTranslation.getRedoCommit(item.commits, item.currentCommit);
                    restoreCommitText(holder, item, git, file, commit);
                } catch (Exception e) {
                    Logger.w(TAG, "error getting commit list", e);
                }
            }
        });
    }

    private void showRestoreMessage(final ListItem item, final String message) {
        if(item.restoreMsg != null) {
            item.restoreMsg.cancel(); // remove previous message
        }

        //create new message to display
        item.restoreMsg = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        item.restoreMsg.setGravity(Gravity.TOP, 0, 0);
        item.restoreMsg.show();
    }

    private void showRestoreMessage(ListItem item, int resId) {
        String message = mContext.getResources().getString(resId);
        showRestoreMessage(item, message);
    }

    public void fetchCommitList(Git git, File file, ListItem item) throws IOException, GitAPIException {
        if(null == item.commits) {
            item.commits = mTargetTranslation.getCommitList(git, file);
        }
    }

    /**
     * restore commited file contents to current fragment
     * @param holder
     * @param item
     * @param git
     * @param file
     * @param commit
     */
    private void restoreCommitText(final ViewHolder holder, final ListItem item, final Git git, final File file, final RevCommit commit) {

        String message = null;

        if (commit != null) {
            Locale current = mContext.getResources().getConfiguration().locale;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", current);
            Date commitTime = new Date(commit.getCommitTime() * 1000L);
            String restoreTimeFormat = mContext.getResources().getString(R.string.restored_to_time);
            message = String.format(restoreTimeFormat, sdf.format(commitTime));
        } else {
            message = mContext.getResources().getString(R.string.restoring_end);
        }

        final String finalMessage = message;
        String committedText = null;

        if (null != commit) {
            committedText = mTargetTranslation.getCommittedFileContents(git, file, commit);
            item.currentCommit = commit;
            holder.mCurrentCommitItem = item;
        } else {
            Logger.i(TAG, "restore commit not found");
        }

        final String finalCommittedText = committedText;

        mContext.runOnUiThread(new Runnable() {
            public void run() {
                if (null != commit) {
                    item.renderedTargetBody = renderSourceText(finalCommittedText, item.translationFormat);
                    holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
                    holder.mTargetEditableBody.setText(item.renderedTargetBody);
                    holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
                }

                holder.mUndoButton.setVisibility(View.VISIBLE);
                holder.mRedoButton.setVisibility(View.VISIBLE);

                showRestoreMessage(item, finalMessage);
            }
        });
    }

    private File getFileForItem(ListItem item) {
        File file = null;

        if(item.isChapterReference) {
            file = mTargetTranslation.getChapterReferenceFile(item.frameTranslation.getChapterId());
        } else if(item.isChapterTitle) {
            file = mTargetTranslation.getChapterTitleFile(item.frameTranslation.getChapterId());
        } else if(item.isProjectTitle) {
            file = mTargetTranslation.getProjectTitleFile();
        } else if(item.isFrame()) {
            file = mTargetTranslation.getFrameFile(item.frameTranslation.getChapterId(), item.frameTranslation.getId());
        }
        if(file != null) { // get relative path
            String path = file.toString();
            String folder = mTargetTranslation.getPath().toString();
            int pos = path.indexOf(folder);
            if(pos >= 0) {
                String subPath = path.substring(pos + folder.length() + 1);
                file = new File(subPath);
            }
        }
        return file;
    }

    private static final Pattern CONSECUTIVE_VERSE_MARKERS =
            Pattern.compile("(<verse [^>]+/>\\s*){2}");

    private static final Pattern VERSE_MARKER =
            Pattern.compile("<verse\\s+number=\"(\\d+)\"[^>]*>");

    /**
     * Performs some validation, and commits changes if ready.
     * @return true if the section was successfully confirmed; otherwise false.
     */
    private boolean onConfirmChunk(final ListItem item, final Chapter chapter, final Frame frame) {
        boolean success = true; // So far, so good.

        // Check for empty translation.
        if (item.bodyTranslation.isEmpty()) {
            Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.translate_first, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
            snack.show();
            success = false;
        }

        // Check for contiguous verse numbers.
        if (success) {
            Matcher matcher = CONSECUTIVE_VERSE_MARKERS.matcher(item.bodyTranslation);
            if (matcher.find()) {
                Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.consecutive_verse_markers, Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                snack.show();
                success = false;
            }
        }

        // Check for out-of-order verse markers.
        if (success) {
            Matcher matcher = VERSE_MARKER.matcher(item.bodyTranslation);
            int lastVerseSeen = 0;
            while (matcher.find()) {
                int currentVerse = Integer.valueOf(matcher.group(1));
                if (currentVerse < lastVerseSeen) {
                    success = false;
                    break;
                } else {
                    lastVerseSeen = currentVerse;
                }
            }
            if (!success) {
                Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.outoforder_verse_markers, Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }

        // Everything looks good so far. Try and commit.
        if (success) {
            if (item.isChapterReference) {
                success = mTargetTranslation.finishChapterReference(chapter);
            } else if (item.isChapterTitle) {
                success = mTargetTranslation.finishChapterTitle(chapter);
            } else if (item.isProjectTitle) {
                success = mTargetTranslation.finishProjectTitle();
            } else {
                success = mTargetTranslation.finishFrame(frame);
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

        Frame  frame = loadFrame(item.chapterSlug, item.frameSlug);

        // resource tabs
        final TranslationNote[] notes = getPreferredNotes(mSourceTranslation, frame);
        if(notes.length > 0) {
            TabLayout.Tab tab = holder.mResourceTabs.newTab();
            tab.setText(R.string.label_translation_notes);
            tab.setTag(TAB_NOTES);
            holder.mResourceTabs.addTab(tab);
            if(mOpenResourceTab[position] == TAB_NOTES) {
                tab.select();
            }
        }
        final TranslationWord[] words = getPreferredWords(mSourceTranslation, frame);
        if(words.length > 0) {
            TabLayout.Tab tab = holder.mResourceTabs.newTab();
            tab.setText(R.string.translation_words);
            tab.setTag(TAB_WORDS);
            holder.mResourceTabs.addTab(tab);
            if(mOpenResourceTab[position] == TAB_WORDS) {
                tab.select();
            }
        }
        final CheckingQuestion[] questions = getPreferredQuestions(mSourceTranslation, frame.getChapterId(), frame.getId());
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

        // resource list
        if(notes.length > 0 || words.length > 0 || questions.length > 0) {
            renderResources(holder, position, notes, words, questions);
        }

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
     * generate spannable for target text.  Will add click listener for notes and verses if USX
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
        if(format == TranslationFormat.USX && frame != null) {
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
                    final VersePinSpan pin = ((VersePinSpan) span);

                    // create drag shadow
                    LayoutInflater inflater = (LayoutInflater)AppContext.context().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                                CharSequence in = editText.getText();
                                CharSequence out = TextUtils.concat(in.subSequence(0, spanRange[0]), in.subSequence(spanRange[1], in.length()));
                                editText.setText(out);
                            } else if(event.getAction() == DragEvent.ACTION_DROP) {
                                int offset = editText.getOffsetForPosition(event.getX(), event.getY());
                                CharSequence text = editText.getText();
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
            USXRenderer usxRenderer = new USXRenderer(verseClickListener, null);
            usxRenderer.setPopulateVerseMarkers(frame.getVerseRange());
            renderingGroup.addEngine(usxRenderer);
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
        }
        if(!text.trim().isEmpty()) {
            renderingGroup.init(text);
            return renderingGroup.start();
        } else {
            return "";
        }
    }

    /**
     * generate spannable for source text.  Will add click listener for notes if USX
     * @param text
     * @param format
     * @return
     */
    private CharSequence renderSourceText(String text, TranslationFormat format) {
        RenderingGroup renderingGroup = new RenderingGroup();
        if (format == TranslationFormat.USX) {
            // TODO: add click listeners for verses and notes
            renderingGroup.addEngine(new USXRenderer(null, new Span.OnClickListener() {
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
            }));
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
        private ListItem mCurrentCommitItem = null;

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
        private RevCommit currentCommit = null; //keeps track of undo position
        private RevCommit[] commits = null; //cache commit history
        private Toast restoreMsg = null;

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
         * Loads the correct translation information into the item
         * @param targetTranslation
         * @param chapter
         * @param frame
         */
        public void loadTranslations(SourceTranslation sourceTranslation, TargetTranslation targetTranslation, Chapter chapter, Frame frame) {
            if(isChapterReference || isChapterTitle) {
                frameTranslation = null;
                chapterTranslation = targetTranslation.getChapterTranslation(chapter);
                translationFormat = chapterTranslation.getFormat();
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
                translationFormat = frameTranslation.getFormat();
                bodyTranslation = frameTranslation.body;
                bodySource = frame.body;
                isTranslationFinished = frameTranslation.isFinished();
            }
        }

    }
}
