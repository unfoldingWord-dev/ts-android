package com.door43.translationstudio.library;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;

/**
 * This adpater handles all of the projects in the server library browser
 */
public class LibraryProjectAdapter extends BaseAdapter {
    private final MainApplication mContext;
    private int mSelectedIndex = -1;
    private Project[] mProjects;

    public LibraryProjectAdapter(MainApplication context, Project[] projects) {
        mProjects = projects;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mProjects.length;
    }

    @Override
    public Project getItem(int i) {
        return mProjects[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = view;
        ViewHolder holder = new ViewHolder();

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_project_library_projects_item, null);
            // layout
            holder.name = (TextView)v.findViewById(R.id.projectNameTextView);
            holder.container = (RelativeLayout)v.findViewById(R.id.projectItemContainer);
            holder.progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
            holder.progressBar.setMax(100);
            holder.downloadedImage = (ImageView)v.findViewById(R.id.downloadedImageView);

            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // set graphite fontface
        Typeface typeface = AppContext.graphiteTypeface(getItem(i).getSelectedSourceLanguage());
        holder.name.setTypeface(typeface, 0);

        // set font size
        float fontsize = AppContext.typefaceSize();
        holder.name.setTextSize(fontsize);

        // set value
        holder.name.setText(getItem(i).getTitle());
        holder.downloadedImage.setVisibility(View.INVISIBLE);

        // indicated selected
        if(i == mSelectedIndex) {
            holder.container.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            holder.name.setTextColor(mContext.getResources().getColor(R.color.white));
        } else {
            holder.container.setBackgroundColor(mContext.getResources().getColor(R.color.white));
            holder.name.setTextColor(mContext.getResources().getColor(R.color.dark_gray));

            // TODO: hook onto update progress.

            // check download status
            if(AppContext.projectManager().isProjectDownloaded(getItem(i).getId())) {
                // check if an update for this project exists
                if(AppContext.projectManager().isProjectUpdateAvailable(getItem(i))) {
                    holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_update_small));
                } else {
                    holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small));
                }
                holder.downloadedImage.setVisibility(View.VISIBLE);
            } else {
                holder.downloadedImage.setVisibility(View.INVISIBLE);
            }
        }

        return v;
    }

    public void changeDataSet(Project[] projects) {
        mProjects = projects;
        notifyDataSetChanged();
    }

    private class ViewHolder {
        public TextView name;
        public RelativeLayout container;
        public ProgressBar progressBar;
        public ImageView downloadedImage;
    }

    public void setSelected(int index) {
        mSelectedIndex = index;
    }
}
