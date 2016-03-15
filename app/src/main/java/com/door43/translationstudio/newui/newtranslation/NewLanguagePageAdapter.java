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
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageQuestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Handles the rendering of the new language cards activity
 */
public class NewLanguagePageAdapter extends BaseAdapter {

    public static final String TRUE_STR = "YES";
    public static final String FALSE_STR = "NO";
    public static final String TAG = NewLanguageActivity.class.getSimpleName();
    private List<NewLanguageQuestion> mQuestions = new ArrayList<>();
    private List<Integer> mDisplayedQuestions = new ArrayList<>();
    private HashMap<Long,Integer> mQuestionIndex;
    private int mSelectedPosition = -1;

    public void loadQuestions(List<NewLanguageQuestion> questions) {
        mQuestions = questions;
        mQuestionIndex = NewLanguagePageFragment.generateIdMap(questions);

        updateDisplayedQuestions();
    }

    static public boolean isCheckBoxAnswerTrue(NewLanguageQuestion question) {
        return TRUE_STR.equals(question.answer);
    }

    private void updateDisplayedQuestions() {
        mDisplayedQuestions = new ArrayList<>();
        for (int i = 0; i < mQuestions.size(); i++) {
            NewLanguageQuestion question = mQuestions.get(i);
            if(shouldDisplay(question)) {
                mDisplayedQuestions.add(i);
            }
        }
        notifyDataSetChanged();
    }

    private boolean hasDependencies(long id) {
        for (NewLanguageQuestion question : mQuestions) {
            if(question.conditionalID == id) {
                return true;
            }
        }
        return false;
    }

    private NewLanguageQuestion getQuestionByID(long id) {
        return NewLanguagePageFragment.getQuestionPositionByID(mQuestions, mQuestionIndex, id);
    }

    private NewLanguageQuestion getDisplayedItemAtPos(int pos) {
        if((pos < 0) || (pos >=  mDisplayedQuestions.size())) {
            return null;
        }
        try {
            Integer index = mDisplayedQuestions.get(pos);
            NewLanguageQuestion question = mQuestions.get(index);
            return question;
        } catch (Exception e) {
            return null;
        }
    }

    protected boolean shouldDisplay(NewLanguageQuestion item) {
        long dependencyID = item.conditionalID;
        NewLanguageQuestion dependency =  getQuestionByID(dependencyID);
        boolean display = true;
        if(dependency != null) {
            if((dependency.answer != null ) && !dependency.answer.isEmpty()) {

                if(dependency.type == NewLanguageQuestion.QuestionType.CHECK_BOX) {
                    display = TRUE_STR.equals(dependency.answer);
                } else {
                    display = true;
                }
            } else {
                display = false;
            }
        }
        return display;
    }



    @Override
    public int getCount() {
        return mDisplayedQuestions.size();
    }

    @Override
    public NewLanguageQuestion getItem(int i) {
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

        final NewLanguageQuestion item = getDisplayedItemAtPos(position);
        if(null != item) {
            int layoutResId;
            if (item.type == NewLanguageQuestion.QuestionType.CHECK_BOX) {
                layoutResId = R.layout.fragment_new_language_checkbox_card;
            } else {
                layoutResId = R.layout.fragment_new_language_edit_card;
            }

            // TODO: 3/14/16 check to see if we can reuse previous v
            v = LayoutInflater.from(parent.getContext()).inflate(layoutResId, null);
            holder = new ViewHolder(v, position);

            holder.question.setText(item.question);

            final boolean hasDependencies = hasDependencies(item.id);
            boolean display = shouldDisplay(item);

            v.setVisibility(display ? View.VISIBLE : View.GONE);

            if (item.type == NewLanguageQuestion.QuestionType.CHECK_BOX) {
                holder.checkBoxAnswer.setChecked(TRUE_STR.equals(item.answer));
                holder.checkBoxAnswer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.answer = isChecked ? TRUE_STR : FALSE_STR;

                        if (hasDependencies) {
                            updateDisplayedQuestions();
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
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        item.answer = s.toString();

                        if (hasDependencies) {
                            updateDisplayedQuestions();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
            }
        }

        return v;
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
