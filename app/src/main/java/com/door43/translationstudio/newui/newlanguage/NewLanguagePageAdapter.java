package com.door43.translationstudio.newui.newlanguage;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
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
        int focusedPosition = mFocusedPosition; // cache value before destroy
        mContentsView.removeAllViews(); // if this is not removed the old children remain on the display and cause strange side effects
        mFocusedPosition = focusedPosition; // restore
    }

    public void updateAnswers() {
        for(int i = 0; i < mQuestions.size(); i++){
            ViewHolder holder = mViewHolders.get(i);
            NewLanguageQuestion question = getItem(i);
            if(holder.answer != null) {
//                question.answer = holder.answer.getText().toString();
                if(i == mFocusedPosition) {
                    mSelection = holder.answer.getSelectionStart();
                }
            }

            if(holder.radioButtonYes != null) {
//                question.answer = null; //default if none selected
                boolean checked = holder.radioButtonYes.isChecked();
                if(checked) {
                    question.setAnswer(true);
                } else if(holder.radioButtonNo != null) {
                    checked = holder.radioButtonNo.isChecked();
                    if(checked) {
                        question.setAnswer(false);
                    }
                }
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
            if(question.reliantQuestionId == id) {
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
        long dependencyID = item.reliantQuestionId;
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
            if (item.type == NewLanguageQuestion.InputType.Boolean) {
                layoutResId = R.layout.fragment_new_language_boolean_card;
            } else {
                layoutResId = R.layout.fragment_new_language_text_input_card;
            }

            v = LayoutInflater.from(parent.getContext()).inflate(layoutResId, null);
            holder = new ViewHolder(v, position);
            mViewHolders.put(position, holder);

            holder.question.setText(item.question);

            final boolean hasDependencies = hasDependencies(item.id);
            final View thisView = v;

            enableQuestion(v, holder, shouldEnable(item));

            if (item.type == NewLanguageQuestion.InputType.Boolean) {
                holder.radioButtonYes.setChecked(item.isBooleanAnswerTrue());
                holder.radioButtonYes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        setCheckedState(isChecked, true, item, hasDependencies, thisView);
                    }
                });

                holder.radioButtonNo.setChecked(item.isBooleanAnswerFalse());
                holder.radioButtonNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        setCheckedState(isChecked, false, item, hasDependencies, thisView);
                    }
                });

            } else {
                String help = item.helpText == null ? "" : item.helpText;
                if(help.length() < 10) {
                    help += "          ";
                }
                holder.answer.setHint(help);
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

                holder.answer.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        boolean intiallyEmpty = item.isAnswerEmpty();
//                        item.answer = s.toString();
                        boolean currentlyEmpty = item.isAnswerEmpty();
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

    private void setCheckedState(boolean checked, boolean newAnswer, NewLanguageQuestion item, boolean hasDependencies, View thisView) {
        if(checked) {
            item.setAnswer(newAnswer);
            if (hasDependencies) {
                updateDisplayedQuestions(thisView);
            }
        }
    }

    /**
     * set enable or disable question.  Sets the appropriate enable states and focus states for
     * question view and it's children.
     * @param v
     * @param holder
     * @param enable
     */
    private void enableQuestion(View v, ViewHolder holder, boolean enable) {
        inputEnable(v, enable);

        if(holder.question != null) {
            int color;
            if(enable) {
                color = AppContext.context().getResources().getColor(R.color.dark_primary_text);
            } else {
                color = AppContext.context().getResources().getColor(R.color.dark_disabled_text);
            }
            holder.question.setTextColor(color);
            setFocusable(holder.question, false);
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

            inputEnable(holder.answer, enable);
        }

        radioEnable(holder.radioButtonYes, enable);
        radioEnable(holder.radioButtonNo, enable);
    }

    private static void radioEnable(View v, boolean enable) {
        if(v != null) {
            v.setEnabled(enable);
            v.setFocusable(enable);
            v.setFocusableInTouchMode(false);
        }
    }

    private static void inputEnable(View v, boolean enable) {
        if(v != null) {
            v.setEnabled(enable);
            setFocusable(v, enable);
        }
    }

    private static void setFocusable(View v, boolean focusable) {
        v.setFocusable(focusable);
        v.setFocusableInTouchMode(focusable);
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
        private final RadioButton radioButtonYes;
        private final RadioButton radioButtonNo;
        private final int position;

        public ViewHolder(View v, int pos) {
            this.question = (TextView)v.findViewById(R.id.label);
            this.answer = (EditText)v.findViewById(R.id.edit_text);
            this.radioButtonYes = (RadioButton)v.findViewById(R.id.radio_button_yes);
            this.radioButtonNo = (RadioButton)v.findViewById(R.id.radio_button_no);
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
