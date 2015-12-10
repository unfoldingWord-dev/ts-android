package com.door43.translationstudio.newui.translate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.Library;
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
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.VersePinSpan;
import com.door43.widget.ViewUtil;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joel on 9/18/2015.
 */
public class ReviewModeAdapter extends ViewModeAdapter<ReviewModeAdapter.ViewHolder> {

    private static final int TAB_NOTES = 0;
    private static final int TAB_WORDS = 1;
    private static final int TAB_QUESTIONS = 2;
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
            if(item.chapterSlug.equals(chapterId) && item.frameSlug != null && item.frameSlug.equals(frameId)) {
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
        final ListItem item = mListItems[position];

        // open/close resources
        if(mResourcesOpened) {
            holder.mMainContent.setWeightSum(.765f);
        } else {
            holder.mMainContent.setWeightSum(1f);
        }

        // fetch translation from disk
        item.loadTranslations(mSourceTranslation, mTargetTranslation, mChapters.get(item.chapterSlug), loadFrame(item.chapterSlug, item.frameSlug));

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

        // disable text watcher
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
            if(holder.mEditableTextWatcher != null) {
                holder.mTargetEditableBody.removeTextChangedListener(holder.mEditableTextWatcher);
            }
            holder.mTargetEditableBody.setText(item.renderedTargetBody);
            if(holder.mEditableTextWatcher != null) {
                holder.mTargetEditableBody.addTextChangedListener(holder.mEditableTextWatcher);
            }
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
                // save
                String translation = Translator.compileTranslation((Editable)s);
                if(item.isChapterReference) {
                    mTargetTranslation.applyChapterReferenceTranslation(item.chapterTranslation, translation);
                } else if(item.isChapterTitle) {
                    mTargetTranslation.applyChapterTitleTranslation(item.chapterTranslation, translation);
                } else if(item.isProjectTitle) {
                    try {
                        mTargetTranslation.applyProjectTitleTranslation(s.toString());
                    } catch (IOException e) {
                        Logger.e(ReviewModeAdapter.class.getName(), "Failed to save the project title translation", e);
                    }
                } else if(item.isFrame()) {
                    mTargetTranslation.applyFrameTranslation(item.frameTranslation, translation);
                }
                item.renderedTargetBody = renderSourceText(translation, item.translationFormat);


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

        // editing button
        final GestureDetector detector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                item.isEditing = !item.isEditing;
                if(item.isEditing) {
                    // open editing mode
                    holder.mEditButton.setImageResource(R.drawable.ic_done_black_24dp);
                    holder.mTargetBody.setVisibility(View.GONE);
                    holder.mTargetEditableBody.setVisibility(View.VISIBLE);
                    holder.mTargetEditableBody.requestFocus();
                    holder.mTargetInnerCard.setBackgroundResource(R.drawable.paper_repeating);
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
                    holder.mEditButton.setImageResource(R.drawable.ic_mode_edit_black_24dp);
                    holder.mTargetBody.setVisibility(View.VISIBLE);
                    holder.mTargetEditableBody.setVisibility(View.GONE);
                    holder.mTargetInnerCard.setBackgroundResource(R.color.white);
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
            holder.mTargetBody.setVisibility(View.GONE);
            holder.mTargetEditableBody.setVisibility(View.VISIBLE);
            holder.mTargetInnerCard.setBackgroundResource(R.drawable.paper_repeating);
        } else {
            holder.mEditButton.setImageResource(R.drawable.ic_mode_edit_black_24dp);
            holder.mTargetBody.setVisibility(View.VISIBLE);
            holder.mTargetEditableBody.setVisibility(View.GONE);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        }

        // display as finished
        if(item.isTranslationFinished) {
            holder.mEditButton.setVisibility(View.GONE);
            holder.mDoneButton.setVisibility(View.GONE);
            holder.mDoneFlag.setVisibility(View.VISIBLE);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        } else {
            holder.mEditButton.setVisibility(View.VISIBLE);
            holder.mDoneButton.setVisibility(View.VISIBLE);
            holder.mDoneFlag.setVisibility(View.GONE);
        }

        // display source language tabs
        renderTabs(holder);

        // done buttons
        holder.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.dialog_html_alert, null);
                HtmlTextView text = (HtmlTextView)layout.findViewById(R.id.text);
                text.setHtmlFromString(mContext.getResources().getString(R.string.chunk_checklist_body), true);

                CustomAlertDialog.Create(mContext)
                    .setTitle(R.string.chunk_checklist_title)
                    .setView(layout)
                    .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    onConfirmChunk(item, chapter, frame);
                                }
                            }
                    )
                    .setNegativeButton(R.string.title_cancel, null)
                    .show("Chunk2");
            }
        });
        holder.mDoneFlag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });
    }

    private void onConfirmChunk(final ListItem item, final Chapter chapter, final Frame frame) {

        boolean finished;
        if (item.isChapterReference) {
            finished = mTargetTranslation.finishChapterReference(chapter);
        } else if (item.isChapterTitle) {
            finished = mTargetTranslation.finishChapterTitle(chapter);
        } else if (item.isProjectTitle) {
            finished = mTargetTranslation.finishProjectTitle();
        } else {
            finished = mTargetTranslation.finishFrame(frame);
        }
        if (finished) {
            item.isEditing = false;
            item.renderedTargetBody = null;
            notifyDataSetChanged();
        } else {
            Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.translate_first, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
            snack.show();
        }
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

    private CharSequence renderSourceText(String text, TranslationFormat format) {
        RenderingGroup renderingGroup = new RenderingGroup();
        if (format == TranslationFormat.USX) {
            // TODO: add click listeners for verses and notes
            renderingGroup.addEngine(new USXRenderer());
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
        public final ImageButton mEditButton;
        public final CardView mResourceCard;
        public final LinearLayout mMainContent;
        public final LinearLayout mResourceLayout;
        public final LinearLayout mDoneButton;
        private final LinearLayout mDoneFlag;
        private final LinearLayout mTargetInnerCard;
        private final TabLayout mResourceTabs;
        private final LinearLayout mResourceList;
        public final EditText mTargetEditableBody;
        public int mLayoutBuildNumber = -1;
        public TextWatcher mEditableTextWatcher;
        public final TextView mTargetTitle;
        public final EditText mTargetBody;
        public final CardView mTargetCard;
        public final CardView mSourceCard;
        public final TabLayout mTranslationTabs;
        public final ImageButton mNewTabButton;
        public TextView mSourceBody;
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
            mTargetEditableBody = (EditText)v.findViewById(R.id.target_translation_editable_body);
            mTranslationTabs = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mEditButton = (ImageButton)v.findViewById(R.id.edit_translation_button);
            mDoneButton = (LinearLayout)v.findViewById(R.id.done_button);
            mDoneFlag = (LinearLayout)v.findViewById(R.id.done_flag);
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
        public boolean isProjectTitle = false;
        private boolean isEditing = false;
        private CharSequence renderedSourceBody;
        private CharSequence renderedTargetBody;
        private TranslationFormat translationFormat;
        private String bodyTranslation;
        private boolean isTranslationFinished;
        private String bodySource;
        private FrameTranslation frameTranslation;
        public ChapterTranslation chapterTranslation;
        private ProjectTranslation projectTranslation;

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
