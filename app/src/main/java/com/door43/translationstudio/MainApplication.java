package com.door43.translationstudio;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Toast;

import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.translations.TranslationManager;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

import java.io.File;

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
    public void setNotice(final String message) {
        if(mCurrentActivity != null) {
            mCurrentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if(mToast != null) {
                        mToast.cancel();
                    }
                    mToast = Toast.makeText(mCurrentActivity, message, Toast.LENGTH_SHORT);
                    mToast.setGravity(Gravity.CENTER, 0, 0);
                    mToast.show();
                }
            });
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
        String keysDirPath = getFilesDir() + "/" + getResources().getString(R.string.keys_dir) + "/";
        File privFile = new File(keysDirPath+"id_rsa");
        File pubFile = new File(keysDirPath+"id_rsa.pub");
        return privFile.exists() && pubFile.exists();
    }

    public void generateKeys() {
        JSch jsch = new JSch();
        int type = KeyPair.RSA;
        String keysDirPath = getFilesDir() + "/" + getResources().getString(R.string.keys_dir) + "/";
        String privateKeyPath = keysDirPath + "id_rsa";
        String publicKeyPath = keysDirPath + "id_rsa.pub";
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
