package com.door43.translationstudio.newui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.ImportUsfm;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.newui.library.Searchable;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.newui.newtranslation.NewTargetTranslationActivity;
import com.door43.translationstudio.newui.newtranslation.ProjectListFragment;
import com.door43.translationstudio.newui.newtranslation.TargetLanguageListFragment;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ImportUsfmActivity extends BaseActivity implements TargetLanguageListFragment.OnItemClickListener {

    public static final int RESULT_DUPLICATE = 2;
    private static final String STATE_TARGET_LANGUAGE_ID = "state_target_language_id";
    public static final int RESULT_ERROR = 3;
    public static final String EXTRA_USFM_IMPORT_FOLDER = "extra_usfm_import_folder";
    public static final String EXTRA_USFM_IMPORT_FILE = "extra_usfm_import_file";
    public static final String EXTRA_USFM_IMPORT_RESOURCE_FILE = "extra_usfm_import_resource_file";
    private Searchable mFragment;

    public static final String TAG = ImportUsfmActivity.class.getSimpleName();
    private TargetLanguage mTargetLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_usfm);

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (Searchable)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                mFragment = new TargetLanguageListFragment();
                ((TargetLanguageListFragment) mFragment).setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.fragment_container, (TargetLanguageListFragment) mFragment).commit();
                // TODO: animate
            }
        }
    }

    private void doFileImport() {
        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        final ImportUsfm usfm = new ImportUsfm(mTargetLanguage);
        boolean success = false;

        if(args.containsKey(EXTRA_USFM_IMPORT_FOLDER)) {
            String importFolder = args.getString(EXTRA_USFM_IMPORT_FOLDER);
        } else if(args.containsKey(EXTRA_USFM_IMPORT_FILE)) {
            String importFile = args.getString(EXTRA_USFM_IMPORT_FILE);
        } else if(args.containsKey(EXTRA_USFM_IMPORT_RESOURCE_FILE)) {
            String importResourceFile = args.getString(EXTRA_USFM_IMPORT_RESOURCE_FILE);
            success = usfm.importResourceFile(this,importResourceFile);
        }

        String[] errors = usfm.getErrors();
        if(!success) {
            Logger.i(TAG, "USFM import finished with errors: " + TextUtils.join("\n", errors));
        } else if(errors.length > 0) {
            Logger.i(TAG, "USFM import finished with warnings: " + TextUtils.join("\n", errors));
        }
    }

    public static void startActivityForFileImport(Activity context, File path) {
        Intent intent = new Intent(context, ImportUsfmActivity.class);
        intent.putExtra(EXTRA_USFM_IMPORT_FILE, path.toString());
        context.startActivity(intent);
    }

    public static void startActivityForFolderImport(Activity context, File folder) {
        Intent intent = new Intent(context, ImportUsfmActivity.class);
        intent.putExtra(EXTRA_USFM_IMPORT_FOLDER, folder.toString());
        context.startActivity(intent);
    }

    public static void startActivityForResourceImport(Activity context, String resourceName) {
        Intent intent = new Intent(context, ImportUsfmActivity.class);
        intent.putExtra(EXTRA_USFM_IMPORT_RESOURCE_FILE, resourceName);
        context.startActivity(intent);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            String targetLanguageId = savedInstanceState.getString(STATE_TARGET_LANGUAGE_ID, null);
            if(targetLanguageId != null) {
                mTargetLanguage = AppContext.getLibrary().getTargetLanguage(targetLanguageId);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_target_translation, menu);
        return true;
    }

     @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(mFragment instanceof ProjectListFragment) {
            menu.findItem(R.id.action_update).setVisible(true);
        } else {
            menu.findItem(R.id.action_update).setVisible(false);
        }
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
            case R.id.action_update:
                CustomAlertDialog.Create(this)
                        .setTitle(R.string.update_projects)
                        .setIcon(R.drawable.ic_local_library_black_24dp)
                        .setMessage(R.string.use_internet_confirmation)
                        .setPositiveButton(R.string.yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(ImportUsfmActivity.this, ServerLibraryActivity.class);
//                                intent.putExtra(ServerLibraryActivity.ARG_SHOW_UPDATES, true);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show("Update");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if(mTargetLanguage != null) {
            outState.putString(STATE_TARGET_LANGUAGE_ID, mTargetLanguage.getId());
        } else {
            outState.remove(STATE_TARGET_LANGUAGE_ID);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(TargetLanguage targetLanguage) {
        mTargetLanguage = targetLanguage;

        if(null != targetLanguage) {
            doFileImport();
        }

        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();
    }

    public interface OnFinishedListener {
        void onFinished(boolean success);
    }
}
