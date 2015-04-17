package com.door43.translationstudio;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;

import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.PopupMenu;

import com.door43.translationstudio.dialogs.FramesListAdapter;
import com.door43.translationstudio.dialogs.FramesReaderDialog;
import com.door43.translationstudio.events.ChapterTranslationStatusChangedEvent;
import com.door43.translationstudio.events.FrameTranslationStatusChangedEvent;
import com.door43.translationstudio.events.OpenedChapterEvent;
import com.door43.translationstudio.events.OpenedFrameEvent;
import com.door43.translationstudio.events.OpenedProjectEvent;
import com.door43.translationstudio.events.SecurityKeysSubmittedEvent;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.right.RightPaneFragment;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;

public class MainActivity extends TranslatorBaseActivity implements TranslatorActivityInterface {
    private final MainActivity me = this;

    // content panes
    private LeftPaneFragment mLeftPane;
    private RightPaneFragment mRightPane;
    private DrawerLayout mDrawerLayout;
    private BroadcastReceiver mMessageReceiver;
    private static Toolbar mMainToolbar;
    private boolean mKeyboardIsOpen;
    private int mPreviousRootViewHeight;
    private TranslatorFragmentInterface mTranslatorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // insert translator fragment
        if(savedInstanceState == null) {
            // TODO: insert the correct fragment
            mTranslatorFragment = new DefaultTranslatorFragment();
            ((Fragment)mTranslatorFragment).setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.translator_container, (Fragment)mTranslatorFragment).commit();
        } else {
            mTranslatorFragment = (TranslatorFragmentInterface)getFragmentManager().findFragmentById(R.id.translator_container);
        }

        // set up toolbars
        mMainToolbar = (Toolbar)findViewById(R.id.toolbar_main);
        mMainToolbar.setVisibility(View.VISIBLE);
        mMainToolbar.setTitle("");
        setSupportActionBar(mMainToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);

        AppContext.context().setMainActivity(this);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLayout.setScrimColor(getResources().getColor(R.color.scrim));

        // watch for the soft keyboard open and close
        final View rootView = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
        mPreviousRootViewHeight = 0;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mPreviousRootViewHeight == 0) {
                    mPreviousRootViewHeight = rootView.getHeight();
                }
                onKeyboardChanged(rootView);
            }
        });

        // Register a listener for when the input method changes (e.g. the keyboard)
        IntentFilter filter = new IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED);
        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onKeyboardChanged(rootView);
            }
        };
        // TRICKY: we need to unregister this when the activity is destroyed
        registerReceiver(mMessageReceiver, filter);

        // set up drawers
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mLeftPane = new LeftPaneFragment();
        mRightPane = new RightPaneFragment();
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            // make both panes fill half the screen when in landscape mode.
            mRightPane.setLayoutWidth(size.x / 2);
            mLeftPane.setLayoutWidth(size.x / 2);
        }
        getFragmentManager().beginTransaction().replace(R.id.leftPaneContent, mLeftPane).commit();
        getFragmentManager().beginTransaction().replace(R.id.rightPaneContent, mRightPane).commit();

        // get notified when drawers open
        mDrawerLayout.setDrawerListener(new ActionBarDrawerToggle(this, mDrawerLayout, R.string.close, R.string.close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                closeKeyboard();
            }
        });

        if(AppContext.projectManager().getSelectedProject() != null && AppContext.projectManager().getSelectedProject().getSelectedChapter() == null) {
            // the project contains no chapters for the current language
            Intent languageIntent = new Intent(me, LanguageSelectorActivity.class);
            languageIntent.putExtra("sourceLanguages", true);
            startActivity(languageIntent);
        }

        if(AppContext.context().shouldShowWelcome()) {
            // perform any welcoming tasks here. This happens when the user first opens the app.
            AppContext.context().setShouldShowWelcome(false);
            openLibraryDrawer();
        } else {
            // open the drawer if the remembered chapter does not exist
            if(AppContext.context().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {
                if(AppContext.projectManager().getSelectedProject() == null || AppContext.projectManager().getSelectedProject().getSelectedChapter() == null) {
                    openLibraryDrawer();
                }
            }
        }
        closeKeyboard();
    }

    /**
     * Notifies the translator fragment to reload it's content
     */
    public void reload() {
        mTranslatorFragment.reload();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void openChaptersTab() {
        mLeftPane.selectTab(1);
        // TODO: make sure the drawer is open
    }

    public void openFramesTab() {
        mLeftPane.selectTab(2);
        // TODO: make sure the drawer is open
    }

    public void reloadFramesTab() {
        mLeftPane.reloadFramesTab();
    }

    private void onKeyboardChanged(View root) {
        Rect r = new Rect();
        root.getWindowVisibleDisplayFrame(r);
        if(root.getHeight() < mPreviousRootViewHeight) {
            onKeyboardOpen(r);
        } else {
            onKeyboardClose(r);
        }
    }

    /**
     * Triggered when the keyboard opens
     * @param r the dimensions of the visible area
     */
    private void onKeyboardOpen(Rect r) {
        if(!mKeyboardIsOpen) {
            mKeyboardIsOpen = true;
            resizeRootView(r);

            // apply contextual menu styles
            mMainToolbar.setBackgroundColor(getResources().getColor(R.color.light_blue));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            invalidateOptionsMenu();
        }
    }

    /**
     * Triggered when the keyboard closes
     * @param r the dimensions of the visible area
     */
    private void onKeyboardClose(Rect r) {
        if(mKeyboardIsOpen) {
            mKeyboardIsOpen = false;
            resizeRootView(r);

            // apply main menu styles
            mMainToolbar.setBackgroundColor(getResources().getColor(R.color.green));
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onBackPressed() {
        // display confirmation before closing the app
        new AlertDialog.Builder(this)
                .setMessage(R.string.exit_confirmation)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * Redraws the ui to fit within the viewport correctly
     * @param r
     */
    private void resizeRootView(Rect r) {
        ViewGroup.LayoutParams params = mDrawerLayout.getLayoutParams();
        int newHeight = r.bottom - getStatusBarHeight();
        if(newHeight != params.height) {
            params.height = newHeight;
            mDrawerLayout.setLayoutParams(params);
        }
    }

    public int getStatusBarHeight() {
        Rect r = new Rect();
        Window w = getWindow();
        w.getDecorView().getWindowVisibleDisplayFrame(r);
        return r.top;
    }

    @Override
    public void closeKeyboard() {
        if(getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } else {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    @Override
    public void disableResourcesDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.END);
    }

    @Override
    public void enableResourcesDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.END);
    }

    @Override
    public void setContextualMenu(final int menuRes) {
        Button button = (Button) findViewById(R.id.contextual_menu_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PopupMenu contextualMenu = new PopupMenu(MainActivity.this, v);
                contextualMenu.getMenuInflater().inflate(menuRes, contextualMenu.getMenu());
                contextualMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return mTranslatorFragment.onContextualMenuItemClick(item);
                    }
                });
                contextualMenu.show();
            }
        });
    }

    @Override
    public void openKeyTerm(Term term) {
        mRightPane.showTerm(term);
        openResourcesDrawer();
    }

    /**
     * opens the keyboard
     */
    @Override
    public void openKeyboard() {
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    /**
     * Closes both of the drawers
     */
    @Override
    public void closeDrawers() {
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void openLibraryDrawer() {
        mDrawerLayout.closeDrawer(Gravity.RIGHT);
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }

    @Override
    public void openResourcesDrawer() {
        mDrawerLayout.closeDrawer(Gravity.LEFT);
        mDrawerLayout.openDrawer(Gravity.RIGHT);
    }

    /**
     * Saves the translation
     */
    @Override
    public void save() {
        mTranslatorFragment.save();
    }

    @Override
    public void refreshLibraryDrawer() {
        mLeftPane.reloadProjectsTab();
        mLeftPane.reloadChaptersTab();
        mLeftPane.reloadFramesTab();
    }

    /**
     * Opens the user settings activity
     */
    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Begins syncing the selected project
     */
    public void openSyncing() {
        if(AppContext.context().isNetworkAvailable()) {
            Intent intent = new Intent(this, UploadWizardActivity.class);
            startActivity(intent);
        } else {
            AppContext.context().showToastMessage(R.string.internet_not_available);
        }
    }

    /**
     * Opens the sharing and export activity
     */
    public void openSharing() {
        Intent intent = new Intent(this, SharingActivity.class);
        startActivity(intent);
    }

    /**
     * Opens the bug reporter
     */
    public void openBugReporter() {
        Intent intent = new Intent(this, BugReporterActivity.class);
        startActivity(intent);
    }

    /**
     * Displays the chapter settings
     */
    public void showChapterSettingsMenu() {
        Intent intent = new Intent(me, ChapterSettingActivity.class);
        startActivity(intent);
    }

    /**
     * Displays the project settings
     */
    @Override
    public void showProjectSettingsMenu() {
        Intent intent = new Intent(me, ProjectSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void refreshResourcesDrawer() {
        mRightPane.reloadNotesTab();
        mRightPane.reloadTermsTab();
    }

    @Override
    public boolean keyboardIsOpen() {
        return mKeyboardIsOpen;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                closeKeyboard();
                return true;
            case R.id.action_share:
                openSharing();
                return true;
            case R.id.action_sync:
                if(AppContext.projectManager().getSelectedProject() != null) {
                    openSyncing();
                } else {
                    AppContext.context().showToastMessage(R.string.choose_a_project);
                }
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_bug:
                openBugReporter();
                return true;
            case R.id.action_update:
                Intent intent = new Intent(this, GetMoreProjectsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_library:
                openLibraryDrawer();
                return true;
            case R.id.action_resources:
                if(AppContext.projectManager().getSelectedProject() != null) {
                    openResourcesDrawer();
                } else {
                    AppContext.context().showToastMessage(R.string.choose_a_project);
                }
                return true;
            case R.id.action_chapter_settings:
                if(AppContext.projectManager().getSelectedProject() != null && AppContext.projectManager().getSelectedProject().getSelectedChapter() != null) {
                    showChapterSettingsMenu();
                } else if(AppContext.projectManager().getSelectedProject() == null) {
                    AppContext.context().showToastMessage(R.string.choose_a_project);
                } else {
                    AppContext.context().showToastMessage(R.string.choose_a_chapter);
                }
                return true;
            case R.id.action_project_settings:
                if(AppContext.projectManager().getSelectedProject() != null) {
                    showProjectSettingsMenu();
                } else {
                    AppContext.context().showToastMessage(R.string.choose_a_project);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * TODO: all of these methods are deprecated.
     */

    /**
     * Triggered by the translation manager after the security keys have been successfully submitted to the server
     * @param event
     */
    @Subscribe
    public void securityKeysSubmitted(SecurityKeysSubmittedEvent event) {
        if(AppContext.context().isNetworkAvailable()) {
            AppContext.translationManager().syncSelectedProject();
        } else {
            AppContext.context().showToastMessage(R.string.internet_not_available);
        }
    }

    /**
     * Triggered any time a frame is cleaned (deleted)
     * @param event
     */
    @Subscribe
    public void frameTranslationStatusChanged(FrameTranslationStatusChangedEvent event) {
        mLeftPane.reloadFramesTab();
    }

    /**
     * Triggered any time a chapter is cleaned (deleted)
     * @param event
     */
    @Subscribe
    public void chapterTranslationStatusChanged(ChapterTranslationStatusChangedEvent event) {
        mLeftPane.reloadChaptersTab();
        mLeftPane.reloadProjectsTab();
    }

    /**
     * Trigged by the navigator when a project is opened
     * @param event
     */
    @Subscribe
    public void onOpenedProject(OpenedProjectEvent event) {
        mLeftPane.reloadProjectsTab();
    }

    /**
     * Trigged by the navigator when a chapter is opened
     * @param event
     */
    @Subscribe
    public void onOpenedChapter(OpenedChapterEvent event) {
        mLeftPane.reloadChaptersTab();
    }

    /**
     * Trigged by the navigator when a frame is opened
     * @param event
     */
    @Subscribe
    public void onOpenedFrame(OpenedFrameEvent event) {
        mLeftPane.reloadFramesTab();
        mTranslatorFragment.reload();
    }
}
