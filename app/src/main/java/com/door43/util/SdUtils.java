package com.door43.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.provider.DocumentFile;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.ui.SettingsActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 12/30/15.
 * Utilities to make it easier to work with SD card access, and in particular
 * the special DOcumentFile access introduced with Lollipop
 */
public class SdUtils {
    public static final String DOWNLOAD_FOLDER = "/Download";
    public static final String DOWNLOAD_TRANSLATION_STUDIO_FOLDER = DOWNLOAD_FOLDER + "/" + App.PUBLIC_DATA_DIR;
    private static String sdCardPath = "";
    private static boolean alreadyReadSdCardDirectory = false;
    private static String verifiedSdCardPath = "";
    public static final int REQUEST_CODE_STORAGE_ACCESS = 42;
    private static final String FILE_TYPE = "file://";
    private static final String CONTENT_TYPE = "content://";
    private static final String CONTENT_DIVIDER = "%3A";

    /**
     * combines string array into single string
     * @param parts
     * @param delimeter
     * @return
     */
    public static String joinString(String[] parts, String delimeter) {
        StringBuilder sbStr = new StringBuilder();
        for (int i = 0, il = parts.length; i < il; i++) {
            if (i > 0) {
                sbStr.append(delimeter);
            }
            sbStr.append(parts[i]);
        }
        return sbStr.toString();
    }

    /**
     * Gets human readable path string
     * @param dir
     * @return
     */
    public static String getPathString(DocumentFile dir) {
        if(null == dir) {
            return "<null>";
        }

        String uriStr = dir.getUri().toString();
        return getPathString(uriStr);
    }

    /**
     * Gets human readable path string
     * @param dir
     * @return
     */
    public static String getPathString(final String dir) {
        if(null == dir) {
            return "<null>";
        }

        String uriStr = dir;

        int pos = uriStr.indexOf(FILE_TYPE);
        if(pos >= 0) {
            String showPath = uriStr.substring(pos + FILE_TYPE.length());
            Logger.i(SdUtils.class.getName(), "converting File path from '" + dir + "' to '" + showPath + "'");
            return showPath;
        }

        pos = uriStr.indexOf(CONTENT_TYPE);
        if(pos >= 0) {
            pos = uriStr.lastIndexOf(CONTENT_DIVIDER);
            if(pos >= 0) {
                String subPath =  uriStr.substring(pos + CONTENT_DIVIDER.length());
                String showPath = "SD_CARD/" + Uri.decode(subPath);
                Logger.i(SdUtils.class.getName(), "converting SD card path from '" + dir + "' to '" + showPath + "'");
                return showPath;
            }
        }

        return uriStr;
    }

    /**
     * returns true if we need to enable SD card access
     */
    public static boolean doWeNeedToRequestSdCardAccess() {
        Logger.i(SdUtils.class.getName(), "version API: " + Build.VERSION.SDK_INT);
        Logger.i(SdUtils.class.getName(), "Environment.getExternalStorageDirectory(): " + Environment.getExternalStorageDirectory());
        Logger.i(SdUtils.class.getName(), "Environment.getExternalStorageState(): " + Environment.getExternalStorageState());

        restoreSdCardWriteAccess(); // only does something if supported on device
        if (!isSdCardAccessable()) { // if accessable, we do not need to request access
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // can only request access if lollipop or greater
                if( isSdCardPresentLollipop() ) {
                    return true; // if there is an SD card present, is there any point in requesting access
                }
            }
        }

