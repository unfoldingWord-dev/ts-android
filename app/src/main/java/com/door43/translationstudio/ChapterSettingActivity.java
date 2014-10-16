package com.door43.translationstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;


public class ChapterSettingActivity extends TranslatorBaseActivity {
    private ChapterSettingActivity me = this;
    private TextView sourceLanguageChapterTitleEditText;
    private TextView sourceLanguageChapterReferenceEditText;
    private EditText targetLanguageChapterTitleEditText;
    private EditText targetLanguageChapterReferenceEditText;
    private Button targetLanguageBtn;
    private Button sourceLanguageBtn;
    private Project mProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_setting);

        mProject = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(mProject == null || mProject.getSelectedChapter() == null) {
            if(mProject != null && mProject.getSelectedChapter() == null) {
                // there are not chapters in the selected source language
                Intent languageIntent = new Intent(me, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", true);
                startActivity(languageIntent);
                finish();
            }
        }

        // expand view fields
        sourceLanguageChapterTitleEditText = (TextView)findViewById(R.id.sourceLanguageChapterTitle);
        sourceLanguageChapterReferenceEditText = (TextView)findViewById(R.id.sourceLanguageChapterReference);
        targetLanguageChapterTitleEditText = (EditText)findViewById(R.id.targetLanguageChapterTitleEditText);
        targetLanguageChapterReferenceEditText = (EditText)findViewById(R.id.targetLanguageChapterReferenceEditText);
        targetLanguageBtn = (Button)findViewById(R.id.switchTargetLanguageButton);
        sourceLanguageBtn = (Button)findViewById(R.id.switchSourceLanguageButton);

        // hook up buttons
        Button cancelBtn = (Button)findViewById(R.id.cancelEditChapterTitleButton);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        Button saveBtn = (Button)findViewById(R.id.saveChapterTitleButton);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mProject.getSelectedChapter() != null) {
                    mProject.getSelectedChapter().setTitleTranslation(targetLanguageChapterTitleEditText.getText().toString());
                    mProject.getSelectedChapter().setReferenceTranslation(targetLanguageChapterReferenceEditText.getText().toString());
                    mProject.getSelectedChapter().save();
                }
                finish();
            }
        });
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
        if(mProject.getSelectedChapter() != null) {
            sourceLanguageChapterTitleEditText.setText(mProject.getSelectedChapter().getTitle());
            sourceLanguageChapterReferenceEditText.setText(mProject.getSelectedChapter().getReference());
            targetLanguageChapterTitleEditText.setText(mProject.getSelectedChapter().getTitleTranslation().getText());
            targetLanguageChapterReferenceEditText.setText(mProject.getSelectedChapter().getReferenceTranslation().getText());
        }
        targetLanguageBtn.setText(mProject.getSelectedTargetLanguage().getName());
        sourceLanguageBtn.setText(mProject.getSelectedSourceLanguage().getName());
    }
}
