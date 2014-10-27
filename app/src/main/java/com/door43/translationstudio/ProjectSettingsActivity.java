package com.door43.translationstudio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;


public class ProjectSettingsActivity extends TranslatorBaseActivity {
    private ProjectSettingsActivity me = this;
    private Button targetLanguageBtn;
    private Button sourceLanguageBtn;
    private Project mProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_settings);

        mProject = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(mProject == null || mProject.getSelectedChapter() == null) {
            if(mProject != null && mProject.getSelectedChapter() == null) {
                app().showToastMessage(R.string.source_language_has_no_chapters);
                Intent languageIntent = new Intent(me, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", true);
                startActivity(languageIntent);
            }
        }

        targetLanguageBtn = (Button)findViewById(R.id.switchTargetLanguageButton);
        sourceLanguageBtn = (Button)findViewById(R.id.switchSourceLanguageButton);

        // hook up buttons
        targetLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent languageIntent = new Intent(me, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", false);
                startActivity(languageIntent);
            }
        });
        sourceLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent languageIntent = new Intent(me, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", true);
                startActivity(languageIntent);
            }
        });

        loadValues();
    }

    public void onResume() {
        super.onResume();
        loadValues();
    }

    private void loadValues() {
        targetLanguageBtn.setText(mProject.getSelectedTargetLanguage().getName());
        sourceLanguageBtn.setText(mProject.getSelectedSourceLanguage().getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.project_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_dismiss_project_settings:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
