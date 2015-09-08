package com.door43.translationstudio.targettranslations;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.library.Searchable;
import com.door43.translationstudio.util.AppContext;

public class NewTargetTranslationActivity extends AppCompatActivity implements TargetLanguageListFragment.OnItemClickListener, ProjectListFragment.OnItemClickListener {

    private TargetLanguage mSelectedTargetLanguage = null;
    private Searchable mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_target_translation);

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                return;
            }

            mFragment = new TargetLanguageListFragment();
            ((TargetLanguageListFragment) mFragment).setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.fragment_container, (TargetLanguageListFragment) mFragment).commit();
            // TODO: animate
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_target_translation, menu);
        return true;
    }

    @Override
    public void onItemClick(TargetLanguage targetLanguage) {
        mSelectedTargetLanguage = targetLanguage;

        // display project list
        mFragment = new ProjectListFragment();
        ((ProjectListFragment) mFragment).setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, (ProjectListFragment) mFragment).commit();
        // TODO: animate
        invalidateOptionsMenu();
    }

    @Override
    public void onItemClick(String projectId) {
        Translator translator = AppContext.getTranslator();
        TargetTranslation existingTranslation = translator.getTargetTranslation(mSelectedTargetLanguage.code, projectId);
        if(existingTranslation == null) {
            // create new target translation
            TargetTranslation tranlsation = AppContext.getTranslator().createTargetTranslation(mSelectedTargetLanguage, projectId);
            // TODO: close this activity and open the new target translation
        } else {
            // that translation already exists
            // TODO: display notice to user
        }
        finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchViewAction = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchViewAction.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mFragment.onSearchQuery(s);
                return true;
            }
        });
        searchViewAction.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_search:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
