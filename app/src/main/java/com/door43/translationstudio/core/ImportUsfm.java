package com.door43.translationstudio.core;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
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
 * For processing USFM input file or zip files into importable package.
 */
public class ImportUsfm {
    public static final String TAG = ImportUsfm.class.getSimpleName();
    public static final String BOOK_NAME_MARKER = "\\\\toc1\\s([^\\n]*)";
    private static final Pattern PATTERN_BOOK_NAME_MARKER = Pattern.compile(BOOK_NAME_MARKER);
    public static final String ID_TAG = "\\\\id\\s([^\\n]*)";
    private static final Pattern ID_TAG_MARKER = Pattern.compile(ID_TAG);
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
            json.putOpt("TargetLanguage", mTargetLanguage.toApiFormatJson());
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
                    TargetLanguage.generate(getOptJsonObject(json,"TargetLanguage")),
                    getOptBoolean(json,"Success"),
                    getOptInteger(json,"CurrentChapter"),
                    getOptInteger(json,"ChaperCount"),
                    MissingNameItem.fromJsonArray(getOptJsonArray(json,"MissingNames")));

        } catch (Exception e) {
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
            String bookName = String.format(format, mFoundBooks.get(i));
            String errors = mErrors.get(i);
            if(errors.isEmpty()) {
                errors = mContext.getResources().getString(R.string.no_error);
            }
            String currentResults = "\n" + (i+1) + " - " + bookName + "\n" + errors;
            results = results + currentResults + "\n";
        }
        return results;
    }

    /**
     * returns string to use for language title
     * @return
     */
    public String getLanguageTitle() {
        String format;
        format = mContext.getResources().getString(R.string.selected_language);
        String language = String.format(format, mTargetLanguage.getId() + " - " + mTargetLanguage.name);
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
            String ext = FilenameUtils.getExtension(file.toString());
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
            String ext = FilenameUtils.getExtension(path);
            boolean zip = "zip".equalsIgnoreCase(ext);

            InputStream usfmStream = AppContext.context().getContentResolver().openInputStream(uri);
            if (!zip) {
                String text = IOUtils.toString(usfmStream, "UTF-8");
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
    public boolean readResourceFile(String fileName) {
        boolean success = true;
        updateStatus(R.string.initializing_import);
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        boolean zip = "zip".equals(ext);

        try {
            InputStream usfmStream = mContext.getAssets().open(fileName);
            if (!zip) {
                String text = IOUtils.toString(usfmStream, "UTF-8");
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
     * add chunk markers (contains verses and chapters) to map by chapter
     *
     * @param book
     * @param chunks
     * @return
     */
    public boolean addChunks(String book, ChunkMarker[] chunks) {
        try {
            for (ChunkMarker chunkMarker : chunks) {

                String chapter = chunkMarker.chapterSlug;
                String firstverse = chunkMarker.firstVerseSlug;

                JSONArray verses = null;
                if (mChunks.containsKey(chapter)) {
                    verses = mChunks.get(chapter);
                } else {
                    verses = new JSONArray();
                    mChunks.put(chapter, verses);
                }
                verses.put(firstverse);
            }

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
            String book = FileUtils.readFileToString(file);
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

    public boolean readText(String book, String name, boolean promptForName, String useName) {
        mCurrentBook = mFoundBooks.size();
        return processBook(book, name, promptForName, useName);
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

            if (!hasVerses) {
                addError(R.string.no_verse);
                return false;
            }

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

            mTempDest = new File(mTempOutput, mBookShortName);
            mProjectFolder = new File(mTempDest, mBookShortName + "-" + mTargetLanguage.getId());

            if (isMissing(mBookName)) {
                addError(R.string.missing_book_name);
                mBookName = mBookShortName;
            }

            ChunkMarker[] markers = AppContext.getLibrary().getChunkMarkers(mBookShortName);
            boolean haveChunksList = markers.length > 0;

            if (!haveChunksList) { // no chunk list
                // TODO: 4/13/16 add support for processing by sections

                addWarning(R.string.no_chunk_list, mBookShortName);
                addBookMissingName(mBookName, mBookShortName, book);
                return promptForName;
            } else { // has chunks

                mChunks = new HashMap<>(); // clear old map
                addChunks(mBookShortName, markers);
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
        mBookName = extractString(book, PATTERN_BOOK_NAME_MARKER);
        mBookShortName = extractString(book, PATTERN_BOOK_SHORT_NAME_MARKER);

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
            Context context = AppContext.context();
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String projectId = mBookShortName;
            String resourceSlug = Resource.REGULAR_SLUG;
            targetTranslation = TargetTranslation.create(context, AppContext.getProfile().getNativeSpeaker(), TranslationFormat.USFM, mTargetLanguage, projectId, TranslationType.TEXT, resourceSlug, pInfo, mProjectFolder);

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
        boolean successOverall = true;
        boolean success;
        while (matcher.find() && successOverall) {
            if(mCancel) {
                return false;
            }
            section = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker
            success = breakUpChapter(section);
            successOverall = successOverall && success;
            mChapter = matcher.group(1); // chapter number for next section
            lastIndex = matcher.start();
        }

        if (successOverall) {
            section = text.subSequence(lastIndex, text.length()); // get last section
            success = breakUpChapter(section);
            successOverall = successOverall && success;
        }
        return successOverall;
    }

    /**
     * break up chapter into sections based on chunk list
     *
     * @param text
     * @return
     */
    private boolean breakUpChapter(CharSequence text) {
        boolean successOverall = true;
        boolean success = true;
        if (!isMissing(mChapter)) {
            try {
                String chapter = getChapterKey(mChapter);
                if (null == chapter) {
                    addError(R.string.could_not_find_chapter, mChapter);
                    return false;
                }

                mCurrentChapter = Integer.valueOf(mChapter);

                JSONArray versebreaks = mChunks.get(chapter);

                updateStatus(R.string.processing_chapter, new Integer(mChaperCount - mCurrentChapter + 1).toString());

                String lastFirst = null;
                for (int i = 0; (i < versebreaks.length()) && success; i++) {
                    String first = versebreaks.getString(i);
                    success = extractVerses(chapter, text, lastFirst, first);
                    successOverall = successOverall && success;
                    lastFirst = first;
                }
                if (successOverall) {
                    success = extractVerses(chapter, text, lastFirst, "999999");
                    successOverall = successOverall && success;
                }

            } catch (Exception e) {
                Logger.e(TAG, "error parsing chapter " + mChapter, e);
                addError(R.string.could_not_parse_chapter, mChapter);
                return false;
            }
        } else { // save stuff before first chapter
            String chapter1 = getChapterKey("1"); // to get width of chapters
            String first = "00";
            try {
                JSONArray versebreaks = mChunks.get(chapter1);
                first = (String) versebreaks.get(0);
            } catch (JSONException e) {
                Logger.e(TAG, "Could not get first verse of chapter 1", e);
            }

            String chapter0 = "0000".substring(0, chapter1.length()); // match length of chapter 1
            String verse0 = "0000".substring(0, first.length()); // match length of verse 1
            success = saveSection(chapter0, verse0, text);
            successOverall = successOverall && success;
            success = saveSection(chapter0, "title", mBookName);
            successOverall = successOverall && success;
        }
        return successOverall;
    }

    private String getChapterKey(String mChapter) {
        String chapter = mChapter;
        if (mChunks.containsKey(chapter)) {
            return chapter;
        }

        chapter = "0" + chapter;
        if (mChunks.containsKey(chapter)) {
            return chapter;
        }

        chapter = "0" + chapter;
        if (mChunks.containsKey(chapter)) {
            return chapter;
        }

        addError(R.string.could_not_find_chapter, mChapter);
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
        if (null == start) { // we need to capture stuff before first verse
            String verse0 = "0000".substring(0, end.length()); // match length
            start = verse0;
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
            boolean done = false;
            boolean matchesFound = false;
            while (matcher.find()) {
                matchesFound = true;

                if (currentVerse >= end) {
                    done = true;
                    break;
                }

                if (currentVerse >= start) {
                    section = section + text.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
                }

                String verse = matcher.group(1);
                try {
                    currentVerse = Integer.valueOf(verse);
                } catch (NumberFormatException e) { // might be a range in format 12-13
                    String[] range = verse.split("-");
                    if (range.length != 2) {
                        return false;
                    }
                    currentVerse = Integer.valueOf(range[0]);
                }
                lastIndex = matcher.start();
            }

            if (!done && matchesFound && (currentVerse >= start) && (currentVerse < end)) {
                section = section + text.subSequence(lastIndex, text.length()); // get last section
            }

            if (!section.isEmpty()) {
                success = saveSection(chapter, firstVerse, section);
                successOverall = successOverall && success;
            } else {
                String format = mContext.getResources().getString(R.string.could_not_find_verses_in_chapter);
                String msg = String.format(format, start, end, chapter);
                addError(msg);
                return false;
            }
        }
        return successOverall;
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
            FileUtils.forceMkdir(chapterFolder);
            File output = new File(chapterFolder, fileName + ".txt");
            FileUtils.write(output, cleanChunk);
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

            saveSection(mChapter, firstVerse, section);
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
        mTempDir = new File(AppContext.context().getCacheDir(), System.currentTimeMillis() + "");
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
        FileUtils.deleteQuietly(mTempDir);
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
        }
        return null;
    }
}