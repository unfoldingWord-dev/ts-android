package com.door43.translationstudio.newui.translate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.VersePinSpan;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joel on 9/18/2015.
 */
public class ReviewModeAdapter extends ViewModeAdapter<ReviewModeAdapter.ViewHolder> {

    private static final int TAB_NOTES = 0;
    private static final int TAB_WORDS = 1;
    private static final int TAB_QUESTIONS = 2;
    private static final String IMAGEVIEW_TAG = "drag icon";
    private final Library mLibrary;
    private final Translator mTranslator;
    private final Activity mContext;
    private final TargetTranslation mTargetTranslation;
    private HashMap<String, Chapter> mChapters;
    private SourceTranslation mSourceTranslation;
    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
    private int[] mOpenResourceTab;
    private Frame[] mFrames;
    private CharSequence[] mRenderedSourceBody;
    private CharSequence[] mRenderedTargetBody;
    private int mLayoutBuildNumber = 0;
    private boolean mResourcesOpened = false;
    private ContentValues[] mTabs;

    public ReviewModeAdapter(Activity context, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId, boolean resourcesOpened) {

        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());
        mResourcesOpened = resourcesOpened;

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        mChapters = new HashMap<>();
        List<Frame> frames = new ArrayList<>();
        for(Chapter c:chapters) {
            mChapters.put(c.getId(), c);
            Frame[] chapterFrames = mLibrary.getFrames(mSourceTranslation, c.getId());
            if(chapterId != null && c.getId().equals(chapterId) && chapterFrames.length > 0) {
                // identify starting selection
                setListStartPosition(frames.size());
                if(frameId != null) {
                    for(Frame frame:chapterFrames) {
                        if(frame.getId().equals(frameId)) {
                            setListStartPosition(frames.size());
                        }
                        frames.add(frame);
                    }
                    continue;
                }
            }
            frames.addAll(Arrays.asList(chapterFrames));
        }
        mFrames = frames.toArray(new Frame[frames.size()]);
        mOpenResourceTab = new int[mFrames.length];
        mRenderedSourceBody = new CharSequence[mFrames.length];
        mRenderedTargetBody = new CharSequence[mFrames.length];

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
                if(mLibrary.getResources(sourceTranslation.projectId, sourceTranslation.sourceLanguageId).length > 1) {
                    values.put("title", sourceTranslation.getSourceLanguageTitle() + " " + sourceTranslation.resourceId.toUpperCase());
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
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<Frame> frames = new ArrayList<>();
        mChapters = new HashMap<>();
        for(Chapter c:chapters) {
            mChapters.put(c.getId(), c);
            Frame[] chapterFrames = mLibrary.getFrames(mSourceTranslation, c.getId());
            frames.addAll(Arrays.asList(chapterFrames));
        }
        mFrames = frames.toArray(new Frame[frames.size()]);
        mOpenResourceTab = new int[mFrames.length];
        mRenderedSourceBody = new CharSequence[mFrames.length];
        mRenderedTargetBody = new CharSequence[mFrames.length];

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
        if(position >= 0 && position < mFrames.length) {
            return mFrames[position].getId();
        }
        return null;
    }

    @Override
    public String getFocusedChapterId(int position) {
        if(position >= 0 && position < mFrames.length) {
            return mFrames[position].getChapterId();
        }
        return null;
    }

    @Override
    public int getItemPosition(String chapterId, String frameId) {
        for(int i = 0; i < mFrames.length; i ++) {
            Frame frame = mFrames[i];
            if(frame.getChapterId().equals(chapterId) && frame.getId().equals(frameId)) {
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

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Frame frame = mFrames[position];

        // render the source frame body
        if(mRenderedSourceBody[position] == null) {
            mRenderedSourceBody[position] = renderSourceText(frame.body, frame.getFormat());
        }

        holder.mSourceBody.setText(mRenderedSourceBody[position]);

        // render source frame title (we don't actually set the source title)
        final Chapter chapter = mChapters.get(frame.getChapterId());
        String sourceChapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        sourceChapterTitle += ":" + frame.getTitle();

        // render the target frame body
        if(mRenderedTargetBody[position] == null) {
            FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
            if(frameTranslation.isFinished()) {
                mRenderedTargetBody[position] = renderSourceText(frameTranslation.body, frameTranslation.getFormat());
            } else {
                mRenderedTargetBody[position] = renderTargeText(frameTranslation.body, frameTranslation.getFormat(), frame, holder);
            }
        }
        if(holder.mTextWatcher != null) {
            holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
        }
        // TODO: 10/1/2015 insert verse markers at front if not already in text
        holder.mTargetBody.setText(TextUtils.concat(mRenderedTargetBody[position], "\n"));
        holder.mTargetBody.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.onTouchEvent(event);
                v.clearFocus();
                return true;
            }
        });
        ViewUtil.makeLinksClickable(holder.mTargetBody);

