package com.door43.translationstudio.library;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;

import java.util.ArrayList;
import java.util.List;

/**
 * This adpater handles all of the projects in the server library browser
 */
public class LibraryProjectAdapter extends BaseAdapter {
    private final MainApplication mContext;
    private boolean mDisplayAsNewProjects;
    private int mSelectedIndex = -1;
    private List<Project> mProjects;
    private ProjectFilter mProjectFilter;
    private List<Project> mOriginalProjects = new ArrayList<>();
    private boolean mSearching = false;

    public LibraryProjectAdapter(MainApplication context, List<Project> projects, boolean displayAsNewProjects) {
        mProjects = projects;
        mOriginalProjects = projects;
        mContext = context;
        mDisplayAsNewProjects = displayAsNewProjects;
    }

    @Override
    public int getCount() {
        return mProjects.size();
    }

    @Override
    public Project getItem(int i) {
        return mProjects.get(i);
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
            holder.languagesText = (TextView)v.findViewById(R.id.availableLanguagesTextView);

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

        // display available languages if searching
        if(mSearching) {
            holder.languagesText.setVisibility(View.VISIBLE);
            String languagesTextList = getItem(i).getSourceLanguage(0).getName();
            for(int j = 1; j < getItem(i).getSourceLanguages().size(); j ++) {
                if(getItem(i).getSourceLanguage(j).checkingLevel() >= AppContext.minCheckingLevel()) {
                    languagesTextList += ", " + getItem(i).getSourceLanguage(j).getName();
                }
            }
            holder.languagesText.setText(languagesTextList);
        } else {
            holder.languagesText.setVisibility(View.GONE);
        }

        // indicated selected
        if(i == mSelectedIndex) {
            holder.container.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            holder.name.setTextColor(mContext.getResources().getColor(R.color.white));
        } else {
            holder.container.setBackgroundColor(mContext.getResources().getColor(R.color.white));
            holder.name.setTextColor(mContext.getResources().getColor(R.color.dark_gray));

            // check download status
            if(!mDisplayAsNewProjects && AppContext.projectManager().isProjectDownloaded(getItem(i).getId())) {
                // check if an update for this project exists
                if(AppContext.projectManager().isProjectUpdateAvailable(getItem(i))) {
                    if(LibraryTempData.getEnableEditing()) {
                        holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_update_small_disabled));
                    } else {
                        holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_update_small));
                    }
                } else {
                    if(LibraryTempData.getEnableEditing()) {
                        holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small_disabled));
                    } else {
                        holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small));
                    }
                }
                holder.downloadedImage.setVisibility(View.VISIBLE);
            }
        }

        return v;
    }

    public void changeDataSet(List<Project> projects) {
        mProjects = projects;
        mOriginalProjects = projects;
        notifyDataSetChanged();
    }

    public void changeDataSet(List<Project> projects, boolean displayAsNewProjects, int selectedIndex) {
        mProjects = projects;
        mOriginalProjects = projects;
        mDisplayAsNewProjects = displayAsNewProjects;
        mSelectedIndex = selectedIndex;
        notifyDataSetChanged();
    }

    /**
     * Returns the list of projects that are currently displayed in the list
     * @return
     */
    public List<Project> getFilteredProjects() {
        return mProjects;
    }

    private class ViewHolder {
        public TextView name;
        public RelativeLayout container;
        public ProgressBar progressBar;
        public ImageView downloadedImage;
        public TextView languagesText;
    }

    public void setSelected(int index) {
        mSelectedIndex = index;
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
                mSearching = false;
                results.values = mOriginalProjects;
                results.count = mOriginalProjects.size();
            } else {
                mSearching = true;
                // perform filter
                List<Project> filteredProjects = new ArrayList<>();
                for(Project p:mOriginalProjects) {
                    // match the project id
                    boolean match = p.getId().toLowerCase().startsWith(charSequence.toString().toLowerCase());
                    if(!match) {
                        for (SourceLanguage l : p.getSourceLanguages()) {
                            if (p.getTitle(l).toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                                // match the project title in any language
                                match = true;
                                break;
                            } else if (l.getId().toLowerCase().startsWith(charSequence.toString().toLowerCase()) || l.getName().toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                                // match the language id or name
                                match = true;
                                break;
                            }
                        }
                    }
                    if(match) {
                        filteredProjects.add(p);
                    }
                }
                results.values = filteredProjects;
                results.count = filteredProjects.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mProjects = (List<Project>)filterResults.values;
            notifyDataSetChanged();
        }
    }}
