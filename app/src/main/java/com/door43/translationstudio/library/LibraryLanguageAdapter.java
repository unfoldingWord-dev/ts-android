package com.door43.translationstudio.library;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.tasks.DownloadLanguageTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;
import com.door43.util.threads.ThreadableUI;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.xml.transform.Source;

/**
 * This adpater handles the display of source languages in the server library
 */
public class LibraryLanguageAdapter extends BaseAdapter implements DownloadLanguageTask.OnProgressListener, ManagedTask.OnFinishedListener {
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

            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.name.setText(getItem(i).getName());
        // TODO: indicate if the language has been downloaded yet.

        // set graphite fontface
        Typeface typeface = AppContext.graphiteTypeface(getItem(i));
        holder.name.setTypeface(typeface, 0);

        // set font size
        float fontsize = AppContext.typefaceSize();
        holder.name.setTextSize(fontsize);

        // connect download task
        if(holder.downloadTask != null) {
            holder.downloadTask.setOnProgressListener(null);
            holder.downloadTask.setOnFinishedListener(null);
        }
        holder.downloadTask = (DownloadLanguageTask)TaskManager.getTask(LanguagesTabFragment.DOWNLOAD_LANGUAGE_PREFIX+mProjectId+"-"+getItem(i).getId());
        if(holder.downloadTask != null) {
            holder.downloadTask.setOnProgressListener(this);
            holder.downloadTask.setOnFinishedListener(this);
        } else {
            // TODO: check the download status of this language and display a notice accordingly
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

    @Override
    public void onProgress(double progress, String message) {
        // TODO: update the progress display
        Log.d("test", progress+"");
    }

    @Override
    public void onFinished(ManagedTask task) {
        // TODO: update the download status
        Log.d("test", "download is finished");
    }

    private class ViewHolder {
        public TextView name;
        public DownloadLanguageTask downloadTask;
    }
}
