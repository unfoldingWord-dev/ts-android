package com.door43.translationstudio.core;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.spannables.USFMVerseSpan;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blm on 4/2/16.
 */
public class ImportUsfm {
    public static final String TAG = ImportUsfm.class.getSimpleName();
    public static final String BOOK_NAME_MARKER = "\\\\toc1\\s([^\\n]*)";
    private static final Pattern PATTERN_BOOK_NAME_MARKER = Pattern.compile(BOOK_NAME_MARKER);
    public static final String BOOK_SHORT_NAME_MARKER = "\\\\toc3\\s([^\\n]*)";
    private static final Pattern PATTERN_BOOK_SHORT_NAME_MARKER = Pattern.compile(BOOK_SHORT_NAME_MARKER);
    public static final String SECTION_MARKER = "\\\\s5([^\\n]*)";
    private static final Pattern PATTERN_SECTION_MARKER = Pattern.compile(SECTION_MARKER);
    public static final String CHAPTER_NUMBER_MARKER = "\\\\c\\s(\\d+(-\\d+)?)\\s";
    private static final Pattern PATTERN_CHAPTER_NUMBER_MARKER = Pattern.compile(CHAPTER_NUMBER_MARKER);
    private static final Pattern PATTERN_USFM_VERSE_SPAN = Pattern.compile(USFMVerseSpan.PATTERN);

    private File mTempDir;
    private File mTempOutput;
    private File mTempDest;
    private File mTempSrce;
    private File mProjectFolder;

    private String mChapter;
    private List<File> mSourceFiles;

    private List<File> mImportProjects;
    private HashMap<String, JSONObject> mChunks; // // TODO: 4/7/16 this will be replaced with system call
    private List<String> mErrors;
    private List<String> mFoundBooks;
    private int mCurrentBook;

    private String mBookName;
    private String mBookShortName;
    private JSONObject mChunk;
    private TargetLanguage mTargetLanguage;
    private Activity mContext;
    private boolean mSuccess;

