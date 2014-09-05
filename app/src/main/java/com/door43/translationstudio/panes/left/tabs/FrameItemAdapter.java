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
 * Created by joel on 8/29/2014.
 */
public class FrameItemAdapter extends BaseAdapter {

    private final MainApplication context;

    /**
     * Creates a new Frame adapter
     * @param c The activity context
     */
    public FrameItemAdapter(MainApplication c) {
        context = c;
    }

    @Override
    public int getCount() {
        return context.getSharedProjectManager().getSelectedProject().getSelectedChapter().numFrames();
    }

    @Override
    public Object getItem(int i) {
        return context.getSharedProjectManager().getSelectedProject().getSelectedChapter().getFrame(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout frameItemView;

        // if it's not recycled, initialize some attributes
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            frameItemView = (LinearLayout)inflater.inflate(R.layout.fragment_pane_left_frames_item, null);
        } else {
            frameItemView = (LinearLayout)view;
        }


        // image

        // title
        TextView frameId = (TextView)frameItemView.findViewById(R.id.frameId);
        frameId.setText(context.getSharedProjectManager().getSelectedProject().getSelectedChapter().getFrame(i).getID());

        // description
        TextView frameDescription = (TextView)frameItemView.findViewById(R.id.frameDescription);
        frameDescription.setText(context.getSharedProjectManager().getSelectedProject().getSelectedChapter().getFrame(i).getText());

        return frameItemView;
    }
}
