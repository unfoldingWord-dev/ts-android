package com.door43.translationstudio.uploadwizard.steps.choose;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.door43.translationstudio.projects.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/17/2015.
 */
@Deprecated
public class ProjectChooserAdapter extends BaseAdapter {
    private List<Project> mProjects = new ArrayList<>();

    @Override
    public int getCount() {
        return mProjects.size();
    }

    @Override
    public Project getItem(int position) {
        return mProjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // TODO: render the view
        return null;
    }

    /**
     * Changes the dataset
     * @param projects
     */
    public void changeDataset(List<Project> projects) {
        mProjects = projects;
        notifyDataSetChanged();
    }
}
