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


public class ImportUsfmActivity extends BaseActivity implements TargetLanguageListFragment.OnItemClickListener {

    public static final int RESULT_DUPLICATE = 2;
    private static final String STATE_TARGET_LANGUAGE_ID = "state_target_language_id";
    public static final int RESULT_ERROR = 3;
    public static final String EXTRA_USFM_IMPORT_URI = "extra_usfm_import_uri";
    public static final String EXTRA_USFM_IMPORT_FILE = "extra_usfm_import_file";
    public static final String EXTRA_USFM_IMPORT_RESOURCE_FILE = "extra_usfm_import_resource_file";
    private Searchable mFragment;

    public static final String TAG = ImportUsfmActivity.class.getSimpleName();
    private TargetLanguage mTargetLanguage;
    private ProgressDialog mProgressDialog = null;
    private Thread mUsfmImportThread = null;


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

        final ImportUsfm usfm = new ImportUsfm(this, mTargetLanguage);
        processUsfmImportWithProgress(intent, args, usfm);
    }

    private void processUsfmImportWithProgress(final Intent intent, final Bundle args, final ImportUsfm usfm) {
        final Handler hand = new Handler(Looper.getMainLooper());

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setTitle(R.string.reading_usfm);
        mProgressDialog.setMessage("");
        mProgressDialog.setMax(100);
        mProgressDialog.show();

        usfm.setListener(new ImportUsfm.UpdateStatusListener() {
            @Override
            public void statusUpdate(final String textStatus, final int percent) {
                Logger.i(TAG,"Update " + textStatus + ", " + percent);
                updateProcessUsfmProgress(textStatus, percent, hand);
            }
        });

        mUsfmImportThread = new Thread() {
            @Override
            public void run() {
                boolean success = processUsfmImport( intent,  args,  usfm );

                ImportUsfm.MissingNameItem[] missingNameItems = usfm.getMissingNames();
                if(missingNameItems.length > 0) { // if we need valid names
                    handleMissingNames(missingNameItems, hand, usfm);
                } else {
                    usfmImportFinished(hand, usfm);
                }
            }
        };
        mUsfmImportThread.start();
    }

    private void handleMissingNames(final ImportUsfm.MissingNameItem[] missingNameItems, final Handler hand, final ImportUsfm usfm) {
        hand.post(new Runnable() {
            @Override
            public void run() {

                final Counter count = new Counter();
                count.setCount(missingNameItems.length);

                for (ImportUsfm.MissingNameItem missingNameItem : missingNameItems) {
                    usfmPromptForName(hand, usfm, missingNameItem, new OnPromptFinishedListener() {

                        @Override
                        public void onFinished(boolean success, String name) {
                            count.decrement();
                            if(success) {
                                // TODO: 4/13/16 process file with new name
                            }
                            if(count.isEmpty()) {
                                usfmImportFinished(hand, usfm);
                            }
                        }
                    });
                }
            }
        });
    }

    class Counter {
        private int counter;

        Counter() {
            counter = 0;
        }

        public boolean isEmpty() {
            return counter == 0;
        }

        public void setCount(int count) {
            counter = count;
        }

        public void increment() {
            counter++;
        }

        public void decrement() {
            if(counter > 0) {
                counter--;
            }
        }
    }

    private void usfmPromptForName(final Handler hand, final ImportUsfm usfm, final ImportUsfm.MissingNameItem item, final OnPromptFinishedListener listener) {

        String message = "";
        if(item.invalidName != null) {
            message = "'" + item.description + "'\nhas invalid book name: '" + item.invalidName + "'\nEnter valid name:";
        } else {
            message = "'" + item.description + "'\nis missing book name" + "\nEnter valid name:";
        }
        final CustomAlertDialog dlg = CustomAlertDialog.Create(ImportUsfmActivity.this);
        dlg.setTitle(R.string.title_activity_import_usfm)
                .setMessage(message)
                .addInputPrompt(true)
                .setPositiveButton(R.string.label_continue, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFinished(true,dlg.getEnteredText().toString());
                    }
                })
                .setNegativeButton(R.string.menu_cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFinished(false,"");
                    }
                })
                .show("getName");
    }

    private void usfmImportFinished(final Handler hand, final ImportUsfm usfm) {
        hand.post(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hide();
                usfm.showResults(new ImportUsfm.OnFinishedListener() {
                    @Override
                    public void onFinished(final boolean success) {
                        if(success) { // if user is OK to continue
                            mProgressDialog.show();
                            mProgressDialog.setProgress(0);
                            mProgressDialog.setTitle(R.string.reading_usfm);
                            mProgressDialog.setMessage("");
                            doImportingWithProgress(hand, usfm);
                        } else {
                            usfmImportDone(usfm);
                        }
                    }
                });
            }
        });
    }

    private void usfmImportDone(ImportUsfm usfm) {
        usfm.cleanup();
        mProgressDialog.dismiss();
        mProgressDialog = null;
        finished();
    }

    private void doImportingWithProgress(final Handler hand, final ImportUsfm usfm) {

        Thread thread = new Thread() {
            @Override
            public void run() {
                File[] imports = usfm.getImportProjects();
                final Translator translator = AppContext.getTranslator();
                int count = 0;
                int size = imports.length;
                try {
                    for (File newDir : imports) {

                        String filename = newDir.getName().toString();
                        String format = getResources().getString(R.string.importing_file);
                        float progress = 100f * count++ / (float) size;
                        updateProcessUsfmProgress(String.format(format, filename), Math.round(progress), hand);

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

                    updateProcessUsfmProgress("", 100, hand);

                } catch (Exception e) {
                    Logger.e(TAG, "Failed to import folder " + imports.toString());
                }

                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        usfmImportDone(usfm);
                    }
                });
            }
        };
        thread.start();
    }

    private void updateProcessUsfmProgress(final String textStatus, final int percent, Handler hand) {
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(mProgressDialog != null) {
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

    private boolean processUsfmImport(Intent intent, Bundle args, final ImportUsfm usfm) {
        boolean success = false;
        if(args.containsKey(EXTRA_USFM_IMPORT_URI)) {
            String uriStr = args.getString(EXTRA_USFM_IMPORT_URI);
            Uri uri = intent.getData();
            success = usfm.readUri( uri);
        } else if(args.containsKey(EXTRA_USFM_IMPORT_FILE)) {
            Serializable serial = args.getSerializable(EXTRA_USFM_IMPORT_FILE);
            File file = (File) serial;
            success = usfm.readFile( file);
        } else if(args.containsKey(EXTRA_USFM_IMPORT_RESOURCE_FILE)) {
            String importResourceFile = args.getString(EXTRA_USFM_IMPORT_RESOURCE_FILE);
            success = usfm.readResourceFile(importResourceFile);
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

//        finished();
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
}
