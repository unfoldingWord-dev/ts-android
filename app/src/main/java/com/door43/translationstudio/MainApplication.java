package com.door43.translationstudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Toast;

import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.translations.TranslationManager;
import com.door43.translationstudio.util.DummyDialogListener;
import com.door43.translationstudio.util.MainContext;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.File;

/**
 * Custom application class so we can effectively handle state accross activities and other classes
 */
public class MainApplication extends Application {
    private Activity mCurrentActivity = null;
    private Toast mToast = null;
    private ProjectManager mProjectManager;
    private ProgressDialog mProgressDialog;
    private TranslationManager mTranslationManager;
    private final String PREFERENCES_TAG = "com.door43.translationstudio";
    private boolean mPauseAutoSave = false;
    private ImageLoader mImageLoader;
    private Activity mCurrentDialogActivity;

    public void onCreate() {

        // initialize basic functions with link to main application
        new MainContext(this);

        // initialize default settings
        // NOTE: make sure to add any new preference files here in order to have their default values properly loaded.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_save_and_sync, false);


        mProjectManager = new ProjectManager(this);
        mTranslationManager = new TranslationManager(this);
    }

    /**
     * Returns the shared instance of the project manager
     * @return
     */
    public ProjectManager getSharedProjectManager() {
        return mProjectManager;
    }

    /**
     * Returns the shared instance of the translation manager
     * @return
     */
    public TranslationManager getSharedTranslationManager() {
        return mTranslationManager;
    }

    /**
     * Sets the current activity so we can access it throughout the app.
     * @param mCurrentActivity
     */
    public void setCurrentActivity(Activity mCurrentActivity) {
        this.mCurrentActivity = mCurrentActivity;
    }

    /**
     * Returns the currently active activity
     * @return
     */
    public Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    /**
     * Generates and returns the image loader
     * @return
     */
    public ImageLoader getImageLoader() {
        if(mImageLoader == null) {
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
            mImageLoader = ImageLoader.getInstance();
            mImageLoader.init(config);
        }
        return mImageLoader;
    }

    /**
     * Displays a standard toast message in the ui
     * @param message
     */
    public void showToastMessage(final String message) {
        showToastMessage(message, Toast.LENGTH_LONG);
    }

