package com.door43.translationstudio;

import android.content.Intent;
import android.os.Bundle;

import com.door43.translationstudio.events.ProjectsLoadedEvent;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;

/**
 * Created by joel on 9/29/2014.
 */
public class SplashActivity extends TranslatorBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread splash_screen= new Thread(){
            public void run() {
                app().getSharedProjectManager().init(new ProjectManager.FinishedLoadingSource() {
                    @Override
                    public void ready() {
                        // Generate the ssh keys
                        if(!app().hasKeys()) {
                            app().generateKeys();
                        }



                        // load previously viewed frame
                        if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {
                            String frameId = app().getLastActiveFrame();
                            String chapterId = app().getLastActiveChapter();
                            String projectSlug = app().getLastActiveProject();
                            app().getSharedProjectManager().setSelectedProject(projectSlug);

                            // load the saved project
                            app().getSharedProjectManager().fetchProjectSource(app().getSharedProjectManager().getSelectedProject());

                            app().getSharedProjectManager().getSelectedProject().setSelectedChapter(chapterId);
                            if(app().getSharedProjectManager().getSelectedProject().getSelectedChapter() != null) {
                                app().getSharedProjectManager().getSelectedProject().getSelectedChapter().setSelectedFrame(frameId);
                            }
                        } else {
                            // load the default project
                            app().getSharedProjectManager().fetchProjectSource(app().getSharedProjectManager().getSelectedProject());
                        }
                        startMainActivity();
                    }
                });
            }
        };
        splash_screen.start();
    }

    public void onResume() {
        super.onResume();
    }

    public void startMainActivity() {
        Intent splashIntent = new Intent(this, MainActivity.class);
        startActivity(splashIntent);
        finish();
    }
}
