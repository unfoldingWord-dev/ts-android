package com.door43.translationstudio.newui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ProjectCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by joel on 9/4/2015.
 */
public class ProjectAdapter extends BaseAdapter {
    private ProjectCategory[] mCategories;
    private ProjectCategory[] mFilteredCategories;
    private ProjectFilter mProjectFilter;

    public ProjectAdapter(ProjectCategory[] categories) {
        List<ProjectCategory> categoriesList = Arrays.asList(categories);
        Collections.sort(categoriesList);
        mCategories = categoriesList.toArray(new ProjectCategory[categoriesList.size()]);
        mFilteredCategories = mCategories;
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
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if(convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_project_list_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // render view
        holder.mProjectView.setText(getItem(position).title);
        if(getItem(position).isProject()) {
            holder.mMoreImage.setVisibility(View.GONE);
        } else {
            holder.mMoreImage.setVisibility(View.VISIBLE);
        }
        // TODO: render icon

        return v;
    }

    /**
     * Updates the data set
     * @param categories
     */
    public void changeData(ProjectCategory[] categories) {
        mCategories = categories;
        mFilteredCategories = categories;
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

    public static class ViewHolder {
        public ImageView mIconImage;
        public TextView mProjectView;
        public ImageView mMoreImage;

        public ViewHolder(View view) {
            mIconImage = (ImageView) view.findViewById(R.id.projectIcon);
            mProjectView = (TextView) view.findViewById(R.id.projectName);
            mMoreImage = (ImageView) view.findViewById(R.id.moreIcon);
            view.setTag(this);
        }
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
     * Sorts target languages by id
     * @param languages
     * @param referenceId languages are sorted according to the reference id
     */
    private static void sortProjectCategories(List<ProjectCategory> languages, final CharSequence referenceId) {
        Collections.sort(languages, new Comparator<ProjectCategory>() {
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
