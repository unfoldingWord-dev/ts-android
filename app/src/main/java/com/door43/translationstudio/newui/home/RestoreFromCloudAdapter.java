package com.door43.translationstudio.newui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.NewLanguagePackage;
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

        String targetTranslationID = getItem(position);
        holder.projectName.setText(targetTranslationID);
        holder.targetLanguageName.setText("");
        try {
            String projectSlug = TargetTranslation.getProjectIdFromId(targetTranslationID);
            String targetLanguageSlug = TargetTranslation.getTargetLanguageIdFromId(targetTranslationID);

            Project p = library.getProject(projectSlug, Locale.getDefault().getLanguage());
            if(p != null) {
                holder.projectName.setText(p.name);
            }
            TargetLanguage tl = library.getTargetLanguage(targetLanguageSlug);
            if(tl != null) {
                holder.targetLanguageName.setText(tl.name);
            } else if(NewLanguagePackage.isNewLanguageCode(targetLanguageSlug)) { // see if temp language
                holder.targetLanguageName.setText(targetLanguageSlug); // default to just show temp language code

                String name = lookupNewLanguageName(targetLanguageSlug); // see if language is already loaded and get name
                if(name != null) {
                    holder.targetLanguageName.setText(name + "-" + targetLanguageSlug);
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return v;
    }

    private String lookupNewLanguageName(String targetLanguageID) {
        NewLanguagePackage newLang = NewLanguagePackage.getNewLanguageFromFileSystem(targetLanguageID);
        if(newLang != null) {
            return newLang.languageName;
        }

        return null;
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