        // render target frame title
        ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
        String targetChapterTitle = chapterTranslation.title;
        final FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
        if(!targetChapterTitle.isEmpty()) {
            targetChapterTitle += ":" + frameTranslation.getTitle();
        } else {
            targetChapterTitle = sourceChapterTitle;
        }
        holder.mTargetTitle.setText(targetChapterTitle + " - " + mTargetLanguage.name);

        // load tabs
        holder.mTranslationTabs.setOnTabSelectedListener(null);
        holder.mTranslationTabs.removeAllTabs();
        for(ContentValues values:mTabs) {
            TabLayout.Tab tab = holder.mTranslationTabs.newTab();
            tab.setText(values.getAsString("title"));
            tab.setTag(values.getAsString("tag"));
            holder.mTranslationTabs.addTab(tab);
        }

        // select correct tab
        for(int i = 0; i < holder.mTranslationTabs.getTabCount(); i ++) {
            TabLayout.Tab tab = holder.mTranslationTabs.getTabAt(i);
            if(tab.getTag().equals(mSourceTranslation.getId())) {
                tab.select();
                break;
            }
        }

        // hook up listener
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

        holder.mNewTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() != null) {
                    getListener().onNewSourceTranslationTabClick();
                }
            }
        });

        // open blind draft mode
        final GestureDetector editButtonDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Bundle args = new Bundle();
                args.putBoolean(ChunkModeFragment.EXTRA_TARGET_OPEN, true);
                args.putString(TargetTranslationActivity.EXTRA_CHAPTER_ID, chapter.getId());
                args.putString(TargetTranslationActivity.EXTRA_FRAME_ID, frame.getId());
                getListener().openTranslationMode(TranslationViewMode.CHUNK, args);
                return true;
            }
        });
        holder.mEditButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return editButtonDetector.onTouchEvent(event);
            }
        });

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

                // TODO: we either need to force the translation to save when the view leaves the window (so we have it if they come back before it was saved)
                // or just always save immediately

                mRenderedTargetBody[position] = renderTargeText(translation, frameTranslation.getFormat(), frame, holder);

                // update view
                // TRICKY: anything worth updating will need to change by at least 7 characters
                // <a></a> <-- at least 7 characters are required to create a tag for rendering.
                int minDeviation = 7;
                if(count - before > minDeviation) {
                    int scrollX = holder.mTargetBody.getScrollX();
                    int scrollY = holder.mTargetBody.getScrollX();
                    int selection = holder.mTargetBody.getSelectionStart();

                    holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
                    holder.mTargetBody.setText(TextUtils.concat(mRenderedTargetBody[position], "\n"));
                    holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);

                    holder.mTargetBody.scrollTo(scrollX, scrollY);
                    if(selection > holder.mTargetBody.length()) {
                        selection = holder.mTargetBody.length();
                    }
                    holder.mTargetBody.setSelection(selection);
                    holder.mTargetBody.clearFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatSub(mContext, holder.mTargetTitle, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
        }

        if(mResourcesOpened) {
            holder.mMainContent.setWeightSum(.765f);
        } else {
            holder.mMainContent.setWeightSum(1f);
        }

        // resource tabs
        holder.mResourceTabs.setOnTabSelectedListener(null);
        holder.mResourceTabs.removeAllTabs();
        final TranslationNote[] notes = getPreferredNotes(mSourceTranslation, frame.getChapterId(), frame.getId());
        if(notes.length > 0) {
            TabLayout.Tab tab = holder.mResourceTabs.newTab();
            tab.setText(R.string.label_translation_notes);
            tab.setTag(TAB_NOTES);
            holder.mResourceTabs.addTab(tab);
            if(mOpenResourceTab[position] == TAB_NOTES) {
                tab.select();
            }
        }
        final TranslationWord[] words = getPreferredWords(mSourceTranslation, frame.getChapterId(), frame.getId());
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
                if((int) tab.getTag() == TAB_NOTES && mOpenResourceTab[position] != TAB_NOTES) {
                    mOpenResourceTab[position] = TAB_NOTES;
                    // render notes
                    renderResources(holder, position, notes, words, questions);
                } else if((int) tab.getTag() == TAB_WORDS && mOpenResourceTab[position] != TAB_WORDS) {
                    mOpenResourceTab[position] = TAB_WORDS;
                    // render words
                    renderResources(holder, position, notes, words, questions);
                } else if((int) tab.getTag() == TAB_QUESTIONS && mOpenResourceTab[position] != TAB_QUESTIONS) {
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

        // done buttons
        holder.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: display confirmation dialog
                if(mTargetTranslation.finishFrame(frame)) {
                    mRenderedTargetBody[position] = null;
                    notifyDataSetChanged();
                } else {
                    Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.translate_first, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });
        holder.mDoneFlag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTargetTranslation.reopenFrame(frame)) {
                    // TODO: display confirmation dialog
                    mRenderedTargetBody[position] = null;
                    notifyDataSetChanged();
                }
            }
        });
        if(frameTranslation.isFinished()) {
            holder.mEditButton.setVisibility(View.GONE);
            holder.mDoneButton.setVisibility(View.GONE);
            holder.mDoneFlag.setVisibility(View.VISIBLE);
            holder.mTargetInnerCard.setBackgroundResource(R.color.white);
        } else {
            holder.mEditButton.setVisibility(View.VISIBLE);
            holder.mDoneButton.setVisibility(View.VISIBLE);
            holder.mDoneFlag.setVisibility(View.GONE);
            holder.mTargetInnerCard.setBackgroundResource(R.drawable.paper_repeating);
        }

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
     * Returns the preferred translation notes.
     * if none exist in the source language it will return the english version
     * @param chapterId
     * @param frameId
     * @return
     */
    private static TranslationNote[] getPreferredNotes(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        Library library = AppContext.getLibrary();
        TranslationNote[] notes = library.getTranslationNotes(sourceTranslation, chapterId, frameId);
        if(notes.length == 0 && !sourceTranslation.sourceLanguageId.equals("en")) {
            notes = library.getTranslationNotes(SourceTranslation.simple(sourceTranslation.projectId, "en", sourceTranslation.resourceId), chapterId, frameId);
        }
        return notes;
    }

    /**
     * Returns the preferred translation words.
     * if none exist in the source language it will return the english version
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    private static TranslationWord[] getPreferredWords(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        Library library = AppContext.getLibrary();
        TranslationWord[] words = library.getTranslationWords(sourceTranslation, chapterId, frameId);
        if(words.length == 0 && !sourceTranslation.sourceLanguageId.equals("en")) {
            words = library.getTranslationWords(SourceTranslation.simple(sourceTranslation.projectId, "en", sourceTranslation.resourceId), chapterId, frameId);
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
        if(questions.length == 0 && !sourceTranslation.sourceLanguageId.equals("en")) {
            questions = library.getCheckingQuestions(SourceTranslation.simple(sourceTranslation.projectId, "en", sourceTranslation.resourceId), chapterId, frameId);
        }
        return questions;
    }

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
                wordView.setText(word.getTitle());
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

    private CharSequence renderTargeText(String text, TranslationFormat format, final Frame frame, final ViewHolder holder) {
        RenderingGroup renderingGroup = new RenderingGroup();
        if(format == TranslationFormat.USX) {
            USXRenderer usxRenderer = new USXRenderer(new Span.OnClickListener() {
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
                            // TODO: every view should have a drag listener and each view should have a unique tag so we can identify a valid drop site
                            // TODO: highlight the drop site.
                            if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                                // delete old span
                                int[] spanRange = (int[])event.getLocalState();
                                CharSequence in = editText.getText();
                                CharSequence out = TextUtils.concat(in.subSequence(0, spanRange[0]), in.subSequence(spanRange[1], in.length()));
                                editText.removeTextChangedListener(holder.mTextWatcher);
                                editText.setText(out);
                                editText.addTextChangedListener(holder.mTextWatcher);
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
                                editText.setText(text);
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                                view.setOnClickListener(null);
                                editText.setSelection(editText.getSelectionEnd());
                                // reset verse if dragged off the view
                                // TODO: 10/5/2015 perhaps we should confirm with the user?
                                if(!hasEntered) {
                                    // place the verse back at the beginning
                                    CharSequence text = editText.getText();
                                    text = TextUtils.concat(pin.toCharSequence(), text);
                                    editText.removeTextChangedListener(holder.mTextWatcher);
                                    editText.setText(text);
                                    editText.addTextChangedListener(holder.mTextWatcher);
                                }
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                                hasEntered = true;
                                Log.d("DRAG", "action");
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                                hasEntered = false;
                                editText.setSelection(editText.getSelectionEnd());
                            } else if(event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                                int offset = editText.getOffsetForPosition(event.getX(), event.getY());
                                if(offset >= 0) {
//                                    editText.setSelection(offset);
                                    Selection.setSelection(editText.getText(), offset);
                                } else {
                                    editText.setSelection(editText.getSelectionEnd());
                                }
                            }
                            return true;
                        }
                    });
                }
            }, null);
            usxRenderer.setPopulateVerseMarkers(frame.getVerseRange());
            renderingGroup.addEngine(usxRenderer);
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
        }
        renderingGroup.init(text);
        return renderingGroup.start();
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
        return mFrames.length;
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
        public final Button mDoneButton;
        private final LinearLayout mDoneFlag;
        private final LinearLayout mTargetInnerCard;
        private final TabLayout mResourceTabs;
        private final LinearLayout mResourceList;
        public int mLayoutBuildNumber = -1;
        public TextWatcher mTextWatcher;
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
            mTranslationTabs = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mEditButton = (ImageButton)v.findViewById(R.id.edit_translation_button);
            mDoneButton = (Button)v.findViewById(R.id.done_button);
            mDoneFlag = (LinearLayout)v.findViewById(R.id.done_flag);
            ViewUtil.tintViewDrawable(mEditButton, context.getResources().getColor(R.color.dark_disabled_text));
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
}
