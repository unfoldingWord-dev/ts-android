package com.door43.translationstudio.newui.home;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.TargetLanguageUtility;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.newlanguage.NewTargetTranslationActivity;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.AppContext;
import com.door43.widget.ViewUtil;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Locale;

public class HomeActivity extends BaseActivity implements WelcomeFragment.OnCreateNewTargetTranslation, TargetTranslationListFragment.OnItemClickListener {
    private static final int REQUEST_CODE_STORAGE_ACCESS = 42;
    private static final int NEW_TARGET_TRANSLATION_REQUEST = 1;
    public static final String TAG = HomeActivity.class.getSimpleName();
    private Library mLibrary;
    private Translator mTranslator;
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FloatingActionButton addTranslationButton = (FloatingActionButton) findViewById(R.id.addTargetTranslationButton);
        addTranslationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateNewTargetTranslation();
            }
        });

        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                // use current fragment
                mFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                if (mTranslator.getTargetTranslations().length > 0) {
                    mFragment = new TargetTranslationListFragment();
                    mFragment.setArguments(getIntent().getExtras());
                } else {
                    mFragment = new WelcomeFragment();
                    mFragment.setArguments(getIntent().getExtras());
                }

                getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
            }
        }

        ImageButton moreButton = (ImageButton)findViewById(R.id.action_more);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu moreMenu = new PopupMenu(HomeActivity.this, v);
                ViewUtil.forcePopupMenuIcons(moreMenu);
                moreMenu.getMenuInflater().inflate(R.menu.menu_home, moreMenu.getMenu());
                moreMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_update:
                                openLibrary();
                                return true;
                            case R.id.action_import:
                                FragmentTransaction backupFt = getFragmentManager().beginTransaction();
                                Fragment backupPrev = getFragmentManager().findFragmentByTag(ImportDialog.TAG);
                                if (backupPrev != null) {
                                    backupFt.remove(backupPrev);
                                }
                                backupFt.addToBackStack(null);

                                ImportDialog importDialog = new ImportDialog();
                                importDialog.show(backupFt, ImportDialog.TAG);
                                return true;
                            case R.id.action_feedback:
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                Fragment prev = getFragmentManager().findFragmentByTag("bugDialog");
                                if (prev != null) {
                                    ft.remove(prev);
                                }
                                ft.addToBackStack(null);

                                FeedbackDialog dialog = new FeedbackDialog();
                                dialog.show(ft, "bugDialog");

