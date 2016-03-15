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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

public class NewTargetTranslationActivity extends BaseActivity implements TargetLanguageListFragment.OnItemClickListener, ProjectListFragment.OnItemClickListener {

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    public static final String EXTRA_NEW_LANGUAGE_DATA = "extra_new_language_data";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_target_translation);

        ImageButton newLanguageButton = (ImageButton) findViewById(R.id.newLanguageRequest);
        if (null != newLanguageButton) {
            newLanguageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // TODO: 3/15/16 for testing
//                    Intent requestNewLangaugeIntent = new Intent(NewTargetTranslationActivity.this,
//                            NewLanguageActivity.class);
//                    requestNewLangaugeIntent.putExtra(NewLanguageActivity.EXTRA_CALLING_ACTIVITY, NewLanguageActivity.ACTIVITY_HOME);
//                    startActivityForResult(requestNewLangaugeIntent, NEW_LANGUAGE_REQUEST);

                    String dummyAnswers = "{\n" +
                            "  \"answers\": [\n" +
                            "    {\n" +
                            "      \"answer\": \"a1\",\n" +
                            "      \"question_id\": 100\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a11\",\n" +
                            "      \"question_id\": 101\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a12\",\n" +
                            "      \"question_id\": 102\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"YES\",\n" +
                            "      \"question_id\": 200\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a21\",\n" +
                            "      \"question_id\": 201\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a22\",\n" +
                            "      \"question_id\": 202\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a23\",\n" +
                            "      \"question_id\": 203\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a3\",\n" +
                            "      \"question_id\": 300\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a4\",\n" +
                            "      \"question_id\": 400\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a41\",\n" +
                            "      \"question_id\": 401\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a42\",\n" +
                            "      \"question_id\": 402\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a43\",\n" +
                            "      \"question_id\": 403\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a5\",\n" +
                            "      \"question_id\": 500\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a51\",\n" +
                            "      \"question_id\": 501\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a52\",\n" +
                            "      \"question_id\": 502\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a53\",\n" +
                            "      \"question_id\": 503\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a54\",\n" +
                            "      \"question_id\": 504\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a6\",\n" +
                            "      \"question_id\": 600\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a61\",\n" +
                            "      \"question_id\": 601\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a62\",\n" +
                            "      \"question_id\": 602\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a63\",\n" +
                            "      \"question_id\": 603\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a7\",\n" +
                            "      \"question_id\": 700\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a71\",\n" +
                            "      \"question_id\": 701\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a72\",\n" +
                            "      \"question_id\": 702\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a73\",\n" +
                            "      \"question_id\": 703\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a8\",\n" +
                            "      \"question_id\": 800\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a81\",\n" +
                            "      \"question_id\": 801\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a82\",\n" +
                            "      \"question_id\": 802\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a83\",\n" +
                            "      \"question_id\": 803\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a9\",\n" +
                            "      \"question_id\": 900\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"answer\": \"a10\",\n" +
                            "      \"question_id\": 1000\n" +
                            "    }\n" +
                            "  ],\n" +
                            "  \"questionaire_id\": 1,\n" +
                            "  \"temp_code\": \"qaa-x-886f57\",\n" +
                            "  \"request_id\": \"d83b2629-6fd8-4d15-a0a2-f24437d1de34\"\n" +
                            "}";
                    useNewLanguage(dummyAnswers);
                }
            });
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

    private void useNewLanguage(String newLanguageDataStr) {
        try {
            mNewLanguageData = newLanguageDataStr;
            JSONObject newLanguageData = new JSONObject(newLanguageDataStr);

            String languageCode = newLanguageData.getString(NewLanguageActivity.NEW_LANGUAGE_TEMP_CODE);
            JSONObject nameAnswer = getAnswerForID(newLanguageData, 100);
            String languageName = nameAnswer.getString(NewLanguageActivity.NEW_LANGUAGE_ANSWER);

            // TODO: 3/15/16 need to add direction and region
            mSelectedTargetLanguage = new TargetLanguage(languageCode, languageName, "uncertain", LanguageDirection.LeftToRight);

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

    private JSONObject getAnswerForID(JSONObject newLanguageData, long id) throws JSONException {

        JSONArray answers = newLanguageData.getJSONArray(NewLanguageActivity.NEW_LANGUAGE_ANSWERS);
        for (int i = 0; i < answers.length(); i++) {
            JSONObject answer = answers.getJSONObject(i);
            long qid = answer.getLong(NewLanguageActivity.NEW_LANGUAGE_QUESTION_ID);
            if (qid == id) {
                return answer;
            }
        }

        return null;
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
        // TRICKY: android only supports translating regular text projects
        TargetTranslation existingTranslation = translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(mSelectedTargetLanguage.getId(), projectId, TranslationType.TEXT, Resource.Type.REGULAR));
        if(existingTranslation == null) {
            // create new target translation
            SourceLanguage sourceLanguage = AppContext.getLibrary().getPreferredSourceLanguage(projectId, Locale.getDefault().getLanguage()); // get project name
            // TODO: 3/2/2016 eventually the format will be specified in the project
            SourceTranslation sourceTranslation = AppContext.getLibrary().getDefaultSourceTranslation(projectId, sourceLanguage.getId());
            Resource resource = AppContext.getLibrary().getResource(sourceTranslation);
            // TRICKY: android only supports "regular" "text" translations
            TargetTranslation targetTranslation = AppContext.getTranslator().createTargetTranslation(AppContext.getProfile().getNativeSpeaker(), mSelectedTargetLanguage, projectId, TranslationType.TEXT, Resource.Type.REGULAR, sourceTranslation.getFormat());
            if(targetTranslation != null) {
                mNewTargetTranslationId = targetTranslation.getId();

                Intent data = new Intent();
                data.putExtra(EXTRA_TARGET_TRANSLATION_ID, mNewTargetTranslationId);
                setResult(RESULT_OK, data);
                finish();
            } else {
                // TRICKY: android only supports translating regular text projects
                AppContext.getTranslator().deleteTargetTranslation(TargetTranslation.generateTargetTranslationId(mSelectedTargetLanguage.getId(), projectId, TranslationType.TEXT, Resource.Type.REGULAR));
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
        if(mNewTargetTranslationId != null) {
            outState.putSerializable(STATE_TARGET_TRANSLATION_ID, mNewTargetTranslationId);
        } else {
            outState.remove(STATE_TARGET_TRANSLATION_ID);
        }
        if(mSelectedTargetLanguage != null) {
            outState.putString(STATE_TARGET_LANGUAGE_ID, mSelectedTargetLanguage.getId());
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
                useNewLanguage(newLanguageData);
            }
        }
    }
}