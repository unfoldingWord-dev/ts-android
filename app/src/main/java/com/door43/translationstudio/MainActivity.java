package com.door43.translationstudio;

import android.app.AlertDialog;
import android.app.Fragment;
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
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
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

import com.door43.translationstudio.events.ChapterTranslationStatusChangedEvent;
import com.door43.translationstudio.events.FrameTranslationStatusChangedEvent;
import com.door43.translationstudio.events.OpenedChapterEvent;
import com.door43.translationstudio.events.OpenedFrameEvent;
import com.door43.translationstudio.events.OpenedProjectEvent;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.right.RightPaneFragment;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.tasks.IndexProjectsTask;
import com.door43.translationstudio.tasks.IndexResourceTask;
import com.door43.translationstudio.translatonui.BlindDraftTranslatorFragment;
import com.door43.translationstudio.translatonui.DefaultTranslatorFragment;
import com.door43.translationstudio.translatonui.TranslatorActivityInterface;
import com.door43.translationstudio.translatonui.TranslatorFragmentInterface;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.squareup.otto.Subscribe;

public class MainActivity extends TranslatorBaseActivity implements TranslatorActivityInterface, GenericTaskWatcher.OnFinishedListener {
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
    private GenericTaskWatcher mIndexTaskWatcher;
    private Button mContextualButton;
    private boolean mContextualButtonEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ensure the languages have been chosen
        Project p = AppContext.projectManager().getSelectedProject();
        if(p != null && (!p.hasSelectedSourceLanguage() || !p.hasSelectedTargetLanguage())) {
            AppContext.projectManager().setSelectedProject(null);
        }

        mContextualButton = (Button) findViewById(R.id.contextual_menu_btn);

        // insert translator fragment
        if(savedInstanceState == null) {
            reloadTranslatorFragment();
        } else {
            mTranslatorFragment = (TranslatorFragmentInterface)getFragmentManager().findFragmentById(R.id.translator_container);
        }

        // set up task watcher
        mIndexTaskWatcher = new GenericTaskWatcher(this, R.string.indexing, R.drawable.ic_index_small);
        mIndexTaskWatcher.setOnFinishedListener(this);

        // set up toolbars
        mMainToolbar = (Toolbar)findViewById(R.id.toolbar_main);
        mMainToolbar.setVisibility(View.VISIBLE);
        mMainToolbar.setTitle("");
        setSupportActionBar(mMainToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);

        AppContext.context().setMainActivity(this);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
//        mDrawerLayout.setScrimColor(getResources().getColor(R.color.scrim));

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
        registerReceiver(mMessageReceiver, filter);

