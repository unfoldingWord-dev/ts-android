package com.door43.translationstudio.newui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;

import org.unfoldingword.gogsclient.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by joel on 11/6/2015.
 */
public class RestoreFromCloudAdapter extends BaseAdapter {

    private List<Repository> repositories = new ArrayList<>();
    private final Library library;

    public RestoreFromCloudAdapter() {
        this.library = App.getLibrary();
    }

    @Override
    public int getCount() {
        return this.repositories.size();
    }

    @Override
    public Repository getItem(int position) {
        return this.repositories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_restore_from_cloud_list_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        holder.setRepository(getItem(position));

        return v;
    }

    /**
     * Sets the data to display
     * @param repositories
     */
    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
        notifyDataSetChanged();
    }

    private class ViewHolder {
        private final TextView projectName;
        private final TextView targetLanguageName;
        private final TextView repoUrl;
        private final ImageView privacy;
        private Repository repository;

        public ViewHolder(View v) {
            projectName = (TextView)v.findViewById(R.id.project_name);
            targetLanguageName = (TextView)v.findViewById(R.id.target_language_name);
            privacy = (ImageView)v.findViewById(R.id.privacy);
            repoUrl = (TextView)v.findViewById(R.id.repository_url);
            v.setTag(this);
        }

        public void setRepository(Repository repository) {
            projectName.setText("");
            targetLanguageName.setText("");
            this.repository = repository;
            if(this.repository.getIsPrivate()) {
                this.privacy.setImageResource(R.drawable.ic_lock_black_18dp);
            } else {
                this.privacy.setImageResource(R.drawable.ic_lock_open_black_18dp);
            }
            repoUrl.setText(repository.getHtmlUrl());
            String[] repoName = this.repository.getFullName().split("/");
            if(repoName.length > 0) {
                String targetTranslationSlug = repoName[repoName.length - 1];
                try {
                    String projectSlug = TargetTranslation.getProjectSlugFromId(targetTranslationSlug);
                    String targetLanguageSlug = TargetTranslation.getTargetLanguageSlugFromId(targetTranslationSlug);

                    Project p = library.getProject(projectSlug, Locale.getDefault().getLanguage());
                    if (p != null) {
                        projectName.setText(p.name);
                    } else {
                        projectName.setText(targetTranslationSlug);
                    }
                    TargetLanguage tl = library.getTargetLanguage(targetLanguageSlug);
                    if (tl != null) {
                        targetLanguageName.setText(tl.name);
                    } else {
                        targetLanguageName.setText(targetLanguageSlug);
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    projectName.setText(targetTranslationSlug);
                }
            }
        }
    }
}
