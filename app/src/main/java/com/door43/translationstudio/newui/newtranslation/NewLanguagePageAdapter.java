package com.door43.translationstudio.newui.newtranslation;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
    public static final int UN_INIT = 0xFFFFFFFE;
    private List<NewLanguageQuestion> mQuestions = new ArrayList<>();
    private HashMap<Long,Integer> mQuestionIndex;
    private int mSelectedPosition = -1;
    private LinearLayout mContentsView = null;

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

    /**
     * sets the listView for this adapture
     * @param listView
     */
    public void setContentsView(LinearLayout listView) {
        mContentsView = listView;
    }

   /**
     * go through children of listView and update their enable state
     * @param exceptView - view to skip (likely the dependent question view)
     */
    private void updateDisplayedQuestions(View exceptView) {

        if(mContentsView != null) {
            for (int i = 0; i < mContentsView.getChildCount(); i++) {
                View v = mContentsView.getChildAt(i);
                if ((v != null) && (v != exceptView)) {
                    setEnableForView(v);
                }
            }
        }
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
     * checks to see if there are other views that are dependent on this view.
     * @param id
     * @return
     */
    private boolean hasDependencies(long id) {
        for (NewLanguageQuestion question : mQuestions) {
            if(question.conditionalID == id) {
                return true;
            }
        }
        return false;
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

            // TODO: 3/14/16 add check to see if we can reuse previous v
            v = LayoutInflater.from(parent.getContext()).inflate(layoutResId, null);
            holder = new ViewHolder(v, position);
            final View thisView = v;

            holder.question.setText(item.question);

            final boolean hasDependencies = hasDependencies(item.id);

            enableQuestion(v, holder, shouldEnable(item));

            if (item.type == NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN) {
                holder.checkBoxAnswer.setChecked(NewLanguagePackage.isCheckBoxAnswerTrue(item));
                holder.checkBoxAnswer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.answer = NewLanguagePackage.getCheckBoxAnswer(isChecked);
                        if (hasDependencies) {
                            updateDisplayedQuestions(thisView);
                        }
                    }
                });
            } else {
                holder.answer.setHint(item.helpText);
                if (item.answer != null) {
                    holder.answer.setText(item.answer);
                }
                holder.answer.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        boolean intiallyEmpty = NewLanguagePackage.isAnswerEmpty(item);
                        item.answer = s.toString();
                        boolean currentlyEmpty = NewLanguagePackage.isAnswerEmpty(item);
                        boolean emptyStateChanged = (intiallyEmpty != currentlyEmpty);

                        if (hasDependencies && emptyStateChanged) {
                            updateDisplayedQuestions(thisView);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
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
