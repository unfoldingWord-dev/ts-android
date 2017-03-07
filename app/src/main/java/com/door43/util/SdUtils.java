package com.door43.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.provider.DocumentFile;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by blm on 12/30/15.
 * Utilities to make it easier to work with SD card access, because there are unique behaviors which
 * each version of Android. There are big changes starting with Lollipop - in particular the special
 * DocumentFile access to SD card.  There are a combination of issues to deal with in this case:
 *
 *      The user must first be prompted to enable SD card access (we launch a special activity for
 *      this).  When the user has approved, the API returns a special uri and code.  This special uri
 *      is the base folder for the SD card access and it must be accessed using DocumentFile rather
 *      than File.  Paths for accessing files will be relative to this.
 *
 *      The special code must be used in combination with the special uri to enable SD card access for
 *      each session.  We store these values so we don't have to request SD card access from the user
 *      each time, but we can unlock SD card whenever we need to read/write to SD card.
 *
 *      We also need to convert these uri's into readable file paths to display to the user.
 *
 *  Each device has a unique path for the SD card, and if you use more than one SD card then each one
 *  may have a different path.  So we have to search for it.
 *
 */
public class SdUtils {
    public static final String DOWNLOAD_FOLDER = "/Download";
    public static final String DOWNLOAD_TRANSLATION_STUDIO_FOLDER = DOWNLOAD_FOLDER + "/" + App.PUBLIC_DATA_DIR;
    public static final int KB = 1024;
    public static final int MB = 1024 * 1024;
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

                String actualPath = findSdCardFolder();
                if(actualPath == null) {
                    actualPath = "SD_CARD"; // use place holder text if we failed to find true path
                }
                String showPath = actualPath + "/" + Uri.decode(subPath);
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

    /**
     * persist the special SD card access values returned by the API
     * @param sdUri
     * @param flags
     */
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

