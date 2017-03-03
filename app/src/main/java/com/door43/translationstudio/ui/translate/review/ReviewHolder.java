package com.door43.translationstudio.ui.translate.review;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.tasks.MergeConflictsParseTask;
import com.door43.translationstudio.ui.translate.TranslationHelp;
import com.door43.widget.LinedEditText;

import org.unfoldingword.resourcecontainer.Language;
import org.unfoldingword.resourcecontainer.Link;
import org.unfoldingword.resourcecontainer.ResourceContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a review mode view
 */
public class ReviewHolder extends RecyclerView.ViewHolder {
    private static final int TAB_NOTES = 0;
    private static final int TAB_WORDS = 1;
    private static final int TAB_QUESTIONS = 2;

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final TabLayout.OnTabSelectedListener mResourceTabClickListener;
    public ReviewListItem currentItem = null;
    public final ImageButton mAddNoteButton;
    public final ImageButton mUndoButton;
    public final ImageButton mRedoButton;
    public final ImageButton mEditButton;
    public final CardView mResourceCard;
    public final LinearLayout mMainContent;
    public final LinearLayout mResourceLayout;
    public final Switch mDoneSwitch;
    public final LinearLayout mTargetInnerCard;
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
    public List<TextView> mMergeText;
    public final LinearLayout mMergeConflictLayout;
    public final TextView mConflictText;
    public final LinearLayout mButtonBar;
    public final Button mCancelButton;
    public final Button mConfirmButton;
    private OnClickListener mListener;
    private List<TranslationHelp> mNotes = new ArrayList<>();
    private List<TranslationHelp> mQuestions = new ArrayList<>();
    private List<Link> mWords = new ArrayList<>();
    private float mInitialTextSize = 0;
    private int mMarginInitialLeft = 0;

    private enum MergeConflictDisplayState {
        NORMAL,
        SELECTED,
        DESELECTED
    }

