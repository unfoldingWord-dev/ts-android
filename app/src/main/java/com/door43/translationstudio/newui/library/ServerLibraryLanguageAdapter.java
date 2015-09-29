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

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.tasks.DownloadSourceLanguageTask;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This adpater handles the display of source languages in the server library
 */
public class ServerLibraryLanguageAdapter extends BaseAdapter {
    private final Context mContext;
    private String mProjectId = null;
    private SourceLanguage[] mLanguages;
    private LibraryUpdates mUpdates;

    public ServerLibraryLanguageAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        if(mLanguages == null) {
            return 0;
        } else {
            return mLanguages.length;
        }
    }

    @Override
    public SourceLanguage getItem(int i) {
        return mLanguages[i];
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

        SourceLanguage sourceLanguage = getItem(position);

        // icon
        boolean isDownloaded = AppContext.getLibrary().sourceLanguageHasSource(mProjectId, sourceLanguage.getId());
        if(isDownloaded) {
            holder.mStatus.setBackgroundResource(R.drawable.ic_bookmark_black_24dp);
        } else {
            holder.mStatus.setBackgroundResource(0);
        }

        // identify updates
        if(isDownloaded && mUpdates.hasSourceLanguageUpdate(mProjectId, sourceLanguage.getId())) {
            holder.mStatus.setBackgroundResource(R.drawable.ic_refresh_black_24dp);
        }

        // name
        holder.mName.setText(getItem(position).name);
        holder.mCode.setText(getItem(position).code);
        Typography.format(mContext, holder.mName, getItem(position).getId(), getItem(position).direction);

        // progress
        holder.mProgressBar.setVisibility(View.GONE);

        final ViewHolder staticHolder = holder;

        // progress listener
        ManagedTask.OnProgressListener progressListener = new ManagedTask.OnProgressListener() {
            @Override
            public void onProgress(final ManagedTask task, final double progress, String message, boolean secondary) {
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
        ManagedTask.OnFinishedListener finishedListener = new ManagedTask.OnFinishedListener() {
            @Override
            public void onFinished(final ManagedTask task) {
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

        // connect to tasks
        List<ManagedTask> tasks = TaskManager.getGroupedTasks(ServerLibraryDetailFragment.DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP);
        for(ManagedTask task:tasks) {
            DownloadSourceLanguageTask t = (DownloadSourceLanguageTask)task;
            if(t.getProjectId().equals(mProjectId) && t.getSourceLanguageId().equals(sourceLanguage.getId())) {
                holder.mProgressBar.setVisibility(View.VISIBLE);
                holder.mProgressBar.setIndeterminate(true);
                t.addOnProgressListener(progressListener);
                t.addOnFinishedListener(finishedListener);
            }
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
        mLanguages = languages.toArray(new SourceLanguage[languages.size()]);
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
                String lhId = lhs.getId();
                String rhId = rhs.getId();
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
}
