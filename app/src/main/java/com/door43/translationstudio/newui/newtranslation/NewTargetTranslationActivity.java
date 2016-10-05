package com.door43.translationstudio.newui.newtranslation;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.unfoldingword.door43client.models.Questionnaire;
import org.unfoldingword.door43client.models.SourceLanguage;
import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.resourcecontainer.Resource;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.newui.library.Searchable;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.newlanguage.NewTempLanguageActivity;
import com.door43.util.StringUtilities;
import com.door43.widget.ViewUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class NewTargetTranslationActivity extends BaseActivity implements TargetLanguageListFragment.OnItemClickListener, ProjectListFragment.OnItemClickListener {

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    public static final String EXTRA_CHANGE_TARGET_LANGUAGE_ONLY = "extra_change_target_language_only";
    public static final int RESULT_DUPLICATE = 2;
    private static final String STATE_TARGET_TRANSLATION_ID = "state_target_translation_id";
    private static final String STATE_TARGET_LANGUAGE = "state_target_language_id";
    public static final int RESULT_ERROR = 3;
    public static final String TAG = NewTargetTranslationActivity.class.getSimpleName();
    public static final int NEW_LANGUAGE_REQUEST = 1001;
    public static final String NEW_LANGUAGE_CONFIRMATION = "new-language-confirmation";
    private static final String STATE_NEW_LANGUAGE = "new_language";
    private TargetLanguage mSelectedTargetLanguage = null;
    private Searchable mFragment;
    private String mNewTargetTranslationId = null;
    private boolean createdNewLanguage = false;
    private boolean mChangeTargetLanguageOnly = false;
    private String mTargetTranslationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_target_translation);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            mTargetTranslationId = extras.getString(EXTRA_TARGET_TRANSLATION_ID, null);
            mChangeTargetLanguageOnly = extras.getBoolean(EXTRA_CHANGE_TARGET_LANGUAGE_ONLY, false);
        }

        if(savedInstanceState != null) {
            createdNewLanguage = savedInstanceState.getBoolean(STATE_NEW_LANGUAGE, false);
            if (savedInstanceState.containsKey(STATE_TARGET_TRANSLATION_ID)) {
                mNewTargetTranslationId = (String) savedInstanceState.getSerializable(STATE_TARGET_TRANSLATION_ID);
            }

            if (savedInstanceState.containsKey(STATE_TARGET_LANGUAGE)) {
                String targetLanguageJsonStr = savedInstanceState.getString(STATE_TARGET_LANGUAGE);
                try {
                    mSelectedTargetLanguage = TargetLanguage.fromJSON(new JSONObject(targetLanguageJsonStr));
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

        if(createdNewLanguage) {
            confirmTempLanguage(mSelectedTargetLanguage);
        }
    }

    /**
     * use new language information passed in JSON format string to create a new target language
     * @param request
     */
    private void registerTempLanguage(NewLanguageRequest request) {
        if(request != null) {
            Questionnaire questionnaire = App.getLibrary().index().getQuestionnaire(request.questionnaireId);
            if (questionnaire != null && App.addNewLanguageRequest(request)) {
                mSelectedTargetLanguage = request.getTempTargetLanguage();
                this.createdNewLanguage = true;
                confirmTempLanguage(mSelectedTargetLanguage);
                return;
            }
        }
        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(R.string.error)
                .setMessage(R.string.try_again)
                .show();
    }

    /**
     * Displays a confirmation for the new language
     * @param language
     */
    private void confirmTempLanguage(final TargetLanguage language) {
        if(language != null) {
            String msg = String.format(getResources().getString(R.string.new_language_confirmation), language.slug, language.name);
            new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setCancelable(false)
//                    .setAutoDismiss(false)
                    .setTitle(R.string.language)
                    .setMessage(msg)
                    .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            onItemClick(language);
                        }
                    })
                    .setNeutralButton(R.string.copy, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            StringUtilities.copyToClipboard(NewTargetTranslationActivity.this, language.slug);
                            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT);
                            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                            snack.show();
                        }
                    })
                    .show();
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

        if(!mChangeTargetLanguageOnly) {
            // display project list
            mFragment = new ProjectListFragment();
            ((ProjectListFragment) mFragment).setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, (ProjectListFragment) mFragment).commit();
            // TODO: animate
            invalidateOptionsMenu();
        } else { // just change the target language
            Translator translator = App.getTranslator();
            TargetTranslation targetTranslation = translator.getTargetTranslation(mTargetTranslationId);
            String targetTranslationId = targetTranslation.getId();

            targetTranslation.changeTargetLanguage(mSelectedTargetLanguage);
            translator.normalizePath(targetTranslation);
            finish();
        }
    }

    @Override
    public void onItemClick(String projectId) {
        Translator translator = App.getTranslator();
        // TRICKY: android only supports translating regular text projects
        String resourceSlug = projectId.equals("obs") ? "obs" : "reg";//Resource.REGULAR_SLUG;
        TargetTranslation existingTranslation = translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(mSelectedTargetLanguage.slug, projectId, TranslationType.TEXT, resourceSlug));
        if(existingTranslation == null) {
            // create new target translation
//            SourceLanguage sourceLanguage = App.getLibrary().getPreferredSourceLanguage(projectId, App.getDeviceLanguageCode()); // get project name
            // TODO: 3/2/2016 eventually the format will be specified in the project

            Project p = App.getLibrary().index().getProject(App.getDeviceLanguageCode(), projectId, true);
            List<Resource> resources = App.getLibrary().index().getResources(p.languageSlug, p.slug);
            ResourceContainer resourceContainer = null;
            try {
                resourceContainer = App.getLibrary().open(p.languageSlug, p.slug, resources.get(0).slug);
            } catch (Exception e) {
                e.printStackTrace();
            }
            TranslationFormat format = TranslationFormat.parse(resourceContainer.contentMimeType);
//            SourceTranslation sourceTranslation = App.getLibrary().getDefaultSourceTranslation(projectId, sourceLanguage.getId());
            final TargetTranslation targetTranslation = App.getTranslator().createTargetTranslation(App.getProfile().getNativeSpeaker(), mSelectedTargetLanguage, projectId, TranslationType.TEXT, resourceSlug, format);
            if(targetTranslation != null) {
                // deploy custom language code request to the translation
                NewLanguageRequest request = App.getNewLanguageRequest(mSelectedTargetLanguage.slug);
                if(request != null) {
                    try {
                        targetTranslation.setNewLanguageRequest(request);
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "Failed to deploy the new language code request", e);
                    }
                }

                newProjectCreated(targetTranslation);
            } else {
                App.getTranslator().deleteTargetTranslation(TargetTranslation.generateTargetTranslationId(mSelectedTargetLanguage.slug, projectId, TranslationType.TEXT, resourceSlug));
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(mFragment instanceof ProjectListFragment) {
            menu.findItem(R.id.action_update).setVisible(true);
        } else {
            menu.findItem(R.id.action_update).setVisible(false);
        }
        if(mFragment instanceof TargetLanguageListFragment) {
            menu.findItem(R.id.action_add_language).setVisible(true);
        } else {
            menu.findItem(R.id.action_add_language).setVisible(false);
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
            case R.id.action_add_language:
                new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.title_new_language_code)
                        .setMessage(R.string.confirm_start_new_language_code)
                        .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent requestNewLangaugeIntent = new Intent(NewTargetTranslationActivity.this, NewTempLanguageActivity.class);
                                startActivityForResult(requestNewLangaugeIntent, NEW_LANGUAGE_REQUEST);
                            }
                        })
                        .setNegativeButton(R.string.title_cancel, null)
                        .show();
                return true;
            case R.id.action_update:
                new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.update_library)
                        .setIcon(R.drawable.ic_local_library_black_24dp)
                        .setMessage(R.string.use_internet_confirmation)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(NewTargetTranslationActivity.this, ServerLibraryActivity.class);
