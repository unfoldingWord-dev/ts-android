package com.door43.translationstudio;

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
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.util.ArrayList;
import java.util.List;


/**
 * This activity allows users to choose a language from the list.
 */
public class LanguageSelectorActivity extends TranslatorBaseActivity {

    public static final String EXTRAS_CHOSEN_LANGUAGE = "language_id";
    public static final String EXTRAS_LANGUAGES = "language_ids";
    public static final String EXTRAS_SOURCE_LANGUAGES = "show_source_languages";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selector);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Boolean showSourceLanguages = false;
        String[] languageIds = null;

        Project p = AppContext.projectManager().getSelectedProject();
        if(p == null) {
            setResult(RESULT_CANCELED, null);
            finish();
        }

        // hook up list view
        ListView list = (ListView)findViewById(R.id.targetLanguageListView);
        final LanguageAdapter adapter;

        Intent intent = getIntent();
        if(intent != null) {
            showSourceLanguages = intent.getBooleanExtra(EXTRAS_SOURCE_LANGUAGES, false);
            languageIds = intent.getStringArrayExtra(EXTRAS_LANGUAGES);
        }
        final boolean willShowSourceLanguages = showSourceLanguages;

        // add items to list view
        if(languageIds != null && languageIds.length > 0) {
            // use provided languages
            List<Language> languages = new ArrayList<>();
            for(String id:languageIds) {
                languages.add(AppContext.projectManager().getLanguage(id));
            }
            // TRICKY: we tell the adapter these are source langauges because no formatting is displayed
            adapter = new LanguageAdapter(languages, this, true);
        } else {
            // use languages from the selected project
            if (willShowSourceLanguages) {
                adapter = new LanguageAdapter((List<Language>) (List<?>) p.getSourceLanguages(), this, showSourceLanguages);
            } else {
                adapter = new LanguageAdapter(AppContext.projectManager().getLanguages(), this, showSourceLanguages);
            }
        }

        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = getIntent();
                intent.putExtra(EXTRAS_CHOSEN_LANGUAGE, adapter.getItem(i).getId());
                setResult(RESULT_OK, intent);
                finish();
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
                setResult(RESULT_CANCELED, null);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
