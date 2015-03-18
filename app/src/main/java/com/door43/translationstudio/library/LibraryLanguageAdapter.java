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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.tasks.DownloadLanguageTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;
import com.door43.util.threads.ThreadableUI;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * This adpater handles the display of source languages in the server library
 */
public class LibraryLanguageAdapter extends BaseAdapter {
    private final Context mContext;
    private final String mProjectId;
    private List<SourceLanguage> mLanguages = new ArrayList<>();

    public LibraryLanguageAdapter(Context context, String projectId) {
        mProjectId = projectId;
        mContext = context;
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

            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.name.setText(getItem(i).getName());
        holder.downloadedImage.setVisibility(View.INVISIBLE);

        // set graphite fontface
        Typeface typeface = AppContext.graphiteTypeface(getItem(i));
        holder.name.setTypeface(typeface, 0);

        // set font size
        float fontsize = AppContext.typefaceSize();
        holder.name.setTextSize(fontsize);

        final Handler hand = new Handler(Looper.getMainLooper());
        final ViewHolder staticHolder = holder;

        holder.progressBar.setVisibility(View.INVISIBLE);
        holder.progressBar.setProgress(0);

        // connect download task
        if(holder.downloadTask != null) {
            holder.downloadTask.setOnProgressListener(null);
            holder.downloadTask.setOnFinishedListener(null);
        }
        holder.downloadTask = (DownloadLanguageTask)TaskManager.getTask(LanguagesTabFragment.DOWNLOAD_LANGUAGE_PREFIX+mProjectId+"-"+getItem(i).getId());
        if(holder.downloadTask != null) {
            holder.downloadedImage.setVisibility(View.VISIBLE);
            holder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_download_small));
            holder.downloadTask.setOnProgressListener(new DownloadLanguageTask.OnProgressListener() {
                @Override
                public void onProgress(final double progress, String message) {
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            staticHolder.progressBar.setVisibility(View.VISIBLE);
                            if(progress == -1) {
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
            holder.downloadTask.setOnFinishedListener(new ManagedTask.OnFinishedListener() {
                @Override
                public void onFinished(ManagedTask task) {
                    TaskManager.clearTask((String)task.getTaskId());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            staticHolder.progressBar.setVisibility(View.GONE);
                            staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small));
                            staticHolder.downloadedImage.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        } else {
            // check if this language has been downloaded
            if(AppContext.projectManager().isSourceLanguageDownloaded(mProjectId, getItem(i).getId())) {
                // check if an update for this language exists
                if(AppContext.projectManager().isSourceLanguageUpdateAvailable(mProjectId, getItem(i))) {
                    staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_update_small));
                } else {
                    staticHolder.downloadedImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_check_small));
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
                    // TODO: filter out by checking level
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
    }
}