//                                intent.putExtra(ServerLibraryActivity.ARG_SHOW_UPDATES, true);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(STATE_TARGET_TRANSLATION_ID, mNewTargetTranslationId);
        outState.putBoolean(STATE_NEW_LANGUAGE, createdNewLanguage);
        if(mSelectedTargetLanguage != null) {
            JSONObject targetLanguageJson = null;
            try {
                targetLanguageJson = mSelectedTargetLanguage.toJSON();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(targetLanguageJson != null) {
                outState.putString(STATE_TARGET_LANGUAGE, targetLanguageJson.toString());
            }
        } else {
            outState.remove(STATE_TARGET_LANGUAGE);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (NEW_LANGUAGE_REQUEST == requestCode) {
            if(RESULT_OK == resultCode) {
                String rawResponse = data.getStringExtra(NewTempLanguageActivity.EXTRA_LANGUAGE_REQUEST);
                registerTempLanguage(NewLanguageRequest.generate(rawResponse));
            } else if(RESULT_FIRST_USER == resultCode) {
                int secondResultCode = data.getIntExtra(NewTempLanguageActivity.EXTRA_RESULT_CODE, -1);
                if(secondResultCode == NewTempLanguageActivity.RESULT_MISSING_QUESTIONNAIRE) {
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.missing_questionnaire, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                } else if(secondResultCode == NewTempLanguageActivity.RESULT_USE_EXISTING_LANGUAGE) {
                    String targetLanguageId = data.getStringExtra(NewTempLanguageActivity.EXTRA_LANGUAGE_ID);
                    TargetLanguage targetLanguage = App.getLibrary().index().getTargetLanguage(targetLanguageId);
                    if(targetLanguage != null) {
                        onItemClick(targetLanguage);
                    }
                } else {
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.error, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        }
    }
}