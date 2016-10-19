package com.door43.translationstudio.core;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.text.TextUtils;

import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.spannables.USFMVerseSpan;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unfoldingword.resourcecontainer.Resource;

import org.unfoldingword.door43client.models.ChunkMarker;

/**
 * For processing USFM input file or zip files into importable package.
 */
public class ImportUsfm {
    public static final String TAG = ImportUsfm.class.getSimpleName();
    public static final String BOOK_TITLE_MARKER = "\\\\toc1\\s([^\\n]*)";
    public static final Pattern PATTERN_BOOK_TITLE_MARKER = Pattern.compile(BOOK_TITLE_MARKER);
    public static final String ID_TAG = "\\\\id\\s([^\\n]*)";
    public static final Pattern ID_TAG_MARKER = Pattern.compile(ID_TAG);
    public static final String BOOK_LONG_NAME_MARKER = "\\\\toc2\\s([^\\n]*)";
    public static final Pattern PATTERN_BOOK_LONG_NAME_MARKER = Pattern.compile(BOOK_LONG_NAME_MARKER);
    public static final String BOOK_ABBREVIATION_MARKER = "\\\\toc3\\s([^\\n]*)";
    public static final Pattern PATTERN_BOOK_ABBREVIATION_MARKER = Pattern.compile(BOOK_ABBREVIATION_MARKER);
    public static final String SECTION_MARKER = "\\\\s5([^\\n]*)";
    private static final Pattern PATTERN_SECTION_MARKER = Pattern.compile(SECTION_MARKER);
    public static final String CHAPTER_NUMBER_MARKER = "\\\\c\\s(\\d+(-\\d+)?)\\s";
    public static final Pattern PATTERN_CHAPTER_NUMBER_MARKER = Pattern.compile(CHAPTER_NUMBER_MARKER);
    public static final Pattern PATTERN_USFM_VERSE_SPAN = Pattern.compile(USFMVerseSpan.PATTERN);
    public static final int END_MARKER = 999999;
    public static final String FIRST_VERSE = "first_verse";
    public static final String FILE_NAME = "file_name";

    private File mTempDir;
    private File mTempOutput;
    private File mTempDest;
    private File mTempSrce;
    private File mProjectFolder;

    private String mChapter;
    private int mLastChapter;
    private List<File> mSourceFiles; // raw list of files found in expanded package
    private HashMap<String, JSONArray> mChunks;

    private List<File> mImportProjects; // files that seem to be actual books.
    private List<String> mErrors;
    private List<String> mFoundBooks; //descriptions of books from raw list
    private int mCurrentBook;

    private String mBookName;
    private String mBookShortName;
    private TargetLanguage mTargetLanguage;
    private Context mContext;
    private boolean mProcessSuccess;
    private UpdateStatusListener mStatusUpdateListener;
    private int mCurrentChapter;
    private int mChaperCount;
    private List<MissingNameItem> mBooksMissingNames;
    private boolean mCancel = false;
    private String[] mChapters;

    /**
     * constructor
     * @param context
     * @param targetLanguage
     */
    public ImportUsfm(Context context, TargetLanguage targetLanguage) {
        mTempDir = null;
        mTempOutput = null;
        mTempDest = null;
        mTempSrce = null;
        mProjectFolder = null;

        createTempFolders();

        mStatusUpdateListener = null;
        mContext = context;
        mChunks = null;

        mSourceFiles = new ArrayList<>();
        mImportProjects = new ArrayList<>();
        mErrors = new ArrayList<>();
        mFoundBooks = new ArrayList<>();
        mTargetLanguage = targetLanguage;
        mCurrentBook = 0;

        mProcessSuccess = false;
        mBooksMissingNames = new ArrayList<>();
        mCurrentChapter = 0;
        mChaperCount = 1;

        mBookName = null;
        mBookShortName = null;
        mChapter = null;
    }

    /**
     * constructor used to create new instance from JSON
     * @param context
     * @param tempDir
     * @param tempOutput
     * @param tempDest
     * @param tempSrce
     * @param projectFolder
     * @param chapter
     * @param sourceFiles
     * @param importProjects
     * @param errors
     * @param foundBooks
     * @param currentBook
     * @param bookName
     * @param bookShortName
     * @param targetLanguage
     * @param success
     * @param currentChapter
     * @param chaperCount
     * @param bookMissingNames
     */
    private ImportUsfm(Activity context, File tempDir, File tempOutput, File tempDest,
                       File tempSrce, File projectFolder, String chapter, List<File> sourceFiles,
                       List<File> importProjects, List<String> errors, List<String> foundBooks,
                       int currentBook, String bookName, String bookShortName, TargetLanguage targetLanguage,
                       boolean success, int currentChapter, int chaperCount, List<MissingNameItem> bookMissingNames) {
        this.mStatusUpdateListener = null;
        this.mContext = context;
        this.mChunks = null;

        this.mTempDir = tempDir;
        this.mTempOutput = tempOutput;
        this.mTempDest = tempDest;
        this.mTempSrce = tempSrce;
        this.mProjectFolder = projectFolder;
        this.mChapter = chapter;
        this.mSourceFiles = sourceFiles;
        this.mImportProjects = importProjects;
        this.mErrors = errors;
        this.mFoundBooks = foundBooks;
        this.mCurrentBook = currentBook;
        this.mBookName = bookName;
        this.mBookShortName = bookShortName;
        this.mTargetLanguage = targetLanguage;
        this.mProcessSuccess = success;
        this.mCurrentChapter = currentChapter;
        this.mChaperCount = chaperCount;
        this.mBooksMissingNames = bookMissingNames;
    }

