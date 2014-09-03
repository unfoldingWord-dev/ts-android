package com.door43.translationstudio.panes.left.tabs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;

/**
 * Created by joel on 9/2/2014.
 */
public class ChaptersItemAdapter extends BaseAdapter {

    private final MainApplication context;

    /**
     * Creates a new Chapter adapter
     * @param c The activity context
     */
    public ChaptersItemAdapter(MainApplication c) {
        context = c;
    }

    @Override
    public int getCount() {
        return context.getSharedProjectManager().getSelectedProject().numChapters();
    }

    @Override
    public Object getItem(int i) {
        return context.getSharedProjectManager().getSelectedProject().getChapter(i);
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
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            chapterItemView = (LinearLayout)inflater.inflate(R.layout.fragment_pane_left_chapters_item, null);
        } else {
            chapterItemView = (LinearLayout)view;
        }


        // image

        // title
        TextView chapterTitle = (TextView)chapterItemView.findViewById(R.id.chapterTitle);
        chapterTitle.setText(context.getSharedProjectManager().getSelectedProject().getChapter(i).getTitle());

        // description
        TextView chapterDescription = (TextView)chapterItemView.findViewById(R.id.chapterDescription);
        chapterDescription.setText(context.getSharedProjectManager().getSelectedProject().getChapter(i).getDescription());

        return chapterItemView;
    }
}