    public ImportUsfm(Activity context, TargetLanguage targetLanguage) {
        createTempFolders();
        mSourceFiles = new ArrayList<>();
        mImportProjects = new ArrayList<>();
        mErrors = new ArrayList<>();
        mFoundBooks = new ArrayList<>();
        mChunks = new HashMap<>();
        mTargetLanguage = targetLanguage;
        mCurrentBook = 0;
        mContext = context;
        mSuccess = false;

        // TODO: 4/5/16 hard coded for now
        String chunkJsonStr = "[{\"chp\": \"01\", \"firstvs\": \"01\"}, {\"chp\": \"01\", \"firstvs\": \"04\"}, {\"chp\": \"01\", \"firstvs\": \"07\"}, {\"chp\": \"01\", \"firstvs\": \"09\"}, {\"chp\": \"01\", \"firstvs\": \"12\"}, {\"chp\": \"01\", \"firstvs\": \"14\"}, {\"chp\": \"01\", \"firstvs\": \"16\"}, {\"chp\": \"01\", \"firstvs\": \"19\"}, {\"chp\": \"01\", \"firstvs\": \"21\"}, {\"chp\": \"01\", \"firstvs\": \"23\"}, {\"chp\": \"01\", \"firstvs\": \"27\"}, {\"chp\": \"01\", \"firstvs\": \"29\"}, {\"chp\": \"01\", \"firstvs\": \"32\"}, {\"chp\": \"01\", \"firstvs\": \"35\"}, {\"chp\": \"01\", \"firstvs\": \"38\"}, {\"chp\": \"01\", \"firstvs\": \"40\"}, {\"chp\": \"01\", \"firstvs\": \"43\"}, {\"chp\": \"01\", \"firstvs\": \"45\"}, {\"chp\": \"02\", \"firstvs\": \"01\"}, {\"chp\": \"02\", \"firstvs\": \"03\"}, {\"chp\": \"02\", \"firstvs\": \"05\"}, {\"chp\": \"02\", \"firstvs\": \"08\"}, {\"chp\": \"02\", \"firstvs\": \"10\"}, {\"chp\": \"02\", \"firstvs\": \"13\"}, {\"chp\": \"02\", \"firstvs\": \"15\"}, {\"chp\": \"02\", \"firstvs\": \"17\"}, {\"chp\": \"02\", \"firstvs\": \"18\"}, {\"chp\": \"02\", \"firstvs\": \"20\"}, {\"chp\": \"02\", \"firstvs\": \"22\"}, {\"chp\": \"02\", \"firstvs\": \"23\"}, {\"chp\": \"02\", \"firstvs\": \"25\"}, {\"chp\": \"02\", \"firstvs\": \"27\"}, {\"chp\": \"03\", \"firstvs\": \"01\"}, {\"chp\": \"03\", \"firstvs\": \"03\"}, {\"chp\": \"03\", \"firstvs\": \"05\"}, {\"chp\": \"03\", \"firstvs\": \"07\"}, {\"chp\": \"03\", \"firstvs\": \"09\"}, {\"chp\": \"03\", \"firstvs\": \"11\"}, {\"chp\": \"03\", \"firstvs\": \"13\"}, {\"chp\": \"03\", \"firstvs\": \"17\"}, {\"chp\": \"03\", \"firstvs\": \"20\"}, {\"chp\": \"03\", \"firstvs\": \"23\"}, {\"chp\": \"03\", \"firstvs\": \"26\"}, {\"chp\": \"03\", \"firstvs\": \"28\"}, {\"chp\": \"03\", \"firstvs\": \"31\"}, {\"chp\": \"03\", \"firstvs\": \"33\"}, {\"chp\": \"04\", \"firstvs\": \"01\"}, {\"chp\": \"04\", \"firstvs\": \"03\"}, {\"chp\": \"04\", \"firstvs\": \"06\"}, {\"chp\": \"04\", \"firstvs\": \"08\"}, {\"chp\": \"04\", \"firstvs\": \"10\"}, {\"chp\": \"04\", \"firstvs\": \"13\"}, {\"chp\": \"04\", \"firstvs\": \"16\"}, {\"chp\": \"04\", \"firstvs\": \"18\"}, {\"chp\": \"04\", \"firstvs\": \"21\"}, {\"chp\": \"04\", \"firstvs\": \"24\"}, {\"chp\": \"04\", \"firstvs\": \"26\"}, {\"chp\": \"04\", \"firstvs\": \"30\"}, {\"chp\": \"04\", \"firstvs\": \"33\"}, {\"chp\": \"04\", \"firstvs\": \"35\"}, {\"chp\": \"04\", \"firstvs\": \"38\"}, {\"chp\": \"04\", \"firstvs\": \"40\"}, {\"chp\": \"05\", \"firstvs\": \"01\"}, {\"chp\": \"05\", \"firstvs\": \"03\"}, {\"chp\": \"05\", \"firstvs\": \"05\"}, {\"chp\": \"05\", \"firstvs\": \"07\"}, {\"chp\": \"05\", \"firstvs\": \"09\"}, {\"chp\": \"05\", \"firstvs\": \"11\"}, {\"chp\": \"05\", \"firstvs\": \"14\"}, {\"chp\": \"05\", \"firstvs\": \"16\"}, {\"chp\": \"05\", \"firstvs\": \"18\"}, {\"chp\": \"05\", \"firstvs\": \"21\"}, {\"chp\": \"05\", \"firstvs\": \"25\"}, {\"chp\": \"05\", \"firstvs\": \"28\"}, {\"chp\": \"05\", \"firstvs\": \"30\"}, {\"chp\": \"05\", \"firstvs\": \"33\"}, {\"chp\": \"05\", \"firstvs\": \"35\"}, {\"chp\": \"05\", \"firstvs\": \"36\"}, {\"chp\": \"05\", \"firstvs\": \"39\"}, {\"chp\": \"05\", \"firstvs\": \"41\"}, {\"chp\": \"06\", \"firstvs\": \"01\"}, {\"chp\": \"06\", \"firstvs\": \"04\"}, {\"chp\": \"06\", \"firstvs\": \"07\"}, {\"chp\": \"06\", \"firstvs\": \"10\"}, {\"chp\": \"06\", \"firstvs\": \"12\"}, {\"chp\": \"06\", \"firstvs\": \"14\"}, {\"chp\": \"06\", \"firstvs\": \"16\"}, {\"chp\": \"06\", \"firstvs\": \"18\"}, {\"chp\": \"06\", \"firstvs\": \"21\"}, {\"chp\": \"06\", \"firstvs\": \"23\"}, {\"chp\": \"06\", \"firstvs\": \"26\"}, {\"chp\": \"06\", \"firstvs\": \"30\"}, {\"chp\": \"06\", \"firstvs\": \"33\"}, {\"chp\": \"06\", \"firstvs\": \"35\"}, {\"chp\": \"06\", \"firstvs\": \"37\"}, {\"chp\": \"06\", \"firstvs\": \"39\"}, {\"chp\": \"06\", \"firstvs\": \"42\"}, {\"chp\": \"06\", \"firstvs\": \"45\"}, {\"chp\": \"06\", \"firstvs\": \"48\"}, {\"chp\": \"06\", \"firstvs\": \"51\"}, {\"chp\": \"06\", \"firstvs\": \"53\"}, {\"chp\": \"06\", \"firstvs\": \"56\"}, {\"chp\": \"07\", \"firstvs\": \"01\"}, {\"chp\": \"07\", \"firstvs\": \"02\"}, {\"chp\": \"07\", \"firstvs\": \"05\"}, {\"chp\": \"07\", \"firstvs\": \"06\"}, {\"chp\": \"07\", \"firstvs\": \"08\"}, {\"chp\": \"07\", \"firstvs\": \"11\"}, {\"chp\": \"07\", \"firstvs\": \"14\"}, {\"chp\": \"07\", \"firstvs\": \"17\"}, {\"chp\": \"07\", \"firstvs\": \"20\"}, {\"chp\": \"07\", \"firstvs\": \"24\"}, {\"chp\": \"07\", \"firstvs\": \"27\"}, {\"chp\": \"07\", \"firstvs\": \"29\"}, {\"chp\": \"07\", \"firstvs\": \"31\"}, {\"chp\": \"07\", \"firstvs\": \"33\"}, {\"chp\": \"07\", \"firstvs\": \"36\"}, {\"chp\": \"08\", \"firstvs\": \"01\"}, {\"chp\": \"08\", \"firstvs\": \"05\"}, {\"chp\": \"08\", \"firstvs\": \"07\"}, {\"chp\": \"08\", \"firstvs\": \"11\"}, {\"chp\": \"08\", \"firstvs\": \"14\"}, {\"chp\": \"08\", \"firstvs\": \"16\"}, {\"chp\": \"08\", \"firstvs\": \"18\"}, {\"chp\": \"08\", \"firstvs\": \"20\"}, {\"chp\": \"08\", \"firstvs\": \"22\"}, {\"chp\": \"08\", \"firstvs\": \"24\"}, {\"chp\": \"08\", \"firstvs\": \"27\"}, {\"chp\": \"08\", \"firstvs\": \"29\"}, {\"chp\": \"08\", \"firstvs\": \"31\"}, {\"chp\": \"08\", \"firstvs\": \"33\"}, {\"chp\": \"08\", \"firstvs\": \"35\"}, {\"chp\": \"08\", \"firstvs\": \"38\"}, {\"chp\": \"09\", \"firstvs\": \"01\"}, {\"chp\": \"09\", \"firstvs\": \"04\"}, {\"chp\": \"09\", \"firstvs\": \"07\"}, {\"chp\": \"09\", \"firstvs\": \"09\"}, {\"chp\": \"09\", \"firstvs\": \"11\"}, {\"chp\": \"09\", \"firstvs\": \"14\"}, {\"chp\": \"09\", \"firstvs\": \"17\"}, {\"chp\": \"09\", \"firstvs\": \"20\"}, {\"chp\": \"09\", \"firstvs\": \"23\"}, {\"chp\": \"09\", \"firstvs\": \"26\"}, {\"chp\": \"09\", \"firstvs\": \"28\"}, {\"chp\": \"09\", \"firstvs\": \"30\"}, {\"chp\": \"09\", \"firstvs\": \"33\"}, {\"chp\": \"09\", \"firstvs\": \"36\"}, {\"chp\": \"09\", \"firstvs\": \"38\"}, {\"chp\": \"09\", \"firstvs\": \"40\"}, {\"chp\": \"09\", \"firstvs\": \"42\"}, {\"chp\": \"09\", \"firstvs\": \"45\"}, {\"chp\": \"09\", \"firstvs\": \"47\"}, {\"chp\": \"09\", \"firstvs\": \"49\"}, {\"chp\": \"10\", \"firstvs\": \"01\"}, {\"chp\": \"10\", \"firstvs\": \"05\"}, {\"chp\": \"10\", \"firstvs\": \"07\"}, {\"chp\": \"10\", \"firstvs\": \"10\"}, {\"chp\": \"10\", \"firstvs\": \"13\"}, {\"chp\": \"10\", \"firstvs\": \"15\"}, {\"chp\": \"10\", \"firstvs\": \"17\"}, {\"chp\": \"10\", \"firstvs\": \"20\"}, {\"chp\": \"10\", \"firstvs\": \"23\"}, {\"chp\": \"10\", \"firstvs\": \"26\"}, {\"chp\": \"10\", \"firstvs\": \"29\"}, {\"chp\": \"10\", \"firstvs\": \"32\"}, {\"chp\": \"10\", \"firstvs\": \"35\"}, {\"chp\": \"10\", \"firstvs\": \"38\"}, {\"chp\": \"10\", \"firstvs\": \"41\"}, {\"chp\": \"10\", \"firstvs\": \"43\"}, {\"chp\": \"10\", \"firstvs\": \"46\"}, {\"chp\": \"10\", \"firstvs\": \"49\"}, {\"chp\": \"10\", \"firstvs\": \"51\"}, {\"chp\": \"11\", \"firstvs\": \"01\"}, {\"chp\": \"11\", \"firstvs\": \"04\"}, {\"chp\": \"11\", \"firstvs\": \"07\"}, {\"chp\": \"11\", \"firstvs\": \"11\"}, {\"chp\": \"11\", \"firstvs\": \"13\"}, {\"chp\": \"11\", \"firstvs\": \"15\"}, {\"chp\": \"11\", \"firstvs\": \"17\"}, {\"chp\": \"11\", \"firstvs\": \"20\"}, {\"chp\": \"11\", \"firstvs\": \"22\"}, {\"chp\": \"11\", \"firstvs\": \"24\"}, {\"chp\": \"11\", \"firstvs\": \"27\"}, {\"chp\": \"11\", \"firstvs\": \"29\"}, {\"chp\": \"11\", \"firstvs\": \"31\"}, {\"chp\": \"12\", \"firstvs\": \"01\"}, {\"chp\": \"12\", \"firstvs\": \"04\"}, {\"chp\": \"12\", \"firstvs\": \"06\"}, {\"chp\": \"12\", \"firstvs\": \"08\"}, {\"chp\": \"12\", \"firstvs\": \"10\"}, {\"chp\": \"12\", \"firstvs\": \"13\"}, {\"chp\": \"12\", \"firstvs\": \"16\"}, {\"chp\": \"12\", \"firstvs\": \"18\"}, {\"chp\": \"12\", \"firstvs\": \"20\"}, {\"chp\": \"12\", \"firstvs\": \"24\"}, {\"chp\": \"12\", \"firstvs\": \"26\"}, {\"chp\": \"12\", \"firstvs\": \"28\"}, {\"chp\": \"12\", \"firstvs\": \"32\"}, {\"chp\": \"12\", \"firstvs\": \"35\"}, {\"chp\": \"12\", \"firstvs\": \"38\"}, {\"chp\": \"12\", \"firstvs\": \"41\"}, {\"chp\": \"12\", \"firstvs\": \"43\"}, {\"chp\": \"13\", \"firstvs\": \"01\"}, {\"chp\": \"13\", \"firstvs\": \"03\"}, {\"chp\": \"13\", \"firstvs\": \"05\"}, {\"chp\": \"13\", \"firstvs\": \"07\"}, {\"chp\": \"13\", \"firstvs\": \"09\"}, {\"chp\": \"13\", \"firstvs\": \"11\"}, {\"chp\": \"13\", \"firstvs\": \"14\"}, {\"chp\": \"13\", \"firstvs\": \"17\"}, {\"chp\": \"13\", \"firstvs\": \"21\"}, {\"chp\": \"13\", \"firstvs\": \"24\"}, {\"chp\": \"13\", \"firstvs\": \"28\"}, {\"chp\": \"13\", \"firstvs\": \"30\"}, {\"chp\": \"13\", \"firstvs\": \"33\"}, {\"chp\": \"13\", \"firstvs\": \"35\"}, {\"chp\": \"14\", \"firstvs\": \"01\"}, {\"chp\": \"14\", \"firstvs\": \"03\"}, {\"chp\": \"14\", \"firstvs\": \"06\"}, {\"chp\": \"14\", \"firstvs\": \"10\"}, {\"chp\": \"14\", \"firstvs\": \"12\"}, {\"chp\": \"14\", \"firstvs\": \"15\"}, {\"chp\": \"14\", \"firstvs\": \"17\"}, {\"chp\": \"14\", \"firstvs\": \"20\"}, {\"chp\": \"14\", \"firstvs\": \"22\"}, {\"chp\": \"14\", \"firstvs\": \"26\"}, {\"chp\": \"14\", \"firstvs\": \"28\"}, {\"chp\": \"14\", \"firstvs\": \"30\"}, {\"chp\": \"14\", \"firstvs\": \"32\"}, {\"chp\": \"14\", \"firstvs\": \"35\"}, {\"chp\": \"14\", \"firstvs\": \"37\"}, {\"chp\": \"14\", \"firstvs\": \"40\"}, {\"chp\": \"14\", \"firstvs\": \"43\"}, {\"chp\": \"14\", \"firstvs\": \"47\"}, {\"chp\": \"14\", \"firstvs\": \"51\"}, {\"chp\": \"14\", \"firstvs\": \"53\"}, {\"chp\": \"14\", \"firstvs\": \"55\"}, {\"chp\": \"14\", \"firstvs\": \"57\"}, {\"chp\": \"14\", \"firstvs\": \"60\"}, {\"chp\": \"14\", \"firstvs\": \"63\"}, {\"chp\": \"14\", \"firstvs\": \"66\"}, {\"chp\": \"14\", \"firstvs\": \"69\"}, {\"chp\": \"14\", \"firstvs\": \"71\"}, {\"chp\": \"15\", \"firstvs\": \"01\"}, {\"chp\": \"15\", \"firstvs\": \"04\"}, {\"chp\": \"15\", \"firstvs\": \"06\"}, {\"chp\": \"15\", \"firstvs\": \"09\"}, {\"chp\": \"15\", \"firstvs\": \"12\"}, {\"chp\": \"15\", \"firstvs\": \"14\"}, {\"chp\": \"15\", \"firstvs\": \"16\"}, {\"chp\": \"15\", \"firstvs\": \"19\"}, {\"chp\": \"15\", \"firstvs\": \"22\"}, {\"chp\": \"15\", \"firstvs\": \"25\"}, {\"chp\": \"15\", \"firstvs\": \"29\"}, {\"chp\": \"15\", \"firstvs\": \"31\"}, {\"chp\": \"15\", \"firstvs\": \"33\"}, {\"chp\": \"15\", \"firstvs\": \"36\"}, {\"chp\": \"15\", \"firstvs\": \"39\"}, {\"chp\": \"15\", \"firstvs\": \"42\"}, {\"chp\": \"15\", \"firstvs\": \"45\"}, {\"chp\": \"16\", \"firstvs\": \"01\"}, {\"chp\": \"16\", \"firstvs\": \"03\"}, {\"chp\": \"16\", \"firstvs\": \"05\"}, {\"chp\": \"16\", \"firstvs\": \"08\"}, {\"chp\": \"16\", \"firstvs\": \"09\"}, {\"chp\": \"16\", \"firstvs\": \"12\"}, {\"chp\": \"16\", \"firstvs\": \"14\"}, {\"chp\": \"16\", \"firstvs\": \"17\"}, {\"chp\": \"16\", \"firstvs\": \"19\"}]";
        addChunk("mrk", chunkJsonStr);
        addChunk("xmrk", chunkJsonStr);
    }

