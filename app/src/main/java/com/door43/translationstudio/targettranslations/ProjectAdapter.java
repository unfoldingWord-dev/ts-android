package com.door43.translationstudio.targettranslations;

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
import java.util.List;

/**
 * Created by joel on 9/4/2015.
 */
public class ProjectAdapter extends BaseAdapter {
    private ProjectCategory[] mCategories;
    private ProjectCategory[] mFilteredCategories;
    private ProjectFilter mProjectFilter;

    public ProjectAdapter(ProjectCategory[] categories) {
        mCategories = categories;
        mFilteredCategories = categories;
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
//                        TODO: search the source language id as well. This requires an update to the project category.
                        else if (category.sourcelanguageId.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {// || l.getName().toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match the language id or name
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
            mFilteredCategories = ((List<ProjectCategory>)filterResults.values).toArray(new ProjectCategory[filterResults.count]);
            notifyDataSetChanged();
        }
    }
}