    /**
     * generate JSON from object
     * @return
     */
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.putOpt("TempDir", mTempDir);
            json.putOpt("TempOutput", mTempOutput);
            json.putOpt("TempDest", mTempDest);
            json.putOpt("TempSrce", mTempSrce);
            json.putOpt("ProjectFolder", mProjectFolder);
            json.putOpt("SourceFiles", toJsonFileArray(mSourceFiles));
            json.putOpt("ImportProjects", toJsonFileArray(mImportProjects));
            json.putOpt("Errors", toJsonStringArray(mErrors));
            json.putOpt("FoundBooks", toJsonStringArray(mFoundBooks));
            json.putOpt("TargetLanguage", mTargetLanguage.toJSON());
            json.putOpt("CurrentBook", mCurrentBook);
            json.putOpt("Success", mProcessSuccess);
            json.putOpt("MissingNames", MissingNameItem.toJsonArray(mBooksMissingNames));
            json.putOpt("CurrentChapter", mCurrentChapter);
            json.putOpt("ChaperCount", mChaperCount);
            json.putOpt("BookName", mBookName);
            json.putOpt("BookShortName", mBookShortName);
            json.putOpt("Chapter", mChapter);

            return json;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * rebuild object from JSON string
     * @param context
     * @param jsonStr
     * @return
     */
    public static ImportUsfm newInstance(Activity context, String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            return ImportUsfm.newInstance(context, jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * cancel any processing
     * @param mCancel
     */
    public void setCancel(boolean mCancel) {
        this.mCancel = mCancel;
    }

    /**
     * was processing successful overall
     * @return
     */
    public boolean isProcessSuccess() {
        return mProcessSuccess;
    }

    /**
     * rebuild object from JSON
     * @param context
     * @param json
     * @return
     */
    public static ImportUsfm newInstance(Activity context, JSONObject json) {
        try {
            return new ImportUsfm(context,
                    getOptFile(json,"TempDir"),
                    getOptFile(json,"TempOutput"),
                    getOptFile(json,"TempDest"),
                    getOptFile(json,"TempSrce"),
                    getOptFile(json,"ProjectFolder"),
                    getOptString(json,"Chapter"),
                    fromJsonArrayToFiles(getOptJsonArray(json,"SourceFiles")),
                    fromJsonArrayToFiles(getOptJsonArray(json,"ImportProjects")),
                    fromJsonArrayToStrings(getOptJsonArray(json,"Errors")),
                    fromJsonArrayToStrings(getOptJsonArray(json,"FoundBooks")),
                    getOptInteger(json,"CurrentBook"),
                    getOptString(json,"BookName"),
                    getOptString(json,"BookShortName"),
                    TargetLanguage.fromJSON(getOptJsonObject(json,"TargetLanguage")),
                    getOptBoolean(json,"Success"),
                    getOptInteger(json,"CurrentChapter"),
                    getOptInteger(json,"ChaperCount"),
                    MissingNameItem.fromJsonArray(getOptJsonArray(json,"MissingNames")));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get list of books that we cant find valid names (resource IDs) for
     * @return
     */
    public MissingNameItem[] getBooksMissingNames() {
        return mBooksMissingNames.toArray(new MissingNameItem[mBooksMissingNames.size()]);
    }

    /**
     * used to keep list of books that are missing names (valid resource IDs)
     * @param description
     * @param invalidName
     * @param contents
     */
    public void addBookMissingName(String description, String invalidName, String contents) {
        mBooksMissingNames.add(new MissingNameItem(description, invalidName, contents));
    }

    /**
     * set status listener
     * @param listener
     */
    public void setUpdateStatusListener(UpdateStatusListener listener) {
        mStatusUpdateListener = listener;
    }

    /**
     * will update the status by calling listener.  Will display text and update
     *   the percent complete
     * @param text
     */
    private void updateStatus(String text) {
        int fileCount = mSourceFiles.size();
        if (fileCount < 1) {
            fileCount = 1;
        }

        float importAmountDone = (float) mCurrentBook / fileCount;
        float bookAmountDone = (float) mCurrentChapter / (mChaperCount + 2);
        float percentage = 100.0f * (importAmountDone + bookAmountDone / fileCount);
        int percentDone = Math.round(percentage);

        if (mStatusUpdateListener != null) {
            if (!isMissing(mBookShortName)) {
                text = mBookShortName + " - " + text;
            }
            mStatusUpdateListener.statusUpdate(text, percentDone);
        }
    }

    /**
     * will update the status by calling listener.  Will display string resource and update
     *   the percent complete
     * @param resource
     */
    private void updateStatus(int resource) {
        String status = mContext.getResources().getString(resource);
        updateStatus(status);
    }

    /**
     * will update the status by calling listener.  Will build status string using resource as string format
     * and applying data to it. Will also update the percent complete.
     * @param resource
     * @param data
     */
    private void updateStatus(int resource, String data) {
        String format = mContext.getResources().getString(resource);
        updateStatus(String.format(format, data));
    }

    /**
     * get processing results as multi-line string
     */
    public String getResultsString() {
        normalizeBookQueue();
        normalizeMessageQueue();
        String results = "";
        String format = mContext.getResources().getString(R.string.found_book);
        for (int i = 0; i <= mCurrentBook; i++) {
            String bookName = mFoundBooks.get(i);
            String bookNameCleaned = getCleanedBookName(format, bookName);
            String errors = mErrors.get(i);
            if(errors.isEmpty()) {
                errors = mContext.getResources().getString(R.string.no_error);
            }
            String currentResults = "\n" + (i+1) + " - " + bookNameCleaned + "\n" + errors;
            results = results + currentResults + "\n";
        }
        return results;
    }

    /**
     * cleanup uri escape characters
     * @param format
     * @param bookName
     * @return
     */
    private String getCleanedBookName(String format, String bookName) {
        String cleaned = bookName;
        String[] parts = bookName.split("%3A");
        if(parts.length == 2) { //look for URI prefix
            cleaned = "SD_CARD/" + parts[1];
        }
        cleaned = Uri.decode(cleaned);

        return String.format(format, cleaned);
    }

    /**
     * returns string to use for language title
     * @return
     */
    public String getLanguageTitle() {
        String format;
        format = mContext.getResources().getString(R.string.selected_language);
        String language = String.format(format, mTargetLanguage.slug + " - " + mTargetLanguage.name);
        return language;
    }

    /**
     * set book name
     * @param bookShortName
     * @param bookName
     */
    private void setBookName(String bookShortName, String bookName) {
        normalizeBookQueue();
        String description = bookName;
        if(!bookShortName.isEmpty()) {
            description = bookShortName + " = " + bookName;
        }
        mFoundBooks.set(mCurrentBook, description);
    }

    /**
     * add error to error list
     *
     * @param resource
     * @param error
     */
    private void addError(int resource, String error) {
        String format = mContext.getResources().getString(resource);
        String newError = String.format(format, error);
        addError(newError);
    }

    /**
     * add error to error list
     *
     * @param resource
     * @param val1
     * @param val2
     * */
    private void addError(int resource, String val1, String val2) {
        String format = mContext.getResources().getString(resource);
        String newError = String.format(format, val1, val2);
        addError(newError);
    }

    /**
     * add error to error list
     *
     * @param resource
     */
    private void addError(int resource) {
        String newError = mContext.getResources().getString(resource);
        addError(newError);
    }

    /**
     * add error to error list
     *
     * @param error
     */
    private void addError(String error) {
        addMessage(error, true);
    }

    /**
     * add message to error list
     *
     * @param message
     */
    private void addMessage(String message, boolean error) {
        normalizeMessageQueue();
        String errors = mErrors.get(mCurrentBook);
        if (!errors.isEmpty()) {
            errors += "\n";
        }
        String format = mContext.getResources().getString(error ? R.string.error_prefix : R.string.warning_prefix);
        String newError = String.format(format, message);
        mErrors.set(mCurrentBook, errors + newError);
        if (error) {
            Logger.e(TAG, newError);
        } else {
            Logger.w(TAG, newError);
        }
    }

    private void normalizeMessageQueue() {
        while (mErrors.size() <= mCurrentBook) {
            mErrors.add("");
        }
    }

    private void normalizeBookQueue() {
        while (mFoundBooks.size() <= mCurrentBook) {
            mFoundBooks.add("");
        }
    }

    /**
     * add warning to error list
     *
     * @param error
     */
    private void addWarning(String error) {
        addMessage(error, false);
    }

    /**
     * add warning to error list
     *
     * @param resource
     * @param error
     */
    private void addWarning(int resource, String error) {
        String format = mContext.getResources().getString(resource);
        String newWarning = String.format(format, error);
        addWarning(newWarning);
    }

    /**
     * add warning to error list
     *
     * @param resource
     * @param val1
     * @param val2
     * */
    private void addWarning(int resource, String val1, String val2) {
        String format = mContext.getResources().getString(resource);
        String newWarning = String.format(format, val1, val2);
        addWarning(newWarning);
    }

    /**
     * unpack and import documents from zip stream
     *
     * @param usfmStream
     * @return
     */
    public boolean readZipStream(InputStream usfmStream) {
        boolean successOverall = true;
        boolean success;
        updateStatus(R.string.initializing_import);
        try {
            Zip.unzipFromStream(usfmStream, mTempSrce);
            File[] usfmFiles = mTempSrce.listFiles();

            for (File usfmFile : usfmFiles) {
                addFilesInFolder(usfmFile);
            }
            Logger.i(TAG, "found files: " + TextUtils.join("\n", mSourceFiles));

            for (mCurrentBook = 0; mCurrentBook < mSourceFiles.size(); mCurrentBook++) {
                mCurrentChapter = 0;
                File file = mSourceFiles.get(mCurrentBook);
                String name = file.getName();
                updateStatus(R.string.found_book, name);
                success = processBook(file);
                if (!success) {
                    addError(R.string.could_not_parse, getShortFilePath(file.toString()));
                }
                successOverall = successOverall && success;
            }

            mCurrentBook = mSourceFiles.size() - 1; // set to last book

        } catch (Exception e) {
            Logger.e(TAG, "error reading stream ", e);
            addError(R.string.zip_read_error);
            successOverall = false;
        }

        updateStatus(R.string.finished_loading);
        mProcessSuccess = successOverall;
        return successOverall;
    }

    /**
     * import single file
     *
     * @param file
     * @return
     */
    public boolean readFile(File file) {
        boolean success = true;
        updateStatus(R.string.initializing_import);
        if (null == file) {
            addError(R.string.file_read_error);
            return false;
        }

        try {
            String ext = FileUtilities.getExtension(file.toString());
            boolean zip = "zip".equalsIgnoreCase(ext);
            if (!zip) {
                success = processBook(file);
            } else {
                InputStream usfmStream = new FileInputStream(file);
                success = readZipStream(usfmStream);
            }
        } catch (Exception e) {
            addError(R.string.file_read_error_detail, file.toString());
            success = false;
        }
        updateStatus(R.string.finished_loading);
        mProcessSuccess = success;
        return success;
    }

    /**
     * import file from uri, if it is a zip file, then all files in zip will be imported
     *
     * @param uri
     * @return
     */
    public boolean readUri(Uri uri) {
        boolean success = true;
        updateStatus(R.string.initializing_import);
        if (null == uri) {
            addError(R.string.file_read_error);
            return false;
        }

        String path = uri.toString();

        try {
            String ext = FileUtilities.getExtension(path);
            boolean zip = "zip".equalsIgnoreCase(ext);

            InputStream usfmStream = App.context().getContentResolver().openInputStream(uri);
            if (!zip) {
                String text = FileUtilities.readStreamToString(usfmStream);
                success = processBook(text, uri.toString());
            } else {
                success = readZipStream(usfmStream);
            }
        } catch (Exception e) {
            addError(R.string.file_read_error_detail, path);
            success = false;
        }
        updateStatus(R.string.finished_loading);
        mProcessSuccess = success;
        return success;
    }

    /**
     * import file from resource. if it is a zip file, then all files in zip will be imported
     *
     * @param fileName
     * @return
     */
    public boolean readResourceFile(Context context, String fileName) {
        boolean success = true;
        updateStatus(R.string.initializing_import);
        String ext = FileUtilities.getExtension(fileName).toLowerCase();
        boolean zip = "zip".equals(ext);

        try {
            InputStream usfmStream = context.getAssets().open(fileName);
            if (!zip) {
                String text = FileUtilities.readStreamToString(usfmStream);
                success = processBook(text, fileName);
            } else {
                success = readZipStream(usfmStream);
            }
        } catch (Exception e) {
            Logger.e(TAG, "error reading " + fileName, e);
            success = false;
        }
        updateStatus(R.string.finished_loading);
        mProcessSuccess = success;
        return success;
    }

    /**
     * parse chunk markers (contains verses and chapters) into map of verses indexed by chapter
     *
     * @param book
     * @param chunks
     * @return
     */
    public boolean parseChunks(String book, List<ChunkMarker> chunks) {
        mChunks = new HashMap<>(); // clear old map
        try {
            for (ChunkMarker chunkMarker : chunks) {

                String chapter = chunkMarker.chapter;
                String firstVerse = chunkMarker.verse;

                JSONArray verses = null;
                if (mChunks.containsKey(chapter)) {
                    verses = mChunks.get(chapter);
                } else {
                    verses = new JSONArray();
                    mChunks.put(chapter, verses);
                }

                JSONObject chunk = new JSONObject();
                chunk.put(FIRST_VERSE, firstVerse);
                // TODO: 10/19/16 will first verse always match file name?  or do we still have chunk 0 issues?
                chunk.put(FILE_NAME, firstVerse);
                verses.put(chunk);
            }

            //extract chapters
            List<String> foundChapters = new ArrayList<>();
            for (String chapter : mChunks.keySet()) {
                foundChapters.add(chapter);
            }
            Collections.sort(foundChapters);
            mChapters = foundChapters.toArray(new String[foundChapters.size()]);;

        } catch (Exception e) {
            Logger.e(TAG, "error parsing chunks " + book, e);
            return false;
        }
        return true;
    }

    /**
     * get the base folder for all the projects
     *
     * @return
     */
    public File getProjectsFolder() {
        return mTempOutput;
    }

    /**
     * get array of the imported project folders
     *
     * @return
     */
    public File[] getImportProjects() {
        if (mImportProjects != null) {
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
    private boolean processBook(File file) {
        boolean success;
        try {
            String book = FileUtilities.readFileToString(file);
            success = processBook(book, file.toString());
        } catch (Exception e) {
            Logger.e(TAG, "error reading book " + file.toString(), e);
            addError(R.string.error_reading_file, file.toString());
            success = false;
        }
        return success;
    }

    private boolean processBook(String book, String name) {
        return processBook(book, name, true, null);
    }

    public boolean processText(String book, String name, boolean promptForName, String useName) {
        mCurrentBook = mFoundBooks.size();
        boolean success = processBook(book, name, promptForName, useName);
        mProcessSuccess = success;
        return success;
    }

    private boolean processBook(String book, String name, boolean promptForName, String useName) {
        if(mCancel) {
            return false;
        }
        boolean successOverall = true;
        boolean success;
        mBookShortName = "";
        String description = getShortFilePath(name);
        setBookName("", description);
        try {
            mCurrentChapter = 0;
            mChaperCount = 1;

            extractBookID(book);

            // TODO: 4/12/16 verify book

            if (null == mTargetLanguage) {
                addError(R.string.missing_language);
                return false;
            }

//            boolean hasSections = isPresent(book, PATTERN_SECTION_MARKER);
            boolean hasVerses = isPresent(book, PATTERN_USFM_VERSE_SPAN);

            if (useName != null) {
                mBookShortName = useName;
            }

            if (isMissing(mBookShortName)) {
                addError(R.string.missing_book_short_name);
                addBookMissingName(name, null, book);
                return promptForName;
            }

            mBookShortName = mBookShortName.toLowerCase();

            setBookName(mBookShortName, description);

            if (!hasVerses) {
                addError(R.string.no_verse);
                return false;
            }

            mTempDest = new File(mTempOutput, mBookShortName);
            mProjectFolder = new File(mTempDest, mBookShortName + "-" + mTargetLanguage.slug);

            if (isMissing(mBookName)) {
                addError(R.string.missing_book_name);
                mBookName = mBookShortName;
            }

            List<ChunkMarker> markers = App.getLibrary().index().getChunkMarkers(mBookShortName, "en-US");
            boolean haveChunksList = markers.size() > 0;

            if (!haveChunksList) { // no chunk list
                // TODO: 4/13/16 add support for processing by sections

                addWarning(R.string.no_chunk_list, mBookShortName);
                addBookMissingName(mBookName, mBookShortName, book);
                return promptForName;
            } else { // has chunks
                parseChunks(mBookShortName, markers);
                mChaperCount = mChunks.size();

                success = extractChaptersFromBook(book);
                successOverall = successOverall && success;
            }

            if(mCancel) {
                successOverall = false;
            }

            if (successOverall) {
                mCurrentChapter = (mChaperCount + 1);
                updateStatus(R.string.building_manifest);

                success = buildManifest();
                successOverall = successOverall && success;
            }

            if (successOverall) {
                mImportProjects.add(mProjectFolder);
            }

        } catch (Exception e) {
            Logger.e(TAG, "error parsing book", e);
            return false;
        }
        return successOverall;
    }

    public String getShortFilePath(String name) {
        String filename = name;
        if(name != null) {
            int pos = name.indexOf(mTempSrce.toString()); // try to strip off temp folder path
            if (pos >= 0) {
                filename = name.substring(pos + mTempSrce.toString().length() + 1);
            } else { // otherwise we use just file name
                String[] parts = name.split("/");
                if (parts.length > 0) {
                    filename = parts[parts.length - 1];
                }
            }
        }
        return filename;
    }

    private void extractBookID(String book) {
        mBookName = extractString(book, PATTERN_BOOK_TITLE_MARKER);
        mBookShortName = extractString(book, PATTERN_BOOK_ABBREVIATION_MARKER);

        String idString = extractString(book, ID_TAG_MARKER);
        if (null != idString) {
            String[] tags = idString.split(" ");
            if (tags.length > 0) {
                mBookShortName = tags[0];
            }
        }
    }

    /**
     * create the manifest for a project
     *
     * @throws JSONException
     */
    private boolean buildManifest() throws JSONException {
        PackageInfo pInfo;
        TargetTranslation targetTranslation;
        try {
            Context context = App.context();
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String projectId = mBookShortName;
            String resourceSlug = Resource.REGULAR_SLUG;
            targetTranslation = TargetTranslation.create(context, App.getProfile().getNativeSpeaker(), TranslationFormat.USFM, mTargetLanguage, projectId, ResourceType.TEXT, resourceSlug, pInfo, mProjectFolder);

        } catch (Exception e) {
            addError(R.string.file_write_error);
            Logger.e(TAG, "failed to build manifest", e);
            return false;
        }
        return true;
    }

    /**
     * extract chapters in book
     *
     * @param text
     * @return
     */
    public boolean extractChaptersFromBook(CharSequence text) {
        Pattern pattern = PATTERN_CHAPTER_NUMBER_MARKER;
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        CharSequence section;
        mChapter = null;
        mLastChapter = 0;
        boolean successOverall = true;
        boolean success;
        boolean foundChapter = false;
        while (matcher.find() && successOverall) {
            if(mCancel) {
                return false;
            }

            foundChapter = true;
            success = true;
            section = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker

            String chapter = matcher.group(1); // chapter number for next section
            mCurrentChapter = Integer.valueOf(chapter);
            if(mCurrentChapter > mChunks.size()) { //make sure in range
                break;
            }

            int expectedChapter = mLastChapter + 1;
            if(mCurrentChapter != expectedChapter) { // if out of order
                if (mCurrentChapter > expectedChapter) { // if gap

                    success = processChapterGap(section, mLastChapter, mCurrentChapter);
                    mLastChapter = mCurrentChapter - 1;

                } else if (mCurrentChapter == expectedChapter) {
                    Logger.e(TAG, "duplicate chapter " + mChapter);
                    addError(R.string.duplicate_chapter, mChapter);
                    return false;
                } else {
                    Logger.e(TAG, "out of order chapter " + mChapter + " after " + mLastChapter);
                    addError(R.string.chapter_out_of_order, mChapter, mLastChapter + "");
                    return false;
                }
            } else {
                success = breakUpChapter( section, mChapter);
            }

            successOverall = successOverall && success;
            if(!success) {
                break;
            }

            mLastChapter++;
            mChapter = chapter; // chapter number for next section
            lastIndex = matcher.end();
        }

        if(!foundChapter) { // if no chapters found
            Logger.e(TAG, "no chapters" );
            addError(R.string.no_chapter);
            return false;
        }

        if (successOverall) {
            section = text.subSequence(lastIndex, text.length()); // get last section
            success = breakUpChapter(section, mChapter);
            mLastChapter = Integer.valueOf(mChapter);
            successOverall = successOverall && success;
        }

        if (successOverall) {
            mCurrentChapter = Integer.valueOf(mChapter);
            if ((mChapter == null) || (mCurrentChapter != mChunks.size())) {

                if(mCurrentChapter < mChunks.size()) {
                    success = processChapterGap("", mCurrentChapter, mChunks.size() + 1);
                    successOverall = successOverall && success;
                } else  {
                    String lastChapter = (mChapter != null) ? mChapter : "(null)";
                    addWarning(R.string.chapter_count_invalid, mChunks.size() + "", lastChapter);
                    return false;
                }
            }
        }
        return successOverall;
    }

    /**
     * handle missing chapters in book
     * @param section
     * @param missingStart
     * @param missingEnd
     * @return
     */
    private boolean processChapterGap(CharSequence section, int missingStart, int missingEnd) {
        boolean success;
        if(missingStart <= 0) { // if first chapter is missing, then we start processing there
            missingStart = 1;
            Logger.w(TAG, "missing chapter " + missingStart);
            addWarning(R.string.missing_chapter_n, missingStart + "");
        }

        success = breakUpChapter(section, missingStart + "");

        for(int i = missingStart + 1; i < missingEnd; i++) { // skip missing gaps
            Logger.w(TAG, "missing chapter " + i);
            addWarning(R.string.missing_chapter_n, i + "");
            breakUpChapter("", i + "");
        }
        return success;
    }

    /**
     * break up chapter into sections based on chunk list
     *
     * @param text
     * @return
     */
    private boolean breakUpChapter(CharSequence text, String currentChapterStr) {
        boolean successOverall = true;
        boolean success = true;
        if (!isMissing(currentChapterStr)) {
            try {
                String chapter = getChapterFolderName(currentChapterStr);
                if (null == chapter) {
                    addError(R.string.could_not_find_chapter, currentChapterStr);
                    return false;
                }

                JSONArray versebreaks = getVerseBreaksObj(chapter);

                int currentChapter = Integer.valueOf(chapter);
                updateStatus(R.string.processing_chapter, new Integer(mChaperCount - currentChapter + 1).toString());

                String lastFirst = null;
                for (int i = 0; (i < versebreaks.length()) && success; i++) {
                    String first = versebreaks.getJSONObject(i).getString(FIRST_VERSE);
                    success = extractVerses(chapter, text, lastFirst, first);
                    successOverall = successOverall && success;
                    lastFirst = first;
                }
                if (successOverall) {
                    success = extractVerses(chapter, text, lastFirst, END_MARKER +"");
                    successOverall = successOverall && success;
                }

            } catch (Exception e) {
                Logger.e(TAG, "error parsing chapter " + currentChapterStr, e);
                addError(R.string.could_not_parse_chapter, currentChapterStr);
                return false;
            }
        } else { // save stuff before first chapter
            String chapter1 = getChapterFolderName("1"); // to get width of chapters
            String chapter0 = "0000".substring(0, chapter1.length()); // match length of chapter 1
            success = saveSection(".", "before", text);
            successOverall = successOverall && success;
            success = saveSection(chapter0, "title", mBookName);
            successOverall = successOverall && success;
        }
        return successOverall;
    }

    /**
     * get the chapter name with the appropriate zero padding expected by app
     * @param findChapter
     * @return
     */
    private String getChapterFolderName(String findChapter) {
        try {
            int chapter = Integer.valueOf(findChapter);
            if (chapter > 0) { // first check in expected location
                String chapterN = mChapters[chapter - 1];
                if (Integer.valueOf(chapterN) == chapter) {
                    return chapterN;
                }
            }

            for (String chapterN : mChapters) { //search for chapter match
                if (Integer.valueOf(chapterN) == chapter) {
                    return chapterN;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        addError(R.string.could_not_find_chapter, findChapter);
        return null;
    }

    /**
     * get the file name to use for verse chunk
     * @param findChapter
     * @param firstVerse
     * @return
     */
    private String getChunkFileName(String findChapter, String firstVerse)  {
        try {
            JSONArray chunks = getVerseBreaksObj(findChapter);
            for (int i = 0; i < chunks.length(); i++) {
                JSONObject chunk = chunks.getJSONObject(i);
                String firstVerseFile = chunk.getString(FIRST_VERSE);
                if (firstVerse.equals(firstVerseFile)) {
                    return firstVerseFile;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return firstVerse; // if not found, use same as chapter id
    }

    /**
     * get the array of verse chunks
     * @param findChapter
     * @return
     */
    private JSONArray getVerseBreaksObj(String findChapter) {
        String chapter = findChapter;
        if (mChunks.containsKey(chapter)) {
            return mChunks.get(chapter);
        }

        chapter = "0" + chapter;
        if (mChunks.containsKey(chapter)) {
            return mChunks.get(chapter);
        }

        chapter = "0" + chapter;
        if (mChunks.containsKey(chapter)) {
            return mChunks.get(chapter);
        }

        //try removing leading spaces
        chapter = findChapter;
        while( !chapter.isEmpty() && (chapter.charAt(0) == '0') ) {
            chapter = chapter.substring(1);
            if (mChunks.containsKey(chapter)) {
                return mChunks.get(chapter);
            }
        }

        addError(R.string.could_not_find_chapter, findChapter);
        return null;
    }

    /**
     * extract verses in range of start to end into new section
     *
     * @param chapter
     * @param text
     * @param start
     * @param end
     * @return
     */
    private boolean extractVerses(String chapter, CharSequence text, String start, String end) {
        boolean success = true;
        if (null == start) { // skip over stuff before verse 1 for now
            return true;
        }

        int startVerse = Integer.valueOf(start);
        int endVerse = Integer.valueOf(end);
        success = extractVerseRange(chapter, text, startVerse, endVerse, start);
        return success;
    }

    /**
     * extract verses in range of start to end into new section
     *
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
        if (!isMissing(chapter)) {
            Pattern pattern = PATTERN_USFM_VERSE_SPAN;
            Matcher matcher = pattern.matcher(text);
            int lastIndex = 0;
            String section = "";
            int currentVerse = 0;
            int foundVerseCount = 0;
            int endVerseRange = 0;
            boolean done = false;
            boolean matchesFound = false;
            while (matcher.find()) {
                matchesFound = true;

                if (currentVerse >= end) {
                    done = true;
                    break;
                }

                if (currentVerse >= start) {
                    if( (currentVerse == 1) && (start == 1) ){ // pick up initial content of chapter
                        lastIndex = 0; // get everything before this first verse
                    }

                    if(end == END_MARKER) { // just include everything to end
                        done = false;
                        break;
                    }

                    while(true) { // find the end of the section

                        if(endVerseRange > 0) {
                            foundVerseCount += (endVerseRange - currentVerse + 1);
                        } else {
                            foundVerseCount++;
                        }

                        String verse = matcher.group(1);
                        int[] verseRange = getVerseRange(verse);
                        if(null == verseRange) {
                            break;
                        }
                        currentVerse = verseRange[0];
                        endVerseRange = verseRange[1];

                        if (currentVerse >= end) {
                             break;
                        }

                        boolean found = matcher.find();
                        if(!found) {
                            break;
                        }
                    }

                    section = section + text.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
                    done = true;
                    break;
                }


                String verse = matcher.group(1);
                int[] verseRange = getVerseRange(verse);
                if(null == verseRange) {
                    return false;
                }
                currentVerse = verseRange[0];
                endVerseRange = verseRange[1];

                lastIndex = matcher.start();
            }

            if (!done && matchesFound && (currentVerse >= start) && (currentVerse < end)) {
                section = section + text.subSequence(lastIndex, text.length()); // get last section
            }

            if(start != 0) { // text before first verse is not a concern
                int delta = foundVerseCount - (end - start);
                if (section.isEmpty()) {
                    String format = mContext.getResources().getString(R.string.could_not_find_verses_in_chapter);
                    String msg = String.format(format, start, end - 1, chapter);
                    addWarning(msg);
                } else if ((end != END_MARKER) && (delta != 0)) {
                    String format;
                    if(delta < 0) {
                        delta = -delta;
                        format = mContext.getResources().getString(R.string.missing_verses_in_chapter);
                    } else {
                        format = mContext.getResources().getString(R.string.extra_verses_in_chapter);
                    }
                    String msg = String.format(format, delta, start, end - 1, chapter);
                    addWarning(msg);
                }
            }

            String chunkFileName = getChunkFileName(chapter, firstVerse);
            success = saveSection(getChapterFolderName(chapter), chunkFileName, section);
            successOverall = successOverall && success;
        }
        return successOverall;
    }

    /**
     * get verse range
     * @param verse
     * @return
     */
    private int[] getVerseRange(String verse) {
        int[] verseRange;
        int currentVerse;
        int endVerseRange;
        try {
            int currentVers = Integer.valueOf(verse);
            verseRange = new int[] {currentVers, 0};
        } catch (NumberFormatException e) { // might be a range in format 12-13
            String[] range = verse.split("-");
            if (range.length < 2) {
                verseRange = null;
            } else {
                currentVerse = Integer.valueOf(range[0]);
                endVerseRange = Integer.valueOf(range[1]);
                verseRange = new int[]{currentVerse, endVerseRange};
            }
        }
        return verseRange;
    }

    /**
     * save section (chunk) to file in chapter folder
     *
     * @param chapter
     * @param fileName
     * @param section
     * @return
     */
    private boolean saveSection(String chapter, String fileName, CharSequence section) {
        File chapterFolder = new File(mProjectFolder, chapter);
        try {
            String cleanChunk = removePattern(section, PATTERN_SECTION_MARKER);
            FileUtilities.forceMkdir(chapterFolder);
            File output = new File(chapterFolder, fileName + ".txt");
            FileUtilities.writeStringToFile(output, cleanChunk);
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chapter " + mChapter, e);
            addError(R.string.file_write_for_verse, chapter + "/" + fileName);
            return false;
        }
    }

    /**
     * test if CharSequence is null or empty
     *
     * @param text
     * @return
     */
    private boolean isMissing(CharSequence text) {
        if (null == text) {
            return true;
        }
        return text.length() == 0;
    }

    /**
     * extract chapters from document text (used for splitting by sections)
     *
     * @param text
     * @return
     */
    private boolean extractChaptersFromDocument(CharSequence text) {
        Pattern pattern = PATTERN_CHAPTER_NUMBER_MARKER;
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        int length = text.length();
        CharSequence chapter;
        mChapter = null;
        while (matcher.find()) {
            chapter = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker
            extractSectionsFromChapter(chapter);
            mChapter = matcher.group(1); // chapter number for next section
            lastIndex = matcher.end();
            mCurrentChapter = Integer.valueOf(mChapter);

            //estimate number of chapters - doesn't need to be exact
            if (mCurrentChapter > 1) {
                float percentIn = (float) lastIndex / length;
                if (percentIn != 0.0f) {
                    mChaperCount = Math.round((mCurrentChapter - 1) / percentIn);
                    if (mChaperCount < 1) { // sanity checks
                        mChaperCount = 1;
                    } else if (mChaperCount > 250) {
                        mChaperCount = 250;
                    } else if (mChaperCount < mCurrentChapter) {
                        mChaperCount = mCurrentChapter;
                    }

                    updateStatus(R.string.processing_chapter, new Integer(mChaperCount - mCurrentChapter + 1).toString());
                }
            }
        }
        chapter = text.subSequence(lastIndex, text.length()); // get last section
        extractSectionsFromChapter(chapter);
        return true;
    }

    /**
     * extract sections from chapter
     *
     * @param chapter
     */
    private void extractSectionsFromChapter(CharSequence chapter) {
        if (!isMissing(mChapter)) {
            Pattern pattern = PATTERN_SECTION_MARKER;
            Matcher matcher = pattern.matcher(chapter);
            int lastIndex = 0;
            CharSequence section;
            while (matcher.find()) {
                section = chapter.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
                if (lastIndex > 0) { // ignore what's before first section
                    processSection(section);
                }
                lastIndex = matcher.end();
            }
            section = chapter.subSequence(lastIndex, chapter.length()); // get last section
            processSection(section);
        }
    }

    /**
     * extract verses from section
     *
     * @param section
     * @return
     */
    private boolean processSection(CharSequence section) {
        if (!isMissing(section)) {
            String firstVerse = extractString(section, PATTERN_USFM_VERSE_SPAN);
            if (null == firstVerse) {
                addError(R.string.missing_verses_in_section);
                return false;
            }

            saveSection(getChapterFolderName(mChapter), firstVerse, section);
        }
        return true;
    }

    /**
     * match regexPattern and get string in group 1 if present
     *
     * @param text
     * @param regexPattern
     * @return
     */
    private String extractString(CharSequence text, Pattern regexPattern) {
        if (text.length() > 0) {
            // find instance
            Matcher matcher = regexPattern.matcher(text);
            String foundItem = null;
            if (matcher.find()) {
                foundItem = matcher.group(1);
                return foundItem.trim();
            }
        }

        return null;
    }

    /**
     * remove pattern if present in text
     *
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
     *
     * @param text
     * @param regexPattern
     * @return
     */
    private boolean isPresent(CharSequence text, Pattern regexPattern) {
        if (text.length() > 0) {
            // find instance
            Matcher matcher = regexPattern.matcher(text);
            if (matcher.find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * create the necessary temp folders for unzipped source and output
     */
    private void createTempFolders() {
        mTempDir = new File(App.context().getCacheDir(), System.currentTimeMillis() + "");
        mTempDir.mkdirs();
        mTempSrce = new File(mTempDir, "source");
        mTempSrce.mkdirs();
        mTempOutput = new File(mTempDir, "output");
        mTempOutput.mkdirs();
    }

    /**
     * cleanup working directory and values
     */
    public void cleanup() {
        FileUtilities.deleteQuietly(mTempDir);
        mTempDir = null;
        mTempSrce = null;
        mTempOutput = null;
        mTempDest = null;
    }

    /**
     * add file and files in sub-folders to list of files to process
     *
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
     *
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

    public interface UpdateStatusListener {
        void statusUpdate(String textStatus, int percentStatus);
    }

    static JSONArray toJsonFileArray(List<File> array) {
        JSONArray jsonArray = new JSONArray();
        for (File item : array) {
            jsonArray.put(item.toString());
        }
        return jsonArray;
    }

    static List<File> fromJsonArrayToFiles(String jsonStr) {
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            return fromJsonArrayToFiles(jsonArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static List<File> fromJsonArrayToFiles(JSONArray jsonArray) throws JSONException {
        List<File> array = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            String path = jsonArray.getString(i);
            File file = new File(path);
            array.add(file);
        }
        return array;
    }

    static JSONArray toJsonStringArray(List<String> array) {
        JSONArray jsonArray = new JSONArray();
        for (String item : array) {
            jsonArray.put(item);
        }
        return jsonArray;
    }

    static List<String> fromJsonArrayToStrings(String jsonStr) {
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            return fromJsonArrayToStrings(jsonArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static List<String> fromJsonArrayToStrings(JSONArray jsonArray) throws JSONException {
        List<String> array = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            String text = jsonArray.getString(i);
            array.add(text);
        }
        return array;
    }

    static Integer getOptInteger(JSONObject json, String key) {
        return (Integer) getOpt(json,key);
    }

    static Boolean getOptBoolean(JSONObject json, String key) {
        return (Boolean) getOpt(json,key);
    }

    static File getOptFile(JSONObject json, String key) {
        String path = getOptString(json, key);
        if(path != null) {
            return new File(path);
        }
        return null;
    }

    static String getOptString(JSONObject json, String key) {
        Object obj = getOpt(json, key);
        return (String) obj;
    }

    static JSONObject getOptJsonObject(JSONObject json, String key) {
        try {
            Object obj = getOpt(json, key);
            return (JSONObject) obj;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static JSONArray getOptJsonArray(JSONObject json, String key) {
        try {
            Object obj = getOpt(json, key);
            return (JSONArray) obj;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    static Object getOpt(JSONObject json, String key) {
        try {
            if(json.has(key)) {
                Object obj = json.get(key);
                return obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}