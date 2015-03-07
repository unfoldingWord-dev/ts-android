package com.door43.translationstudio;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;


public class ProjectSettingsActivity extends TranslatorBaseActivity {
    private Button targetLanguageBtn;
    private Button sourceLanguageBtn;
    private Project mProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProject = AppContext.projectManager().getSelectedProject();
        if(mProject == null || mProject.getSelectedChapter() == null) {
            if(mProject != null && mProject.getSelectedChapter() == null) {
                app().showToastMessage(R.string.missing_chapter); // the language does not contain any chapters
                Intent languageIntent = new Intent(ProjectSettingsActivity.this, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", true);
                startActivity(languageIntent);
            } else {
                finish();
            }
        }

        targetLanguageBtn = (Button)findViewById(R.id.switchTargetLanguageButton);
        sourceLanguageBtn = (Button)findViewById(R.id.switchSourceLanguageButton);

        // hook up buttons
        targetLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent languageIntent = new Intent(ProjectSettingsActivity.this, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", false);
                startActivity(languageIntent);
            }
        });
        sourceLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent languageIntent = new Intent(ProjectSettingsActivity.this, LanguageSelectorActivity.class);
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
        if(mProject != null) {
            targetLanguageBtn.setText(mProject.getSelectedTargetLanguage().getName());
            sourceLanguageBtn.setText(mProject.getSelectedSourceLanguage().getName());
            if (mProject.getSelectedTargetLanguage().isTranslating(mProject)) {
                targetLanguageBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_project_status_translating_light, 0);
            } else {
                targetLanguageBtn.setCompoundDrawables(null, null, null, null);
            }

            // set graphite fontface
            targetLanguageBtn.setTypeface(AppContext.graphiteTypeface(mProject.getSelectedTargetLanguage()), 0);
            sourceLanguageBtn.setTypeface(AppContext.graphiteTypeface(mProject.getSelectedSourceLanguage()), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.project_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_dismiss_project_settings:
                if(!mProject.hasChosenTargetLanguage()) {
                    mProject.setSelectedTargetLanguage(mProject.getSelectedTargetLanguage().getId());
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
