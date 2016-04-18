package com.door43.translationstudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.door43.tools.reporting.GlobalExceptionHandler;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.service.BackupService;
import com.door43.util.DummyDialogListener;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Custom application class so we can effectively handle state accross activities and other classes
 * TODO: we are slowly stripping everything out of this class and placing it into the AppContext class
 */
public class MainApplication extends Application {
    private Activity mCurrentActivity = null;
    private Toast mToast = null;
//    private ProjectManager mProjectManager;
    private ProgressDialog mProgressDialog;
    public static final String PREFERENCES_TAG = "com.door43.translationstudio";
    private ImageLoader mImageLoader;
//    private Activity mCurrentDialogActivity;
//    private Map<String, ArrayList<String>> mNotificationsMap = new HashMap<String, ArrayList<String>>();
//    private static Typeface mTranslationTypeface;
//    private static String mSelectedTypeface = "";
    private static Activity mMainActivity;
//    private Term mSelectedKeyTerm;
    private boolean mShowImportantTerms;
    public static final String STACKTRACE_DIR = "crashes";
    private boolean mClosingProgressDialog = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // initialize basic functions with link to main application
        new AppContext(this);

        File dir = new File(AppContext.getPublicDirectory(), STACKTRACE_DIR);
        GlobalExceptionHandler.register(dir);

        // configure logger
        int minLogLevel = Integer.parseInt(getUserPreferences().getString(SettingsActivity.KEY_PREF_LOGGING_LEVEL, getResources().getString(R.string.pref_default_logging_level)));
        configureLogger(minLogLevel);

        // initialize default settings
        // NOTE: make sure to add any new preference files here in order to have their default values properly loaded.
        PreferenceManager.setDefaultValues(this, R.xml.general_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.server_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.sharing_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences, false);

        // begins the backup manager service
        Intent backupIntent = new Intent(this, BackupService.class);
        startService(backupIntent);
    }

    public void configureLogger(int minLogLevel) {
        Logger.configure(new File(AppContext.getPublicDirectory(), "log.txt"), Logger.Level.getLevel(minLogLevel));
    }

//    /**
//     * Sets the main activity that can be used for displaying dialogs.
//     * @param activity
//     */
//    public static void setMainActivity(Activity activity) {
//        mMainActivity = activity;
//    }

    /**
     * Checks if the app should always share resources.
     * @return
     */
    public boolean alwaysShare() {
        return getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_ALWAYS_SHARE, Boolean.parseBoolean(getResources().getString(R.string.pref_default_always_share)));
    }

    /**
     * Moves an asset into the cache directory and returns a file reference to it
     * @param path
     * @return
     */
    public File getAssetAsFile(String path) {
        // TODO: we probably don't want to do this for everything.
        // think about changing this up a bit.
        // TODO: we need to figure out when the clear out these cached files. Probably just on version bumps.
        File cacheFile = new File(getCacheDir(), "assets/" + path);
        if(!cacheFile.exists()) {
            cacheFile.getParentFile().mkdirs();
            try {
                InputStream is = getAssets().open(path);
                try {
                    FileOutputStream outputStream = new FileOutputStream(cacheFile);
                    try {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            outputStream.write(buf, 0, len);
                        }
                    } finally {
                        outputStream.close();
                    }
                } finally {
                    is.close();
                }

            } catch (IOException e) {
                return null;
            }
        }
        return cacheFile;
    }

    /**
     * Sends a new local notification
     */
