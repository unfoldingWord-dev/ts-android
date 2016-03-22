package com.door43.translationstudio.newui.newlanguage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.NewLanguageQuestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Handles the rendering of the questions in NewLanguageActivity
 */
public class NewLanguagePageAdapter extends BaseAdapter {

    public static final String TAG = NewLanguagePageAdapter.class.getSimpleName();
    private List<NewLanguageQuestion> mQuestions = new ArrayList<>();
    private HashMap<Long,Integer> mQuestionIndex;
    private HashMap<Integer,ViewHolder> mViewHolders = new HashMap<>();
    private int mSelectedPosition = -1;
    private LinearLayout mContentsView = null;
    private int mFocusedPosition = -1;
    private int mSelection = -1;

    /**
     * loads list of questions into adapter
     * @param questions
     */
    public void loadQuestions(List<NewLanguageQuestion> questions) {
        mQuestions = questions;
        mQuestionIndex = NewLanguagePageFragment.generateIdMap(questions);

        for (int i = 0; i < mQuestions.size(); i++) {
            View v = getView(i, null, mContentsView);
            mContentsView.addView(v);
        }
    }

    public void restoreFocus(final int focusedPosition, final int selection) {

        mContentsView.post(new Runnable() { // set focus after children have been shown
            @Override
            public void run() {
                mFocusedPosition = focusedPosition;
                mSelection = selection;

                if(mViewHolders.containsKey(mFocusedPosition)) {
                    ViewHolder holder = mViewHolders.get(mFocusedPosition);
                    if(holder.answer != null) {
                        holder.answer.setSelection(mSelection);

                        boolean gotFocus = holder.answer.requestFocus();
                        InputMethodManager mgr = (InputMethodManager)
                                mContentsView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        mgr.showSoftInput(holder.answer, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }
        });
    }

    /**
     * cleanup
     */
    public void cleanup() {
        updateAnswers();
        int focusedPosition = mFocusedPosition; // cache value before destroy
        mContentsView.removeAllViews(); // if this is not removed the old children remain on the display and cause strange side effects
        mFocusedPosition = focusedPosition; // restore
    }

    public void updateAnswers() {
        for(int i = 0; i < mQuestions.size(); i++){
            ViewHolder holder = mViewHolders.get(i);
            NewLanguageQuestion question = getItem(i);
            if(holder.answer != null) {
                question.answer = holder.answer.getText().toString();
                if(i == mFocusedPosition) {
                    mSelection = holder.answer.getSelectionStart();
                }
            }
            if(holder.checkBoxAnswer != null) {
                boolean checked = holder.checkBoxAnswer.isChecked();
                question.setAnswer(checked);
            }
        }
    }

    /**
     * sets the listView for this adaptor
     * @param listView
     */
    public void setContentsView(LinearLayout listView) {
        mContentsView = listView;
    }

    /**
     * determine if this view should be enabled and update enable state
     * @param v
     */
    private void setEnableForView(View v) {
        ViewHolder holder = (ViewHolder) v.getTag();
        if (holder != null) {
            NewLanguageQuestion item = getItem(holder.position);
            if (item != null) {
                enableQuestion(v, holder, shouldEnable(item));
            }
        }
    }

    /**
     * lookup question by question ID
     * @param id
     * @return
     */
    private NewLanguageQuestion getQuestionByID(long id) {
        return NewLanguagePageFragment.getQuestionPositionByID(mQuestions, mQuestionIndex, id);
    }

    /**
     * determine if this question depends on other questions being answers.  Enable if dependencies are satisfied.
     * @param item
     * @return
     */
    protected boolean shouldEnable(NewLanguageQuestion item) {
        long dependencyID = item.conditionalID;
        NewLanguageQuestion dependency =  getQuestionByID(dependencyID);
        return NewLanguagePackage.isDependencyMet(dependency);
    }


    @Override
    public int getCount() {
        return mQuestions.size();
    }

    @Override
    public NewLanguageQuestion getItem(int i) {
        if( (i < 0) || (i >= mQuestions.size())) {
            return null;
        }
        return mQuestions.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        View v = convertView;
        final ViewHolder holder;

        final NewLanguageQuestion item = getItem(position);
        if(null != item) {
            int layoutResId;
            if (item.type == NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN) {
                layoutResId = R.layout.fragment_new_language_checkbox_card;
            } else {
                layoutResId = R.layout.fragment_new_language_edit_card;
            }

            v = LayoutInflater.from(parent.getContext()).inflate(layoutResId, null);
            holder = new ViewHolder(v, position);
            mViewHolders.put(position,holder);

            holder.question.setText(item.question);

            enableQuestion(v, holder, shouldEnable(item));

            if (item.type == NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN) {
                holder.checkBoxAnswer.setChecked(item.isBooleanAnswerTrue());
             } else {
                holder.answer.setHint(item.helpText);
                holder.answer.setText(item.getAnswerNotNull());

                holder.answer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            mFocusedPosition = position;
                        } else if (mFocusedPosition == position) { //see if we deselected this position
                            mFocusedPosition = -1;
                        }
                    }
                });
            }
        }

        return v;
    }

    /**
     * set enable or disable question.  Sets the appropriate enable states and focus states for
     * question view and it's children.
     * @param v
     * @param holder
     * @param enable
     */
    private void enableQuestion(View v, ViewHolder holder, boolean enable) {
        v.setEnabled(enable);
        v.setFocusable(enable);
        v.setFocusableInTouchMode(enable);

        if(holder.question != null) {
            int color;
            if(enable) {
                color = AppContext.context().getResources().getColor(R.color.dark_primary_text);
            } else {
                color = AppContext.context().getResources().getColor(R.color.dark_disabled_text);
            }
            holder.question.setTextColor(color);
            holder.question.setFocusable(false);
            holder.question.setFocusableInTouchMode(false);
        }

        if(holder.answer != null) {
            int color,colorHint;
            if(enable) {
                color = AppContext.context().getResources().getColor(R.color.less_dark_primary_text);
                colorHint = AppContext.context().getResources().getColor(R.color.transparent);
            } else {
                color = AppContext.context().getResources().getColor(R.color.dark_disabled_text);
                colorHint = AppContext.context().getResources().getColor(R.color.half_transparent);
            }
            holder.answer.setTextColor(color);
            holder.answer.setHintTextColor(colorHint);

            holder.answer.setEnabled(enable);
            holder.answer.setFocusable(enable);
            holder.answer.setFocusableInTouchMode(enable);
        }

        if(holder.checkBoxAnswer != null) {
            holder.checkBoxAnswer.setEnabled(enable);
            holder.checkBoxAnswer.setFocusable(enable);
            holder.checkBoxAnswer.setFocusableInTouchMode(enable);
        }
    }

    public int getFocusedPosition() {
        return mFocusedPosition;
    }

    public int getSelection() {
        return mSelection;
    }

    private static class ViewHolder {
        private final EditText answer;
        private final TextView question;
        private final CheckBox checkBoxAnswer;
        private final int position;

        public ViewHolder(View v, int pos) {
            this.question = (TextView)v.findViewById(R.id.label);
            this.answer = (EditText)v.findViewById(R.id.edit_text);
            this.checkBoxAnswer = (CheckBox)v.findViewById(R.id.check_box);
            this.position = pos;
            v.setTag(this);
        }
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public void setSelectedPosition(int mSelectedPosition) {
        this.mSelectedPosition = mSelectedPosition;
    }
}
