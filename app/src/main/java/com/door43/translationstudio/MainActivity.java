package com.door43.translationstudio;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.right.RightPaneFragment;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.translations.TranslationSyncResponse;
import com.door43.translationstudio.util.TranslatorBaseActivity;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends TranslatorBaseActivity implements DelegateListener {
    private final MainActivity me = this;

    private static final String LANG_CODE = "en"; // TODO: this will eventually need to be managed dynamically by the project manager

    // content panes
    private LeftPaneFragment mLeftPane;
    private RightPaneFragment mRightPane;
    private LinearLayout mCenterPane;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app().getSharedTranslationManager().registerDelegateListener(this);

        // TODO: we should display a splash screen until the project manager has finished loading.
        // we should also not load the project manager when initialized but manually in the splash loader.
        // we should also generate the keys in the splash loader
        // Generate the ssh keys
        if(!app().hasKeys()) {
            app().generateKeys();
        }

        mCenterPane = (LinearLayout)findViewById(R.id.centerPane);

        initPanes();

        if(app().shouldShowWelcome()) {
            // perform any welcoming tasks here
            app().setShouldShowWelcome(false);
            openLeftDrawer();
        } else {
            // automatically open the last viewed frame when the app opens
            if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {
                String frameId = app().getLastActiveFrame();
                String chapterId = app().getLastActiveChapter();
                String projectSlug = app().getLastActiveProject();
                app().getSharedProjectManager().setSelectedProject(projectSlug);
                app().getSharedProjectManager().getSelectedProject().setSelectedChapter(chapterId);
                app().getSharedProjectManager().getSelectedProject().getSelectedChapter().setSelectedFrame(frameId);
            }
            app().pauseAutoSave(true);
            reloadCenterPane();
            app().pauseAutoSave(false);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Returns the left pane fragment
     * @return
     */
    public LeftPaneFragment getLeftPane() {
        return mLeftPane;
    }

    /**
     * set up the content panes
     */
    private void initPanes() {
        mLeftPane = new LeftPaneFragment();
        mRightPane = new RightPaneFragment();


        getFragmentManager().beginTransaction().replace(R.id.leftPaneContent, mLeftPane).commit();
        getFragmentManager().beginTransaction().replace(R.id.rightPaneContent, mRightPane).commit();

        // close the side panes when the center content is clicked
        findViewById(R.id.inputText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeDrawers();
            }
        });
        TextView sourceText = ((TextView)findViewById(R.id.sourceText));
        sourceText.setMovementMethod(new ScrollingMovementMethod());
        sourceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeDrawers();
            }
        });
        final TextView helpText = (TextView)findViewById(R.id.helpTextView);

        // display help text when sourceText is empty.
        // TODO: enable/disable inputText as sourceText becomes available.
        sourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(charSequence.length() > 0) {
                    helpText.setVisibility(View.GONE);
                } else {
                    helpText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // automatically save changes to inputText
        final EditText inputText = (EditText)findViewById(R.id.inputText);

        inputText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int saveDelay = Integer.parseInt(app().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTOSAVE, getResources().getString(R.string.pref_default_autosave)));
                timer.cancel();
                if(saveDelay != -1) {
                    timer = new Timer();
                    if (!app().pauseAutoSave()) {
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // save the changes
                                me.save();
                            }
                        }, saveDelay);
                    }
                }
            }
        });
    }

    /**
     * Updates the center pane with the selected source frame text and any existing translations
     */
    public void reloadCenterPane() {
        // load source text
        TextView sourceText = (TextView)mCenterPane.findViewById(R.id.sourceText);
        sourceText.setText(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame().getText());

        // load translation
        Project p = app().getSharedProjectManager().getSelectedProject();
        String translation = app().getSharedTranslationManager().getTranslation(p.getId(), LANG_CODE, p.getSelectedChapter().getSelectedFrame().getChapterFrameId());
        EditText inputText = (EditText)mCenterPane.findViewById(R.id.inputText);
        inputText.setText(translation);

        // updates preferences so the app opens to the last opened frame
        app().setActiveProject(app().getSharedProjectManager().getSelectedProject().getId());
        app().setActiveChapter(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getId());
        app().setActiveFrame(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame().getId());
    }

    /**
     * Closes all the navigation drawers
     */
    public void closeDrawers() {
        ((DrawerLayout)findViewById(R.id.drawer_layout)).closeDrawers();
        app().pauseAutoSave(true);
        reloadCenterPane();
        app().pauseAutoSave(false);
    }

    public void openLeftDrawer() {
        ((DrawerLayout)findViewById(R.id.drawer_layout)).openDrawer(Gravity.LEFT);
    }

    /**
     * Saves the translated content found in inputText
     */
    public void save() {
        if (!app().pauseAutoSave()) {
            // do not allow saves to stack up when saves are running slowly.
            app().pauseAutoSave(true);
            String inputTextValue = ((EditText) findViewById(R.id.inputText)).getText().toString();
            Project p = app().getSharedProjectManager().getSelectedProject();

            // TODO: we need a way to manage what language the translation is being made in. This is different than the source languages
            app().getSharedTranslationManager().save(inputTextValue, p.getId(), LANG_CODE, p.getSelectedChapter().getSelectedFrame().getChapterFrameId());
            app().pauseAutoSave(false);
        }
    }

    /**
     * Override the device contextual menu button to open our custom menu
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU) {
            showContextualMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Displays the app contextual menu
     */
    public void showContextualMenu() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        app().closeToastMessage();
        // Create and show the dialog.
        MenuDialogFragment newFragment = new MenuDialogFragment();
        newFragment.show(ft, "dialog");
    }

    @Override
    public void onDelegateResponse(String id, DelegateResponse response) {
        if(TranslationSyncResponse.class == response.getClass()) {
            if (((TranslationSyncResponse)response).isSuccess()) {
                showContextualMenu();
            } else {
                // error
            }
        }
    }

    @Override
    public void onDestroy() {
        app().getSharedTranslationManager().removeDelegateListener(this);
        super.onDestroy();
    }
}
