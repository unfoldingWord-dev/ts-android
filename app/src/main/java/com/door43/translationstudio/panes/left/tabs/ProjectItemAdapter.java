package com.door43.translationstudio.panes.left.tabs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectItemAdapter extends BaseAdapter {

    private final MainApplication mContext;

    /**
     * Creates a new Project adapter
     * @param c The activity context
     */
    public ProjectItemAdapter(MainApplication c) {
        mContext = c;
    }

    @Override
    public int getCount() {
        return mContext.getSharedProjectManager().numProjects();
    }

    @Override
    public Object getItem(int i) {
        return getProjectItem(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        RelativeLayout projectItemView;

        // if it's not recycled, initialize some attributes
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            projectItemView = (RelativeLayout)inflater.inflate(R.layout.fragment_pane_left_projects_item, null);
        } else {
            projectItemView = (RelativeLayout)view;
        }

        // image
        final ImageView projectIcon = (ImageView)projectItemView.findViewById(R.id.projectIcon);
        String imageUri = "assets://"+ getProjectItem(i).getImagePath();
        mContext.getImageLoader().loadImage(imageUri, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                projectIcon.setImageBitmap(loadedImage);
            }
        });

        // title
        TextView projectTitle = (TextView)projectItemView.findViewById(R.id.projectTitle);
        projectTitle.setText(getProjectItem(i).getTitle());

        // description
        TextView projectDescription = (TextView)projectItemView.findViewById(R.id.projectDescription);
        projectDescription.setText(getProjectItem(i).getDescription());

        // translation in progress
        ImageView translationIcon = (ImageView)projectItemView.findViewById(R.id.translationStatusIcon);
        if(getProjectItem(i).isTranslating()) {
            translationIcon.setVisibility(View.VISIBLE);
        } else {
            translationIcon.setVisibility(View.GONE);
        }

        // highlight selected project
        if(mContext.getSharedProjectManager().getSelectedProject() != null && mContext.getSharedProjectManager().getSelectedProject().getId() == getProjectItem(i).getId()) {
            projectItemView.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            projectDescription.setTextColor(Color.WHITE);
            projectTitle.setTextColor(Color.WHITE);
            translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_wrench));
        } else {
            projectItemView.setBackgroundColor(Color.TRANSPARENT);
            projectDescription.setTextColor(mContext.getResources().getColor(R.color.gray));
            projectTitle.setTextColor(mContext.getResources().getColor(R.color.black));
            translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_wrench_dark));
        }

        return projectItemView;
    }

    private Project getProjectItem(int i) {
        return mContext.getSharedProjectManager().getProject(i);
    }
}
