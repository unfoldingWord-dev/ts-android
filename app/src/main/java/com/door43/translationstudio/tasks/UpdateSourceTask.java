package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;

import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Indexes all of the source meta from the api
 */
public class UpdateSourceTask extends ManagedTask {
    public static final String TASK_ID = "update-source-task";
    private int maxProgress = 0;
    private boolean success = false;

    @Override
    public void start() {
        publishProgress(-1, "");
        try {
            String server = App.getPref(SettingsActivity.KEY_PREF_MEDIA_SERVER, App.getRes(R.string.pref_default_media_server));
            String rootApiUrl = server + App.getRes(R.string.root_catalog_api);
            App.getLibrary().updateSources(rootApiUrl, new org.unfoldingword.door43client.OnProgressListener() {
                @Override
                public void onProgress(String tag, long max, long complete) {
                    maxProgress = (int)max;
                    publishProgress((float)complete/(float)max, tag);

                    // TODO: 11/14/16 - this is a hack to interrupt download by throwing an exception.  Need a cleaner way to interrupt Door43Client
                    if(UpdateSourceTask.this.isCanceled()) {
                        int n = 1/0; // generate exception
                    }
                }
            });
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int maxProgress() {
        return maxProgress;
    }

    public boolean isSuccess() {
        return success;
    }
}
