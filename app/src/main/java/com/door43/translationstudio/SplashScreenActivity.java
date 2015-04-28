package com.door43.translationstudio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.migration.UpdateManager;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.tasks.IndexProjectsTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.door43.util.threads.TaskManager;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by joel on 9/29/2014.
 */
public class SplashScreenActivity extends TranslatorBaseActivity {
    private TextView mProgressTextView;
    private ProgressBar mProgressBar;
    private static LoadAppTask mLoadingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mProgressTextView = (TextView)findViewById(R.id.loadingText);
        mProgressBar = (ProgressBar)findViewById(R.id.loadingBar);
        // we need a high precision bar because we are loading a ton of things
        mProgressBar.setMax(10000);
        mProgressBar.setIndeterminate(true);
        if(savedInstanceState != null) {
            mProgressBar.setProgress(savedInstanceState.getInt("progress"));
            mProgressTextView.setText(savedInstanceState.getString("message"));
        } else {
            mProgressBar.setProgress(0);
        }

        // check if we crashed
        File dir = new File(getExternalCacheDir(), app().STACKTRACE_DIR);
        if (dir.exists()) {
            String[] files = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return !new File(file, s).isDirectory();
                }
            });
            if (files.length > 0) {
                Intent intent = new Intent(this, CrashReporterActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }

        // these checks allow the app to rotate without restarting the loading task
        if(mLoadingTask == null && !AppContext.projectManager().isLoaded()) {
            mLoadingTask = new LoadAppTask();
            mLoadingTask.execute();
        } else if(mLoadingTask == null && AppContext.projectManager().isLoaded()) {
            // go ahead and start the main activity if the project manager has been loaded.
            startMainActivity();
        }
    }

    /**
     * Starts the main activity
     */
    public void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        mLoadingTask = null;
        finish();
    }

    /**
     * TODO: this needs to be migrated over to the new task manager
     */
    private class LoadAppTask extends AsyncTask<Void, String, Void> {
        private int mProgress = 0;

        @Override
        protected Void doInBackground(Void... voids) {

            // begin loading app resources
            AppContext.projectManager().init(new ProjectManager.OnProgressCallback() {
                @Override
                public void onProgress(double progress, String message) {
                    mProgress = (int)(progress * 100); // project manager returns 100 based percent values not 10,000
                    publishProgress(message);
                }

                @Override
                public void onSuccess() {
                    // Generate the ssh keys
                    if(!AppContext.context().hasKeys()) {
                        AppContext.context().generateKeys();
                    }

                    // handle app version changes
                    SharedPreferences settings = getSharedPreferences(MainApplication.PREFERENCES_TAG, MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    int lastVersionCode = settings.getInt("last_version_code", 0);
                    PackageInfo pInfo = null;
                    try {
                        pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        editor.putInt("last_version_code", pInfo.versionCode);
                        editor.apply();
                        if(pInfo.versionCode > lastVersionCode) {
                            // update!
                            mProgress = 0;
                            publishProgress("Performing updates");
                            UpdateManager updater = new UpdateManager(lastVersionCode, pInfo.versionCode);
                            updater.run(new UpdateManager.OnProgressCallback() {
                                @Override
                                public void onProgress(double progress, String message) {
                                    mProgress = (int)(progress * 100); // update manager returns 100 based percent values not 10,000
                                    publishProgress(message);
                                }

                                @Override
                                public void onSuccess() {
                                    // load after the migration
                                    loadSelectedProject();
                                }

                                @Override
                                public void onError(String message) {
                                    // TODO: display an error message to the user.
                                }
                            });
                        } else {
                            // load the app by default
                            loadSelectedProject();
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // just load the app by default
                        loadSelectedProject();
                    }
                }
            });

            // index all the projects
            IndexProjectsTask task = new IndexProjectsTask(AppContext.projectManager().getProjects());
            TaskManager.addTask(task, IndexProjectsTask.TASK_ID);
            return null;
        }

        protected void onProgressUpdate(String... items) {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(mProgress);
            mProgressTextView.setText(items[0]);
        }

        protected void onPostExecute(Void item) {
            mProgressBar.setProgress(mProgressBar.getMax());
            mProgressBar.setIndeterminate(true);
            mProgressTextView.setText(getResources().getString(R.string.launching_translator));
            startMainActivity();
        }

        private void loadSelectedProject() {
            // load previously viewed frame
            publishProgress(getResources().getString(R.string.loading_preferences));
            if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {

                if(AppContext.projectManager().getSelectedProject() != null) {
                    // load the saved project without displaying a notice to the user
                    AppContext.projectManager().fetchProjectSource(AppContext.projectManager().getSelectedProject(), false);
                }
            } else {
                // load the default project without display a notice to the user
                if(AppContext.projectManager().getSelectedProject() != null) {
                    AppContext.projectManager().fetchProjectSource(AppContext.projectManager().getSelectedProject(), false);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("message", mProgressTextView.getText().toString());
        outState.putInt("progress", mProgressBar.getProgress());
        super.onSaveInstanceState(outState);
    }
}
