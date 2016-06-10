package com.door43.translationstudio.newui.newtranslation;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.LanguageDirection;
import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.newui.library.Searchable;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.newui.newlanguage.NewLanguageActivity;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class NewTargetTranslationActivity extends BaseActivity implements TargetLanguageListFragment.OnItemClickListener, ProjectListFragment.OnItemClickListener {

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    public static final String EXTRA_NEW_LANGUAGE_DATA = "extra_new_language_data";
    public static final String EXTRA_NEW_LANGUAGE_DIRECTION = "extra_new_language_direction";
    public static final int RESULT_DUPLICATE = 2;
    private static final String STATE_TARGET_TRANSLATION_ID = "state_target_translation_id";
    private static final String STATE_TARGET_LANGUAGE_ID = "state_target_language_id";
    public static final int RESULT_ERROR = 3;
    public static final String TAG = NewTargetTranslationActivity.class.getSimpleName();
    public static final int NEW_LANGUAGE_REQUEST = 1001;
    private TargetLanguage mSelectedTargetLanguage = null;
    private Searchable mFragment;
    private String mNewTargetTranslationId = null;
    private String mNewLanguageData = null;
    private ImageButton mNewLanguageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_target_translation);

        mNewLanguageButton = (ImageButton) findViewById(R.id.newLanguageRequest);
        if (null != mNewLanguageButton) {
            mNewLanguageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent requestNewLangaugeIntent = new Intent(NewTargetTranslationActivity.this,
                            NewLanguageActivity.class);
                    requestNewLangaugeIntent.putExtra(NewLanguageActivity.EXTRA_CALLING_ACTIVITY, NewLanguageActivity.ACTIVITY_HOME);
                    startActivityForResult(requestNewLangaugeIntent, NEW_LANGUAGE_REQUEST);
                }
            });
        }

        if(savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_TARGET_TRANSLATION_ID)) {
                mNewTargetTranslationId = (String) savedInstanceState.getSerializable(STATE_TARGET_TRANSLATION_ID);
            }

            if (savedInstanceState.containsKey(STATE_TARGET_LANGUAGE_ID)) {
                String targetLanguageJsonStr = savedInstanceState.getString(STATE_TARGET_LANGUAGE_ID);
                try {
                    mSelectedTargetLanguage = TargetLanguage.generate(new JSONObject(targetLanguageJsonStr));
                } catch (Exception e) { }
            }
        }

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

    /**
     * use new language information passed in JSON format string to create a new target language
     * @param newLangDataJsonStr
     */
    private void useNewLanguage(String newLangDataJsonStr, boolean newLanguageDirectionLtor) {
        try {
            mNewLanguageData = newLangDataJsonStr;
            NewLanguagePackage newLang = NewLanguagePackage.parse(newLangDataJsonStr);

            String languageCode = newLang.tempLanguageCode;
            String languageName = newLang.languageName;

            // TODO: 3/15/16 need to add region
            LanguageDirection languageDirection = newLanguageDirectionLtor ? LanguageDirection.LeftToRight : LanguageDirection.RightToLeft;
            mSelectedTargetLanguage = new TargetLanguage(languageCode, languageName, "uncertain", languageDirection);

            // display project list
            mFragment = new ProjectListFragment();
            ((ProjectListFragment) mFragment).setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, (ProjectListFragment) mFragment).commit();
            // TODO: animate
            invalidateOptionsMenu();

        } catch (Exception e) {
            Logger.e(TAG, "Error Adding new language", e);
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            mNewTargetTranslationId = savedInstanceState.getString(STATE_TARGET_TRANSLATION_ID, null);
            String targetLanguageId = savedInstanceState.getString(STATE_TARGET_LANGUAGE_ID, null);
            if(targetLanguageId != null) {
                mSelectedTargetLanguage = AppContext.getLibrary().getTargetLanguage(targetLanguageId);
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
    /**
     * user has selected a language
     */
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
    /**
     * user has selected a project
     */
    public void onItemClick(String projectId) {
        Translator translator = AppContext.getTranslator();
        // TRICKY: android only supports translating regular text projects
        String resourceSlug = projectId.equals("obs") ? "obs" : Resource.REGULAR_SLUG;
        TargetTranslation existingTranslation = translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(mSelectedTargetLanguage.getId(), projectId, TranslationType.TEXT, resourceSlug));
        if(existingTranslation == null) {
            // create new target translation
            SourceLanguage sourceLanguage = AppContext.getLibrary().getPreferredSourceLanguage(projectId, Locale.getDefault().getLanguage()); // get project name
            // TODO: 3/2/2016 eventually the format will be specified in the project
            SourceTranslation sourceTranslation = AppContext.getLibrary().getDefaultSourceTranslation(projectId, sourceLanguage.getId());
            final TargetTranslation targetTranslation = AppContext.getTranslator().createTargetTranslation(AppContext.getProfile().getNativeSpeaker(), mSelectedTargetLanguage, projectId, TranslationType.TEXT, resourceSlug, sourceTranslation.getFormat());
            if(targetTranslation != null) {

                if(mNewLanguageData != null) {
                    saveNewLanguageData(targetTranslation, mNewLanguageData);

                    String msg = String.format(AppContext.context().getResources().getString(R.string.new_language_confirmation), targetTranslation.getTargetLanguageId(), targetTranslation.getTargetLanguageName());
                    CustomAlertDialog.Create(this)
                            .setTitle(R.string.language)
                            .setMessage(msg)
                            .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    newProjectCreated(targetTranslation);
                                }
                            })
                            .setNegativeButton(R.string.title_cancel, null)
                            .show("NewLang");
                } else {
                    newProjectCreated(targetTranslation);
                }
            } else {
                AppContext.getTranslator().deleteTargetTranslation(TargetTranslation.generateTargetTranslationId(mSelectedTargetLanguage.getId(), projectId, TranslationType.TEXT, resourceSlug));
                Intent data = new Intent();
                setResult(RESULT_ERROR, data);
                finish();
            }
        } else {
            // that translation already exists
            Intent data = new Intent();
            data.putExtra(EXTRA_TARGET_TRANSLATION_ID, existingTranslation.getId());
            setResult(RESULT_DUPLICATE, data);
            finish();
        }
    }

    private void newProjectCreated(TargetTranslation targetTranslation) {
        mNewTargetTranslationId = targetTranslation.getId();

        Intent data = new Intent();
        data.putExtra(EXTRA_TARGET_TRANSLATION_ID, mNewTargetTranslationId);
        setResult(RESULT_OK, data);
        finish();
    }

    /**
     * save new language data into target translation as well as "new_languages" folder
     * @param targetTranslation
     * @param newLanguageData
     * @return
     */
    private boolean saveNewLanguageData(TargetTranslation targetTranslation, String newLanguageData) {
        String path = "";
        try {
            NewLanguagePackage newLang = NewLanguagePackage.parse(newLanguageData);
            if(null == newLang) {
                return false;
            }

            File folder = targetTranslation.getPath();
            path = folder.toString();
            newLang.commit(folder);

            File dataPath = NewLanguagePackage.getNewLanguageFolder();
            path = dataPath.toString();
            FileUtils.forceMkdir(dataPath);
            File newLanguagePath = new File(dataPath,targetTranslation.getId() + NewLanguagePackage.NEW_LANGUAGE_FILE_EXTENSION);
            path = newLanguagePath.toString();
            newLang.commitToFile(newLanguagePath);

        } catch (Exception e) {
            Logger.e(TAG, "Could not write new language data to: " + path, e);
            return false;
        }
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
                                Intent intent = new Intent(NewTargetTranslationActivity.this, ServerLibraryActivity.class);
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
        outState.putSerializable(STATE_TARGET_TRANSLATION_ID, mNewTargetTranslationId);

        if(mSelectedTargetLanguage != null) {
            JSONObject targetLanguageJson = mSelectedTargetLanguage.toApiFormatJson();
            if(targetLanguageJson != null) {
                outState.putString(STATE_TARGET_LANGUAGE_ID, targetLanguageJson.toString());
            }
        } else {
            outState.remove(STATE_TARGET_LANGUAGE_ID);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (NEW_LANGUAGE_REQUEST == requestCode) {
            if(RESULT_OK == resultCode) {
                String newLanguageData = data.getStringExtra(NewTargetTranslationActivity.EXTRA_NEW_LANGUAGE_DATA);
                boolean newLanguageDirectionLtor = data.getBooleanExtra(NewTargetTranslationActivity.EXTRA_NEW_LANGUAGE_DIRECTION, true);
                useNewLanguage(newLanguageData, newLanguageDirectionLtor);
                mNewLanguageButton.setVisibility(View.INVISIBLE);
            }
        }
    }
}