//    public void sendNotification(int notificationId, int titleResourceId, String message) {
//        // keep track of all the notifications
//        ArrayList<String> notifications;
//        if(mNotificationsMap.containsKey(""+notificationId)) {
//            notifications = mNotificationsMap.get(""+notificationId);
//        } else {
//            // add new notification group
//            notifications = new ArrayList<String>();
//            mNotificationsMap.put("" + notificationId, notifications);
//        }
//
//        // build notification
//        notifications.add(message);
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this)
//                        .setSmallIcon(R.drawable.ic_stat_notify_msg)
//                        .setContentTitle(getResourceSlugs().getString(titleResourceId))
//                        .setContentText(message)
//                        .setAutoCancel(true)
//                        .setNumber(notifications.size());
//
//        // build big notification
//        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
//        inboxStyle.setBigContentTitle(getResourceSlugs().getString(titleResourceId));
//        for (String event:notifications) {
//            inboxStyle.addLine(event);
//        }
//        mBuilder.setStyle(inboxStyle);
//
//        // issue notification
//        NotificationManager mNotifyMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            mNotifyMgr.notify(notificationId, mBuilder.build());
//        } else {
//            mNotifyMgr.notify(notificationId, mBuilder.getNotification());
//        }
//    }

//    /**
//     * Sets the current activity so we can access it throughout the app.
//     * @param mCurrentActivity
//     */
//    public void setCurrentActivity(Activity mCurrentActivity) {
//        this.mCurrentActivity = mCurrentActivity;
//    }

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
     * Checks if this apk was installed from the playstore or sideloaded
     * @return
     */
    public boolean isStoreVersion() {
        String installer = getPackageManager().getInstallerPackageName(getPackageName());
        return !TextUtils.isEmpty(installer);
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

//    public void showMessageDialog(int title, int msg, int positiveBtn, DialogInterface.OnClickListener positiveListenerr) {
//        showMessageDialog(title, getString(msg), positiveBtn, R.string.title_cancel, positiveListenerr, new DummyDialogListener());
//    }

//    public void showMessageDialog(int title, String msg, int positiveBtn, DialogInterface.OnClickListener positiveListenerr) {
//        showMessageDialog(title, msg, positiveBtn, R.string.title_cancel, positiveListenerr, new DummyDialogListener());
//    }

//    /**
//     *
//     * @param title
//     * @param msg
//     * @param positiveBtn
//     * @param negativeBtn
//     * @param positiveListener
//     * @param negativeListener
//     */
//    public void showMessageDialog(int title, String msg, int positiveBtn, int negativeBtn, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(title).setMessage(msg)
//                .setPositiveButton(positiveBtn, positiveListener)
//                .setNegativeButton(negativeBtn, negativeListener).show();
//    }

//    public void showMessageDialog(int title, int msg, int positiveBtn, int negativeBtn, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
//        showMessageDialog(title, getResources().getString(msg), positiveBtn, negativeBtn, positiveListener, negativeListener);
//    }

    public void showMessageDialog(int title, String msg) {
        showMessageDialog(getResources().getString(title), msg);
    }

    public void showMessageDialog(String title, String msg) {
        CustomAlertDialog.Create(this.getCurrentActivity())
            .setTitle(title).setMessage(msg).setPositiveButton(R.string.label_ok, null).show("ShowMsg");
    }

//    public void showMessageDialog(int title, int msg) {
//        showMessageDialog(title, getResources().getString(msg));
//    }

    /**
     * Displays a message dialog to the user with a detailed view
     * @param title
     * @param msg
     * @param details
     */
    @Deprecated
    public void showMessageDialogDetails(final int title, int msg, final String details) {
        CustomAlertDialog.Create(this.getCurrentActivity())
            .setTitle(title).setMessage(msg).setPositiveButton(R.string.label_ok, null)
                .setNeutralButton(R.string.label_details, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMessageDialog(title, details);
                    }
                }).show("ShowMsgDetail");
    }

    public void showException(Throwable t) {
        showToastMessage(t.getMessage());
        Logger.e(this.getClass().getName(), "non-critical exception", t);
    }

    public void showException(Throwable t, int res) {
        showToastMessage(res);
        Logger.e(this.getClass().getName(), "non-critical exception", t);
    }

    /**
     * Displays a progress dialog
     * TODO: Hack alert! we are using a quick fix to avoid leaked windows while showing dialogs from async tasks. Right now we are only using this for the upload manager.
     * @param message the message to display in the dialog
     */
    public void showProgressDialog(final String message) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMainActivity != null && !mClosingProgressDialog) {
                    if (mProgressDialog == null) { // was using: getCurrentActivity()
                        closeProgressDialog();
                        mProgressDialog = new ProgressDialog(mMainActivity); // was using: getCurrentActivity()
                    }
                    mProgressDialog.setMessage(message);
                    if (!mProgressDialog.isShowing()) {
                        mProgressDialog.show();
                    }
                } else if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                mClosingProgressDialog = false;
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
     * Closes the current progress dialog.
     * You probably want to make sure not to call this twice unnessesarily
     */
    public void closeProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            try {
                mProgressDialog.dismiss();
            } catch (Exception e) {
                showToastMessage(e.getMessage());
            }
        }
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
     * Returns the private key file
     * @return
     */
    public File getPrivateKey() {
        File keysDir = getKeysFolder();
        return  new File(keysDir.getAbsolutePath()+"/id_rsa");
    }

    /**
     * Generates a new RSA key pair for use with ssh, this also flags that the keys are not registered
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
            new File(privateKeyPath).createNewFile();
            kpair.writePrivateKey(privateKeyPath);
            new File(publicKeyPath).createNewFile();
            kpair.writePublicKey(publicKeyPath, AppContext.udid());
            kpair.dispose();
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
     * @deprecated we will always try to push first and register if it fails
     */
    public boolean hasRegisteredKeys() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        return settings.getBoolean("has_registered_with_server", false);
    }

    /**
     * Sets whether the client has sent it's ssh key to the server
     * @param hasRegistered
     * @deprecated
     */
    public void setHasRegisteredKeys(Boolean hasRegistered) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("has_registered_with_server", hasRegistered);
        editor.apply();
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
        editor.apply();
    }

    /**
     * Checks if the client has accepted the terms of use
     * @return
     */
