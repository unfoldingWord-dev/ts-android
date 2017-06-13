package com.door43.translationstudio.ui.newtranslation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.unfoldingword.door43client.models.CategoryEntry;

/**
 * Created by joel on 9/4/2015.
 */
public class ProjectCategoryAdapter extends BaseAdapter {
    private CategoryEntry[] mCategories;
    private CategoryEntry[] mFilteredCategories;
    private ProjectCategoryFilter mProjectFilter;

    public ProjectCategoryAdapter(List<CategoryEntry> categories) {
        mCategories = categories.toArray(new CategoryEntry[categories.size()]);
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
    public CategoryEntry getItem(int position) {
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
        holder.mProjectView.setText(getItem(position).name);
        if(getItem(position).entryType == CategoryEntry.Type.PROJECT) {
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
    public void changeData(List<CategoryEntry> categories) {
        mCategories = categories.toArray(new CategoryEntry[categories.size()]);
        mFilteredCategories = mCategories;
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
                List<CategoryEntry> filteredCategories = new ArrayList<>();
                for(CategoryEntry category:mCategories) {
                    boolean match = false;
                    if(category.entryType == CategoryEntry.Type.PROJECT) {
                        // match the project id
                        match = category.slug.toLowerCase().startsWith(charSequence.toString().toLowerCase());
                    }
                    if(!match) {
                        String[] categoryComponents = category.slug.split("-");
                        String[] titleComponents = category.name.split(" ");
                        if (category.name.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match the project title in any language
                            match = true;
                        } else if (category.sourceLanguageSlug.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {// || l.getName().toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                            // match the language id or name
                            match = true;
                        } else {
                            // match category id components
                            for(String component:categoryComponents) {
                                if (component.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                                    match = true;
                                    break;
                                }
                            }
                            if(!match) {
                                // match title components
                                for(String component:titleComponents) {
                                    if (component.toLowerCase().startsWith(charSequence.toString().toLowerCase())) {
                                        match = true;
                                        break;
                                    }
                                }
                            }
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
            List<CategoryEntry> filteredProjects = ((List<CategoryEntry>) filterResults.values);
            if(filteredProjects != null) {
                mFilteredCategories = filteredProjects.toArray(new CategoryEntry[filterResults.count]);
            } else {
                mFilteredCategories = new CategoryEntry[0];
            }
            notifyDataSetChanged();
        }
    }

    /**
     * Sorts project categories by id
     * @param categories
     * @param referenceId categories are sorted according to the reference id
     */
    private static void sortProjectCategories(List<CategoryEntry> categories, final CharSequence referenceId) {
        Collections.sort(categories, new Comparator<CategoryEntry>() {
            @Override
            public int compare(CategoryEntry lhs, CategoryEntry rhs) {
                String lhId = lhs.slug;
                String rhId = rhs.slug;
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