        // set up drawers
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        if(savedInstanceState == null) {
            mLeftPane = new LeftPaneFragment();
            mRightPane = new RightPaneFragment();
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                // make both panes fill half the screen when in landscape mode.
                mRightPane.setLayoutWidth(size.x / 2);
                mLeftPane.setLayoutWidth(size.x / 2);
            }
            getFragmentManager().beginTransaction().replace(R.id.leftPaneContent, mLeftPane).commit();
            getFragmentManager().beginTransaction().replace(R.id.rightPaneContent, mRightPane).commit();
        } else {
            mLeftPane = (LeftPaneFragment)getFragmentManager().findFragmentById(R.id.leftPaneContent);
            mRightPane = (RightPaneFragment)getFragmentManager().findFragmentById(R.id.rightPaneContent);
        }

        // get notified when drawers open
        mDrawerLayout.setDrawerListener(new ActionBarDrawerToggle(this, mDrawerLayout, R.string.title_close_drawer, R.string.title_close_drawer) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                closeKeyboard();
            }
        });

        if(AppContext.context().shouldShowWelcome()) {
            // TODO: perform any welcoming tasks here. This happens when the user first opens the app.
            AppContext.context().setShouldShowWelcome(false);
        }

        // open the drawer if no frame is selected
        if(!frameIsSelected()) {
            if(chapterIsSelected()) {
                openFramesTab();
            } else if(AppContext.projectManager().getSelectedProject() != null) {
                openChaptersTab();
            } else {
                openProjectsTab();
            }
            openLibraryDrawer();
        }

        closeKeyboard();
    }

    public void onResume() {
        super.onResume();

        closeKeyboard();

        // connect to tasks
        IndexProjectsTask indexProjectsTask = (IndexProjectsTask)TaskManager.getTask(IndexProjectsTask.TASK_ID);
        IndexProjectsTask indexResourcesTask = (IndexProjectsTask)TaskManager.getTask(IndexProjectsTask.TASK_ID);
        if(indexProjectsTask != null) {
            mIndexTaskWatcher.watch(indexProjectsTask);
        } else if(indexResourcesTask != null) {
            mIndexTaskWatcher.watch(indexResourcesTask);
        }

        // rebuild the index if it was destroyed
        Project p = AppContext.projectManager().getSelectedProject();
        if(p != null && !IndexStore.hasIndex(p) && indexProjectsTask == null) {
            indexProjectsTask = new IndexProjectsTask(AppContext.projectManager().getProjects());
            mIndexTaskWatcher.watch(indexProjectsTask);
            TaskManager.addTask(indexProjectsTask, IndexProjectsTask.TASK_ID);
        }

        // reload the translations
        if(indexProjectsTask == null && indexResourcesTask == null) {
            // TODO: load the chapters and the frames for the selected chapter
            reload();
        }
    }

    /**
     * Checks if a chapter is selected
     * @return
     */
    private boolean chapterIsSelected() {
        Project p = AppContext.projectManager().getSelectedProject();
        return p != null && p.getSelectedChapter() != null;
    }

    /**
     * Checks if a frame is selected
     */
    private boolean frameIsSelected() {
        Project p = AppContext.projectManager().getSelectedProject();
        return p != null && p.getSelectedChapter() != null && p.getSelectedChapter().getSelectedFrame() != null;
    }

    /**
     * Notifies the translator fragment to reload it's content
     */
    public void reload() {
        // reload the ui
        if(mContextualButtonEnabled && frameIsSelected()) {
            mContextualButton.setVisibility(View.VISIBLE);
        } else {
            mContextualButton.setVisibility(View.GONE);
        }
        mTranslatorFragment.reload();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void openProjectsTab() {
        mLeftPane.selectTab(LeftPaneFragment.TAB_INDEX_PROJECTS);
    }

    public void openChaptersTab() {
        mLeftPane.selectTab(LeftPaneFragment.TAB_INDEX_CHAPTERS);
    }

    public void openFramesTab() {
        mLeftPane.selectTab(LeftPaneFragment.TAB_INDEX_FRAMES);
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
            mMainToolbar.setBackgroundColor(getResources().getColor(R.color.accent));
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
            mMainToolbar.setBackgroundColor(getResources().getColor(R.color.primary));
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
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
    }

    @Override
    public void enableResourcesDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
    }

    @Override
    public void setContextualMenu(final int menuRes) {
        mContextualButtonEnabled = true;
        if(frameIsSelected()) {
            mContextualButton.setVisibility(View.VISIBLE);
        } else {
            mContextualButton.setVisibility(View.GONE);
        }
        mContextualButton.setOnClickListener(new View.OnClickListener() {
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
                mTranslatorFragment.onPrepareContextualMenu(contextualMenu.getMenu());
                contextualMenu.show();
            }
        });
    }

    public void hideContextualMenu() {
        Button button = (Button) findViewById(R.id.contextual_menu_btn);
        button.setVisibility(View.GONE);
    }

    @Override
    public void openKeyTerm(Term term) {
        mRightPane.showTerm(term);
        openResourcesDrawer();
    }

    @Override
    public void reloadTranslatorFragment() {
        hideContextualMenu();
        SharedPreferences settings = AppContext.context().getSharedPreferences(AppContext.context().PREFERENCES_TAG, AppContext.context().MODE_PRIVATE);
        TranslatorFragmentInterface fragment;
        if(settings.getBoolean("enable_blind_draft_mode", false)) {
            fragment = new BlindDraftTranslatorFragment();
        } else {
            fragment = new DefaultTranslatorFragment();
        }
        ((Fragment) fragment).setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(R.id.translator_container, (Fragment) fragment).commit();
        mTranslatorFragment = fragment;
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

    /**
     * Allows the user to change the drawer state
     */
    public void unlockLibraryDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
    }

    /**
     * Prevents the user from changing the drawer state
     */
    public void lockLibraryDrawer() {
        if(mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, Gravity.LEFT);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }
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
        mIndexTaskWatcher.stop();
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
        reload();
    }

    @Override
    public void onFinished(final ManagedTask task) {
        mIndexTaskWatcher.stop();
        Project p = AppContext.projectManager().getSelectedProject();
        if (task instanceof IndexProjectsTask && p != null && !IndexStore.hasResourceIndex(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource())) {
            IndexResourceTask newTask = new IndexResourceTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
            mIndexTaskWatcher.watch(newTask);
            TaskManager.addTask(newTask, IndexResourceTask.TASK_ID);
        } else if (task instanceof IndexResourceTask) {
            reload();
        }
    }
}