        return false;
    }

    /**
     * if available, this triggers browser dialog for user to select SD card folder to allow access
     * @param context
     */
    public static void triggerStorageAccessFramework(final Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // doesn't look like this is possible on Kitkat
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            context.startActivityForResult(intent, REQUEST_CODE_STORAGE_ACCESS);
        } else {
            Logger.w(SdUtils.class.toString(),"triggerStorageAccessFramework: not supported for " + Build.VERSION.SDK_INT);
        }
    }

    /**
     * persists write permission for SD card access
     * @param sdUri - uri to persist
     * @param flags - permission flags
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean validateSdCardWriteAccess(final Uri sdUri, final int flags) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true;
        }

        boolean success = persistSdCardWriteAccess(sdUri, flags);

        String sdCardActualFolder = findSdCardFolder();
        if(sdCardActualFolder != null) {
            Logger.i(SdUtils.class.getName(), "found card at = " + sdCardActualFolder);
        } else {
            Logger.i(SdUtils.class.getName(), "invalid access Uri = " + sdUri);
            storeSdCardAccess(null, 0); // clear value since invalid
            success = false;
        }

        return success;
    }

    /**
     * persists write permission for SD card access
     * @param sdUri - uri to persist
     * @param flags - permission flags
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean persistSdCardWriteAccess(final Uri sdUri, final int flags) {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true;
        }

        storeSdCardAccess(sdUri, flags);

        restoreSdCardWriteAccess(); // apply settings

        boolean success = isSdCardAccessable();
        if(!success) {
            storeSdCardAccess(null, 0); // clear value since invalid
        }
        return success;
    }

    private static void storeSdCardAccess(Uri sdUri, int flags) {
        String uriStr = (null == sdUri) ? null : sdUri.toString();
        App.setUserString(SettingsActivity.KEY_SDCARD_ACCESS_URI, uriStr);
        App.setUserString(SettingsActivity.KEY_SDCARD_ACCESS_FLAGS, String.valueOf(flags));
        Logger.i(SdUtils.class.getName(), "URI = " + sdUri);
        verifiedSdCardPath = ""; // reset persisted path to SD card, will need to find it again
    }

    /**
     * restores previously granted write permission for SD card access
     * @return
     */
    public static boolean restoreSdCardWriteAccess() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String flagStr = App.getUserString(SettingsActivity.KEY_SDCARD_ACCESS_FLAGS, null);
            String path = App.getUserString(SettingsActivity.KEY_SDCARD_ACCESS_URI, null);
            if ((path != null) && (flagStr != null)) {

                Integer flags = Integer.parseInt(flagStr);
                Uri sdUri = Uri.parse(path);
                Logger.i(SdUtils.class.getName(), "Restore URI = " + sdUri.toString());
                applyPermissions(sdUri, flags);
                return true;
            }
        }
        return false;
    }

    public static boolean applyPermissions(Uri sdUri, Integer flags) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Logger.i(SdUtils.class.getName(), "Apply permissions to URI '" + sdUri.toString() + "' flags: " + flags);
            int takeFlags = flags
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            App.context().grantUriPermission(App.context().getPackageName(), sdUri, takeFlags); //TODO 12/22/2015 need to find way to remove this warning
            App.context().getContentResolver().takePersistableUriPermission(sdUri, takeFlags);
            return true;
        }
        return false;
    }

    /**
     * reads the stored URI for SD card access
     * @return
     */
    public static String getSdCardAccessUriStr() {
        String path = App.getUserString(SettingsActivity.KEY_SDCARD_ACCESS_URI, null);
        return path;
    }

    /**
     * gets the SD card downloads folder
     * @return
     */
    public static DocumentFile getSdCardDownloadsFolder() {
        DocumentFile downloadFolder = sdCardMkdirs( DOWNLOAD_FOLDER);
        return downloadFolder;
    }

    /**
     * Returns true if an external SD card is present and writeable on Android Lollipop or greater
     * @return
     */
    public static boolean isSdCardPresentLollipop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!verifiedSdCardPath.isEmpty()) { // see if we already have detected an SD card this session
                return true;
            }

            // see if we already have enabled access
            if( getSdCardAccessUriStr() != null) { // if user has already chosen a path for SD card
                if(sdCardMkdirs(null) != null) { // verify card is still present and writeable
                    return true;
                }
            }

            // rough check to see if SD card is currently present
            File sdCard = getSdCardDirectory();
            if(sdCard != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the SD card directory.  Warning this may not be writeable.
     * Just a rough check to see if one is possibly present.
     * @return
     */
    public static File getSdCardDirectory() {

        if (!verifiedSdCardPath.isEmpty()) {
            return new File(verifiedSdCardPath);
        }

        if(alreadyReadSdCardDirectory) {
            if (!sdCardPath.isEmpty()) {
                return new File(sdCardPath);
            } else {
                return null;
            }
        }

        alreadyReadSdCardDirectory = true;

        String[] mounts = getExternalDirectories();

        try {
            if( (mounts != null) && (mounts.length > 0)) {
                String path = null;
                for(String mount:mounts) {
                    File mountFile = new File(mount);
                    String state = EnvironmentCompat.getStorageState(mountFile);
                    boolean mounted = Environment.MEDIA_MOUNTED.equals(state);
                    if(mounted) {
                        if(mount.toLowerCase().indexOf("emulated") < 0) {
                            path = mount;
                            break;
                        }
                    }
                }
                if(path != null) {
                    File absolute = new File(path).getCanonicalFile();
                    sdCardPath = absolute.toString(); // cache value
                    return absolute;
                }
                sdCardPath = "";
            }
        } catch (Exception e) {
            Logger.w(SdUtils.class.toString(),"Error getting external card folder", e);
        }
        return null;
    }

    /**
     * Returns list of all the external directories.  Requires additional checking to see if any of
     * these belong to external SD card.
     * @return
     */
    public static String[] getExternalDirectories() {

        List<String> mounts = new ArrayList<>();
        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            String line;
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                if (line.contains("secure")) continue;
                if (line.contains("asec")) continue;

                Logger.i(SdUtils.class.getName(),"Checking: " + line);

                if (line.contains("fat")) {//TF card
                    String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        mounts.add(0,columns[1]);
                        Logger.i(SdUtils.class.getName(), "Adding: " + columns[1]);
                    }
                } else if (line.contains("fuse")) {//internal storage
                    String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        mounts.add(columns[1]);
                        Logger.i(SdUtils.class.getName(), "Adding: " + columns[1]);
                    }
                }
            }

            return mounts.toArray(new String[mounts.size()]);

        } catch (Exception e) {
            Logger.w(SdUtils.class.toString(),"Error getting external card folder", e);
        }

        return null;
    }

    /**
     * get external storage folder - may not be mounted
     * @return
     */
    private static File getLegacyExternalStorageDirectory() {
        String path = System.getenv("EXTERNAL_STORAGE");
        return new File(path);
    }


    /**
     * Checks if the external media is mounted and writeable
     * @return
     */
    public static boolean isSdCardAccessable() {
        // TRICKY: KITKAT introduced changes to the external media that made sd cards read only,
        //      and now starting with Lollipop the user has to grant access permission
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            File sdCard = getSdCardDirectory();
            return sdCard != null;
        } else {
            DocumentFile sdCard = sdCardMkdirs(null);
            return sdCard != null;
        }
    }

    /**
     * Searches and verifies write location on SD card
     * @return
     */
    public static String findSdCardFolder() {

        if(!verifiedSdCardPath.isEmpty()) {
            return verifiedSdCardPath;
        }

        String[] mounts = getExternalDirectories();
        String sdPath = null;

        final String testFolder = "__testing.dir__";

        if(null == mounts) {
            return null;
        }

        DocumentFile sdCardTempFolder = sdCardMkdirs(testFolder);
        boolean success = sdCardTempFolder != null;
        if (success) {
            if (sdCardTempFolder.canWrite()) {
                DocumentFile file = sdCardTempFolder.createFile("text/plain", "_zzztestzzz_.txt"); // make sure URI write accessable
                String testData = "test Data";
                success = documentFolderWrite(file, testData, false); // make sure we can write
                if(file.length() < testData.length()) {
                    success = false;
                }
                file.delete(); // cleanup after use

                try {
                    if(success) {
                        if (mounts.length > 0) {

                            for (String mount : mounts) {

                                final String externalStorageState = EnvironmentCompat.getStorageState(new File(mount));
                                boolean mounted = Environment.MEDIA_MOUNTED.equals(externalStorageState);
                                if (!mounted) { // do a double check
                                    continue;
                                }

                                File testFolderFile = new File(mount, testFolder);
                                boolean isPresent = testFolderFile.exists();
                                if (isPresent) {
                                    Logger.i(SdUtils.class.toString(), "found folder: " + testFolderFile.toString());
                                    sdPath = mount;
                                    break;
                                }
                            } // end for mounts
                        }

                        if (null == sdPath) {
                            Logger.i(SdUtils.class.toString(), "SD card folder not found");
                        } else {
                            verifiedSdCardPath = sdPath;
                        }
                    }

                    sdCardTempFolder.delete(); // remove test folder

                } catch (Exception e) {
                    Logger.w(SdUtils.class.toString(),"Error getting external card folder", e);
                }
            }
        }
        return sdPath;
    }

    /**
     * write string to document file
     * @param document - document to write
     * @param data - text to write to file
     * @param append - if true then data is appended to file, if false then it is overwritten
     * @return
     */
    public static boolean documentFolderWrite(final DocumentFile document, final String data, final boolean append) {

        boolean success = true;
        OutputStream fout = null;

        try {
            fout = App.context().getContentResolver().openOutputStream(document.getUri());
            fout.write(data.getBytes());
            fout.close();
        } catch (Exception e) {
            Logger.i(SdUtils.class.getName(), "Could not write to folder");
            success = false; // write failed
        } finally {
            try {
                fout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return success;
    }

    /**
     * creates and returns the selected subfolder on SD card.  Returns null if error.
     * @param subFolderName - name of subfolder to move to
     * @return
     */
    public static DocumentFile sdCardMkdirs(final String subFolderName) {

        String sdCardFolderUriStr = getSdCardAccessUriStr();
        if(null == sdCardFolderUriStr) {
            return null;
        }

        Uri sdCardFolderUri = Uri.parse(sdCardFolderUriStr);
        DocumentFile document = DocumentFile.fromTreeUri(App.context(), sdCardFolderUri);
        DocumentFile subDocument = documentFileMkdirs(document, subFolderName);
        if ( (subDocument != null) && subDocument.isDirectory() && subDocument.canWrite() ) {
            return subDocument;
        }

        return null;
    }

    /**
     * creates a OutputStream for a DocumentFile.  throws exception on error.
     * @param outputFile -
     * @return
     */
    public static OutputStream createOutputStream(DocumentFile outputFile) throws FileNotFoundException {
        return App.context().getContentResolver().openOutputStream(outputFile.getUri());
    }

    /**
     * creates a DocumentFile in Uri.  throws exception on error.
     * @param baseUri - base folder
     * @param fileName - name of subfolder to move to
     * @return
     */
    public static DocumentFile documentFileCreate(final Uri baseUri, final String fileName) {
        DocumentFile documentFileFolder = getDocumentFileFolder(baseUri);
        DocumentFile documentFile = documentFileCreate(documentFileFolder, fileName);
        return documentFile;
    }

    /**
     * traverse URL to get DocumentFile for folder
     * @param baseUri
     * @return
     */
    private static DocumentFile getDocumentFileFolder(Uri baseUri) {
        DocumentFile sdCardFolder = DocumentFile.fromTreeUri(App.context(), baseUri); // get base for path
        String baseUrlStr = baseUri.toString();
        String sdCardFolderUriStr = sdCardFolder.getUri().toString();

        if(sdCardFolderUriStr != null) {
            int location = baseUrlStr.indexOf(sdCardFolderUriStr); // get subfolders from base
            if(location >= 0) {
                String subFolderStr = baseUrlStr.substring(location + sdCardFolderUriStr.length());
                subFolderStr = Uri.decode(subFolderStr);
                DocumentFile subFolder = documentFileMkdirs(sdCardFolder, subFolderStr); // travers subfolders
                if(subFolder != null) {
                    sdCardFolder = subFolder;
                }
            }
        }
        return sdCardFolder;
    }

    /**
     * creates a DocumentFile in basefolder.  throws exception on error.
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @return
     */
    public static DocumentFile documentFileCreate(final DocumentFile baseFolder, final String subFolderName) {
        return baseFolder.createFile("image", subFolderName);
    }

    /**
     * creates and returns the selected subfolder.  Returns null if error.
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @return
     */
    public static DocumentFile documentFileMkdirs(final DocumentFile baseFolder, final String subFolderName) {
        return traverseSubDocFolders(baseFolder, subFolderName, true);
    }

    /**
     * get the selected subfolder recursively or null if not present
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @return
     */
    public static DocumentFile documentFileChgdirs(final DocumentFile baseFolder, final String subFolderName) {
        return traverseSubDocFolders(baseFolder, subFolderName, false);
    }

    /**
     * get the selected subfolder recursively or null if error
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @param createFolders - if true then missing folders will be created
     * @return
     */
    private static DocumentFile traverseSubDocFolders( DocumentFile baseFolder, final String subFolderName, boolean createFolders) {
        if(null == baseFolder) {
            return null;
        }

        if(subFolderName != null) {
            String[] parts = subFolderName.split("\\/");
            if (parts.length < 1) {
                return null;
            }

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].isEmpty()) { // skip over extraneous slashes
                    continue;
                }

                DocumentFile nextDocument = documentFolderChgdir(baseFolder, parts[i], createFolders);
                if (null == nextDocument) {
                    return null;
                }

                baseFolder = nextDocument;
            }
        }

        return baseFolder;
    }

    /**
     * get the selected subfolder or null if error
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @param createFolders - if true then missing folders will be created
     * @return
     */
    private static DocumentFile documentFolderChgdir(final DocumentFile baseFolder, final String subFolderName, boolean createFolders) {

        if(baseFolder == null) {
            return null;
        }

        DocumentFile nextDocument = baseFolder.findFile(subFolderName);
        try {

            if( (nextDocument == null) && createFolders) {
                nextDocument = baseFolder.createDirectory(subFolderName);
            }

        } catch (Exception e) {
            Logger.w(SdUtils.class.getName(),"Failed to create folder", e);
            return null;
        }

        return nextDocument;
    }

    /**
     * find first instance of file in folder or null if not found
     * @param folder - folder to search
     * @param fileName - filename to find
     * @return
     */
    public static DocumentFile documentFileFind(final DocumentFile folder, final String fileName) {

        if(folder == null) {
            return null;
        }

        DocumentFile nextDocument = folder.findFile(fileName);
        return nextDocument;
    }

    /**
     * finds first instance of file type in folder
     * @param baseFolder - base folder for search
     * @param extension - extension to match
     * @return
     */
    public static String searchFolderAndParentsForDocFile(final DocumentFile baseFolder, final String extension) {

        if(null == baseFolder) {
            return null;
        }

        String[] paths = new String[]{ DOWNLOAD_TRANSLATION_STUDIO_FOLDER, DOWNLOAD_FOLDER, ""}; // try in this order

        for(String path: paths) {
            DocumentFile document = documentFileChgdirs(baseFolder, path);
            if (null == document) {
                continue;
            }

            DocumentFile[] files = document.listFiles();
            for(DocumentFile file: files) {
                String ext = FileUtilities.getExtension(file.getName());
                if(ext != null) {
                    if (ext.toLowerCase().equals(extension)) {
                        if ((file != null) && file.canRead()) {
                            return path;
                        }
                    }
                }
            }
        }
        return null;
    }
}
