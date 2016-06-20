package com.door43.translationstudio.newui.newtranslation;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.translationstudio.core.Questionnaire;
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
import com.door43.translationstudio.newui.newlanguage.NewTempLanguageActivity;
import com.door43.util.StringUtilities;
import com.door43.widget.ViewUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class NewTargetTranslationActivity extends BaseActivity implements TargetLanguageListFragment.OnItemClickListener, ProjectListFragment.OnItemClickListener {

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_target_translation);

        if(savedInstanceState != null) {
            createdNewLanguage = savedInstanceState.getBoolean(STATE_NEW_LANGUAGE, false);
            if (savedInstanceState.containsKey(STATE_TARGET_TRANSLATION_ID)) {
                mNewTargetTranslationId = (String) savedInstanceState.getSerializable(STATE_TARGET_TRANSLATION_ID);
            }

            if (savedInstanceState.containsKey(STATE_TARGET_LANGUAGE)) {
                String targetLanguageJsonStr = savedInstanceState.getString(STATE_TARGET_LANGUAGE);
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
            Questionnaire questionnaire = AppContext.getLibrary().getQuestionnaire(request.questionnaireId);
            if (questionnaire != null && AppContext.addNewLanguageRequest(request)) {
                mSelectedTargetLanguage = request.getTempTargetLanguage();
                this.createdNewLanguage = true;
                confirmTempLanguage(mSelectedTargetLanguage);
                return;
            }
        }
        CustomAlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.try_again)
                .show("error-questionnaire");
    }

    /**
     * Displays a confirmation for the new language
     * @param language
     */
    private void confirmTempLanguage(final TargetLanguage language) {
        if(language != null) {
            String msg = String.format(getResources().getString(R.string.new_language_confirmation), language.getId(), language.name);
            final CustomAlertDialog dialog = CustomAlertDialog.Builder(this)
                    .setCancelableChainable(false)
                    .setAutoDismiss(false)
                    .setTitle(R.string.language)
                    .setMessage(msg);
            dialog.setPositiveButton(R.string.label_continue, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    onItemClick(language);
                }
            })
            .setNeutralButton(R.string.copy, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StringUtilities.copyToClipboard(NewTargetTranslationActivity.this, language.code);
                    Snackbar snack = Snackbar.make(v, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            });
            dialog.show(NEW_LANGUAGE_CONFIRMATION);
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
        String resourceSlug = projectId.equals("obs") ? "obs" : Resource.REGULAR_SLUG;
        TargetTranslation existingTranslation = translator.getTargetTranslation(TargetTranslation.generateTargetTranslationId(mSelectedTargetLanguage.getId(), projectId, TranslationType.TEXT, resourceSlug));
        if(existingTranslation == null) {
            // create new target translation
            SourceLanguage sourceLanguage = AppContext.getLibrary().getPreferredSourceLanguage(projectId, Locale.getDefault().getLanguage()); // get project name
            // TODO: 3/2/2016 eventually the format will be specified in the project
            SourceTranslation sourceTranslation = AppContext.getLibrary().getDefaultSourceTranslation(projectId, sourceLanguage.getId());
            final TargetTranslation targetTranslation = AppContext.getTranslator().createTargetTranslation(AppContext.getProfile().getNativeSpeaker(), mSelectedTargetLanguage, projectId, TranslationType.TEXT, resourceSlug, sourceTranslation.getFormat());
            if(targetTranslation != null) {
                // deploy custom language code request to the translation
                NewLanguageRequest request = AppContext.getNewLanguageRequest(mSelectedTargetLanguage.getId());
                if(request != null) {
                    try {
                        targetTranslation.setNewLanguageRequest(request);
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "Failed to deploy the new language code request", e);
                    }
                }

                newProjectCreated(targetTranslation);
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
                CustomAlertDialog.Builder(this)
                        .setTitle(R.string.title_new_language_code)
                        .setMessage(R.string.confirm_start_new_language_code)
                        .setPositiveButton(R.string.label_continue, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent requestNewLangaugeIntent = new Intent(NewTargetTranslationActivity.this, NewTempLanguageActivity.class);
                                startActivityForResult(requestNewLangaugeIntent, NEW_LANGUAGE_REQUEST);
                            }
                        })
                        .setNegativeButton(R.string.title_cancel, null)
                        .show("confirm-start-new-language");
                return true;
            case R.id.action_update:
                CustomAlertDialog.Builder(this)
                        .setTitle(R.string.update_library)
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
        outState.putBoolean(STATE_NEW_LANGUAGE, createdNewLanguage);
        if(mSelectedTargetLanguage != null) {
            JSONObject targetLanguageJson = mSelectedTargetLanguage.toApiFormatJson();
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
                    TargetLanguage targetLanguage = AppContext.getLibrary().getTargetLanguage(targetLanguageId);
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