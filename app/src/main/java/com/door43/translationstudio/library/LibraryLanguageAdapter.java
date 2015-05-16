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
import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.tasks.DownloadLanguageTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.util.tasks.ThreadableUI;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * This adpater handles the display of source languages in the server library
 */
public class LibraryLanguageAdapter extends BaseAdapter {
    private final Context mContext;
    private final String mProjectId;
    private final String mTaskIdPrefix;
    private final boolean mIsDrafts;
    private List<SourceLanguage> mLanguages = new ArrayList<>();

    public LibraryLanguageAdapter(Context context, String projectId, String taskIdPrefix, boolean isDrafts) {
        mProjectId = projectId;
        mContext = context;
        mTaskIdPrefix = taskIdPrefix;
        mIsDrafts = isDrafts;
    }

    @Override
    public int getCount() {
        return mLanguages.size();
    }

    @Override
    public SourceLanguage getItem(int i) {
        return mLanguages.get(i);
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

        holder.name.setText(getItem(i).getName());
        holder.downloadedImage.setVisibility(View.INVISIBLE);

        if(LibraryTempData.getEnableEditing()) {
            holder.deleteImage.setVisibility(View.VISIBLE);
        } else {
            holder.deleteImage.setVisibility(View.GONE);
        }

        // set graphite fontface
        if(!holder.hasFont) {
            holder.hasFont = true;
            Typeface typeface = AppContext.graphiteTypeface(getItem(i));
            holder.name.setTypeface(typeface, 0);
        }

        // set font size
        float fontsize = AppContext.typefaceSize();
        holder.name.setTextSize(fontsize);

        final Handler hand = new Handler(Looper.getMainLooper());
        final ViewHolder staticHolder = holder;

        holder.progressBar.setVisibility(View.INVISIBLE);
        holder.progressBar.setProgress(0);

        // connect download task
        if(holder.downloadTask != null) {
            holder.downloadTask.destroy();
        }
        holder.downloadTask = (DownloadLanguageTask)TaskManager.getTask(mTaskIdPrefix+mProjectId+"-"+getItem(i).getId());
        if(holder.downloadTask != null) {
            holder.downloadedImage.setVisibility(View.VISIBLE);
            holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_download_small));
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
                            if (LibraryTempData.getEnableEditing()) {
                                staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small_disabled));
                            } else {
                                staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small));
                            }
                            staticHolder.downloadedImage.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        } else {
            boolean isDownloaded;
            if(mIsDrafts) {
                isDownloaded = AppContext.projectManager().isSourceLanguageDraftDownloaded(mProjectId, getItem(i).getId());
            } else {
                isDownloaded = AppContext.projectManager().isSourceLanguageDownloaded(mProjectId, getItem(i).getId());
            }
            if(isDownloaded) {
                boolean hasUpdate;
                if(mIsDrafts) {
                    hasUpdate = AppContext.projectManager().isSourceLanguageDraftUpdateAvailable(mProjectId, getItem(i));
                } else {
                    hasUpdate = AppContext.projectManager().isSourceLanguageUpdateAvailable(mProjectId, getItem(i));
                }
                if(hasUpdate) {
                    if(LibraryTempData.getEnableEditing()) {
                        staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_update_small_disabled));
                    } else {
                        staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_update_small));
                    }
                } else {
                    if(LibraryTempData.getEnableEditing()) {
                        staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small_disabled));
                    } else {
                        staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small));
                    }
                }
                staticHolder.downloadedImage.setVisibility(View.VISIBLE);
            } else {
                staticHolder.downloadedImage.setVisibility(View.INVISIBLE);
            }
        }
        return v;
    }

    /**
     * Changes the dataset
     * @param languages
     */
    public void changeDataSet(List<SourceLanguage> languages) {
        mLanguages = languages;

        sortAndFilter();
    }

    private void sortAndFilter() {
        new ThreadableUI(mContext) {

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                List<SourceLanguage> tempList = new ArrayList<>();

                // sort alphabetically
                ListIterator<SourceLanguage> li = mLanguages.listIterator();
                while(li.hasNext()) {
                    SourceLanguage l = li.next();
                    tempList.add(l);
                }
                mLanguages = tempList;
            }

            @Override
            public void onPostExecute() {
                LibraryLanguageAdapter.this.notifyDataSetChanged();
            }
        }.start();
    }

    private class ViewHolder {
        public TextView name;
        public DownloadLanguageTask downloadTask;
        public ProgressBar progressBar;
        public ImageView downloadedImage;
        public ImageView deleteImage;
        public boolean hasFont;
    }
}
