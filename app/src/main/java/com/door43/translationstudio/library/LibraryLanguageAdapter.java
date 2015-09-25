package com.door43.translationstudio.library;

import android.content.Context;
import android.graphics.Typeface;
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
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.library.temp.ServerLibraryCache;
import com.door43.translationstudio.tasks.DownloadLanguageTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This adpater handles the display of source languages in the server library
 */
public class LibraryLanguageAdapter extends BaseAdapter {
    private final Context mContext;
    private String mProjectId = null;
    private final String mTaskIdPrefix;
    private final boolean mDrafts;
    private SourceLanguage[] mLanguages;

    public LibraryLanguageAdapter(Context context, String taskIdPrefix, boolean drafts) {
        mContext = context;
        mTaskIdPrefix = taskIdPrefix;
        mDrafts = drafts;
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
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = view;
        ViewHolder holder = new ViewHolder();

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_project_library_languages_item, null);
            // layout
            holder.name = (TextView)v.findViewById(R.id.languageNameTextView);
            holder.progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
            holder.progressBar.setMax(100);
            holder.downloadedImage = (ImageView)v.findViewById(R.id.downloadedImageView);
            holder.deleteImage = (ImageView)v.findViewById(R.id.deleteImageView);

            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.name.setText(getItem(i).name);
        holder.downloadedImage.setVisibility(View.INVISIBLE);

        if(ServerLibraryCache.getEnableEditing()) {
            holder.deleteImage.setVisibility(View.VISIBLE);
        } else {
            holder.deleteImage.setVisibility(View.GONE);
        }

        Typography.format(mContext, holder.name, getItem(i).getId(), getItem(i).direction);

        final Handler hand = new Handler(Looper.getMainLooper());
        final ViewHolder staticHolder = holder;

        holder.progressBar.setVisibility(View.INVISIBLE);
        holder.progressBar.setProgress(0);

        // TODO: hook up task
        // connect download task
        if(holder.downloadTask != null) {
            holder.downloadTask.destroy();
        }
        holder.downloadTask = (DownloadLanguageTask)TaskManager.getTask(mTaskIdPrefix+mProjectId+"-"+getItem(i).getId());
        if(holder.downloadTask != null) {
            holder.downloadedImage.setVisibility(View.VISIBLE);
            holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.icon_update_cloud_blue));
            holder.downloadTask.addOnProgressListener(new ManagedTask.OnProgressListener() {
                @Override
                public void onProgress(ManagedTask task, final double progress, String message) {
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            staticHolder.progressBar.setVisibility(View.VISIBLE);
                            if (progress == -1) {
                                staticHolder.progressBar.setIndeterminate(true);
                                staticHolder.progressBar.setProgress(staticHolder.progressBar.getMax());
                            } else {
                                staticHolder.progressBar.setIndeterminate(false);
                                staticHolder.progressBar.setProgress((int) (100 * progress));
                            }
                        }
                    });
                }
            });
            holder.downloadTask.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
                @Override
                public void onFinished(ManagedTask task) {
                    TaskManager.clearTask(task.getTaskId());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            staticHolder.progressBar.setVisibility(View.INVISIBLE);
                            if (ServerLibraryCache.getEnableEditing()) {
                                staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_done_black_24dp));
                            } else {
                                staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_done_black_24dp));
                            }
                            staticHolder.downloadedImage.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        } else {
            // TODO: hook thisup
//            boolean isDownloaded;
//            if(mDrafts) {
//                isDownloaded = AppContext.projectManager().isSourceLanguageDraftDownloaded(mProjectId, getItem(i).getId());
//            } else {
//                isDownloaded = AppContext.projectManager().isSourceLanguageDownloaded(mProjectId, getItem(i).getId());
//            }
//            if(isDownloaded) {
//                boolean hasUpdate;
//                if(mDrafts) {
//                    hasUpdate = AppContext.projectManager().isSourceLanguageDraftUpdateAvailable(mProjectId, getItem(i));
//                } else {
//                    hasUpdate = AppContext.projectManager().isSourceLanguageUpdateAvailable(mProjectId, getItem(i));
//                }
//                if(hasUpdate) {
//                    if(ServerLibraryCache.getEnableEditing()) {
//                        staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.icon_update_cloud_blue));
//                    } else {
//                        staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.icon_update_cloud_blue));
//                    }
//                } else {
//                    if(ServerLibraryCache.getEnableEditing()) {
//                        staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_done_black_24dp));
//                    } else {
//                        staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_done_black_24dp));
//                    }
//                }
//                staticHolder.downloadedImage.setVisibility(View.VISIBLE);
//            } else {
//                staticHolder.downloadedImage.setVisibility(View.INVISIBLE);
//            }
        }
        return v;
    }

    /**
     * Changes the dataset
     * @param languages
     */
    public void changeDataSet(List<SourceLanguage> languages) {
        sortSourceLanguages(languages, "");
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

    public void setProjectId(String projectId) {
        mProjectId = projectId;
    }

    private class ViewHolder {
        public TextView name;
        public DownloadLanguageTask downloadTask;
        public ProgressBar progressBar;
        public ImageView downloadedImage;
        public ImageView deleteImage;
    }
}
