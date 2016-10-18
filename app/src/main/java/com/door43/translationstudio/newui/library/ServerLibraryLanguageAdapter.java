package com.door43.translationstudio.newui.library;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.tasks.DownloadSourceLanguageTask;

import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import org.unfoldingword.door43client.models.SourceLanguage;

/**
 * This adpater handles the display of source languages in the server library
 */
public class ServerLibraryLanguageAdapter extends BaseAdapter {
    private final Context mContext;
    private String mProjectId = null;
    private ListItem[] items;
    private LibraryUpdates mUpdates;

    public ServerLibraryLanguageAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        if(items == null) {
            return 0;
        } else {
            return items.length;
        }
    }

    @Override
    public ListItem getItem(int i) {
        return items[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        View v = view;
        ViewHolder holder;

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_server_library_languages_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        ListItem item = getItem(position);

        // disconnect from task
        DownloadSourceLanguageTask task = (DownloadSourceLanguageTask)TaskManager.getTask(mProjectId + "-" + item.sourceLanguage.slug);
        if(task != null) {
            task.removeOnFinishedListener(item.onFinishedListener);
            task.removeOnProgressListener(item.onProgressListener);
        }

        // icon
        boolean isDownloaded = false;//App.getLibrary().sourceLanguageHasSource(mProjectId, item.sourceLanguage.slug);
        if(isDownloaded) {
            holder.mStatus.setBackgroundResource(R.drawable.ic_bookmark_black_24dp);
        } else {
            holder.mStatus.setBackgroundResource(0);
        }

        // identify updates
        if(isDownloaded && mUpdates.hasSourceLanguageUpdate(mProjectId, item.sourceLanguage.slug)) {
            holder.mStatus.setBackgroundResource(R.drawable.ic_refresh_black_24dp);
        }

        // name
        holder.mName.setText(getItem(position).sourceLanguage.name);
        holder.mCode.setText(getItem(position).sourceLanguage.slug);
        Typography.format(mContext, Typography.TranslationType.SOURCE, holder.mName, getItem(position).sourceLanguage.slug, getItem(position).sourceLanguage.direction);

        // progress
        holder.mProgressBar.setVisibility(View.GONE);

        final ViewHolder staticHolder = holder;

        // progress listener
        item.onProgressListener = new ManagedTask.OnProgressListener() {
            @Override
            public void onTaskProgress(final ManagedTask task, final double progress, String message, boolean secondary) {
                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        staticHolder.mStatus.setBackgroundResource(R.drawable.ic_cloud_download_black_24dp);
                        staticHolder.mProgressBar.setVisibility(View.VISIBLE);
                        staticHolder.mProgressBar.setMax(task.maxProgress());
                        if (progress == -1) {
                            staticHolder.mProgressBar.setIndeterminate(true);
                            staticHolder.mProgressBar.setProgress(staticHolder.mProgressBar.getMax());
                        } else {
                            staticHolder.mProgressBar.setIndeterminate(false);
                            staticHolder.mProgressBar.setProgress((int)progress);
                        }
                    }
                });
            }
        };

        // finish listener
        item.onFinishedListener = new ManagedTask.OnFinishedListener() {
            @Override
            public void onTaskFinished(final ManagedTask task) {
                TaskManager.clearTask(task);
                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        staticHolder.mProgressBar.setVisibility(View.GONE);
                        if(((DownloadSourceLanguageTask)task).getSuccess()) {
                            staticHolder.mStatus.setBackgroundResource(R.drawable.ic_bookmark_black_24dp);
                        } else {
                            // TODO: the download failed notify the user with option to report bug
                            staticHolder.mStatus.setBackgroundResource(R.drawable.ic_warning_black_24dp);
                        }
                    }
                });
            }
        };

        // connect to task
        if(task != null) {
            task.addOnProgressListener(item.onProgressListener);
            task.addOnFinishedListener(item.onFinishedListener);
        }
        return v;
    }

    /**
     * Changes the dataset
     * @param languages
     */
    public void changeDataSet(String projectId, LibraryUpdates updates, List<SourceLanguage> languages) {
        mProjectId = projectId;
        sortSourceLanguages(languages, "");
        mUpdates = updates;
        items = new ListItem[languages.size()];
        for(int i = 0; i < languages.size(); i ++) {
            items[i] = new ListItem(languages.get(i));
        }
        notifyDataSetChanged();
    }

    /**
     * Sorts source languages by id
     * @param languages
     * @param referenceId languages are sorted according to the reference id
     */
    private static void sortSourceLanguages(List<SourceLanguage> languages, final CharSequence referenceId) {
        Collections.sort(languages, new Comparator<SourceLanguage>() {
            @Override
            public int compare(SourceLanguage lhs, SourceLanguage rhs) {
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

    private class ViewHolder {
        private final TextView mCode;
        private final ImageView mStatus;
        public TextView mName;
        public ProgressBar mProgressBar;

        public ViewHolder(View v) {
            mName = (TextView)v.findViewById(R.id.language_name);
            mCode = (TextView)v.findViewById(R.id.language_code);
            mStatus = (ImageView)v.findViewById(R.id.status_icon);
            mProgressBar = (ProgressBar)v.findViewById(R.id.progress_bar);
            mProgressBar.setMax(100);
            v.setTag(this);
        }
    }

    public class ListItem {
        public SourceLanguage sourceLanguage;
        public ManagedTask.OnProgressListener onProgressListener;
        public ManagedTask.OnFinishedListener onFinishedListener;

        public ListItem(SourceLanguage sourceLanguage) {
            this.sourceLanguage = sourceLanguage;
        }
    }
}