    /**
     * Displays a standard toast message in the ui.
     * If a toast message is currently visible it will be replaced.
     * @param message The message to display to the user
     * @param duration
     */
    public void showToastMessage(final String message, final int duration) {
        if(mCurrentActivity != null) {
            mCurrentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if(mToast != null) {
                        mToast.cancel();
                    }
                    mToast = Toast.makeText(mCurrentActivity, message, duration);
                    mToast.setGravity(Gravity.TOP, 0, 0);
                    mToast.show();
                }
            });
        }
    }

    /**
     * Cancels any toast message that is currently being displayed.
     */
    public void closeToastMessage() {
        if(mCurrentActivity != null) {
            mCurrentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mToast != null) mToast.cancel();
                }
            });
        }
    }

    public void showToastMessage(int resId, int duration) {
        showToastMessage(getString(resId), duration);
    }
    public void showToastMessage(int resId) {
        showToastMessage(getString(resId));
    }

    public void showMessageDialog(int title, int msg, int positiveBtn, DialogInterface.OnClickListener positiveListenerr) {
        showMessageDialog(title, getString(msg), positiveBtn, R.string.label_cancel, positiveListenerr, new DummyDialogListener());
    }

    public void showMessageDialog(int title, String msg, int positiveBtn, DialogInterface.OnClickListener positiveListenerr) {
        showMessageDialog(title, msg, positiveBtn, R.string.label_cancel, positiveListenerr, new DummyDialogListener());
    }

    public void showMessageDialog(int title, String msg, int positiveBtn, int negativeBtn, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(msg)
                .setPositiveButton(positiveBtn, positiveListener)
                .setNegativeButton(negativeBtn, negativeListener).show();
    }

    public void showMessageDialog(int title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getCurrentActivity());
        builder.setTitle(title).setMessage(msg).setPositiveButton(R.string.label_ok, new DummyDialogListener()).show();
    }

    /**
     * Displays a message dialog to the user with a detailed view
     * @param title
     * @param msg
     * @param details
     */
    public void showMessageDialogDetails(final int title, int msg, final String details) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getCurrentActivity());
        builder.setTitle(title).setMessage(msg).setPositiveButton(R.string.label_ok, new DummyDialogListener()).setNeutralButton(R.string.label_details, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showMessageDialog(title, details);
            }
        }).show();
    }

    public void showException(Throwable t) {
        showToastMessage(t.getMessage());
        t.printStackTrace();
    }

    public void showException(Throwable t, int res) {
        showToastMessage(res);
        t.printStackTrace();
    }

    /**
     * Displays a progress dialog
     * @param message the message to display in the dialog
     */
    public void showProgressDialog(final String message) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mProgressDialog == null || mCurrentDialogActivity != getCurrentActivity()) {
                    closeProgressDialog();
                    mProgressDialog = new ProgressDialog(getCurrentActivity());
                }
                mProgressDialog.setMessage(message);
                if(!mProgressDialog.isShowing()) {
                    mProgressDialog.show();
                }
            }
        });
    }

    /**
     * Displays a progress dialog
     * @param res the resource id of the text to display
     */
    public void showProgressDialog(int res) {
        showProgressDialog(getResources().getString(res));
    }

    /**
     * Closes the current progress dialog
     */
    public void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * Stores the active project in the app preferences so it can load automatically next time.
     * @param slug
     */
    public void setActiveProject(String slug) {
        if (slug == null) slug = "";
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("project_slug", slug);
        editor.commit();
    }

    /**
     * Stores the active chapter in the app preferences so it can load automatically next time.
     * @param id
     */
    public void setActiveChapter(String id) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("chapter_id", id);
        editor.commit();
    }

    /**
     * Stores the active frame in the app preferences so it can load automatically next time.
     * @param id
     */
    public void setActiveFrame(String id) {
        if (id == null) id = "";
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("frame_id", id);
        editor.commit();
    }

    /**
     * Returns the active project from the preferences
     * @return
     */
    public String getLastActiveProject() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getString("project_slug", "");
    }

    /**
     * Returns the active chapter from the preferences
     * @return
     */
    public String getLastActiveChapter() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getString("chapter_id", "");
    }

    /**
     * Returns the active from from the preferences
     * @return
     */
    public String getLastActiveFrame() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getString("frame_id", "");
    }

    /**
     * Flags the app to pause the auto save functionality
     * @param shouldPause
     */
    public void pauseAutoSave(boolean shouldPause) {
        mPauseAutoSave = shouldPause;
    }

    /**
     * Checks if the auto save is paused
     * @return
     */
    public boolean pauseAutoSave() {
        return mPauseAutoSave;
    }

    /**
     * Checks if the ssh keys have already been generated
     * @return
     */
    public boolean hasKeys() {
        File keysDir = getKeysFolder();
        File privFile = new File(keysDir.getAbsolutePath()+"/id_rsa");
        File pubFile = new File(keysDir.getAbsolutePath()+"/id_rsa.pub");
        return privFile.exists() && pubFile.exists();
    }

    /**
     * Returns the directory in which the ssh keys are stored
     * @return
     */
    public File getKeysFolder() {
        File folder = new File(getFilesDir() + "/" + getResources().getString(R.string.keys_dir) + "/");
        if(!folder.exists()) {
            folder.mkdir();
        }
        return folder;
    }

    /**
     * Returns the public key file
     * @return
     */
    public File getPublicKey() {
        File keysDir = getKeysFolder();
        return  new File(keysDir.getAbsolutePath()+"/id_rsa.pub");
    }

    /**
     * Generates a new RSA key pair for use with ssh
     * TODO: this should not be done on the main thread
     */
    public void generateKeys() {
        JSch jsch = new JSch();
        int type = KeyPair.RSA;
        File keysDir = getKeysFolder();
        String privateKeyPath = keysDir.getAbsolutePath() + "/id_rsa";
        String publicKeyPath = keysDir.getAbsolutePath() + "/id_rsa.pub";

        try{
            KeyPair kpair=KeyPair.genKeyPair(jsch, type);
            kpair.writePrivateKey(privateKeyPath);
            kpair.writePublicKey(publicKeyPath, getUDID());
            kpair.dispose();
            showToastMessage("SSH keys were successfully generated");
        }
        catch(Exception e){
            showException(e);
        }
        // require the app to re-submit generated keys to the server
        setHasRegisteredKeys(false);
    }

    /**
     * Checks if the client has sent it's ssh key to the server
     * @return
     */
    public boolean hasRegisteredKeys() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getBoolean("has_registered_with_server", false);
    }

    /**
     * Sets whether the client has sent it's ssh key to the server
     * @param hasRegistered
     */
    public void setHasRegisteredKeys(Boolean hasRegistered) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("has_registered_with_server", hasRegistered);
        editor.commit();
    }

    /**
     * Checks if the app should opperate as if this is the first time it has opened.
     * @return
     */
    public boolean shouldShowWelcome() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getBoolean("show_welcome", true);
    }

    /**
     * Sets whether the app should operate as if this is the first time it has opened.
     * @param shouldWelcome
     */
    public void setShouldShowWelcome(Boolean shouldWelcome) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("show_welcome", shouldWelcome);
        editor.commit();
    }

    /**
     * Checks if the client has accepted the terms of use
     * @return
     */
    public boolean hasAcceptedTerms() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getBoolean("has_accepted_terms", false);
    }

    /**
     * Sets whether the client has accepted the terms of use.
     * @param hasAcceptedTerms
     */
    public void setHasAcceptedTerms(Boolean hasAcceptedTerms) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("has_accepted_terms", hasAcceptedTerms);
        editor.commit();
    }

    /**
     * Returns the device id
     * @return
     */
    public String getUDID() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Returns an instance of the user preferences.
     * This is just the default shared preferences
     * @return
     */
    public SharedPreferences getUserPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }
}
