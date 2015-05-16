package com.door43.translationstudio.uploadwizard.steps.review;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.door43.translationstudio.projects.CheckingQuestion;

import java.util.List;

/**
 * Created by joel on 5/16/2015.
 */
public class CheckingQuestionAdapter extends BaseAdapter {
    private List<CheckingQuestion> mQuestions;

    @Override
    public int getCount() {
        return mQuestions.size();
    }

    @Override
    public CheckingQuestion getItem(int position) {
        return mQuestions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO: generate the view
        return null;
    }

    /**
     * Changes the data in the adapter
     * @param questions
     */
    public void changeDataset(List<CheckingQuestion> questions) {
        mQuestions = questions;
        notifyDataSetChanged();
    }
}