    /**
     * get error list
     * @return
     */
    public void showResults(final OnFinishedListener listener) {
        normalizeBookQueue();
        normalizeMessageQueue();
        String format = mContext.getResources().getString(R.string.found_book);
        String results = "";
        for(int i = 0; i <= mCurrentBook; i++) {
            String bookName = String.format(format, mFoundBooks.get(i));
            String currentResults = "\n" + bookName + "\n" + mErrors.get(i);
            results = results + currentResults + "\n";
        }

        format = mContext.getResources().getString(R.string.selected_language);
        String language = String.format(format,mTargetLanguage.getId() + " - " + mTargetLanguage.name);
        results = language + "\n" + results;

        CustomAlertDialog.Create(mContext)
                .setTitle(mSuccess ? R.string.title_import_usfm_summary : R.string.title_import_usfm_error)
                .setMessage(results)
                .setPositiveButton(R.string.label_continue, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFinished(true);
                    }
                })
                .setNegativeButton(R.string.menu_cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFinished(false);
                    }
                })
                .show("results");
    }

    /**
     * add error to error list
     */
    private void setBookName() {
        setBookName(mBookShortName, mBookName);
    }

    /**
     * add error to error list
     */
    private void setBookName(String bookShortName, String bookName) {
        mFoundBooks.add(mCurrentBook, bookShortName + " = " + bookName);
    }

    /**
     * add error to error list
     * @param resource
     * @param error
     */
    private void addError(int resource, String error) {
        String format = mContext.getResources().getString(resource);
        String newError = String.format(format, error);
        addError( newError);
    }

    /**
     * add error to error list
     * @param resource
     */
    private void addError(int resource) {
        String newError = mContext.getResources().getString(resource);
        addError( newError);
    }

    /**
     * add error to error list
     * @param error
     */
    private void addError(String error) {
        addMessage( error, true);
    }

    /**
     * add message to error list
     * @param message
     */
    private void addMessage(String message, boolean error) {
        normalizeMessageQueue();
        String errors = mErrors.get(mCurrentBook);
        if(!errors.isEmpty()) {
            errors += "\n";
        }
        String format = mContext.getResources().getString(error ? R.string.error_prefix : R.string.warning_prefix);
        String newError = String.format(format, message);
        mErrors.set(mCurrentBook,  newError);
        if(error) {
            Logger.e(TAG, newError);
        } else {
            Logger.w(TAG, newError);
        }
    }

    private void normalizeMessageQueue() {
        while(mErrors.size() <= mCurrentBook) {
            mErrors.add("");
        }
    }

    private void normalizeBookQueue() {
        while(mFoundBooks.size() <= mCurrentBook) {
            mFoundBooks.add("");
        }
    }

    /**
     * add warning to error list
     * @param error
     */
    private void addWarning(String error) {
        addMessage( error, false);
    }


    /**
     * unpack and import documents from zip stream
     * @param usfmStream
     * @return
     */
    public boolean importZipStream(InputStream usfmStream) {
        boolean successOverall = true;
        boolean success;
        try {
            Zip.unzipFromStream(usfmStream, mTempSrce);
            File[] usfmFiles = mTempSrce.listFiles();

            for (File usfmFile : usfmFiles) {
                addFilesInFolder(usfmFile);
            }
            Logger.i(TAG, "found files: " + TextUtils.join("\n", mSourceFiles));

            for(mCurrentBook = 0; mCurrentBook < mSourceFiles.size(); mCurrentBook++) {
                File file = mSourceFiles.get(mCurrentBook);
                success = processBook( file, false);
                if(!success) {
                    addError( "Could not parse " + file.toString());
                }
                successOverall = successOverall && success;
            }

            mCurrentBook = mSourceFiles.size() - 1; // set to last book

            finishImport();

        } catch (Exception e) {
            Logger.e(TAG, "error reading stream ", e);
            addError( R.string.zip_read_error);
            successOverall = false;
        }

        mSuccess = successOverall;
        return successOverall;
    }

    /**
     * import single file
     * @param file
     * @return
     */
    public boolean importFile(File file) {
        boolean success = true;
        if(null == file) {
            addError(R.string.file_read_error);
            return false;
        }

        try {
            String ext = FilenameUtils.getExtension(file.toString()).toLowerCase();
            boolean zip = "zip".equals(ext);
            if(!zip) {
                success = processBook( file, true);
            } else {
                InputStream usfmStream = new FileInputStream(file);
                success = importZipStream( usfmStream);
            }
        } catch (Exception e) {
            addError( R.string.file_read_error_detail, file.toString());
            success = false;
        }
        mSuccess = success;
        return success;
    }

    /**
     * import file from uri, if it is a zip file, then all files in zip will be imported
     * @param uri
     * @return
     */
    public boolean importUri(Uri uri) {
        boolean success = true;
        if(null == uri) {
            addError( R.string.file_read_error);
            return false;
        }

        String path = uri.toString();

        try {
            String ext = FilenameUtils.getExtension(path).toLowerCase();
            boolean zip = "zip".equals(ext);

            InputStream usfmStream = AppContext.context().getContentResolver().openInputStream(uri);
            if(!zip) {
                String text = IOUtils.toString(usfmStream, "UTF-8");
                success = processBook( text, true, uri.toString());
            } else {
                success = importZipStream( usfmStream);
            }
        } catch (Exception e) {
            addError( R.string.file_read_error_detail, path);
            success = false;
        }
        mSuccess = success;
        return success;
    }

    /**
     * import file from resource. if it is a zip file, then all files in zip will be imported
     * @param fileName
     * @return
     */
    public boolean importResourceFile(String fileName) {
        boolean success = true;

        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        boolean zip = "zip".equals(ext);

        try {
            InputStream usfmStream = mContext.getAssets().open(fileName);
            if(!zip) {
                String text = IOUtils.toString(usfmStream, "UTF-8");
                success = processBook( text, true, fileName.toString());
            } else {
                success = importZipStream( usfmStream);
            }
        } catch (Exception e) {
            Logger.e(TAG,"error reading " + fileName, e);
            success = false;
        }
        mSuccess = success;
        return success;
    }

    /**
     * add chunkJson (contains verses for each section) to map
     * @param book
     * @param chunk
     * @return
     */
    public boolean addChunk(String book, JSONArray chunk) {
        try {
            JSONObject processedChunk = new JSONObject();
            int length = chunk.length();
            for (int i = 0; i < length; i++) {
                JSONObject item = (JSONObject) chunk.get(i);
                String chapter = item.getString("chp");
                String firstverse = item.getString("firstvs");

                JSONArray verses = null;
                if (processedChunk.has(chapter)) {
                    verses = processedChunk.getJSONArray(chapter);
                } else {
                    verses = new JSONArray();
                    processedChunk.put(chapter, verses);
                }
                verses.put(firstverse);
            }

            mChunks.put(book.toLowerCase(), processedChunk);
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chunk " + book, e);
            return false;
        }
        return true;
    }

    /**
     * add chunkJson (contains verses for each section) to map
     * @param book
     * @param chunkStr
     * @return
     */
    public boolean addChunk(String book, String chunkStr) {
        try {
            JSONArray chunkJson = new JSONArray(chunkStr);
            return addChunk(book, chunkJson);
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chunk " + book, e);
        }
        return false;
    }

    /**
     * get the base folder for all the projects
     * @return
     */
    public File getProjectsFolder() {
        return mTempOutput;
    }

    /**
     * get array of the imported project folders
     * @return
     */
    public File[] getImportProjects() {
        if( mImportProjects != null ) {
            return mImportProjects.toArray(new File[mImportProjects.size()]);
        }
        return new File[0];
    }

    /**
     * process single document and create a project
     *
     * @param file
     * @return
     */
    private boolean processBook(File file, boolean lastFile) {
        boolean success;
        try {
            String book = FileUtils.readFileToString(file);
            success = processBook( book, lastFile, file.toString());
        } catch (Exception e) {
            Logger.e(TAG, "error reading book " + file.toString(), e);
            addError( R.string.error_reading_file, file.toString());
            success = false;
        }
        return success;
    }

    /**
     * process single document and create a project
     * @param book
     * @return
     */
    private boolean processBook(String book, boolean lastFile, String name) {
        boolean successOverall = true;
        boolean success;
        setBookName("", name);
        try {
            mBookName = extractString(book, PATTERN_BOOK_NAME_MARKER);
            mBookShortName = extractString(book, PATTERN_BOOK_SHORT_NAME_MARKER).toLowerCase();

            if (null == mTargetLanguage) {
                addError( R.string.missing_language);
                return false;
            }

            if (isMissing(mBookShortName)) {
                addError( R.string.missing_book_short_name);
                return false;
            }

            setBookName();

            mTempDest = new File(mTempOutput, mBookShortName);
            mProjectFolder = new File(mTempDest, mBookShortName + "-" + mTargetLanguage.getId());

            if (isMissing(mBookName)) {
                addError( R.string.missing_book_name);
                mBookName = mBookShortName;
            }

            boolean hasSections = isPresent(book, PATTERN_SECTION_MARKER);
            boolean hasVerses = isPresent(book, PATTERN_USFM_VERSE_SPAN);
            boolean haveChunksList = mChunks.containsKey(mBookShortName);

            if (!hasSections && !hasVerses) {
                addError( R.string.no_section_no_verse);
                return false;
            }

            if (!haveChunksList) { // no chunk list, so use sections

                addWarning( "No chunk list found for " + mBookShortName);

                // TODO: 4/7/16 add prompting

                if (!hasSections) {
                    addError( R.string.no_section_no_verse);
                    return false;
                }

                addWarning( "Using sections");
                success = extractChaptersFromDocument(book);
                successOverall = successOverall && success;
            }
            else { // has chunks

                mChunk = mChunks.get(mBookShortName);

                success = extractChaptersFromBook(book);
                successOverall = successOverall && success;
            }

            // TODO: 4/3/16 build tstudio package
            success = buildManifest();
            successOverall = successOverall && success;

            if(successOverall) {
                mImportProjects.add(mProjectFolder);
            }

            if(lastFile) {
                finishImport();
            }

        } catch (Exception e) {
            Logger.e(TAG, "error parsing book", e);
            return false;
        }
        return successOverall;
    }

    private void finishImport() {
       // place holder for post ops
    }

    /**
     * create the manifest for a project
     * @throws JSONException
     */
    private boolean buildManifest() throws JSONException {
        PackageInfo pInfo;
        TargetTranslation targetTranslation;
        try {
            Context context = AppContext.context();
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String projectId = mBookShortName;
            String resourceSlug = Resource.REGULAR_SLUG;
            targetTranslation = TargetTranslation.create( context, AppContext.getProfile().getNativeSpeaker(), TranslationFormat.USFM, mTargetLanguage, projectId, TranslationType.TEXT, resourceSlug, pInfo, mProjectFolder);

        } catch (Exception e) {
            addError(R.string.file_write_error);
            Logger.e(TAG, "failed to build manifest", e);
            return false;
        }

        return true;
    }

