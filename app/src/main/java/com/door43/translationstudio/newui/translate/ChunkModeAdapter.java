package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
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
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.LinedEditText;
import com.door43.translationstudio.core.ProjectTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.ClickableRenderingEngine;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.widget.ViewUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.unfoldingword.door43client.models.TargetLanguage;

/**
 * Created by joel on 9/9/2015.
 */
public class ChunkModeAdapter extends ViewModeAdapter<ChunkModeAdapter.ViewHolder> {
    public static final int HIGHLIGHT_COLOR = Color.YELLOW;
    private final String startingChapterSlug;
    private final String startingChunkSlug;
    private TargetLanguage mTargetLanguage;
    private final Activity mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;
    private final TargetTranslation mTargetTranslation;
    private ResourceContainer mSourceContainer;
    private final Door43Client mLibrary;
    private final Translator mTranslator;
    private ListItem[] mUnfilteredItems;
    private ListItem[] mFilteredItems;
    private int mLayoutBuildNumber = 0;
    private ContentValues[] mTabs;
    private SearchFilter mSearchFilter;
    private CharSequence mSearchString;
    private List<String> mUnFilteredChapters = new ArrayList();
    private List<String> mFilteredChapters = new ArrayList<>();

    public ChunkModeAdapter(Activity context, String targetTranslationId, String sourceContainerSlug, String startingChapterSlug, String startingChunkSlug, boolean openSelectedTarget) {
        this.startingChapterSlug = startingChapterSlug;
        this.startingChunkSlug = startingChunkSlug;

        mLibrary = App.getLibrary();
        mTranslator = App.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mTargetLanguage = App.languageFromTargetTranslation(mTargetTranslation);

        setSourceTranslation(sourceContainerSlug);
    }

