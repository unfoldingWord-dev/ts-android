package com.door43.translationstudio;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import org.w3c.dom.Text;

/**
 * Created by joel on 9/29/2014.
 */
public class SplashScreenActivity extends TranslatorBaseActivity {
    private SplashScreenActivity me = this;
    private TextView mProgressTextView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mProgressTextView = (TextView)findViewById(R.id.progressTextView);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        // we need a high precision bar because we are loading a ton of things
        mProgressBar.setMax(10000);
        mProgressBar.setProgress(0);

        LoadAppTask task = new LoadAppTask();
        task.execute();

    }

    public void onResume() {
        super.onResume();
    }

    public void startMainActivity() {
        Intent splashIntent = new Intent(this, MainActivity.class);
        startActivity(splashIntent);
        finish();
    }

    private class LoadAppTask extends AsyncTask<Void, String, Void> {
        private int mProgress = 0;

        @Override
        protected Void doInBackground(Void... voids) {
            app().getSharedProjectManager().init(new ProjectManager.OnProgressCallback() {
                @Override
                public void onProgress(double progress, String message) {
                    mProgress = (int)(progress * 100); // project manager returns 100 based percent values not 10,000
                    publishProgress(message);
                }

                @Override
                public void finished() {
                    // Generate the ssh keys
                    if(!app().hasKeys()) {
                        // this is so short we don't update the progress bar
                        publishProgress("generating security keys");
                        app().generateKeys();
                    }

                    // load previously viewed frame
                    publishProgress("loading preferences");
                    if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {
                        String frameId = app().getLastActiveFrame();
                        String chapterId = app().getLastActiveChapter();
                        String projectSlug = app().getLastActiveProject();
                        app().getSharedProjectManager().setSelectedProject(projectSlug);

                        // load the saved project without displaying a notice to the user
                        app().getSharedProjectManager().fetchProjectSource(app().getSharedProjectManager().getSelectedProject(), false);

                        app().getSharedProjectManager().getSelectedProject().setSelectedChapter(chapterId);
                        if(app().getSharedProjectManager().getSelectedProject().getSelectedChapter() != null) {
                            app().getSharedProjectManager().getSelectedProject().getSelectedChapter().setSelectedFrame(frameId);
                        }
                    } else {
                        // load the default project without display a notice to the user
                        app().getSharedProjectManager().fetchProjectSource(app().getSharedProjectManager().getSelectedProject(), false);
                    }
                }
            });
            return null;
        }

        protected void onProgressUpdate(String... items) {
            mProgressBar.setProgress(mProgress);
            mProgressTextView.setText(items[0]);
        }

        protected void onPostExecute(Void item) {
            mProgressTextView.setText("launching translator");
            startMainActivity();
        }
    }
}
