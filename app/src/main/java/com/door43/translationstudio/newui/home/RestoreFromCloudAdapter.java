package com.door43.translationstudio.newui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;

import java.util.Locale;

/**
 * Created by joel on 11/6/2015.
 */
public class RestoreFromCloudAdapter extends BaseAdapter {

    private String[] targetTranslationSlugs = new String[0];
    private final Library library;

    public RestoreFromCloudAdapter() {
        this.library = AppContext.getLibrary();
    }

    @Override
    public int getCount() {
        return this.targetTranslationSlugs.length;
    }

    @Override
    public String getItem(int position) {
        return this.targetTranslationSlugs[position];
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
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_restore_from_cloud_list_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.projectName.setText(getItem(position));
        holder.targetLanguageName.setText("");
        try {
            String projectSlug = TargetTranslation.getProjectSlugFromId(getItem(position));
            String targetLanguageSlug = TargetTranslation.getTargetLanguageSlugFromId(getItem(position));

            Project p = library.getProject(projectSlug, Locale.getDefault().getLanguage());
            if(p != null) {
                holder.projectName.setText(p.name);
            }
            TargetLanguage tl = library.getTargetLanguage(targetLanguageSlug);
            if(tl != null) {
                holder.targetLanguageName.setText(tl.name);
            }
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return v;
    }

    /**
     * Sets the data to display
     * @param targetTranslationSlugs
     */
    public void setTargetTranslations(String[] targetTranslationSlugs) {
        this.targetTranslationSlugs = targetTranslationSlugs;
        notifyDataSetChanged();
    }

    private class ViewHolder {
        private final TextView projectName;
        private final TextView targetLanguageName;

        public ViewHolder(View v) {
            projectName = (TextView)v.findViewById(R.id.project_name);
            targetLanguageName = (TextView)v.findViewById(R.id.target_language_name);
            v.setTag(this);
        }
    }
}