    @Override
    public void setSourceTranslation(String sourceContainerSlug) {
        try {
            mSourceContainer = mLibrary.open(sourceContainerSlug);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.mUnFilteredChapters = new ArrayList();
        List<ListItem> listItems = new ArrayList<>();

        // TODO: there is also a map form of the toc.
        setListStartPosition(0);
        for(Map tocChapter:(List<Map>)mSourceContainer.toc) {
            String chapterSlug = (String)tocChapter.get("chapter");
            this.mUnFilteredChapters.add(chapterSlug);
            List<String> tocChunks = (List)tocChapter.get("chunks");
            for(String chunkSlug:tocChunks) {
                if(chapterSlug.equals(startingChapterSlug) && chunkSlug.equals(startingChunkSlug)) {
                    setListStartPosition(listItems.size());
                }
                listItems.add(new ListItem(chapterSlug, chunkSlug));
            }
        }

        mUnfilteredItems = listItems.toArray(new ListItem[listItems.size()]);
        mFilteredItems = mUnfilteredItems;
        mFilteredChapters = mUnFilteredChapters;

        loadTabInfo();

        triggerNotifyDataSetChanged();

        clearScreenAndStartNewSearch(mSearchString, isTargetSearch());
    }

    /**
     * Rebuilds the card tabs
     */
    private void loadTabInfo() {
        List<ContentValues> tabContents = new ArrayList<>();
        String[] sourceTranslationIds = App.getSelectedSourceTranslations(mTargetTranslation.getId());
        for(String id:sourceTranslationIds) {
            ResourceContainer rc = null;
            try {
                rc = mLibrary.open(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(rc != null) {
                ContentValues values = new ContentValues();
                // include the resource id if there are more than one
                if(mLibrary.index().getResources(rc.language.slug, rc.project.slug).size() > 1) {
                    values.put("title", rc.language.name + " " + rc.resource.slug.toUpperCase());
                } else {
                    values.put("title", rc.language.name);
                }
                values.put("tag", rc.slug);
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
        if(position >= 0 && position < mFilteredItems.length) {
            return mFilteredItems[position].chunkSlug;
        }
        return null;
    }

    @Override
    public String getFocusedChapterSlug(int position) {
        if(position >= 0 && position < mFilteredItems.length) {
            return mFilteredItems[position].chapterSlug;
        }
        return null;
    }

    @Override
    public int getItemPosition(String chapterSlug, String frameSlug) {
        for(int i = 0; i < mFilteredItems.length; i ++) {
            ListItem item = mFilteredItems[i];
            if(item.isChunk() && item.chapterSlug.equals(chapterSlug) && item.chunkSlug.equals(frameSlug)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void reload() {
        setSourceTranslation(mSourceContainer.slug);
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
        ListItem item = mFilteredItems[position];
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

         item.loadTranslations(mSourceContainer, mTargetTranslation, this);

        // render the content
//        if(item.isChapterReference()) {
//            renderChapterReference(holder, position);
//        } else if(item.isChapterTitle()) {
//            renderChapterTitle(holder, position);
//        } else if(item.isProjectTitle()) {
//            renderProjectTitle(holder, position);
//        } else {
            renderChunk(holder, position);
//        }

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;

            Typography.formatSub(mContext, holder.mSourceTitle, mSourceContainer.language.slug, mSourceContainer.language.direction);
            Typography.format(mContext, holder.mSourceBody, mSourceContainer.language.slug, mSourceContainer.language.direction);
            Typography.formatSub(mContext, holder.mTargetTitle, mTargetLanguage.slug, mTargetLanguage.direction);
            Typography.format(mContext, holder.mTargetBody, mTargetLanguage.slug, mTargetLanguage.direction);
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
//            Chapter chapter = mLibrary.getChapter(mSourceContainer, item.chapterSlug);
//            if (null != chapter) {
                mTargetTranslation.reopenChapterReference(item.chapterSlug);
//            }
        } else if (item.isChapterTitle()) {
//            Chapter chapter = mLibrary.getChapter(mSourceContainer, item.chapterSlug);
//            if (null != chapter) {
                mTargetTranslation.reopenChapterTitle(item.chapterSlug);
//            }
        } else if(item.isProjectTitle()) {
            mTargetTranslation.openProjectTitle();
        } else {
//            Frame frame = mLibrary.getFrame(mSourceContainer, item.chapterSlug, item.chunkSlug);
//            if(null != frame) {
                mTargetTranslation.reopenFrame(item.chapterSlug, item.chunkSlug);
//            }
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
     * Renders the frame cards
     * @param holder
     * @param position
     */
    private void renderChunk(final ViewHolder holder, final int position) {
        final ListItem item = mFilteredItems[position];

        // render source text
        if(item.renderedSourceText == null) {
            item.renderedSourceText = renderText(item.sourceText, item.translationFormat, !isTargetSearch());
        }
        holder.mSourceBody.setText(item.renderedSourceText);

        // render target text
        if(item.renderedTargetText == null) {
            item.renderedTargetText = renderText(item.targetText, item.translationFormat, isTargetSearch());
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
            String verseSpan = Frame.parseVerseTitle(item.sourceText, item.translationFormat);
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
                    mTargetTranslation.applyFrameTranslation(mTargetTranslation.getFrameTranslation(item.chapterSlug, item.chunkSlug, item.translationFormat), translation);
                }

                item.renderedTargetText = renderText(translation, item.translationFormat, isTargetSearch());
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
        triggerNotifyDataSetChanged();
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
        ListItem item = mFilteredItems[position];
        return mFilteredChapters.indexOf(item.chapterSlug);
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
        private final String chunkSlug;
        private final String chapterSlug;

        private boolean isTargetCardOpen = false;

        private String sourceText;
        private CharSequence renderedSourceText;
        private String targetText;
        private CharSequence renderedTargetText;

        private TranslationFormat translationFormat;
        private FrameTranslation ft;
        private ChapterTranslation ct;
        private ProjectTranslation pt;

        private boolean mHighlightSource = false;
        private boolean mHighlightTarget = false;
        private TargetLanguage targetLanguage;
        private ResourceContainer sourceContainer;
        private boolean isComplete = false;

        public ListItem(String chapterSlug, String chunkSlug) {
            this.chunkSlug = chunkSlug;
            this.chapterSlug = chapterSlug;
        }

        public String getTargetTitle() {
            if(isProjectTitle()) {
                return targetLanguage.name;
            } else if(isChapter()) {
                if(!pt.getTitle().trim().isEmpty()) {
                    return pt.getTitle().trim() + " - " + targetLanguage.name;
                } else {
                    return sourceContainer.project.name.trim() + " - " + targetLanguage.name;
                }
            } else {
                // use chapter title
                String title = ct.title.trim();
                if(title.isEmpty()) {
                    title = sourceContainer.readChunk(chapterSlug, "title").trim();
                }
                // use project title
                if(title.isEmpty()) {
                    title = pt.getTitle().trim();
                    if(title.isEmpty()) {
                        title = sourceContainer.project.name.trim();
                    }
                    title += " " + Integer.parseInt(chapterSlug);
                }
                String verseSpan = Frame.parseVerseTitle(sourceText, translationFormat);
                if(verseSpan.isEmpty()) {
                    title += ":" + Integer.parseInt(chunkSlug);
                } else {
                    title += ":" + verseSpan;
                }
                return title + " - " + targetLanguage.name;
            }
        }

        public boolean isChunk() {
            return !isChapter() && !isProjectTitle();
        }

        public boolean isChapter() {
            return isChapterReference() || isChapterTitle();
        }

        public boolean isProjectTitle() {
            return chapterSlug.equals("front") && chunkSlug.equals("title");
        }

        public boolean isChapterTitle() {
            return !chapterSlug.equals("front") && !chapterSlug.equals("back") && chunkSlug.equals("title");
        }

        public boolean isChapterReference() {
            return !chapterSlug.equals("front") && !chapterSlug.equals("back") && chunkSlug.equals("reference");
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
                if(enable) { // disable highlighting
                    if(mHighlightTarget) {
                        renderedTargetText = null; // remove rendered text so will be re-rendered without highlighting
                    }
                } else { // enable highlighting
                    renderedTargetText = null; // remove rendered text so will be re-rendered with new highlighting
                }
                mHighlightTarget = enable;
            } else { // source
                if(!enable) { // disable highlighting
                    if(mHighlightSource) {
                        renderedSourceText = null; // remove rendered text so will be re-rendered without highlighting
                    }
                } else { // enable highlighting
                    renderedSourceText = null; // remove rendered text so will be re-rendered with new highlighting
                }
                mHighlightSource = enable;
            }
        }

        /**
         * Loads the translation text from the disk
         * @param sourceContainer
         * @param targetTranslation
         * @param adapter
         */
        public void loadTranslations(ResourceContainer sourceContainer, TargetTranslation targetTranslation, ChunkModeAdapter adapter) {
            this.pt = targetTranslation.getProjectTranslation();
            this.sourceContainer = sourceContainer;
            this.targetLanguage = targetTranslation.getTargetLanguage();
            if(this.sourceText == null) {
                this.sourceText = sourceContainer.readChunk(chapterSlug, chunkSlug);
            }
            this.translationFormat = TranslationFormat.parse(sourceContainer.contentMimeType);
            // TODO: 10/1/16 this will be simplified once we migrate target translations to resource containers
            if(chapterSlug.equals("front")) {
                // project stuff
                if (chunkSlug.equals("title")) {
                    this.targetText = pt.getTitle();
                    this.isComplete = pt.isTitleFinished();
                }
            } else if(chapterSlug.equals("back")) {
                // back matter

            } else {
                // chapter stuff
                this.ct = targetTranslation.getChapterTranslation(chapterSlug);
                if(chunkSlug.equals("title")) {
                    this.targetText = ct.title;
                    this.renderedTargetText = this.targetText;
                    this.renderedSourceText = this.sourceText;
                    this.isComplete = ct.isTitleFinished();
                } else if(chunkSlug.equals("reference")) {
                    this.targetText = ct.reference;
                    this.renderedTargetText = this.targetText;
                    this.renderedSourceText = this.sourceText;
                    this.isComplete = ct.isReferenceFinished();
                } else {
                    this.ft = targetTranslation.getFrameTranslation(chapterSlug, chunkSlug, this.translationFormat);
                    this.targetText = ft.body;
                    this.isComplete = ft.isFinished();
                    boolean targetSearch = adapter.isTargetSearch();
                    // TODO: 10/3/16 this is bad programming
                    this.renderedSourceText = adapter.renderText(this.sourceText, this.translationFormat, !targetSearch);
                    this.renderedTargetText = adapter.renderText(this.targetText, this.translationFormat, targetSearch);
                }
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
        triggerNotifyDataSetChanged();

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
        List<String> filteredChapters = mUnFilteredChapters;

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
                filteredChapters = mUnFilteredChapters;
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
                        if (item.renderedSourceText == null) { // if source hasn't been rendered
                            item.loadTranslations(mSourceContainer, mTargetTranslation, ChunkModeAdapter.this);
                        }
                        if (item.renderedSourceText != null) {
                            match = item.renderedSourceText.toString().toLowerCase().contains(matchString);
                        }
                    } else { // search the target
                        if (item.renderedTargetText == null) { // if target hasn't been rendered
                            item.loadTranslations(mSourceContainer, mTargetTranslation, ChunkModeAdapter.this);
                        }
                        if (item.renderedTargetText != null) {
                            match = item.renderedTargetText.toString().toLowerCase().contains(matchString);
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
            mFilteredChapters = filteredChapters;
            oldSearch = mSearchString;
            triggerNotifyDataSetChanged();
            getListener().onSetBusyIndicator(false);
        }
    }
}
