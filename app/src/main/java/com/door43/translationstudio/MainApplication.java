package com.door43.translationstudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.widget.Toast;

import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.translations.TranslationManager;
import com.door43.translationstudio.util.CustomExceptionHandler;
import com.door43.translationstudio.util.DummyDialogListener;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.MainContext;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;
import org.kamranzafar.jtar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Custom application class so we can effectively handle state accross activities and other classes
 */
public class MainApplication extends Application {
    private Activity mCurrentActivity = null;
    private Toast mToast = null;
    private ProjectManager mProjectManager;
    private ProgressDialog mProgressDialog;
    private TranslationManager mTranslationManager;
    public static final String PREFERENCES_TAG = "com.door43.translationstudio";
    private ImageLoader mImageLoader;
    private Activity mCurrentDialogActivity;
    private Map<String, ArrayList<String>> mNotificationsMap = new HashMap<String, ArrayList<String>>();
    static final int BUFFER = 2048;
    private static Typeface mTranslationTypeface;
    private static String mSelectedTypeface = "";
    private static Activity mMainActivity;
    private Term mSelectedKeyTerm;
    private boolean mShowImportantTerms;
    public static final String STACKTRACE_DIR = "stacktrace";
    private boolean mClosingProgressDialog = false;

    public void onCreate() {

        // initialize basic functions with link to main application
        new MainContext(this);

        // specify the logging level to store.
        // TODO: we should provide user settings so users can control which levels to log.
        Logger.setLoggingLevel(Logger.Level.Warning);

        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            File dir = new File(getExternalCacheDir(), STACKTRACE_DIR);
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(dir));
        }

        // initialize default settings
        // NOTE: make sure to add any new preference files here in order to have their default values properly loaded.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_save_and_sync, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_sharing, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_advanced, false);

        mProjectManager = new ProjectManager(this);
        mTranslationManager = new TranslationManager(this);
    }

    /**
     * Sets the main activity that can be used for displaying dialogs.
     * @param activity
     */
    public static void setMainActivity(Activity activity) {
        mMainActivity = activity;
    }

    /**
     * Returns the custom typeface used for translation
     * @return
     */
    public Typeface getTranslationTypeface() {
        String typeFace = MainContext.getContext().getUserPreferences().getString(SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE, MainContext.getContext().getResources().getString(R.string.pref_default_translation_typeface));
        if(!mSelectedTypeface.equals(typeFace)) {
            mTranslationTypeface = Typeface.createFromAsset(getAssets(), "fonts/" + typeFace);
        }
        mSelectedTypeface = typeFace;

        // TODO: fonts should be initialized when the app starts.
        // TODO: should return the default font if the font is missing.
        return mTranslationTypeface;
    }

    /**
     * Checks if the app should use the saved positions.
     * @return
     */
    public boolean rememberLastPosition() {
        return getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)));
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
    public void sendNotification(int notificationId, int titleResourceId, String message) {
        // keep track of all the notifications
        ArrayList<String> notifications;
        if(mNotificationsMap.containsKey(""+notificationId)) {
            notifications = mNotificationsMap.get(""+notificationId);
        } else {
            // add new notification group
            notifications = new ArrayList<String>();
            mNotificationsMap.put("" + notificationId, notifications);
        }

        // build notification
        notifications.add(message);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_notify_msg)
                        .setContentTitle(getResources().getString(titleResourceId))
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setNumber(notifications.size());

        // build big notification
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(getResources().getString(titleResourceId));
        for (String event:notifications) {
            inboxStyle.addLine(event);
        }
        mBuilder.setStyle(inboxStyle);

        // issue notification
        NotificationManager mNotifyMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNotifyMgr.notify(notificationId, mBuilder.build());
        } else {
            mNotifyMgr.notify(notificationId, mBuilder.getNotification());
        }
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

    /**
     *
     * @param title
     * @param msg
     * @param positiveBtn
     * @param negativeBtn
     * @param positiveListener
     * @param negativeListener
     */
    public void showMessageDialog(int title, String msg, int positiveBtn, int negativeBtn, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(msg)
                .setPositiveButton(positiveBtn, positiveListener)
                .setNegativeButton(negativeBtn, negativeListener).show();
    }

    public void showMessageDialog(int title, int msg, int positiveBtn, int negativeBtn, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        showMessageDialog(title, getResources().getString(msg), positiveBtn, negativeBtn, positiveListener, negativeListener);
    }

    public void showMessageDialog(int title, String msg) {
        showMessageDialog(getResources().getString(title), msg);
    }

    public void showMessageDialog(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getCurrentActivity());
        builder.setTitle(title).setMessage(msg).setPositiveButton(R.string.label_ok, new DummyDialogListener()).show();
    }

    public void showMessageDialog(int title, int msg) {
        showMessageDialog(title, getResources().getString(msg));
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
                if(mMainActivity != null && !mClosingProgressDialog) {
                    if (mProgressDialog == null || mCurrentDialogActivity != mMainActivity) { // was using: getCurrentActivity()
                        closeProgressDialog();
                        mProgressDialog = new ProgressDialog(mMainActivity); // was using: getCurrentActivity()
                    }
                    mProgressDialog.setMessage(message);
                    if (!mProgressDialog.isShowing()) {
                        mProgressDialog.show();
                    }
                } else if(mProgressDialog != null) {
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
     * Closes the progress dialog
     * @param agressive if set to true the progress dialog will be closed the next time it opens if it is not currently shown
     */
    public void closeProgressDialog(boolean agressive) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mClosingProgressDialog = false;
            try {
                mProgressDialog.dismiss();
            } catch (Exception e) {
                showToastMessage(e.getMessage());
            }
        } else if(mProgressDialog != null) {
            mClosingProgressDialog = true;
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
    public boolean hasAcceptedTerms() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        int termsVersion = getResources().getInteger(R.integer.terms_of_use_version);
        return settings.getBoolean("has_accepted_terms_v"+termsVersion, false);
    }

    /**
     * Sets whether the client has accepted the terms of use.
     * @param hasAcceptedTerms
     */
    public void setHasAcceptedTerms(Boolean hasAcceptedTerms) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        int termsVersion = getResources().getInteger(R.integer.terms_of_use_version);
        editor.putBoolean("has_accepted_terms_v"+termsVersion, hasAcceptedTerms);
        editor.apply();
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

    /**
     * Creates a zip archive
     * http://stackoverflow.com/questions/6683600/zip-compress-a-folder-full-of-files-on-android
     * @param sourcePath
     * @param destPath
     * @throws IOException
     */
    public void zip(String sourcePath, String destPath) throws IOException {
        final int BUFFER = 2048;
        File sourceFile = new File(sourcePath);
        BufferedInputStream origin = null;
        FileOutputStream dest = new FileOutputStream(destPath);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                dest));
        if (sourceFile.isDirectory()) {
            // TRICKY: we add 1 to the base path length to exclude the leading path separator
            zipSubFolder(out, sourceFile, sourceFile.getParent().length() + 1);
        } else {
            byte data[] = new byte[BUFFER];
            FileInputStream fi = new FileInputStream(sourcePath);
            origin = new BufferedInputStream(fi, BUFFER);
            String[] segments = sourcePath.split("/");
            String lastPathComponent = segments[segments.length - 1];
            ZipEntry entry = new ZipEntry(lastPathComponent);
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
        }
        out.close();
    }

    /**
     * Zips up a sub folder
     * @param out
     * @param folder
     * @param basePathLength
     * @throws IOException
     */
    private void zipSubFolder(ZipOutputStream out, File folder, int basePathLength) throws IOException {
        final int BUFFER = 2048;
        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath.substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    /**
     * Extracts a zip archive
     * @param zipPath
     * @throws IOException
     */
    public void unzip(String zipPath, String destPath) throws IOException {
        InputStream is;
        ZipInputStream zis;
        String filename;
        ZipEntry ze;
        int count;
        byte[] buffer = new byte[1024];
        is = new FileInputStream(zipPath);
        zis = new ZipInputStream(new BufferedInputStream(is));

        File destDir = new File(destPath);
        destDir.mkdirs();

        while ((ze = zis.getNextEntry()) != null) {
            filename = ze.getName();
            File f = new File(destPath, filename);
            if (ze.isDirectory()) {
                f.mkdirs();
                continue;
            }
            f.getParentFile().mkdirs();
            f.createNewFile();
            FileOutputStream fout = new FileOutputStream(f.getAbsolutePath());
            while ((count = zis.read(buffer)) != -1) {
                fout.write(buffer, 0, count);
            }
            fout.close();
            zis.closeEntry();
        }
        zis.close();
    }


    /**
     * Extracts a tar file
     * @param tarPath
     * @throws IOException
     */
    public void untarTarFile(String tarPath, String destPath) throws IOException {
        File destFolder = new File(destPath);
        destFolder.mkdirs();

        File zf = new File(tarPath);

        TarInputStream tis = new TarInputStream(new BufferedInputStream(new FileInputStream(zf)));
        untar(tis, destFolder.getAbsolutePath());

        tis.close();

    }

    private void untar(TarInputStream tis, String destFolder) throws IOException {
        BufferedOutputStream dest = null;

        TarEntry entry;
        while ((entry = tis.getNextEntry()) != null) {
//            System.out.println("Extracting: " + entry.getName());
            int count;
            byte data[] = new byte[BUFFER];

            if (entry.isDirectory()) {
                new File(destFolder + "/" + entry.getName()).mkdirs();
                continue;
            } else {
                int di = entry.getName().lastIndexOf('/');
                if (di != -1) {
                    new File(destFolder + "/" + entry.getName().substring(0, di)).mkdirs();
                }
            }

            FileOutputStream fos = new FileOutputStream(destFolder + "/" + entry.getName());
            dest = new BufferedOutputStream(fos);

            while ((count = tis.read(data)) != -1) {
                dest.write(data, 0, count);
            }

            dest.flush();
            dest.close();
        }
    }

    /**
     * Generates a zipped archive of the project
     * @param sourcePath the directory to archive
     * @return the path to the project archive
     */
    public void tar(String sourcePath, String destPath) throws IOException {
        // build dest
        FileOutputStream dest = new FileOutputStream(destPath);
        TarOutputStream out = new TarOutputStream( new BufferedOutputStream( dest ) );
        tarFolder(null, sourcePath, out);
        out.close();
    }

    private void tarFolder(String parent, String path, TarOutputStream out) throws IOException {
        BufferedInputStream origin;
        File f = new File(path);
        String files[] = f.list();

        // is file
        if (files == null) {
            files = new String[1];
            files[0] = f.getName();
        }

        parent = ((parent == null) ? (f.isFile()) ? "" : f.getName() + "/" : parent + f.getName() + "/");

        for (int i = 0; i < files.length; i++) {
//            System.out.println("Adding: " + files[i]);
            File fe = f;
            byte data[] = new byte[BUFFER];

            if (f.isDirectory()) {
                fe = new File(f, files[i]);
            }

            if (fe.isDirectory()) {
                String[] fl = fe.list();
                if (fl != null && fl.length != 0) {
                    tarFolder(parent, fe.getPath(), out);
                } else {
                    TarEntry entry = new TarEntry(fe, parent + files[i] + "/");
                    out.putNextEntry(entry);
                }
                continue;
            }

            FileInputStream fi = new FileInputStream(fe);
            origin = new BufferedInputStream(fi);
            TarEntry entry = new TarEntry(fe, parent + files[i]);
            out.putNextEntry(entry);

            int count;

            while ((count = origin.read(data)) != -1) {
                out.write(data, 0, count);
            }

            out.flush();

            origin.close();
        }
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
    public void setSelectedKeyTerm(Term term) {
        mSelectedKeyTerm = term;
    }

    /**
     * Returns the curently selected key term
     * @return
     */
    public Term getSelectedKeyTerm() {
        return mSelectedKeyTerm;
    }

    /**
     * Check if the important terms should be shown
     * @return
     */
    public boolean getShowImportantTerms() {
        return mShowImportantTerms;
    }

    /**
     * Sets if the resources pane should display the important terms.
     * This is needed to persist selection between orientation change
     * @param showImportantTerms
     */
    public void setShowImportantTerms(boolean showImportantTerms) {
        this.mShowImportantTerms = showImportantTerms;
    }
}