    public ReviewHolder(Context context, View v) {
        super(v);
        mContext = context;
        mInflater = LayoutInflater.from(context);
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
        mMergeConflictLayout = (LinearLayout)v.findViewById(R.id.merge_cards);
        mConflictText = (TextView)v.findViewById(R.id.conflict_label);
        mButtonBar = (LinearLayout)v.findViewById(R.id.button_bar);
        mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        mConfirmButton = (Button)v.findViewById(R.id.confirm_button);
        mResourceTabClickListener = new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tag = (int) tab.getTag();
                if(mListener == null) return;

                if (tag == TAB_NOTES) {
                    mListener.onResourceTabNotesSelected(ReviewHolder.this, currentItem);
                } else if (tag == TAB_WORDS) {
                    mListener.onResourceTabWordsSelected(ReviewHolder.this, currentItem);
                } else if (tag == TAB_QUESTIONS) {
                    mListener.onResourceTabQuestionsSelected(ReviewHolder.this, currentItem);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                clearHelps();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        };
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

    public void setResources(Language language, List<TranslationHelp> notes, List<TranslationHelp> questions, List<Link> words) {
        mNotes = notes;
        mQuestions = questions;
        mWords = words;
        clearHelps();
        mResourceTabs.removeOnTabSelectedListener(mResourceTabClickListener);

        // rebuild tabs
        mResourceTabs.removeAllTabs();
        if(notes.size() > 0) {
            TabLayout.Tab tab = mResourceTabs.newTab();
            tab.setText(R.string.label_translation_notes);
            tab.setTag(TAB_NOTES);
            mResourceTabs.addTab(tab);
        }
        if(words.size() > 0) {
            TabLayout.Tab tab = mResourceTabs.newTab();
            tab.setText(R.string.translation_words);
            tab.setTag(TAB_WORDS);
            mResourceTabs.addTab(tab);
        }
        if(questions.size()> 0) {
            TabLayout.Tab tab = mResourceTabs.newTab();
            tab.setText(R.string.questions);
            tab.setTag(TAB_QUESTIONS);
            mResourceTabs.addTab(tab);
        }

        // select default tab
        if(mResourceTabs.getTabCount() > 0 ) {
            TabLayout.Tab tab = mResourceTabs.getTabAt(0);
            if(tab != null) {
                tab.select();
                // show the contents
                switch((int)tab.getTag()) {
                    case TAB_NOTES:
                        showNotes(language);
                        break;
                    case TAB_WORDS:
                        showWords(language);
                        break;
                    case TAB_QUESTIONS:
                        showQuestions(language);
                        break;
                }
            }
        }
        mResourceTabs.addOnTabSelectedListener(mResourceTabClickListener);
    }

    /**
     * Removes the tabs and all the loaded resources from the resource tab
     */
    public void clearResourceCard() {
        clearHelps();
        mResourceTabs.removeOnTabSelectedListener(mResourceTabClickListener);
        mResourceTabs.removeAllTabs();
        mNotes = new ArrayList<>();
        mQuestions = new ArrayList<>();
        mWords = new ArrayList<>();
    }

    private void clearHelps() {
        if(mResourceList.getChildCount() > 0) mResourceList.removeAllViews();
    }

    /**
     * Displays the notes
     * @param language
     */
    public void showNotes(Language language) {
        clearHelps();
        for(final TranslationHelp note:mNotes) {
            // TODO: 2/28/17 it would be better if we could build this in code
            TextView v = (TextView) mInflater.inflate(R.layout.fragment_resources_list_item, null);
            v.setText(note.title);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener!= null) {
                        mListener.onNoteClick(note, getResourceCardWidth());
                    }
                }
            });
            Typography.formatSub(mContext, TranslationType.SOURCE, v, language.slug, language.direction);
            mResourceList.addView(v);
        }
    }

    /**
     * Displays the words
     * @param language
     */
    public void showWords(Language language) {
        clearHelps();
        for(final Link word:mWords) {
            TextView wordView = (TextView) mInflater.inflate(R.layout.fragment_resources_list_item, null);
            wordView.setText(word.title);
            wordView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        ResourceContainer rc = ContainerCache.cacheClosest(App.getLibrary(), word.language, word.project, word.resource);
                        mListener.onWordClick(rc.slug, word.chapter, getResourceCardWidth());
                    }
                }
            });
            Typography.formatSub(mContext, TranslationType.SOURCE, wordView, language.slug, language.direction);
            mResourceList.addView(wordView);
        }
    }

    /**
     * Displays the questions
     * @param language
     */
    public void showQuestions(Language language) {
        clearHelps();
        for(final TranslationHelp question:mQuestions) {
            TextView questionView = (TextView) mInflater.inflate(R.layout.fragment_resources_list_item, null);
            questionView.setText(question.title);
            questionView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onQuestionClick(question, getResourceCardWidth());
                    }
                }
            });
            Typography.formatSub(mContext, TranslationType.SOURCE, questionView, language.slug, language.direction);
            mResourceList.addView(questionView);
        }
    }

    /**
     * set up the merge conflicts on the card
     * @param task
     * @param item
     */
    public void displayMergeConflictsOnTargetCard(Language language, MergeConflictsParseTask task, final ReviewListItem item) {
        item.mergeItems = task.getMergeConflictItems();

        if(mMergeText != null) { // if previously rendered (could be recycled view)
            while (mMergeText.size() > item.mergeItems.size()) { // if too many items, remove extras
                int lastPosition = mMergeText.size() - 1;
                TextView v = mMergeText.get(lastPosition);
                mMergeConflictLayout.removeView(v);
                mMergeText.remove(lastPosition);
            }
        } else {
            mMergeText = new ArrayList<>();
        }

        int tailColor = mContext.getResources().getColor(R.color.tail_background);

        for(int i = 0; i < item.mergeItems.size(); i++) {
            CharSequence mergeConflictCard = item.mergeItems.get(i);

            boolean createNewCard = (i >= mMergeText.size());

            TextView textView = null;
            if(createNewCard) {
                // create new card
                textView = (TextView) LayoutInflater.from(mContext).inflate(R.layout.fragment_merge_card, mMergeConflictLayout, false);
                mMergeConflictLayout.addView(textView);
                mMergeText.add(textView);

                if (i % 2 == 1) { //every other card is different color
                    textView.setBackgroundColor(tailColor);
                }
            } else {
                textView = mMergeText.get(i); // get previously created card
            }

            if(mInitialTextSize == 0) { // see if we need to initialize values
                mInitialTextSize = Typography.getFontSize(mContext, TranslationType.SOURCE);
                mMarginInitialLeft = leftMargin(textView);
            }

            Typography.format(mContext, TranslationType.SOURCE, textView, language.slug, language.direction);

            final int pos = i;

            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.mergeItemSelected = pos;
                    displayMergeConflictSelectionState(item);
                }
            });
        }

        displayMergeConflictSelectionState(item);
    }

    /**
     * set merge conflict selection state
     * @param item
     */
    public void displayMergeConflictSelectionState(ReviewListItem item) {
        for(int i = 0; i < item.mergeItems.size(); i++ ) {
            CharSequence mergeConflictCard = item.mergeItems.get(i);
            TextView textView = mMergeText.get(i);

            if (item.mergeItemSelected >= 0) {
                if (item.mergeItemSelected == i) {
                    displayMergeSelectionState(MergeConflictDisplayState.SELECTED, textView, mergeConflictCard);
                    mConflictText.setVisibility(View.GONE);
                    mButtonBar.setVisibility(View.VISIBLE);
                } else {
                    displayMergeSelectionState(MergeConflictDisplayState.DESELECTED, textView, mergeConflictCard);
                    mConflictText.setVisibility(View.GONE);
                    mButtonBar.setVisibility(View.VISIBLE);
                }

            } else {
                displayMergeSelectionState(MergeConflictDisplayState.NORMAL, textView, mergeConflictCard);
                mConflictText.setVisibility(View.VISIBLE);
                mButtonBar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * display the selection state for card
     * @param state
     */
    private void displayMergeSelectionState(MergeConflictDisplayState state, TextView view, CharSequence text) {

        SpannableStringBuilder span;

        switch (state) {
            case SELECTED:
                setLeftRightMargins( view, mMarginInitialLeft); // shrink margins to emphasize
                span = new SpannableStringBuilder(text);
                // bold text to emphasize
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, mInitialTextSize * 1.0f); // grow text to emphasize
                span.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                view.setText(span);
                break;

            case DESELECTED:
                setLeftRightMargins( view, 2 * mMarginInitialLeft); // grow margins to de-emphasize
                span = new SpannableStringBuilder(text);
                // set text gray to de-emphasize
                span.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.dark_disabled_text)), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, mInitialTextSize * 0.8f); // shrink text to de-emphasize
                view.setText(span);
                break;

            case NORMAL:
            default:
                setLeftRightMargins( view, mMarginInitialLeft); // restore original margins
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, mInitialTextSize * 1.0f); // restore initial test size
                view.setText(text); // remove text emphasis
                break;
        }
    }

    /**
     * change left and right margins to emphasize/de-emphasize
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
     * get the left margin for view
     * @param v
     * @return
     */
    private int leftMargin(View v) {
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        int lm = p.leftMargin;
        return lm;
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }


    public interface OnClickListener {
        void onNoteClick(TranslationHelp note, int resourceCardWidth);
        void onWordClick(String wordId, String chapterId, int resourceCardWidth);
        void onQuestionClick(TranslationHelp question, int resourceCardWidth);
        void onResourceTabNotesSelected(ReviewHolder holder, ReviewListItem item);
        void onResourceTabWordsSelected(ReviewHolder holder, ReviewListItem item);
        void onResourceTabQuestionsSelected(ReviewHolder holder, ReviewListItem item);
    }
}
