package com.door43.translationstudio.newui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.ImportUsfm;
import com.door43.translationstudio.core.MissingNameItem;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.newui.library.Searchable;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.newui.newtranslation.ProjectListFragment;
import com.door43.translationstudio.newui.newtranslation.TargetLanguageListFragment;
import com.door43.util.FileUtilities;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.Serializable;


public class ImportUsfmActivity extends BaseActivity implements TargetLanguageListFragment.OnItemClickListener, ProjectListFragment.OnItemClickListener {

    public static final int RESULT_DUPLICATE = 2;
    private static final String STATE_TARGET_LANGUAGE_ID = "state_target_language_id";
    public static final int RESULT_ERROR = 3;
    public static final String EXTRA_USFM_IMPORT_URI = "extra_usfm_import_uri";
    public static final String EXTRA_USFM_IMPORT_FILE = "extra_usfm_import_file";
    public static final String EXTRA_USFM_IMPORT_RESOURCE_FILE = "extra_usfm_import_resource_file";
    public static final String STATE_USFM = "state_usfm";
    public static final String STATE_CURRENT_STATE = "state_current_state";
    public static final String STATE_PROMPT_NAME_COUNTER = "state_prompt_name_counter";
    private Searchable mFragment;

    public static final String TAG = ImportUsfmActivity.class.getSimpleName();
    private TargetLanguage mTargetLanguage;
    private ProgressDialog mProgressDialog = null;
    private Thread mUsfmImportThread = null;
    private Counter mCount;
    private MissingNameItem[] mMissingNameItems;
    private ImportUsfm mUsfm;
    private Handler mHand;
    private eImportState mCurrentState = eImportState.needLanguage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_usfm);

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (Searchable)getFragmentManager().findFragmentById(R.id.fragment_container);
            }
        }
    }

    private void doFileImport() {
        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        mCurrentState = eImportState.processingFiles;

        mUsfm = new ImportUsfm(this, mTargetLanguage);
        processUsfmImportWithProgress(intent, args);
    }

    private void processUsfmImportWithProgress(final Intent intent, final Bundle args) {
        showProgressDialog();

        mUsfmImportThread = new Thread() {
            @Override
            public void run() {
                boolean success = processUsfmImport( intent,  args );

                mMissingNameItems = mUsfm.getMissingNames();
                if(mMissingNameItems.length > 0) { // if we need valid names
                    mCount = new Counter(mMissingNameItems.length);
                    usfmPromptForNextName();
                } else {
                    usfmProcessFileFinished();
                }
            }
        };
        mUsfmImportThread.start();
    }

    private void showProgressDialog() {
        if(null == mProgressDialog) {
            mHand = new Handler(Looper.getMainLooper());

            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setTitle(R.string.reading_usfm);
            mProgressDialog.setMessage("");
            mProgressDialog.setMax(100);
            mProgressDialog.show();

            mUsfm.setListener(new ImportUsfm.UpdateStatusListener() {
                @Override
                public void statusUpdate(final String textStatus, final int percent) {
                    Logger.i(TAG,"Update " + textStatus + ", " + percent);
                    updateProcessUsfmProgress(textStatus, percent);
                }
            });
        }
    }

    private class Counter {
        public int counter;

        Counter(int initialCount) {
            counter = initialCount;
        }

        public boolean isEmpty() {
            return counter == 0;
        }

        public void setCount(int count) {
            counter = count;
        }

        public int increment() {
            return ++counter;
        }

        public int decrement() {
            if(counter > 0) {
                counter--;
            }
            return counter;
        }
    }

    private void usfmPromptForNextName() {

        if(mCount != null) {
            if (mCount.isEmpty()) {
                usfmProcessFileFinished();
                return;
            }

            mHand.post(new Runnable() {
                @Override
                public void run() {

                    usfmPromptForName();
                }
            });
        }
    }

    private void usfmPromptForName() {
        if(mCount != null) {
            mCurrentState = eImportState.promptingForBookName;
            int i = mCount.decrement();
            final MissingNameItem item = mMissingNameItems[i];

            String message = "";
            if (item.invalidName != null) {
                message = "'" + item.description + "'\nhas invalid book name: '" + item.invalidName + "'\nDo you wish to select a book name?";
            } else {
                message = "'" + item.description + "'\nis missing book name" + "\nDo you wish to select a book name?";
            }

            final CustomAlertDialog dlg = CustomAlertDialog.Create(ImportUsfmActivity.this);
            dlg.setTitle(R.string.title_activity_import_usfm_language)
                    .setMessage(message)
                    .setPositiveButton(R.string.label_continue, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mFragment = new ProjectListFragment();
                            ((ProjectListFragment) mFragment).setArguments(getIntent().getExtras());
                            getFragmentManager().beginTransaction().replace(R.id.fragment_container, (ProjectListFragment) mFragment).commit();
                            String title = getResources().getString(R.string.title_activity_import_usfm_book);
                            String book = item.description;
                            String[] sections = item.description.split("/");
                            if(sections.length > 0) {
                                book = sections[sections.length - 1];
                            }
                            title += " " + book;
                            setTitle(title);
                            mProgressDialog.hide();
                        }
                    })
                    .setNegativeButton(R.string.menu_cancel, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            usfmPromptForNextName();
                        }
                    })
                    .show("getName");
        }
    }

    private void usfmProcessBook(final MissingNameItem item, final String book) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                boolean success2 = mUsfm.readText(item.contents, item.description, false, book);
                Logger.i(TAG, book + " success = " + success2);
                usfmPromptForNextName();
            }
        };
        thread.start();
    }

    private void usfmProcessFileFinished() {
        mCurrentState = eImportState.showingResults;

        mHand.post(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hide();

                mUsfm.showResults(new ImportUsfm.OnFinishedListener() {
                    @Override
                    public void onFinished(final boolean success) {
                        if(success) { // if user is OK to continue
                            mProgressDialog.show();
                            mProgressDialog.setProgress(0);
                            mProgressDialog.setTitle(R.string.reading_usfm);
                            mProgressDialog.setMessage("");
                            doImportingWithProgress();
                        } else {
                            usfmImportDone();
                        }
                    }
                });
            }
        });
    }

    private void usfmImportDone() {
        mCurrentState = eImportState.finished;
        mUsfm.cleanup();
        mProgressDialog.dismiss();
        mProgressDialog = null;
        finished();
    }

    private void doImportingWithProgress() {
        mCurrentState = eImportState.importingFiles;
        Thread thread = new Thread() {
            @Override
            public void run() {
                File[] imports = mUsfm.getImportProjects();
                final Translator translator = AppContext.getTranslator();
                int count = 0;
                int size = imports.length;
                try {
                    for (File newDir : imports) {

                        String filename = newDir.getName().toString();
                        String format = getResources().getString(R.string.importing_file);
                        float progress = 100f * count++ / (float) size;
                        updateProcessUsfmProgress(String.format(format, filename), Math.round(progress));

                        TargetTranslation newTargetTranslation = TargetTranslation.open(newDir);
                        if (newTargetTranslation != null) {
                            // TRICKY: the correct id is pulled from the manifest to avoid propagating bad folder names
                            String targetTranslationId = newTargetTranslation.getId();
                            File localDir = new File(translator.getPath(), targetTranslationId);
                            TargetTranslation localTargetTranslation = TargetTranslation.open(localDir);
                            if (localTargetTranslation != null) {
                                // commit local changes to history
                                if (localTargetTranslation != null) {
                                    localTargetTranslation.commitSync();
                                }

                                // merge translations
                                try {
                                    localTargetTranslation.merge(newDir);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    continue;
                                }
                            } else {
                                // import new translation
                                FileUtilities.safeDelete(localDir); // in case local was an invalid target translation
                                FileUtils.moveDirectory(newDir, localDir);
                            }
                            // update the generator info. TRICKY: we re-open to get the updated manifest.
                            TargetTranslation.updateGenerator(ImportUsfmActivity.this, TargetTranslation.open(localDir));
                        }
                    }

                    updateProcessUsfmProgress("", 100);

                } catch (Exception e) {
                    Logger.e(TAG, "Failed to import folder " + imports.toString());
                }

                mHand.post(new Runnable() {
                    @Override
                    public void run() {
                        usfmImportDone();
                    }
                });
            }
        };
        thread.start();
    }

    private void updateProcessUsfmProgress(final String textStatus, final int percent) {
        if(mHand != null) {
            mHand.post(new Runnable() {
                @Override
                public void run() {
                    if (mProgressDialog != null) {
                        if (null != textStatus) {
                            mProgressDialog.setMessage(textStatus);
                        }

                        int percentStatus = percent;
                        if (percentStatus > 100) {
                            percentStatus = 100;
                        } else if (percentStatus < 0) {
                            percentStatus = 0;
                        }

                        mProgressDialog.setProgress(percentStatus);
                    }
                }
            });
        }
    }

    private boolean processUsfmImport(Intent intent, Bundle args) {
        boolean success = false;
        if(args.containsKey(EXTRA_USFM_IMPORT_URI)) {
            String uriStr = args.getString(EXTRA_USFM_IMPORT_URI);
            Uri uri = intent.getData();
            success = mUsfm.readUri( uri);
        } else if(args.containsKey(EXTRA_USFM_IMPORT_FILE)) {
            Serializable serial = args.getSerializable(EXTRA_USFM_IMPORT_FILE);
            File file = (File) serial;
            success = mUsfm.readFile( file);
        } else if(args.containsKey(EXTRA_USFM_IMPORT_RESOURCE_FILE)) {
            String importResourceFile = args.getString(EXTRA_USFM_IMPORT_RESOURCE_FILE);
            success = mUsfm.readResourceFile(importResourceFile);
        }

        return success;
    }

    public static void startActivityForFileImport(Activity context, File path) {
        Intent intent = new Intent(context, ImportUsfmActivity.class);
        intent.putExtra(EXTRA_USFM_IMPORT_FILE, path);
        context.startActivity(intent);
    }

    public static void startActivityForUriImport(Activity context, Uri uri) {
        Intent intent = new Intent(context, ImportUsfmActivity.class);
        intent.putExtra(EXTRA_USFM_IMPORT_URI, uri.toString()); // flag that we are using Uri
        intent.setData(uri); // only way to pass data since Uri does not serialize
        context.startActivity(intent);
    }

    public static void startActivityForResourceImport(Activity context, String resourceName) {
        Intent intent = new Intent(context, ImportUsfmActivity.class);
        intent.putExtra(EXTRA_USFM_IMPORT_RESOURCE_FILE, resourceName);
        context.startActivity(intent);
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
            case R.id.home:
                onBackPressed();
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

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            String targetLanguageId = savedInstanceState.getString(STATE_TARGET_LANGUAGE_ID, null);
            if(targetLanguageId != null) {
                mTargetLanguage = AppContext.getLibrary().getTargetLanguage(targetLanguageId);
            }

            mCurrentState = eImportState.fromInt(savedInstanceState.getInt(STATE_CURRENT_STATE, eImportState.needLanguage.getValue()));

            String usfmStr = savedInstanceState.getString(STATE_USFM, null);
            if(usfmStr != null) {
                mUsfm = ImportUsfm.generate(this, usfmStr);
            }

            if(savedInstanceState.containsKey(STATE_PROMPT_NAME_COUNTER)) {
                int count = savedInstanceState.getInt(STATE_PROMPT_NAME_COUNTER);
                mCount = new Counter(count + 1); // backup one
                mMissingNameItems = mUsfm.getMissingNames();
            }
        }

        startState(mCurrentState);
    }

    private void startState(eImportState currentState) {
        mCurrentState = currentState;

        switch (currentState) {
            case needLanguage:
                if(null == mFragment) {
                    mFragment = new TargetLanguageListFragment();
                    ((TargetLanguageListFragment) mFragment).setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, (TargetLanguageListFragment) mFragment).commit();
                    // TODO: animate
                }
                break;

            case processingFiles:
                // not resumable - presume completed, proceed to next

            case promptingForBookName:
                if(mCount != null) {
                    showProgressDialog();
                    usfmPromptForName();
                    break;
                }
                // otherwise we go down to showing results

            case showingResults:
                showProgressDialog();
                usfmProcessFileFinished();
                break;

            case importingFiles:
                // not resumable - presume completed
                break;
        }
    }

    @Override
    public void onBackPressed() {
        switch (mCurrentState) {
            case needLanguage:
                cancelled();
                break;

            case showingResults:
            case promptingForBookName:
            case processingFiles:
                if(mUsfm != null) {
                    mUsfm.cleanup();
                }
                startState(eImportState.needLanguage);
                break;

            default:
                // not backup-able - presume completed
                break;
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if(mTargetLanguage != null) {
            outState.putString(STATE_TARGET_LANGUAGE_ID, mTargetLanguage.getId());
        } else {
            outState.remove(STATE_TARGET_LANGUAGE_ID);
        }

        if(mUsfm != null) {
            outState.putString(STATE_USFM, mUsfm.toJson().toString());
        } else {
            outState.remove(STATE_USFM);
        }

        if(mCount != null) {
            outState.putInt(STATE_PROMPT_NAME_COUNTER, mCount.counter);
        } else {
            outState.remove(STATE_PROMPT_NAME_COUNTER);
        }


        outState.putInt(STATE_CURRENT_STATE, mCurrentState.getValue());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(TargetLanguage targetLanguage) {
        mTargetLanguage = targetLanguage;

        if(null != targetLanguage) {
            getFragmentManager().beginTransaction().remove((TargetLanguageListFragment) mFragment).commit();
            mFragment = null;
            doFileImport();
        }
        else {
            cancelled();
        }
    }

    @Override
    public void onItemClick(String projectId) {

        getFragmentManager().beginTransaction().remove((ProjectListFragment) mFragment).commit();
        mFragment = null;
        mProgressDialog.show();
        final MissingNameItem item = mMissingNameItems[mCount.counter];
        usfmProcessBook(item, projectId);
    }

    private void cancelled() {
        Intent data = new Intent();
        setResult(RESULT_CANCELED, data);
        finish();
    }

    private void finished() {
        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();
    }

    public interface OnFinishedListener {
        void onFinished(boolean success);
    }

    public interface OnPromptFinishedListener {
        void onFinished(boolean success, String name);
    }

    public enum eImportState {
        needLanguage(0),
        processingFiles(1),
        promptingForBookName(2),
        showingResults(3),
        importingFiles(4),
        finished(5);


        private int _value;

        eImportState(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static eImportState fromInt(int i) {
            for (eImportState b : eImportState.values()) {
                if (b.getValue() == i) { return b; }
            }
            return null;
        }
    }
}
