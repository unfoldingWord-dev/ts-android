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
            finish();
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
                mProject.getSelectedChapter().setTitleTranslation(targetLanguageChapterTitleEditText.getText().toString());
                mProject.getSelectedChapter().setReferenceTranslation(targetLanguageChapterReferenceEditText.getText().toString());
                mProject.getSelectedChapter().save();
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chapter_setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onResume() {
        super.onResume();
        loadValues();
    }

    private void loadValues() {
        sourceLanguageChapterTitleEditText.setText(mProject.getSelectedChapter().getTitle());
        sourceLanguageChapterReferenceEditText.setText(mProject.getSelectedChapter().getReference());
        targetLanguageChapterTitleEditText.setText(mProject.getSelectedChapter().getTitleTranslation().getText());
        targetLanguageChapterReferenceEditText.setText(mProject.getSelectedChapter().getReferenceTranslation().getText());
        targetLanguageBtn.setText(mProject.getSelectedTargetLanguage().getName());
        sourceLanguageBtn.setText(mProject.getSelectedSourceLanguage().getName());
    }
}