    /**
     * This enables SD card access for this session.
     * @param sdUri - this is a special uri that is the base for the SD card access.  Paths will be relative to this.
     * @param flags - this is a special code that must be used in combination with the special uri to enable SD card access.
     * @return
     */
    public static boolean applyPermissions(Uri sdUri, Integer flags) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Logger.i(SdUtils.class.getName(), "Apply permissions to URI '" + sdUri.toString() + "' flags: " + flags);
            int takeFlags = flags
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                App.context().grantUriPermission(App.context().getPackageName(), sdUri, takeFlags); //TODO 12/22/2015 need to find way to remove this warning
                App.context().getContentResolver().takePersistableUriPermission(sdUri, takeFlags);
            } catch (Exception e) {
                Logger.e(SdUtils.class.getName(), "Failed to Apply Permissions",e);
                return false;
            }
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
            fout = createOutputStream(document);
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
     * @param subFolder - path of subfolder of SD card to move to
     * @return
     */
    public static DocumentFile sdCardMkdirs(final String subFolder) {
        String sdCardFolderUriStr = getSdCardAccessUriStr();
        if(null == sdCardFolderUriStr) {
            return null;
        }

        Uri sdCardFolderUri = Uri.parse(sdCardFolderUriStr);
        DocumentFile document = DocumentFile.fromTreeUri(App.context(), sdCardFolderUri);
        DocumentFile subDocument = documentFileMkdirs(document, subFolder);
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
        OutputStream out = App.context().getContentResolver().openOutputStream(outputFile.getUri(), "w");
        return new BufferedOutputStream(out); // add buffering to improve performance on writing to SD card
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
     * deletes a DocumentFile in Uri.
     * @param baseUri - base folder
     * @param fileName - name of subfolder to move to
     * @return
     */
    public static boolean documentFileDelete(final Uri baseUri, final String fileName) {
        try {
            DocumentFile documentFileFolder = getDocumentFileFolder(baseUri);
            DocumentFile file = documentFileFolder.findFile(fileName);
            if(file != null) {
                file.delete();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * traverse URI to get the DocumentFile for a folder
     * @param baseUri
     * @return
     */
    private static DocumentFile getDocumentFileFolder(Uri baseUri) {
        DocumentFile folder = DocumentFile.fromTreeUri(App.context(), baseUri); // get base for path
        String treeUriStr = folder.getUri().toString(); // see how much of the uri is covered by tree
        if(treeUriStr != null) {
            String baseUriStr = baseUri.toString();
            int location = baseUriStr.indexOf(treeUriStr); // get subfolders from base Uri
            if(location >= 0) {
                String subFolderStr = baseUriStr.substring(location + treeUriStr.length());
                subFolderStr = Uri.decode(subFolderStr);
                DocumentFile subFolder = documentFileMkdirs(folder, subFolderStr); // traverse subfolders
                if(subFolder != null) {
                    folder = subFolder;
                }
            }
        }
        return folder;
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
                String fileName = file.getName();
                if(fileName != null) { // will get null if no permissions
                    String ext = FileUtilities.getExtension(fileName);
                    if (ext != null) {
                        if (ext.toLowerCase().equals(extension)) {
                            if ((file != null) && file.canRead()) {
                                return path;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * determine if Uri points to a regular document (returns true)
     *      DocumentFiles would return false
     * @param folderUri
     * @return
     */
    public static boolean isRegularFile(Uri folderUri) {
        if(folderUri == null) {
            return false;
        }
        String scheme = folderUri.getScheme();
        return "file".equalsIgnoreCase(scheme);
    }

    /**
     * method to determine if file exists, handles DocumentFile type as well as File
     * @param path
     * @param filename
     * @return
     */
    public static boolean exists(Uri path, String filename) {
        boolean isOutputToDocumentFile = !SdUtils.isRegularFile(path);
        if(isOutputToDocumentFile) {
            DocumentFile sdCardFolder = getDocumentFileFolder(path);
            if(sdCardFolder == null) {
                return false;
            }
            DocumentFile sdCardFile = sdCardFolder.findFile(filename);
            return (sdCardFile != null);
        }

        File exportFile = new File(path.getPath(), filename);
        return exportFile.exists();
    }


    /**
     * gets modified date string for file
     * @param context
     * @param path
     * @param filename
     * @return
     */
    public static String getDate(Context context, Uri path, String filename) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM,
                                                            getCurrentLocale(context));
        boolean isOutputToDocumentFile = !SdUtils.isRegularFile(path);
        if(isOutputToDocumentFile) {
            DocumentFile sdCardFolder = getDocumentFileFolder(path);
            if(sdCardFolder != null) {
                DocumentFile sdCardFile = sdCardFolder.findFile(filename);
                if (sdCardFile != null) {
                    Date lastModified = new Date(sdCardFile.lastModified());
                    return format.format(lastModified);
                }
            }
        } else {
            File exportFile = new File(path.getPath(), filename);
            if (exportFile.exists()) {
                Date lastModified = new Date(exportFile.lastModified());
                return format.format(lastModified);
            }
        }
        return "";
    }

    /**
     * gets size of file with units and number localized
     * @param path
     * @param filename
     * @return
     */
    public static String getFormattedFileSize(Context context, Uri path, String filename) {
        long size = getFileSize( path, filename);
        return getFormattedStorageSize(context, size);
    }

    /**
     * gets size of file with units and number localized
     * @param context
     * @param size
     * @return
     */
    public static String getFormattedStorageSize(Context context, double size) {
        final int numberSignificantDigits = 3;
        if(size > MB) {
            double sizeMB = size / MB;
            String formattedStr = getLocalizedDecimalWithSignificantDigits(context, sizeMB, numberSignificantDigits);
            return formattedStr + " " + context.getString(R.string.megabytes_short);
        } else if(size > KB) {
            double sizeKB = size / KB;
            String formattedStr = getLocalizedDecimalWithSignificantDigits(context, sizeKB, numberSignificantDigits);
            return formattedStr + " " + context.getString(R.string.kilobytes_short);
        }
        String formattedStr = getLocalizedDecimalWithSignificantDigits(context, size, 4);
        return formattedStr + " " + context.getString(R.string.bytes_short);
    }

    /**
     * gets size of file with units and number localized
     * @param context
     * @param value
     * @return
     */
    public static String getLocalizedDecimalWithSignificantDigits(Context context, double value, int desiredSignificantDigits) {
        int decimalDigits = getDecimalDigits(value, 3);
        Locale current = getCurrentLocale(context);
        return getLocalizedDecimal(current, value, decimalDigits);
    }

    /**
     * calculate number of decimal digits to get desired number of significant digits for number
     * @param value
     * @param desiredSignificantDigits
     * @return
     */
    private static int getDecimalDigits(double value, int desiredSignificantDigits) {
        if(value == 0) {
            return 0;
        }
        double log10 = Math.log10(value);
        if(log10 >= desiredSignificantDigits) {
            return 0;
        }
        if (log10 < 1) {
            return ((int) -log10) + desiredSignificantDigits - 1;
        }

        return desiredSignificantDigits - 1 - (int) log10;
    }

    /**
     * gets size of file with units and number localized
     * @param current
     * @param value
     * @param maxDecimals
     * @return
     */
    public static String getLocalizedDecimal(Locale current, double value, int maxDecimals) {
        DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(current));
        df.setGroupingUsed(true);
        df.setMaximumFractionDigits(maxDecimals); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
        return df.format(value);
    }

    /**
     * get Locale
     * @return
     */
    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getCurrentLocale(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return context.getResources().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }

    /**
     * gets size of file
     * @param path
     * @param filename
     * @return
     */
    public static long getFileSize(Uri path, String filename) {
        DateFormat format = DateFormat.getDateTimeInstance();
        boolean isOutputToDocumentFile = !SdUtils.isRegularFile(path);
        if(isOutputToDocumentFile) {
            DocumentFile sdCardFolder = getDocumentFileFolder(path);
            if(sdCardFolder != null) {
                DocumentFile sdCardFile = sdCardFolder.findFile(filename);
                if (sdCardFile != null) {
                    return sdCardFile.length();
                }
            }
        } else {
            File exportFile = new File(path.getPath(), filename);
            if (exportFile.exists()) {
                return exportFile.length();
            }
        }
        return 0;
    }

    /**
     * Gets human readable path string
     * @param path
     * @param filename
     * @return
     */
    public static String getPathString(Uri path, String filename) {
        boolean isOutputToDocumentFile = !SdUtils.isRegularFile(path);
        if(isOutputToDocumentFile) {
            String pathStr = getPathString(path.toString());
            return pathStr + "/" + filename;
        }

        File exportFile = new File(path.getPath(), filename);
        return exportFile.toString();
    }
}