//    /**
//     * copy output folder into downloads for testing
//     * @return
//     */
//    public boolean copyProjectToDownloads() {
//        File dest = null;
//        try {
//            File target = AppContext.getPublicDownloadsDirectory();
//            dest = new File(target,"test");
//            if(dest.exists()) {
//                FileUtilities.safeDelete(dest);
//            }
//            FileUtils.copyDirectory(mTempOutput, dest);
//        } catch (Exception e) {
//            Logger.e(TAG, "error moving files to " + dest.toString(), e);
//            return false;
//        }
//        return true;
//    }

    /**
     * extract chapters in book
     * @param text
     * @return
     */
    public boolean extractChaptersFromBook(CharSequence text) {
        Pattern pattern = PATTERN_CHAPTER_NUMBER_MARKER;
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        CharSequence section;
        mChapter = null;
        boolean successOverall = true;
        boolean success;
        while(matcher.find()) {
            section = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker
            success = breakUpChapter(section);
            successOverall = successOverall && success;
            mChapter = matcher.group(1); // chapter number for next section
            lastIndex = matcher.start();
        }
        section = text.subSequence(lastIndex, text.length()); // get last section
        success = breakUpChapter(section);
        successOverall = successOverall && success;
        return successOverall;
    }

    /**
     * break up chapter into sections based on chunk list
     * @param text
     * @return
     */
    private boolean breakUpChapter(CharSequence text) {
        boolean successOverall = true;
        boolean success;
        if(!isMissing(mChapter)) {
            try {
                String chapter = mChapter;
                if (!mChunk.has(chapter)) {
                    chapter = "0" + chapter;
                    if (!mChunk.has(chapter)) {
                        chapter = "0" + chapter;
                    }
                }

                if (!mChunk.has(chapter)) {
                    addError(R.string.could_not_find_chapter, mChapter);
                    return false;
                }

                JSONArray versebreaks = mChunk.getJSONArray(chapter);
                String lastFirst = null;
                for (int i = 0; i < versebreaks.length(); i++) {
                    String first = versebreaks.getString(i);
                    success = extractVerses(chapter, text, lastFirst, first);
                    successOverall = successOverall && success;
                    lastFirst = first;
                }
                success = extractVerses(chapter, text, lastFirst, "999999");
                successOverall = successOverall && success;

            } catch (Exception e) {
                Logger.e(TAG, "error parsing chapter " + mChapter, e);
                addError(R.string.could_not_parse_chapter, mChapter);
                return false;
            }
        }
        return successOverall;
    }

    /**
     * extract verses in range of start to end into new section
     * @param chapter
     * @param text
     * @param start
     * @param end
     * @return
     */
    private boolean extractVerses(String chapter, CharSequence text, String start, String end) {
        boolean success = true;
        if(null != start) {
            int startVerse = Integer.valueOf(start);
            int endVerse = Integer.valueOf(end);
            success = extractVerseRange(chapter, text, startVerse, endVerse, start);
        }
        return success;
    }

    /**
     * extract verses in range of start to end into new section
     * @param chapter
     * @param text
     * @param start
     * @param end
     * @param firstVerse
     * @return
     */
    private boolean extractVerseRange(String chapter, CharSequence text, int start, int end, String firstVerse) {
        boolean successOverall = true;
        boolean success;
        if(!isMissing(chapter)) {
            Pattern pattern = PATTERN_USFM_VERSE_SPAN;
            Matcher matcher = pattern.matcher(text);
            int lastIndex = -1;
            String section = "";
            int currentVerse = 0;
            boolean done = false;
            while (matcher.find()) {
                if(currentVerse >= end) {
                    done = true;
                    break;
                }

                if(currentVerse >= start) {
                    section = section + text.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
                }

                String verse = matcher.group(1);
                currentVerse = Integer.valueOf(verse);
                lastIndex = matcher.start();
            }

            if(!done && (lastIndex >= 0) && (currentVerse < end)) {
                section = section + text.subSequence(lastIndex, text.length()); // get section before this chunk marker
            }

            if(!section.isEmpty()) {
                success = saveSection(chapter, firstVerse, section);
                successOverall = successOverall && success;
            }
        }
        return successOverall;
    }

    /**
     * save section (chunk) to file in chapter folder
     * @param chapter
     * @param firstVerse
     * @param section
     * @return
     */
    private boolean saveSection(String chapter, String firstVerse, CharSequence section) {
        File chapterFolder = new File(mProjectFolder, chapter);
        try {
            String cleanChunk = removePattern(section, PATTERN_SECTION_MARKER);
            FileUtils.forceMkdir(chapterFolder);
            File output = new File(chapterFolder, firstVerse + ".txt");
            FileUtils.write(output,cleanChunk);
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chapter " + mChapter, e);
            addError(R.string.file_write_for_verse, chapter + "/" + firstVerse);
            return false;
        }
    }

    /**
     * test if CharSequence is null or empty
     * @param text
     * @return
     */
    private boolean isMissing(CharSequence text) {
        if(null == text) {
            return true;
        }
        return text.length() == 0;
    }

    /**
     * extract chapters from document text (used for splitting by sections)
     * @param text
     * @return
     */
    private boolean extractChaptersFromDocument(CharSequence text) {
        Pattern pattern = PATTERN_CHAPTER_NUMBER_MARKER;
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        CharSequence section;
        mChapter = null;
        while(matcher.find()) {
            section = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker
            extractSectionsFromChapter(section);
            mChapter = matcher.group(1); // chapter number for next section
            lastIndex = matcher.end();
        }
        section = text.subSequence(lastIndex, text.length()); // get last section
        extractSectionsFromChapter(section);
        return true;
    }

    /**
     * extract sections from chapter
     * @param chapter
     */
    private void extractSectionsFromChapter(CharSequence chapter) {
        if(!isMissing(mChapter)) {
            Pattern pattern = PATTERN_SECTION_MARKER;
            Matcher matcher = pattern.matcher(chapter);
            int lastIndex = 0;
            CharSequence section;
            while (matcher.find()) {
                section = chapter.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
                processSection(section);
                lastIndex = matcher.end();
            }
            section = chapter.subSequence(lastIndex, chapter.length()); // get last section
            processSection(section);
        }
    }

    /**
     * extract verses from section
     * @param section
     * @return
     */
    private boolean processSection(CharSequence section) {
        if(!isMissing(section)) {
            String firstVerse = extractString(section, PATTERN_USFM_VERSE_SPAN);
            if (null == firstVerse) {
                addError(R.string.missing_verses_in_section);
                return false;
            }

            saveSection(mChapter, firstVerse, section);
        }
        return true;
    }

    /**
     * match regexPattern and get string in group 1 if present
     * @param text
     * @param regexPattern
     * @return
     */
    private String extractString(CharSequence text, Pattern regexPattern) {
        if(text.length() > 0) {
            // find instance
            Matcher matcher = regexPattern.matcher(text);
            String foundItem = null;
            if(matcher.find()) {
                foundItem = matcher.group(1);
            }
            return foundItem.trim();
        }

        return null;
    }

    /**
     * remove pattern if present in text
     * @param text
     * @param removePattern
     * @return
     */
    private String removePattern(CharSequence text, Pattern removePattern) {
        String out = "";
        Matcher matcher = removePattern.matcher(text);
        int lastIndex = 0;
        while (matcher.find()) {
            out = out + text.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
            lastIndex = matcher.end();
        }
        out = out + text.subSequence(lastIndex, text.length()); // get last section
        return out;
    }

    /**
     * test to see if regex pattern is present in text
     * @param text
     * @param regexPattern
     * @return
     */
    private boolean isPresent(CharSequence text, Pattern regexPattern) {
        if(text.length() > 0) {
            // find instance
            Matcher matcher = regexPattern.matcher(text);
            if(matcher.find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * create the necessary temp folders for unzipped source and output
     */
    private void createTempFolders() {
        mTempDir = new File(AppContext.context().getCacheDir(), System.currentTimeMillis() + "");
        mTempDir.mkdirs();
        mTempSrce = new File(mTempDir,"source");
        mTempSrce.mkdirs();
        mTempOutput = new File(mTempDir,"output");
        mTempOutput.mkdirs();
    }

    /**
     * cleanup working directory and values
     */
    public void cleanup() {
        FileUtils.deleteQuietly(mTempDir);
        mTempDir = null;
        mTempSrce = null;
        mTempOutput = null;
        mTempDest = null;
    }

    /**
     * add file and files in sub-folders to list of files to process
     * @param usfmFile
     * @return
     */
    private boolean addFilesInFolder(File usfmFile) {
        Logger.i(TAG, "processing folder: " + usfmFile.toString());

        if (usfmFile.isDirectory()) {
            File[] usfmSubFiles = usfmFile.listFiles();
            for (File usfmSuile : usfmSubFiles) {
                addFilesInFolder(usfmSuile);
            }
            Logger.i(TAG, "found files: " + usfmSubFiles.toString());
        } else {
            addFile(usfmFile);
        }
        return true;
    }

    /**
     * add file to list of files to process
     * @param usfmFile
     * @return
     */
    private boolean addFile(File usfmFile) {
        Logger.i(TAG, "processing file: " + usfmFile.toString());
        mSourceFiles.add(usfmFile);
        return true;
    }

    public interface OnFinishedListener {
        void onFinished(boolean success);
    }

    public interface OnLanguageSelectedListener {
        void onFinished(boolean success, TargetLanguage targetLanguage);
    }
}