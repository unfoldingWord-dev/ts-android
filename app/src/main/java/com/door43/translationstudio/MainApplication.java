package com.door43.translationstudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Toast;

import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.DummyDialogListener;
import com.door43.translationstudio.util.MainContextLink;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

import java.io.File;
import java.io.IOException;

/**
 * Custom application class so we can effectively handle state accross activities and other classes
 */
public class MainApplication extends Application {
    private Activity mCurrentActivity = null;
    private Toast mToast = null;
    private ProjectManager mProjectManager;
    private TranslationManager mTranslationManager;
    private final String PREFERENCES_TAG = "com.door43.translationstudio";
    private boolean mPauseAutoSave = false;

    public void onCreate() {
        // initialize basic functions with link to main application
        new MainContextLink(this);

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
     * Displays a standard toast message in the ui.
     * If a toast message is currently visible it will be replaced.
     * @param message The message to display to the user
     */
    public void showToastMessage(final String message) {
        if(mCurrentActivity != null) {
            mCurrentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if(mToast != null) {
                        mToast.cancel();
                    }
                    mToast = Toast.makeText(mCurrentActivity, message, Toast.LENGTH_LONG);
                    mToast.setGravity(Gravity.CENTER, 0, 0);
                    mToast.show();
                }
            });
        }
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

    public void showException(Throwable t) {
        showToastMessage(t.getMessage());
        t.printStackTrace();
    }

    public void showException(Throwable t, int res) {
        showToastMessage(res);
        t.printStackTrace();
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
    public void setActiveChapter(Integer id) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("chapter_id", id);
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
    public String getActiveProject() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getString("project_slug", "");
    }

    /**
     * Returns the active chapter from the preferences
     * @return
     */
    public Integer getActiveChapter() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getInt("chapter_id", 0);
    }

    /**
     * Returns the active from from the preferences
     * @return
     */
    public String getActiveFrame() {
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
     * Generates a new RSA key pair for use with ssh
     */
    public void generateKeys() {
        JSch jsch = new JSch();
        int type = KeyPair.RSA;
        File keysDir = getKeysFolder();
        String privateKeyPath = keysDir.getAbsolutePath() + "/id_rsa";
        String publicKeyPath = keysDir.getAbsolutePath() + "/id_rsa.pub";
        String udid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        try{
            KeyPair kpair=KeyPair.genKeyPair(jsch, type);
            kpair.writePrivateKey(privateKeyPath);
            kpair.writePublicKey(publicKeyPath, udid);
            System.out.println("Finger print: "+kpair.getFingerPrint());
            kpair.dispose();
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}
