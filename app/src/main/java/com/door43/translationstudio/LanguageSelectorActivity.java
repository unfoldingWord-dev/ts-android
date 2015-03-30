package com.door43.translationstudio;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.dialogs.LanguageAdapter;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ThreadableUI;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.util.List;


public class LanguageSelectorActivity extends TranslatorBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selector);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Boolean showSourceLanguages = false;

        Project p = AppContext.projectManager().getSelectedProject();
        if(p == null) {
            finish();
        }

        // hook up list view
        ListView list = (ListView)findViewById(R.id.targetLanguageListView);
        final LanguageAdapter adapter;

        Intent intent = getIntent();
        if(intent != null) {
            showSourceLanguages = intent.getBooleanExtra("sourceLanguages", false);
        }
        final boolean willShowSourceLanguages = showSourceLanguages;

        // add items to list view
        if(willShowSourceLanguages) {
            adapter = new LanguageAdapter((List<Language>)(List<?>)p.getSourceLanguages(), this, showSourceLanguages);
        } else {
            adapter = new LanguageAdapter(AppContext.projectManager().getLanguages(), this, showSourceLanguages);
        }

        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final ProgressDialog dialog = new ProgressDialog(LanguageSelectorActivity.this);
                dialog.setMessage(getResources().getString(R.string.loading_project_chapters));
                dialog.show();
                if(willShowSourceLanguages) {
                    AppContext.projectManager().getSelectedProject().setSelectedSourceLanguage(adapter.getItem(i).getId());
                    new ThreadableUI(LanguageSelectorActivity.this) {

                        @Override
                        public void onStop() {

                        }

                        @Override
                        public void run() {
                            AppContext.projectManager().fetchProjectSource(AppContext.projectManager().getSelectedProject());
                        }

                        @Override
                        public void onPostExecute() {
                            dialog.dismiss();
                            finish();
                        }
                    }.start();

                } else {
                    AppContext.projectManager().getSelectedProject().setSelectedTargetLanguage(adapter.getItem(i).getId());
                    dialog.dismiss();
                    finish();
                }
            }
        });
        list.setTextFilterEnabled(true);
        EditText searchField = (EditText)findViewById(R.id.inputSearchLanguage);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if(count < before) {
                    adapter.resetData();
                }
                adapter.getFilter().filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        TextView titleText = (TextView)findViewById(R.id.languageMenuTitleText);
        if(willShowSourceLanguages) {
            titleText.setText(R.string.choose_source_language);
        } else {
            titleText.setText(R.string.choose_target_language);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.language_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_dismiss_language_search:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
