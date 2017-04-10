package com.door43.translationstudio.ui.home;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Typography;

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
    private boolean textOnlyResources = false; // if set then anything not a text resource is disabled

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

    /**
     * determine if item at position is supported project type
     * @param position
     * @return
     */
    public boolean isSupported(int position) {
        Item item = loadItem(position);
        if(item != null) {
            return item.isSupported();
        }
        return false;
    }

    /**
     * get project name for item at position
     * @param position
     * @return
     */
    public String getProjectName(int position) {
        Item item = loadItem(position);
        if(item != null) {
            String projectName = item.projectName;
            if(!projectName.equalsIgnoreCase(item.targetTranslationSlug)) {
                projectName += " (" + item.targetTranslationSlug + ")"; // if not same as project name, add project id
            }
            return projectName;
        }
        return "";
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

        holder.setItem(loadItem(position), parent.getContext());

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
        notifyDataSetChanged(); // make sure we trigger redraw even if there are no items in list
    }


    public void setTextOnlyResources(boolean textOnlyResources) {
        this.textOnlyResources = textOnlyResources;
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
            String code = "en"; // default font language if language is not found
            String direction = "ltor"; // default font language direction if language is not found
            int notSupportedID = 0;
            String targetTranslationSlug = "";
            if (repoName.length > 0) {
                targetTranslationSlug = repoName[repoName.length - 1];
                try {
                    String projectSlug = TargetTranslation.getProjectSlugFromId(targetTranslationSlug);
                    String targetLanguageSlug = TargetTranslation.getTargetLanguageSlugFromId(targetTranslationSlug);

                    if(textOnlyResources) {
                        String resourceTypeSlug = TargetTranslation.getResourceTypeFromId(targetTranslationSlug);
                        if (!"text".equals(resourceTypeSlug)) { // we only support text
                            if ("tw".equals(resourceTypeSlug)) {
                                notSupportedID = R.string.translation_words;
                            } else if ("tn".equals(resourceTypeSlug)) {
                                notSupportedID = R.string.label_translation_notes;
                            } else if ("tq".equals(resourceTypeSlug)) {
                                notSupportedID = R.string.translation_questions;
                            } else if ("ta".equals(resourceTypeSlug)) {
                                notSupportedID = R.string.translation_academy;
                            } else {
                                notSupportedID = R.string.unsupported;
                            }
                        }
                    }

                    Project p = library.index.getProject(App.getDeviceLanguageCode(), projectSlug, true);
                    if (p != null) {
                        projectName = p.name;
                    } else {
                        projectName = targetTranslationSlug;
                    }
                    TargetLanguage tl = library.index.getTargetLanguage(targetLanguageSlug);
                    if (tl != null) {
                        languageName = tl.name;
                        direction = tl.direction;
                        code = tl.slug;
                    } else {
                        languageName = targetLanguageSlug;
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    projectName = targetTranslationSlug;
                    notSupportedID = R.string.unsupported;
                }
            }
            items[position] = new Item(languageName, projectName, targetTranslationSlug, code, direction, repo.getHtmlUrl(), repo.getIsPrivate(), notSupportedID);
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
        public void setItem(Item item, Context context) {
            targetLanguageName.setText(item.languageName);

            // set default typeface for language
            Typeface typeface = Typography.getBestFontForLanguage(context, TranslationType.SOURCE, item.languageCode, item.languageDirection);
            targetLanguageName.setTypeface(typeface, 0);

            if(item.isPrivate) {
                this.privacy.setImageResource(R.drawable.ic_lock_black_18dp);
            } else {
                this.privacy.setImageResource(R.drawable.ic_lock_open_black_18dp);
            }
            String projectNameStr = item.projectName;
            if(item.isSupported()) {
                projectName.setTypeface(null, Typeface.BOLD);
                projectName.setTextColor(context.getResources().getColor(R.color.dark_primary_text));
            } else {
                projectName.setTypeface(null, Typeface.NORMAL);
                projectName.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
                projectNameStr += " - " + context.getString(item.notSupportedId);
            }
            projectName.setText(projectNameStr);
            repoUrl.setText(item.url);
        }
    }

    /**
     * Represents the loaded data of a repository that will be displayed in the list
     */
    private class Item {
        private final String projectName;
        private final String targetTranslationSlug;
        private final String languageName;
        private final String languageCode;
        private final String languageDirection;
        private final String url;
        private final boolean isPrivate;
        private final int notSupportedId; // a project type that ts-android cannot import will have a non-zero resource ID here

        public Item(String languageName, String projectName, String targetTranslationSlug, String languageCode, String languageDirection, String url, boolean isPrivate, int notSupportedId) {
            this.projectName = projectName;
            this.languageName = languageName;
            this.targetTranslationSlug = targetTranslationSlug;
            this.url = url;
            this.isPrivate = isPrivate;
            this.notSupportedId = notSupportedId;
            this.languageCode = languageCode;
            this.languageDirection = languageDirection;
        }

        public boolean isSupported() {
            return (notSupportedId == 0);
        }
    }
}
