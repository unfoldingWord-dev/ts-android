package com.door43.translationstudio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.LanguageAdapter;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;


public class LanguageSelectorActivity extends TranslatorBaseActivity {
    LanguageSelectorActivity me = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selector);

        Boolean showSourceLanguages = false;

        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
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
            adapter = new LanguageAdapter(p.getSourceLanguages(), this);
        } else {
            adapter = new LanguageAdapter(MainContext.getContext().getSharedProjectManager().getLanguages(), this);
        }

        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(willShowSourceLanguages) {
                    MainContext.getContext().getSharedProjectManager().getSelectedProject().setSelectedSourceLanguage(adapter.getItem(i).getId());
                    finish();
                } else {
                    MainContext.getContext().getSharedProjectManager().getSelectedProject().setSelectedTargetLanguage(adapter.getItem(i).getId());
                    finish();
                }
            }
        });
        list.setTextFilterEnabled(true);
        EditText searchField = (EditText)findViewById(R.id.inputSearchTargetLanguage);
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
            titleText.setText("Choose The Source Language");
        } else {
            titleText.setText("Choose The Target Language");
        }

        // hook up buttons
        Button cancelBtn = (Button)findViewById(R.id.cancelSwitchTargetLanguageButton);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.language_selector, menu);
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
}