//    public boolean hasAcceptedTerms() {
//        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
//        int termsVersion = getResources().getInteger(R.integer.terms_of_use_version);
//        return settings.getBoolean("has_accepted_terms_v"+termsVersion, false);
//    }

    /**
     * Sets whether the client has accepted the terms of use.
     * @param hasAcceptedTerms
     */
//    public void setHasAcceptedTerms(Boolean hasAcceptedTerms) {
//        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
//        SharedPreferences.Editor editor = settings.edit();
//        int termsVersion = getResources().getInteger(R.integer.terms_of_use_version);
//        editor.putBoolean("has_accepted_terms_v"+termsVersion, hasAcceptedTerms);
//        editor.apply();
//    }

    /**
     * Returns an instance of the user preferences.
     * This is just the default shared preferences
     * @return
     */
    public SharedPreferences getUserPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * Checks if we have internet
     * @return
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Sets the currently selected key term in the resources pane.
     * We store this here so selections persist between orientation changes
     * @param term
     */
//    public void setSelectedKeyTerm(Term term) {
//        mSelectedKeyTerm = term;
//    }

    /**
     * Returns the curently selected key term
     * @return
     */
//    public Term getSelectedKeyTerm() {
//        return mSelectedKeyTerm;
//    }

    /**
     * Check if the important terms should be shown
     * @return
     */
//    public boolean getShowImportantTerms() {
//        return mShowImportantTerms;
//    }

    /**
     * Sets if the resources pane should display the important terms.
     * This is needed to persist selection between orientation change
     * @param showImportantTerms
     */
//    public void setShowImportantTerms(boolean showImportantTerms) {
//        this.mShowImportantTerms = showImportantTerms;
//    }

    /**
     * Returns the directory where temporary indexes are stored
     * @return
     */
//    public File getCacheIndexDir() {
//        return new File(getCacheDir(), "index");
//    }

    /**
     * Returns the directory where indexes are stored
     * @return
     */
//    public File getIndexDir() {
//        return new File(getFilesDir(), "index");
//    }
}
