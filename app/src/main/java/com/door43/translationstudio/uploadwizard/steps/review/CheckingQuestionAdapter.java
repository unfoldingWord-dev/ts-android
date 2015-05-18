package com.door43.translationstudio.uploadwizard.steps.review;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.CheckingQuestionChapter;
import com.door43.translationstudio.projects.CheckingQuestion;

import java.security.acl.Group;
import java.util.List;

/**
 * Created by joel on 5/16/2015.
 */
public class CheckingQuestionAdapter extends BaseExpandableListAdapter {
    private final Context mContext;
    private List<CheckingQuestionChapter> mChapters;

    public CheckingQuestionAdapter(Context context) {
        mContext = context;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View v = convertView;
        ChildHolder holder = new ChildHolder();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_upload_review_item, null);
            holder.questionText = (TextView)v.findViewById(R.id.questionTextView);
            holder.answerText = (TextView)v.findViewById(R.id.answerTextView);
            holder.imageView = (ImageView)v.findViewById(R.id.imageView);
            v.setTag(holder);
        } else {
            holder = (ChildHolder)v.getTag();
        }

        holder.questionText.setText(getChild(groupPosition, childPosition).question);
        holder.answerText.setText(getChild(groupPosition, childPosition).answer);
        if(getChild(groupPosition, childPosition).isViewed()) {
            holder.imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small));
            holder.answerText.setVisibility(View.VISIBLE);
        } else {
            holder.imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small_disabled));
            holder.answerText.setVisibility(View.GONE);
        }

        return v;
    }

    /**
     * Changes the data in the adapter
     * @param chapters
     */
    public void changeDataset(List<CheckingQuestionChapter> chapters) {
        mChapters = chapters;
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        if(mChapters != null) {
            return mChapters.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if(mChapters != null) {
            return mChapters.get(groupPosition).getCount();
        } else {
            return 0;
        }
    }

    @Override
    public CheckingQuestionChapter getGroup(int groupPosition) {
        if(mChapters != null) {
            return mChapters.get(groupPosition);
        } else {
            return null;
        }
    }

    @Override
    public CheckingQuestion getChild(int groupPosition, int childPosition) {
        if(mChapters != null) {
            return mChapters.get(groupPosition).getQuestion(childPosition);
        } else {
            return null;
        }
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View v = convertView;
        GroupHolder holder = new GroupHolder();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_upload_review_group_item, null);
            holder.title = (TextView)v.findViewById(R.id.groupListHeader);
            holder.imageView = (ImageView)v.findViewById(R.id.statusImage);
            v.setTag(holder);
        } else {
            holder = (GroupHolder)v.getTag();
        }

        holder.title.setText(String.format(mContext.getResources().getString(R.string.label_chapter_title_detailed), getGroup(groupPosition).getId()));
        // TODO: this will be expensive we need a better way to keep track of what's been viewed. perhaps we can check when the user clicks on an item and then cache the value. we could also cache the value when laoding the questions.
        if(getGroup(groupPosition).isViewedCached()) {
            holder.imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small));
        } else {
            holder.imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small_disabled));
        }
        return v;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private class ChildHolder {
        public TextView questionText;
        public TextView answerText;
        public ImageView imageView;
    }

    private class GroupHolder {

        public TextView title;
        public ImageView imageView;
    }
}
