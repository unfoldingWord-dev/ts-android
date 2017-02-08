package com.door43.translationstudio.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ImportUsfm;
import com.door43.translationstudio.core.MissingNameItem;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.newtranslation.ProjectListFragment;
import com.door43.translationstudio.ui.newtranslation.TargetLanguageListFragment;
import com.door43.util.FileUtilities;

import java.io.File;
import java.io.Serializable;

import org.unfoldingword.door43client.models.TargetLanguage;

/**
 * Handles the workflow UI for importing a USFM file.
 */
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
    public static final String STATE_FINISH_SUCCESS = "state_finish_success";
    public static final int RESULT_MERGED = 2001;
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
    private AlertDialog mStatusDialog;
    private boolean mFinishedSuccess = false;
    private boolean mShuttingDown = false;
    private boolean mMergeConflict = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_usfm);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                mFragment = (Searchable) getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                setActivityStateTo(eImportState.needLanguage);
            }
        }
    }

    /**
     * process an USFM file using the selected language
     */
    private void processUsfmFile() {
        final Intent intent = getIntent();
        final Bundle args = intent.getExtras();

        mCurrentState = eImportState.processingFiles;

        mHand = new Handler(Looper.getMainLooper());
        mHand.post(new Runnable() {
            @Override
            public void run() {
                mUsfm = new ImportUsfm(ImportUsfmActivity.this, mTargetLanguage);
                setTitle(mUsfm.getLanguageTitle());
                processUsfmWithProgress(intent, args);
            }
        });
    }

    /**
     * process an USFM file using the selected language showing progress dialog
     *
     * @param intent
     * @param args
     */
    private void processUsfmWithProgress(final Intent intent, final Bundle args) {
        showProgressDialog();

        mUsfmImportThread = new Thread() {
            @Override
            public void run() {
                boolean success = beginUsfmProcessing(intent, args);

                mMissingNameItems = mUsfm.getBooksMissingNames();
                if (mMissingNameItems.length > 0) { // if we need valid names
                    mCount = new Counter(mMissingNameItems.length);
                    usfmPromptForNextName();
                } else {
                    usfmShowProcessingResults();
                }
            }
        };
        mUsfmImportThread.start();
    }

    /**
     * creates and displays progress dialog if not yet created, otherwise reuses existing dialog
     */
    private void showProgressDialog() {
        if(mShuttingDown) {
            return;
        }

        if (null == mProgressDialog) {
            mHand = new Handler(Looper.getMainLooper());

            mProgressDialog = new ProgressDialog(ImportUsfmActivity.this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setTitle(R.string.reading_usfm);
            mProgressDialog.setMessage("");
            mProgressDialog.setMax(100);
            mProgressDialog.show();

            mUsfm.setUpdateStatusListener(new ImportUsfm.UpdateStatusListener() {
                @Override
                public void statusUpdate(final String textStatus, final int percent) {
                    Logger.i(TAG, "Update " + textStatus + ", " + percent);
                    updateProcessUsfmProgress(textStatus, percent);
                }
            });
        }
    }

    /**
     * class to keep track of number of books left to prompt for resource ID
     */
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
            if (counter > 0) {
                counter--;
            }
            return counter;
        }
    }

    /**
     * will prompt for resource name of next book, or if done will move on to processing finish and import
     */
    private void usfmPromptForNextName() {

        if (mCount != null) {
            if (mCount.isEmpty()) {
                usfmShowProcessingResults();
                return;
            }

            mHand.post(new Runnable() {
                @Override
                public void run() {
                    setActivityStateTo(eImportState.promptingForBookName);
                }
            });
        }
    }

    /**
     * will display prompt to user asking if they want to select the resource name for the book
     */
    private void usfmPromptForName() {
        if (mCount != null) {
            mProgressDialog.hide();
            int i = mCount.decrement();
            final MissingNameItem item = mMissingNameItems[i];

            String message = "";
            final String description = mUsfm.getShortFilePath(item.description);
            if (item.invalidName != null) {
                String format = getResources().getString(R.string.invalid_book_name_prompt);
                message = String.format(format, description, item.invalidName);
            } else {
                String format = getResources().getString(R.string.missing_book_name_prompt);
                message = String.format(format, description);
            }

            new AlertDialog.Builder(this,R.style.AppTheme_Dialog)
                    .setTitle(R.string.title_activity_import_usfm_language)
                    .setMessage(message)
                    .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            mFragment = new ProjectListFragment();
                            ((ProjectListFragment) mFragment).setArguments(getIntent().getExtras());
                            getFragmentManager().beginTransaction().replace(R.id.fragment_container, (ProjectListFragment) mFragment).commit();
                            String title = getResources().getString(R.string.title_activity_import_usfm_book);
                            title += " " + description;
                            setTitle(title);
                            mProgressDialog.hide();
                        }
                    })
                    .setNegativeButton(R.string.menu_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            usfmPromptForNextName();
                        }
                    })
                    .setCancelable(true)
                    .show();
        }
    }

    /**
     * process selected book with specified resource name
     *
     * @param item
     * @param resourceID
     */
    private void usfmProcessBook(final MissingNameItem item, final String resourceID) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                boolean success2 = mUsfm.processText(item.contents, item.description, false, resourceID);
                Logger.i(TAG, resourceID + " success = " + success2);
                usfmPromptForNextName();
            }
        };
        thread.start();
    }

    /**
     * processing of all books in file finished, show processing results and verify
     * that user wants to import.
     */
    private void usfmShowProcessingResults() {
        if(mShuttingDown) {
            return;
        }

        mCurrentState = eImportState.showingProcessingResults;

        mHand.post(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.hide();

                    boolean processSuccess = mUsfm.isProcessSuccess();
                    String results = mUsfm.getResultsString();
                    String language = mUsfm.getLanguageTitle();
                    String message = language + "\n" + results;

                    View.OnClickListener continueListener = null;

                    AlertDialog.Builder mStatusDialog = new AlertDialog.Builder(ImportUsfmActivity.this, R.style.AppTheme_Dialog);

                    mStatusDialog.setTitle(processSuccess ? R.string.title_processing_usfm_summary : R.string.title_import_usfm_error)
                            .setMessage(message)
                            .setNegativeButton(R.string.menu_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    usfmImportDone(true);
                                }
                            });

                    if(processSuccess) { // only show continue if successful processing
                        mMergeConflict = checkForMergeConflict();
                        if(mMergeConflict) {
                            mStatusDialog.setTitle(R.string.merge_conflict_title);
                            String warning = getResources().getString(R.string.import_usfm_merge_conflict);
                            mStatusDialog.setMessage(message + "\n" + warning);
                        }

                        mStatusDialog.setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mProgressDialog.show();
                                mProgressDialog.setProgress(0);
                                mProgressDialog.setTitle(R.string.reading_usfm);
                                mProgressDialog.setMessage("");
                                doImportingWithProgress();
                            }
                        });
                    }
                    mStatusDialog.show();
                }
            }
        });
    }

    private boolean checkForMergeConflict() {
        File[] imports = mUsfm.getImportProjects();
        if((imports == null) || (imports.length < 1)) {
            return false;
        }
        TargetTranslation newTargetTranslation = TargetTranslation.open(imports[0]);
        Translator translator = App.getTranslator();

        // TRICKY: the correct id is pulled from the manifest to avoid propagating bad folder names
        String targetTranslationId = newTargetTranslation.getId();
        File localDir = new File(translator.getPath(), targetTranslationId);
        TargetTranslation localTargetTranslation = TargetTranslation.open(localDir);
        return (localTargetTranslation != null);
    }

    /**
     * import has finished
     *
     * @param cancelled
     */
    private void usfmImportDone(boolean cancelled) {
        mCurrentState = eImportState.finished;
        cleanupUsfmImport();

        if (cancelled) {
            cancelled();
        } else {
            finished();
        }
    }

    /**
     * do importing of found books with progress updates
     */
    private void doImportingWithProgress() {
        mCurrentState = eImportState.importingFiles;
        Thread thread = new Thread() {
            @Override
            public void run() {
                File[] imports = mUsfm.getImportProjects();
                final Translator translator = App.getTranslator();
                int count = 0;
                int size = imports.length;
                final int numSteps = 4;
                final float subStepSize = 100f / (float) numSteps  / (float) size;

                if(mProgressDialog != null) {
                    mProgressDialog.setTitle(R.string.importing_usfm);
                }

                boolean success = true;
                try {
                    for (File newDir : imports) {

                        String filename = newDir.getName().toString();
                        float progress = 100f * count++ / (float) size;
                        updateImportProgress(filename, progress);

                        TargetTranslation newTargetTranslation = TargetTranslation.open(newDir);
                        if (newTargetTranslation != null) {
                            newTargetTranslation.commitSync();

                            updateImportProgress(filename, progress + subStepSize);

                            // TRICKY: the correct id is pulled from the manifest to avoid propagating bad folder names
                            String targetTranslationId = newTargetTranslation.getId();
                            File localDir = new File(translator.getPath(), targetTranslationId);
                            TargetTranslation localTargetTranslation = TargetTranslation.open(localDir);
                            if (localTargetTranslation != null) {
                                // commit local changes to history
                                if (localTargetTranslation != null) {
                                    localTargetTranslation.commitSync();
                                }

                                updateImportProgress(filename, progress + 2*subStepSize);

                                // merge translations
                                try {
                                    localTargetTranslation.merge(newDir);
                                } catch (Exception e) {
                                    Logger.e(TAG, "Failed to merge import folder " + newDir.toString(), e);
                                    success = false;
                                    continue;
                                }
                            } else {
                                // import new translation
                                FileUtilities.safeDelete(localDir); // in case local was an invalid target translation
                                FileUtilities.moveOrCopyQuietly(newDir, localDir);
                            }
                            // update the generator info. TRICKY: we re-open to get the updated manifest.
                            TargetTranslation.updateGenerator(ImportUsfmActivity.this, TargetTranslation.open(localDir));
                        }
                    }

                    updateProcessUsfmProgress("", 100);

                } catch (Exception e) {
                    Logger.e(TAG, "Failed to import folder " + imports.toString(), e);
                    success = false;
                }

                mFinishedSuccess = success;
                mHand.post(new Runnable() {
                    @Override
                    public void run() {
                        usfmShowImportResults();
                    }
                });
            }
        };
        thread.start();
    }

    /**
     * update the import progress dialog
     * @param filename
     * @param progress
     */
    private void updateImportProgress(String filename, float progress) {
        String format = getResources().getString(R.string.importing_file);
        updateProcessUsfmProgress(String.format(format, filename), Math.round(progress));
    }


    /**
     * show results of import
     */
    private void usfmShowImportResults() {
        if(mShuttingDown) {
            return;
        }

        mCurrentState = eImportState.showingImportResults;

        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(mFinishedSuccess ? R.string.title_import_usfm_results : R.string.title_import_usfm_error)
                .setMessage(mFinishedSuccess ? R.string.import_usfm_success : R.string.import_usfm_failed)
                .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        usfmImportDone(false);
                    }
                })
                .show();

        cleanupUsfmImport();
    }

    /**
     * called to display progress of USFM processing or importing
     *
     * @param textStatus
     * @param percent
     */
    private void updateProcessUsfmProgress(final String textStatus, final int percent) {
        if (mHand != null) {
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

    /**
     * begin USFM processing using type passed (URI, File, or resource)
     *
     * @param intent
     * @param args
     * @return
     */
    private boolean beginUsfmProcessing(Intent intent, Bundle args) {
        boolean success = false;
        if (args.containsKey(EXTRA_USFM_IMPORT_URI)) {
            String uriStr = args.getString(EXTRA_USFM_IMPORT_URI);
            Uri uri = intent.getData();
            success = mUsfm.readUri(uri);
        } else if (args.containsKey(EXTRA_USFM_IMPORT_FILE)) {
            Serializable serial = args.getSerializable(EXTRA_USFM_IMPORT_FILE);
            File file = (File) serial;
            success = mUsfm.readFile(file);
        } else if (args.containsKey(EXTRA_USFM_IMPORT_RESOURCE_FILE)) {
            String importResourceFile = args.getString(EXTRA_USFM_IMPORT_RESOURCE_FILE);
            success = mUsfm.readResourceFile(this, importResourceFile);
        }

        return success;
    }

    /**
     * begins activity to process and import a file
     *
     * @param context
     * @param path
     */
    public static void startActivityForFileImport(Activity context, File path) {
        Intent intent = new Intent(context, ImportUsfmActivity.class);
        intent.putExtra(EXTRA_USFM_IMPORT_FILE, path);
        context.startActivity(intent);
    }

    /**
     * begins an activity to process and import a Uri
     *
     * @param context
     * @param uri
     */
    public static void startActivityForUriImport(Activity context, Uri uri) {
        Intent intent = new Intent(context, ImportUsfmActivity.class);
        intent.putExtra(EXTRA_USFM_IMPORT_URI, uri.toString()); // flag that we are using Uri
        intent.setData(uri); // only way to pass data since Uri does not serialize
        context.startActivity(intent);
    }

    /**
     * begins an activity to process and import a resource
     *
     * @param context
     * @param resourceName
     */
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
        if (mFragment instanceof ProjectListFragment) {
            menu.findItem(R.id.action_update).setVisible(true);
        } else {
            menu.findItem(R.id.action_update).setVisible(false);
        }
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
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

        switch (id) {
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
                // TODO: 10/18/16 display dialog for updating
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mShuttingDown = false;
        if (savedInstanceState != null) {
            String targetLanguageId = savedInstanceState.getString(STATE_TARGET_LANGUAGE_ID, null);
            if (targetLanguageId != null) {
                mTargetLanguage = App.getLibrary().index().getTargetLanguage(targetLanguageId);
            }

            mCurrentState = eImportState.fromInt(savedInstanceState.getInt(STATE_CURRENT_STATE, eImportState.needLanguage.getValue()));

            String usfmStr = savedInstanceState.getString(STATE_USFM, null);
            if (usfmStr != null) {
                mUsfm = ImportUsfm.newInstance(this, usfmStr);
            }

            if (savedInstanceState.containsKey(STATE_PROMPT_NAME_COUNTER) && (mUsfm != null)) {
                int count = savedInstanceState.getInt(STATE_PROMPT_NAME_COUNTER);
                mCount = new Counter(count + 1); // backup one
                mMissingNameItems = mUsfm.getBooksMissingNames();
            }

            mFinishedSuccess = savedInstanceState.getBoolean(STATE_FINISH_SUCCESS, false);

            mHand = new Handler(Looper.getMainLooper());
            mHand.post(new Runnable() {
                @Override
                public void run() {
                    setActivityStateTo(mCurrentState);
                }
            });
        }
    }

    /**
     * update UI for specified state (e.g. prompt for language, book name selection, display processing results...)
     * and begin that state
     *
     * @param currentState
     */
    private void setActivityStateTo(eImportState currentState) {
        if(mShuttingDown) {
            return;
        }

        Logger.i(TAG, "setActivityStateTo(" + currentState + ")");
        mCurrentState = currentState;

        if (mUsfm != null) {
            setTitle(mUsfm.getLanguageTitle());
        }

        switch (currentState) {
            case needLanguage:
                if (null == mFragment) {
                    mFragment = new TargetLanguageListFragment();
                    ((TargetLanguageListFragment) mFragment).setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, (TargetLanguageListFragment) mFragment).commit();
                    // TODO: animate
                }
                break;

            case processingFiles:
                if((mUsfm != null) && (mTargetLanguage != null)) {
                    processUsfmFile();
                    break;
                }

            case promptingForBookName:
                if (mCount != null) {
                    showProgressDialog();
                    usfmPromptForName();
                    break;
                }
                // otherwise we go down to showing results

            case showingProcessingResults:
                showProgressDialog();
                usfmShowProcessingResults();
                break;

            case showingImportResults:
                usfmShowImportResults();
                break;

            case importingFiles:
                // not resumable - presume completed
                mCurrentState = eImportState.finished;

            case finished:
                if(mUsfm != null) {
                    mUsfm.cleanup();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        switch (mCurrentState) {
            case needLanguage:
                cancelled();
                break;

            case promptingForBookName:
                setBook(null);
                break;

            case showingProcessingResults:
            case processingFiles:
                if (mUsfm != null) {
                    mUsfm.cleanup();
                }
                setActivityStateTo(eImportState.needLanguage);
                break;

            case showingImportResults:
                usfmImportDone(false);
                break;

            default:
                // not backup-able - presume completed
                break;
        }
    }

    public void onSaveInstanceState(Bundle outState) {

        mShuttingDown = true;

        if(mStatusDialog != null) {
            mStatusDialog.dismiss();
        }
        mStatusDialog = null;

        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;

        eImportState currentState = mCurrentState; //capture state before it is changed
        outState.putInt(STATE_CURRENT_STATE, currentState.getValue());

        if (mTargetLanguage != null) {
            outState.putString(STATE_TARGET_LANGUAGE_ID, mTargetLanguage.slug);
        } else {
            outState.remove(STATE_TARGET_LANGUAGE_ID);
        }

        if (mUsfm != null) { //save state and make sure it's not running
            outState.putString(STATE_USFM, mUsfm.toJson().toString());
            mUsfm.setUpdateStatusListener(null);
            mUsfm.setCancel(true);

            if((currentState == eImportState.processingFiles) // if doing initial processing, we clean up and start over
                    || (currentState == eImportState.finished)) { // if finished we cleanup
                mUsfm.cleanup();
            }
         } else {
            outState.remove(STATE_USFM);
        }

        if (mCount != null) {
            outState.putInt(STATE_PROMPT_NAME_COUNTER, mCount.counter);
        } else {
            outState.remove(STATE_PROMPT_NAME_COUNTER);
        }

        outState.putBoolean(STATE_FINISH_SUCCESS, mFinishedSuccess);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(TargetLanguage targetLanguage) {
        mTargetLanguage = targetLanguage;

        if (null != targetLanguage) {
            getFragmentManager().beginTransaction().remove((TargetLanguageListFragment) mFragment).commit();
            mFragment = null;
            processUsfmFile();
        } else {
            cancelled();
        }
    }

    @Override
    public void onItemClick(String projectId) {
        setBook(projectId);
    }

    /**
     * use the project ID
     *
     * @param projectId
     */
    private void setBook(String projectId) {
        if (projectId != null) {
            getFragmentManager().beginTransaction().remove((ProjectListFragment) mFragment).commit();
            mFragment = null;
            mProgressDialog.show();
            final MissingNameItem item = mMissingNameItems[mCount.counter];
            usfmProcessBook(item, projectId);
        } else { //book cancelled
            usfmPromptForNextName();
        }
    }

    /**
     * user cancelled import
     */
    private void cancelled() {
        cleanupUsfmImport();

        Intent data = new Intent();
        setResult(RESULT_CANCELED, data);
        finish();
    }

    /**
     * user completed import
     */
    private void finished() {
        cleanupUsfmImport();

        Intent data = new Intent();
        setResult(mMergeConflict ? RESULT_MERGED : RESULT_OK, data);
        finish();
    }

    private void cleanupUsfmImport() {
        if (mUsfm != null) {
            mUsfm.cleanup();
            mUsfm = null;
        }
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public interface OnFinishedListener {
        void onFinished(boolean success);
    }

    public interface OnPromptFinishedListener {
        void onFinished(boolean success, String name);
    }

    /**
     * enum that keeps track of current state of USFM import
     */
    public enum eImportState {
        needLanguage(0),
        processingFiles(1),
        promptingForBookName(2),
        showingProcessingResults(3),
        importingFiles(4),
        showingImportResults(5),
        finished(6);


        private int _value;

        eImportState(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static eImportState fromInt(int i) {
            for (eImportState b : eImportState.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}
