package com.door43.translationstudio.newui.translate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
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
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
        mChapters = new HashMap<>();
        List<ListItem> listItems = new ArrayList<>();
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

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final ListItem item = mListItems[position];
        final Frame frame = mLibrary.getFrame(mSourceTranslation, item.chapterSlug, item.frameSlug);
        final FrameTranslation frameTranslation;
        final ChapterTranslation chapterTranslation;
        boolean translationIsFinished;
        final TranslationFormat translationFormat;

        if(item.isChapterReference || item.isChapterTitle) {
            frameTranslation = null;
            chapterTranslation = mTargetTranslation.getChapterTranslation(mChapters.get(item.chapterSlug));
            translationFormat = chapterTranslation.getFormat();
        } else {
            chapterTranslation = null;
            frameTranslation = mTargetTranslation.getFrameTranslation(frame);
            translationFormat = frameTranslation.getFormat();
        }

        // render the content
        if(item.isChapterReference) {
            renderChapterReference(holder, position, item.chapterSlug);
            translationIsFinished = chapterTranslation.isReferenceFinished();
        } else if(item.isChapterTitle) {
            renderChapterTitle(holder, position, item.chapterSlug);
            translationIsFinished = chapterTranslation.isTitleFinished();
        } else {
            renderFrame(holder, frame, position);
            translationIsFinished = frameTranslation.isFinished();
        }

        // display editing mode
        if(item.isEditing) {
            holder.mEditButton.setImageResource(R.drawable.ic_done_black_24dp);
            holder.mTargetBody.setVisibility(View.GONE);
            holder.mTargetEditableBody.setVisibility(View.VISIBLE);
        } else {
            holder.mEditButton.setImageResource(R.drawable.ic_mode_edit_black_24dp);
            holder.mTargetBody.setVisibility(View.VISIBLE);
            holder.mTargetEditableBody.setVisibility(View.GONE);
        }

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

        // enable editing
        final GestureDetector editButtonDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if(item.isEditing) {
                    holder.mEditButton.setImageResource(R.drawable.ic_mode_edit_black_24dp);
                    item.isEditing = false;
                    holder.mTargetBody.setVisibility(View.VISIBLE);
                    holder.mTargetEditableBody.setVisibility(View.GONE);
                    holder.mTargetBody.requestFocus();
                    getListener().closeKeyboard();
                } else {
                    holder.mEditButton.setImageResource(R.drawable.ic_done_black_24dp);
                    item.isEditing = true;
                    holder.mTargetBody.setVisibility(View.GONE);
                    holder.mTargetEditableBody.setVisibility(View.VISIBLE);
                    holder.mTargetEditableBody.requestFocus();
                    InputMethodManager mgr = (InputMethodManager)
                            mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(holder.mTargetEditableBody, InputMethodManager.SHOW_IMPLICIT);
                }
                item.renderedTargetBody = null;
                if(item.isChapterReference) {
                    renderChapterReference(holder, position, item.chapterSlug);
                } else if (item.isChapterTitle) {
                    renderChapterTitle(holder, position, item.chapterSlug);
                } else {
                    renderFrame(holder, frame, position);
                }
                return true;
            }
        });
        holder.mEditButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return editButtonDetector.onTouchEvent(event);
            }
        });

        // remove old watcher
        if(holder.mTextWatcher != null) {
            holder.mTargetEditableBody.removeTextChangedListener(holder.mTextWatcher);
        }
        // create watcher
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
                if(item.isChapterReference) {
                    mTargetTranslation.applyChapterReferenceTranslation(chapterTranslation, translation);
                } else if(item.isChapterTitle) {
                    mTargetTranslation.applyChapterTitleTranslation(chapterTranslation, translation);
                } else {
                    mTargetTranslation.applyFrameTranslation(frameTranslation, translation);
                }
                item.renderedTargetBody = renderSourceText(translation, translationFormat);


                // update view
                // TRICKY: anything worth updating will need to change by at least 7 characters
                // <a></a> <-- at least 7 characters are required to create a tag for rendering.
                int minDeviation = 7;
                if(count - before > minDeviation) {
                    int scrollX = holder.mTargetEditableBody.getScrollX();
                    int scrollY = holder.mTargetEditableBody.getScrollX();
                    int selection = holder.mTargetEditableBody.getSelectionStart();

                    holder.mTargetEditableBody.removeTextChangedListener(holder.mTextWatcher);
                    holder.mTargetEditableBody.setText(TextUtils.concat(item.renderedTargetBody, "\n"));
                    holder.mTargetEditableBody.addTextChangedListener(holder.mTextWatcher);

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
        holder.mTargetEditableBody.addTextChangedListener(holder.mTextWatcher);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatSub(mContext, holder.mTargetTitle, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetEditableBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
        }

        // display resources as opened
        if(mResourcesOpened) {
            holder.mMainContent.setWeightSum(.765f);
        } else {
            holder.mMainContent.setWeightSum(1f);
        }

        // done buttons
        holder.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: display confirmation dialog
                if(mTargetTranslation.finishFrame(frame)) {
                    item.isEditing = false;
                    item.renderedTargetBody = null;
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
                    item.renderedTargetBody = null;
                    notifyDataSetChanged();
                }
            }
        });
        if(translationIsFinished) {
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

    private void renderChapterReference(ViewHolder holder, int position, String chapterSlug) {
        final ListItem item = mListItems[position];
        Chapter chapter = mChapters.get(chapterSlug);
        if(chapter != null) {
            // source title
            String sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
            // TODO: 10/20/2015 need to enable the title and display it here
//            holder.mSourceTitle.setText(sourceChapterTitle);

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
            if(chapterTranslation.isReferenceFinished()) {
                holder.mTargetBody.setEnabled(false);
                holder.mTargetInnerCard.setBackgroundResource(R.color.white);
            } else {
                holder.mTargetBody.setEnabled(true);
                holder.mTargetInnerCard.setBackgroundResource(R.drawable.paper_repeating);
            }
        }

        holder.mResourceCard.setVisibility(View.INVISIBLE);
    }

    private void renderChapterTitle(ViewHolder holder, int position, String chapterSlug) {
        final ListItem item = mListItems[position];
        Chapter chapter = mChapters.get(chapterSlug);
        if(chapter != null) {
            // source title
            String sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
            // TODO: 10/20/2015 need to renable the source title and display it here
//            holder.mSourceTitle.setText(sourceChapterTitle);

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
            if(chapterTranslation.isTitleFinished()) {
                holder.mTargetBody.setEnabled(false);
                holder.mTargetInnerCard.setBackgroundResource(R.color.white);
            } else {
                holder.mTargetBody.setEnabled(true);
                holder.mTargetInnerCard.setBackgroundResource(R.drawable.paper_repeating);
            }
        }

        holder.mResourceCard.setVisibility(View.INVISIBLE);
    }

    private void renderFrame(ViewHolder holder, Frame frame, int position) {
        FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
        final ListItem item = mListItems[position];

        // render the source frame body
        if(item.renderedSourceBody == null) {
            item.renderedSourceBody = renderSourceText(frame.body, frame.getFormat());
        }

        holder.mSourceBody.setText(item.renderedSourceBody);

        // render source frame title
        Chapter chapter = mChapters.get(frame.getChapterId());
        String sourceChapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        sourceChapterTitle += ":" + frame.getTitle();

        // render the target frame body
        if(item.renderedTargetBody == null) {
            if(frameTranslation.isFinished() || item.isEditing) {
                item.renderedTargetBody = renderSourceText(frameTranslation.body, frameTranslation.getFormat());
            } else {
                item.renderedTargetBody = renderTargetText(frameTranslation.body, frameTranslation.getFormat(), frame, frameTranslation, holder, item);
            }
        }
        if(holder.mTextWatcher != null) {
            holder.mTargetEditableBody.removeTextChangedListener(holder.mTextWatcher);
        }
        holder.mTargetEditableBody.setText(TextUtils.concat(item.renderedTargetBody, "\n"));
        if(holder.mTextWatcher != null) {
            holder.mTargetEditableBody.addTextChangedListener(holder.mTextWatcher);
        }
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

        // render the target frame title
        ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
        String targetChapterTitle = chapterTranslation.title;
        if(!targetChapterTitle.isEmpty()) {
            targetChapterTitle += ":" + frameTranslation.getTitle();
        } else {
            targetChapterTitle = sourceChapterTitle;
        }
        holder.mTargetTitle.setText(targetChapterTitle + " - " + mTargetLanguage.name);

        renderResourcesCard(holder, frame, position);
    }

    public void renderResourcesCard(final ViewHolder holder, Frame frame, final int position) {
        // resource tabs
        holder.mResourceTabs.setOnTabSelectedListener(null);
        holder.mResourceTabs.removeAllTabs();
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
        } else if(holder.mResourceList.getChildCount() > 0) {
            holder.mResourceList.removeAllViews();
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
            notes = library.getTranslationNotes(SourceTranslation.simple(sourceTranslation.projectSlug, "en", sourceTranslation.resourceSlug), frame.getChapterId(), frame.getId());
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
            words = library.getTranslationWords(SourceTranslation.simple(sourceTranslation.projectSlug, "en", sourceTranslation.resourceSlug), frame.getChapterId(), frame.getId());
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
            questions = library.getCheckingQuestions(SourceTranslation.simple(sourceTranslation.projectSlug, "en", sourceTranslation.resourceSlug), chapterId, frameId);
        }
        return questions;
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
        if(format == TranslationFormat.USX) {
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
            };
            USXRenderer usxRenderer = new USXRenderer(verseClickListener, null);
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
        public final Button mDoneButton;
        private final LinearLayout mDoneFlag;
        private final LinearLayout mTargetInnerCard;
        private final TabLayout mResourceTabs;
        private final LinearLayout mResourceList;
        public final EditText mTargetEditableBody;
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
            mTargetEditableBody = (EditText)v.findViewById(R.id.target_translation_editable_body);
            mTranslationTabs = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mEditButton = (ImageButton)v.findViewById(R.id.edit_translation_button);
            mDoneButton = (Button)v.findViewById(R.id.done_button);
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
        private boolean isEditing = false;
        private CharSequence renderedSourceBody;
        private CharSequence renderedTargetBody;

        public ListItem(String frameSlug, String chapterSlug) {
            this.frameSlug = frameSlug;
            this.chapterSlug = chapterSlug;
        }
    }
}
