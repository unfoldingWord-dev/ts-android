package com.door43.translationstudio.newui.library;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.App;

import org.unfoldingword.door43client.Door43Client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.unfoldingword.resourcecontainer.Project;

/**
 * This adapter handles all of the projects in the server library browser
 */
public class ServerLibraryAdapter extends BaseAdapter {
    private final Activity mContext;
    private final Door43Client mLibrary;
    private int mSelectedIndex = -1;
    private boolean mSearching = false;
    private boolean[] mHasSource;
    private boolean[] mIsProcessed; // used to determine if we've already pulled the necessary info from the index
    private Project[] mProjects;
    private Project[] mFilteredProjects;
    private ProjectFilter mProjectFilter;
    private LibraryUpdates mUpdates;

    public ServerLibraryAdapter(Activity context) {
        mContext = context;
        mLibrary = App.getLibrary();
    }

    @Override
    public int getCount() {
        if(mFilteredProjects != null) {
            return mFilteredProjects.length;
        } else {
            return 0;
        }
    }

    @Override
    public Project getItem(int position) {
        return mFilteredProjects[position];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if(convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_server_project_list_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.mProjectView.setText(getItem(position).name);

        // selection
        if(mSelectedIndex == position) {
            holder.mBackground.setBackgroundColor(mContext.getResources().getColor(R.color.accent));
            holder.mProjectView.setTextColor(mContext.getResources().getColor(R.color.light_primary_text));
        } else {
            holder.mBackground.setBackgroundColor(Color.TRANSPARENT);
            holder.mProjectView.setTextColor(mContext.getResources().getColor(R.color.dark_primary_text));
        }

        // indicate downloaded
        if(!mIsProcessed[position]) {
            mIsProcessed[position] = true;
            mHasSource[position] = mLibrary.projectHasSource(getItem(position).getId());
        }
        if(mHasSource[position]) {
            if(mSelectedIndex == position) {
                holder.mStatus.setBackgroundResource(R.drawable.ic_bookmark_white_24dp);
            } else {
                holder.mStatus.setBackgroundResource(R.drawable.ic_bookmark_black_24dp);
            }
        } else {
            holder.mStatus.setBackgroundResource(0);
        }

        // indicate updates
        if(mHasSource[position] && mUpdates.hasProjectUpdate(getItem(position).getId())) {
            if(mSelectedIndex == position) {
                holder.mStatus.setBackgroundResource(R.drawable.ic_refresh_white_24dp);
            } else {
                holder.mStatus.setBackgroundResource(R.drawable.ic_refresh_black_24dp);
            }
        }
        return v;
    }

    /**
     * Returns the list of projects that are currently displayed in the list
     * @return
     */
    public List<Project> getFilteredProjects() {
        return null;
    }

    /**
     * Sets the new list data
     * @param availableUpdates
     * @param projects
     */
    public void setData(LibraryUpdates availableUpdates, Project[] projects) {
        mUpdates = availableUpdates;
        mProjects = projects;
        mFilteredProjects = projects;
        mHasSource = new boolean[projects.length];
        mIsProcessed = new boolean[projects.length];
        notifyDataSetChanged();
    }

    public static class ViewHolder {
        private final LinearLayout mBackground;
        public TextView mProjectView;
        public ImageView mStatus;

        public ViewHolder(View view) {
            mProjectView = (TextView) view.findViewById(R.id.projectName);
            mStatus = (ImageView) view.findViewById(R.id.status_icon);
            mBackground = (LinearLayout) view.findViewById(R.id.item_background);
            view.setTag(this);
        }
    }

    public void setSelected(int index) {
        mSelectedIndex = index;
        notifyDataSetChanged();
    }

    /**
     * Returns the project filter
     * @return
     */
    public Filter getFilter() {
        if(mProjectFilter == null) {
            mProjectFilter = new ProjectFilter();
        }
        return mProjectFilter;
    }

    /**
     * A filter for projects
     */
    private class ProjectFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();
            if(charSequence == null || charSequence.length() == 0) {
                // no filter
                results.values = Arrays.asList(mProjects);
                results.count = mProjects.length;
            } else {
                // perform filter
                List<Project> filteredProjects = new ArrayList<>();
                for(Project project: mProjects) {
                    // match the project id
                    boolean match = project.slug.toLowerCase().startsWith(charSequence.toString().toLowerCase());
                    if(!match) {
                        if (project.name.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match the project title in any language
                            match = true;
                        }
//                        TODO: search the source language title as well. This requires an update to the project category.
                        else if (project.name.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {// || l.getName().toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match the language id or name
                            match = true;
                        } else if (project.slug.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match category id
                            match = true;
                        }
                    }
                    if(match) {
                        filteredProjects.add(project);
                    }
                }
                results.values = filteredProjects;
                results.count = filteredProjects.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            List<Project> filteredProjects = ((List<Project>) filterResults.values);
            mFilteredProjects = filteredProjects.toArray(new Project[filterResults.count]);
            notifyDataSetChanged();
        }
    }
}
