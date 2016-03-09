package com.door43.translationstudio.newui.newtranslation;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ArchiveDetails;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.core.Util;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Handles the rendering of the new language cards activity
 */
public class RequestNewLanguageStepAdapter extends BaseAdapter {

    public static final String TRUE_STR = "YES";
    public static final String FALSE_STR = "NO";
    private List<NewLanguageQuestion> mQuestions = new ArrayList<>();
    private int mSelectedPosition = -1;

    public void loadQuestions(List<NewLanguageQuestion> questions) {
        mQuestions = questions;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mQuestions.size();
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
        ViewHolder holder;

        NewLanguageQuestion item = getItem(position);
        int layoutResId;
        if (item.type == NewLanguageQuestion.QuestionType.CHECK_BOX) {
            layoutResId = R.layout.fragment_new_language_checkbox_card;
        } else {
            layoutResId = R.layout.fragment_new_language_edit_card;
        }

        if(convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(layoutResId, null);
            holder = new ViewHolder(v,position);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.question.setText(item.question);

        if (item.type == NewLanguageQuestion.QuestionType.CHECK_BOX) {
            holder.checkBoxAnswer.setChecked(TRUE_STR.equals(item.answer));
        } else {
            holder.answer.setHint(item.helpText);
            if(item.answer != null) {
                holder.answer.setText(item.answer);
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
