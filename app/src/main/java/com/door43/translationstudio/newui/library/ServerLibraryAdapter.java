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
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This adpater handles all of the projects in the server library browser
 */
public class ServerLibraryAdapter extends BaseAdapter {
    private final Activity mContext;
    private final Library mLibrary;
    private int mSelectedIndex = -1;
    private boolean mSearching = false;

    private ProjectCategory[] mCategories;
    private ProjectCategory[] mFilteredCategories;
    private ProjectCategoryFilter mProjectFilter;
    private LibraryUpdates mUpdates;

    public ServerLibraryAdapter(Activity context) {
        mContext = context;
        mLibrary = AppContext.getLibrary();
    }

    @Override
    public int getCount() {
        if(mFilteredCategories != null) {
            return mFilteredCategories.length;
        } else {
            return 0;
        }
    }

    @Override
    public ProjectCategory getItem(int position) {
        return mFilteredCategories[position];
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

        holder.mProjectView.setText(getItem(position).title);

        // selection
        if(mSelectedIndex == position) {
            holder.mBackground.setBackgroundColor(mContext.getResources().getColor(R.color.accent));
            holder.mProjectView.setTextColor(mContext.getResources().getColor(R.color.light_primary_text));
        } else {
            holder.mBackground.setBackgroundColor(Color.TRANSPARENT);
            holder.mProjectView.setTextColor(mContext.getResources().getColor(R.color.dark_primary_text));
        }

        // indicate downloaded
        boolean isDownloaded = mLibrary.projectHasSource(getItem(position).projectId);
        if(isDownloaded) {
            if(mSelectedIndex == position) {
                holder.mStatus.setBackgroundResource(R.drawable.ic_bookmark_white_24dp);
            } else {
                holder.mStatus.setBackgroundResource(R.drawable.ic_bookmark_black_24dp);
            }
        } else {
            holder.mStatus.setBackgroundResource(0);
        }

        // indicate updates
        if(isDownloaded && mUpdates.hasProjectUpdate(getItem(position).projectId)) {
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
     * @param categories
     */
    public void setData(LibraryUpdates availableUpdates, ProjectCategory[] categories) {
        mUpdates = availableUpdates;
        mCategories = categories;
        mFilteredCategories = categories;
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
            mProjectFilter = new ProjectCategoryFilter();
        }
        return mProjectFilter;
    }

    /**
     * A filter for projects
     */
    private class ProjectCategoryFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();
            if(charSequence == null || charSequence.length() == 0) {
                // no filter
                results.values = Arrays.asList(mCategories);
                results.count = mCategories.length;
            } else {
                // perform filter
                List<ProjectCategory> filteredCategories = new ArrayList<>();
                for(ProjectCategory category:mCategories) {
                    // match the project id
                    boolean match = category.projectId.toLowerCase().startsWith(charSequence.toString().toLowerCase());
                    if(!match) {
                        if (category.title.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match the project title in any language
                            match = true;
                        }
//                        TODO: search the source language title as well. This requires an update to the project category.
                        else if (category.sourcelanguageId.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {// || l.getName().toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match the language id or name
                            match = true;
                        } else if (category.getId().toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match category id
                            match = true;
                        }
                    }
                    if(match) {
                        filteredCategories.add(category);
                    }
                }
                results.values = filteredCategories;
                results.count = filteredCategories.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            List<ProjectCategory> filteredProjects = ((List<ProjectCategory>) filterResults.values);
            if(charSequence != null && charSequence.length() > 0) {
                sortProjectCategories(filteredProjects, charSequence);
            }
            mFilteredCategories = filteredProjects.toArray(new ProjectCategory[filterResults.count]);
            notifyDataSetChanged();
        }
    }

    /**
     * Sorts project categories by id
     * @param categories
     * @param referenceId categories are sorted according to the reference id
     */
    private static void sortProjectCategories(List<ProjectCategory> categories, final CharSequence referenceId) {
        Collections.sort(categories, new Comparator<ProjectCategory>() {
            @Override
            public int compare(ProjectCategory lhs, ProjectCategory rhs) {
                String lhId = lhs.projectId;
                String rhId = rhs.projectId;
                // give priority to matches with the reference
                if (lhId.startsWith(referenceId.toString().toLowerCase())) {
                    lhId = "!" + lhId;
                }
                if (rhId.startsWith(referenceId.toString().toLowerCase())) {
                    rhId = "!" + rhId;
                }
                return lhId.compareToIgnoreCase(rhId);
            }
        });
    }
}
