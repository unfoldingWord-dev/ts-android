package com.door43.translationstudio.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.gogsclient.Repository;
import org.unfoldingword.resourcecontainer.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of translation repositories in the cloud
 */
public class TranslationRepositoryAdapter extends BaseAdapter {

    private List<Repository> repositories = new ArrayList<>();
    private Item[] items = new Item[0];
    private final Door43Client library;

    public TranslationRepositoryAdapter() {
        this.library = App.getLibrary();
    }

    @Override
    public int getCount() {
        return this.repositories.size();
    }

    /**
     * TRICKY: we don't actually use this
     * @param position the list position
     * @return the repository at this position
     */
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

        holder.setItem(loadItem(position));

        return v;
    }

    /**
     * Sets the data to display
     * @param repositories the list of repositories to be displayed
     */
    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
        this.items = new Item[repositories.size()];
        // preload items
        for(int i = 0; i < this.repositories.size(); i ++) {
            loadItem(i);
            // re-load the list every 5 repos
            if(i % 5 == 0) notifyDataSetChanged();
        }
    }

    /**
     * Loads the data in an item
     * @param position the position of the item in the list
     */
    private Item loadItem(int position) {
        if(position < 0 || position >= items.length) return null;

        // load new item
        if(items[position] == null) {
            Repository repo = this.repositories.get(position);
            String[] repoName = repo.getFullName().split("/");
            String projectName = "";
            String languageName = "";
            if (repoName.length > 0) {
                String targetTranslationSlug = repoName[repoName.length - 1];
                try {
                    String projectSlug = TargetTranslation.getProjectSlugFromId(targetTranslationSlug);
                    String targetLanguageSlug = TargetTranslation.getTargetLanguageSlugFromId(targetTranslationSlug);

                    Project p = library.index.getProject(App.getDeviceLanguageCode(), projectSlug, true);
                    if (p != null) {
                        projectName = p.name;
                    } else {
                        projectName = targetTranslationSlug;
                    }
                    TargetLanguage tl = library.index.getTargetLanguage(targetLanguageSlug);
                    if (tl != null) {
                        languageName = tl.name;
                    } else {
                        languageName = targetLanguageSlug;
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    projectName = targetTranslationSlug;
                }
            }
            items[position] = new Item(languageName, projectName, repo.getHtmlUrl(), repo.getIsPrivate());
        }
        return items[position];
    }

    private class ViewHolder {
        private final TextView projectName;
        private final TextView targetLanguageName;
        private final TextView repoUrl;
        private final ImageView privacy;

        public ViewHolder(View v) {
            projectName = (TextView)v.findViewById(R.id.project_name);
            targetLanguageName = (TextView)v.findViewById(R.id.target_language_name);
            privacy = (ImageView)v.findViewById(R.id.privacy);
            repoUrl = (TextView)v.findViewById(R.id.repository_url);
            v.setTag(this);
        }

        /**
         * Loads the item into the view
         * @param item the item to be displayed in this view
         */
        public void setItem(Item item) {
            targetLanguageName.setText(item.languageName);
            projectName.setText(item.projectName);
            if(item.isPrivate) {
                this.privacy.setImageResource(R.drawable.ic_lock_black_18dp);
            } else {
                this.privacy.setImageResource(R.drawable.ic_lock_open_black_18dp);
            }
            repoUrl.setText(item.url);
        }
    }

    /**
     * Represents the loaded data of a repository that will be displayed in the list
     */
    private class Item {
        private final String projectName;
        private final String languageName;
        private final String url;
        private final boolean isPrivate;

        public Item(String languageName, String projectName, String url, boolean isPrivate) {
            this.projectName = projectName;
            this.languageName = languageName;
            this.url = url;
            this.isPrivate = isPrivate;
        }
    }
}
