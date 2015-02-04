package com.door43.translationstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;


public class ChapterSettingActivity extends TranslatorBaseActivity {
    private TextView sourceLanguageChapterTitleText;
    private TextView sourceLanguageChapterReferenceText;
    private EditText targetLanguageChapterTitleEditText;
    private EditText targetLanguageChapterReferenceEditText;
    private TextView sourceLanguageNameText;
    private TextView targetLanguageNameText;
    private Project mProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProject = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(mProject == null || mProject.getSelectedChapter() == null) {
            if(mProject != null && mProject.getSelectedChapter() == null) {
                // there are not chapters in the selected source language
                Intent languageIntent = new Intent(this, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", true);
                startActivity(languageIntent);
                finish();
            }
        }

        // expand view fields
        sourceLanguageNameText = (TextView)findViewById(R.id.sourceLanguageName);
        targetLanguageNameText = (TextView)findViewById(R.id.targetLanguageName);
        sourceLanguageChapterTitleText = (TextView)findViewById(R.id.sourceLanguageChapterTitle);
        sourceLanguageChapterReferenceText = (TextView)findViewById(R.id.sourceLanguageChapterReference);
        targetLanguageChapterTitleEditText = (EditText)findViewById(R.id.targetLanguageChapterTitleEditText);
        targetLanguageChapterReferenceEditText = (EditText)findViewById(R.id.targetLanguageChapterReferenceEditText);

        loadValues();
    }


    public void onResume() {
        super.onResume();
        loadValues();
    }

    private void loadValues() {
        if(mProject.getSelectedChapter() != null) {
            sourceLanguageChapterTitleText.setText(mProject.getSelectedChapter().getTitle());
            sourceLanguageChapterReferenceText.setText(mProject.getSelectedChapter().getReference());
            targetLanguageChapterTitleEditText.setText(mProject.getSelectedChapter().getTitleTranslation().getText());
            targetLanguageChapterReferenceEditText.setText(mProject.getSelectedChapter().getReferenceTranslation().getText());
        }
        sourceLanguageNameText.setText(mProject.getSelectedSourceLanguage().getName());
        targetLanguageNameText.setText(mProject.getSelectedTargetLanguage().getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chapter_settings_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save_chapter_settings:
                if(mProject.getSelectedChapter() != null) {
                    mProject.getSelectedChapter().setTitleTranslation(targetLanguageChapterTitleEditText.getText().toString());
                    mProject.getSelectedChapter().setReferenceTranslation(targetLanguageChapterReferenceEditText.getText().toString());
                    mProject.getSelectedChapter().save();
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