//                                CustomAlertDialog.test(HomeActivity.this);

                                return true;
                            case R.id.action_share_apk:
                                try {
                                    PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                    File apkFile = new File(pinfo.applicationInfo.publicSourceDir);
                                    File exportFile = new File(AppContext.getSharingDir(), pinfo.applicationInfo.loadLabel(getPackageManager()) + "_" + pinfo.versionName + ".apk");
                                    FileUtils.copyFile(apkFile, exportFile);
                                    if (exportFile.exists()) {
                                        Uri u = FileProvider.getUriForFile(HomeActivity.this, "com.door43.translationstudio.fileprovider", exportFile);
                                        Intent i = new Intent(Intent.ACTION_SEND);
                                        i.setType("application/zip");
                                        i.putExtra(Intent.EXTRA_STREAM, u);
                                        startActivity(Intent.createChooser(i, getResources().getString(R.string.send_to)));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    // todo notify user app could not be shared
                                }
                                return true;
                            case R.id.action_settings:
                                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                                startActivity(intent);
                                return true;
                        }
                        return false;
                    }
                });
                moreMenu.show();
            }
        });

        Intent intent = getIntent();
        if(intent != null) {
            String action = intent.getAction();
            if(action != null) {
                if (action.compareTo(Intent.ACTION_VIEW) == 0 || action.compareTo(Intent.ACTION_DEFAULT) == 0) {
                    String scheme = intent.getScheme();
                    ContentResolver resolver = getContentResolver();

                    if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
                        // TODO: 2/10/2016 confirm with user that they want to import the file
                    }
                }
                return;
            }
        }

        // open last project when starting the first time
        if (savedInstanceState == null) {
            TargetTranslation targetTranslation = getLastOpened();
            if (targetTranslation != null) {
                onItemClick(targetTranslation);
                return;
            }
        }
    }

    /**
     * Triggers the process of opening the server library
     */
    private void openLibrary() {
        CustomAlertDialog.Create(HomeActivity.this)
            .setTitle(R.string.update_projects)
            .setIcon(R.drawable.ic_local_library_black_24dp)
            .setMessage(R.string.use_internet_confirmation)
            .setPositiveButton(R.string.yes, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, ServerLibraryActivity.class);
                    startActivity(intent);
                }
            })
            .setNegativeButton(R.string.no, null)
            .show("UpdateLib");
    }

    @Override
    public void onResume() {
        super.onResume();

        int numTranslations = mTranslator.getTargetTranslations().length;
        if(numTranslations > 0 && mFragment instanceof WelcomeFragment) {
            // display target translations list
            mFragment = new TargetTranslationListFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
        } else if(numTranslations == 0 && mFragment instanceof TargetTranslationListFragment) {
            // display welcome screen
            mFragment = new WelcomeFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
        } else if(numTranslations > 0 && mFragment instanceof TargetTranslationListFragment) {
            // reload list
            ((TargetTranslationListFragment)mFragment).reloadList();
        }
    }

    /**
     * get last project opened and make sure it is still present
     * @return
     */
    @Nullable
    private TargetTranslation getLastOpened() {
        String lastTarget = AppContext.getLastFocusTargetTranslation();
        if (lastTarget != null) {
            TargetTranslation targetTranslation = mTranslator.getTargetTranslation(lastTarget);
            if (targetTranslation != null) {
                return targetTranslation;
            }
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        // display confirmation before closing the app
        CustomAlertDialog.Create(this)
                .setMessage(R.string.exit_confirmation)
                .setPositiveButton(R.string.yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show("ExitConfirm");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(NEW_TARGET_TRANSLATION_REQUEST == requestCode ) {
            if(RESULT_OK == resultCode ) {
                if(mFragment instanceof WelcomeFragment) {
                    // display target translations list
                    mFragment = new TargetTranslationListFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                } else {
                    ((TargetTranslationListFragment) mFragment).reloadList();
                }

                Intent intent = new Intent(HomeActivity.this, TargetTranslationActivity.class);
                intent.putExtra(AppContext.EXTRA_TARGET_TRANSLATION_ID, data.getStringExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID));
                startActivity(intent);
            } else if( NewTargetTranslationActivity.RESULT_DUPLICATE == resultCode ) {
                // display duplicate notice to user
                String targetTranslationId = data.getStringExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID);
                TargetTranslation existingTranslation = mTranslator.getTargetTranslation(targetTranslationId);
                if(existingTranslation != null) {
                    Project project = mLibrary.getProject(existingTranslation.getProjectId(), Locale.getDefault().getLanguage());
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), String.format(getResources().getString(R.string.duplicate_target_translation), project.name, existingTranslation.getTargetLanguageName()), Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            } else if( NewTargetTranslationActivity.RESULT_ERROR == resultCode) {
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.error), Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }
    }

    @Override
    public void onCreateNewTargetTranslation() {
        Intent intent = new Intent(HomeActivity.this, NewTargetTranslationActivity.class);
        startActivityForResult(intent, NEW_TARGET_TRANSLATION_REQUEST);
    }

    @Override
    public void onItemDeleted(String targetTranslationId) {
        if(mTranslator.getTargetTranslations().length > 0) {
            ((TargetTranslationListFragment) mFragment).reloadList();
        } else {
            // display welcome screen
            mFragment = new WelcomeFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
        }
    }

    @Override
    public void onItemClick(TargetTranslation targetTranslation) {
        // validate project (make sure it was downloaded)
        Project project = AppContext.getLibrary().getProject(targetTranslation.getProjectId(), "en");
        TargetLanguage targetLanguage = TargetLanguageUtility.getTargetLanguageWithFallback(targetTranslation.getTargetLanguageId(), targetTranslation.getId());

        if(project == null || targetLanguage == null || !AppContext.getLibrary().projectHasSource(project.getId())) {
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.missing_project, Snackbar.LENGTH_LONG);
            snack.setAction(R.string.download, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openLibrary();
                }
            });
            snack.setActionTextColor(getResources().getColor(R.color.light_primary_text));
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
        } else {
            Intent intent = new Intent(HomeActivity.this, TargetTranslationActivity.class);
            intent.putExtra(AppContext.EXTRA_TARGET_TRANSLATION_ID, targetTranslation.getId());
            startActivity(intent);
        }
    }

    public void notifyDatasetChanged() {
        onResume();
    }
}
