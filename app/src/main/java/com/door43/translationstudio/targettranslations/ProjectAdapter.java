package com.door43.translationstudio.targettranslations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ProjectCategory;

/**
 * Created by joel on 9/4/2015.
 */
public class ProjectAdapter extends BaseAdapter {
    private ProjectCategory[] mCategories;

    public ProjectAdapter(ProjectCategory[] categories) {
        mCategories = categories;
    }

    @Override
    public int getCount() {
        if(mCategories != null) {
            return mCategories.length;
        } else {
            return 0;
        }
    }

    @Override
    public ProjectCategory getItem(int position) {
        return mCategories[position];
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

        return v;
    }

    /**
     * Updates the data set
     * @param categories
     */
    public void changeData(ProjectCategory[] categories) {
        mCategories = categories;
        notifyDataSetChanged();
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
}
