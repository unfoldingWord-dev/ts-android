package com.door43.translationstudio.panes.left.tabs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;

import java.util.Date;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectItemAdapter extends BaseAdapter {

    private final MainApplication context;

    /**
     * Creates a new Project adapter
     * @param c The activity context
     */
    public ProjectItemAdapter(MainApplication c) {
        context = c;
    }

    @Override
    public int getCount() {
        return context.getSharedProjectManager().size();
    }

    @Override
    public Object getItem(int i) {
        return context.getSharedProjectManager().getProject(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout projectItemView;

        // if it's not recycled, initialize some attributes
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            projectItemView = (LinearLayout)inflater.inflate(R.layout.fragment_pane_left_projects_item, null);
        } else {
            projectItemView = (LinearLayout)view;
        }


        // image

        // title
        TextView projectTitle = (TextView)projectItemView.findViewById(R.id.projectTitle);
        projectTitle.setText(context.getSharedProjectManager().getProject(i).getTitle());

        // description
        TextView projectDescription = (TextView)projectItemView.findViewById(R.id.projectDescription);
        projectDescription.setText(context.getSharedProjectManager().getProject(i).getDescription());

        return projectItemView;
    }
}
