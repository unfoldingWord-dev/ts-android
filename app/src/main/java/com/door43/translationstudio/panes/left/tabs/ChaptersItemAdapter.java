package com.door43.translationstudio.panes.left.tabs;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;

/**
 * Created by joel on 9/2/2014.
 */
public class ChaptersItemAdapter extends BaseAdapter {

    private final MainApplication mContext;

    /**
     * Creates a new Chapter adapter
     * @param c The activity context
     */
    public ChaptersItemAdapter(MainApplication c) {
        mContext = c;
    }

    @Override
    public int getCount() {
        return mContext.getSharedProjectManager().getSelectedProject().numChapters();
    }

    @Override
    public Object getItem(int i) {
        return getChaptertItem(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout chapterItemView;

        // if it's not recycled, initialize some attributes
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            chapterItemView = (LinearLayout)inflater.inflate(R.layout.fragment_pane_left_chapters_item, null);
        } else {
            chapterItemView = (LinearLayout)view;
        }


        // image

        // title
        TextView chapterTitle = (TextView)chapterItemView.findViewById(R.id.chapterTitle);
        chapterTitle.setText(getChaptertItem(i).getTitle());

        // description
        TextView chapterDescription = (TextView)chapterItemView.findViewById(R.id.chapterDescription);
        chapterDescription.setText(getChaptertItem(i).getDescription());

        // highlight selected chapter
        if(mContext.getSharedProjectManager().getSelectedProject().getSelectedChapter().getId() == getChaptertItem(i).getId()) {
            chapterItemView.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            chapterDescription.setTextColor(Color.WHITE);
            chapterTitle.setTextColor(Color.WHITE);
        } else {
            chapterItemView.setBackgroundColor(Color.TRANSPARENT);
            chapterDescription.setTextColor(mContext.getResources().getColor(R.color.gray));
            chapterTitle.setTextColor(mContext.getResources().getColor(R.color.black));
        }

        return chapterItemView;
    }

    private Chapter getChaptertItem(int i) {
        return mContext.getSharedProjectManager().getSelectedProject().getChapter(i);
    }
}
