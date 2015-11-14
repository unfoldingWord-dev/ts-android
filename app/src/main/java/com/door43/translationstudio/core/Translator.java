package com.door43.translationstudio.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.Editable;
import android.text.SpannedString;

import com.door43.tools.reporting.Logger;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 8/29/2015.
 */
public class Translator {
    private static final int TSTUDIO_PACKAGE_VERSION = 2;
    private static final String GENERATOR_NAME = "ts-android";
    public static final String ARCHIVE_EXTENSION = "tstudio";
    private final File mRootDir;
    private final Context mContext;

    public Translator(Context context, File rootDir) {
        mContext = context;
        mRootDir = rootDir;
    }

    /**
     * Returns the root directory to the target translations
     * @return
     */
    public File getPath() {
        return mRootDir;
    }

    /**
     * Returns an array of all active translations
     * @return
     */
    public TargetTranslation[] getTargetTranslations() {
        final List<TargetTranslation> translations = new ArrayList<>();
        mRootDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(!filename.equalsIgnoreCase("cache")) {
                    TargetTranslation translation = getTargetTranslation(filename);
                    if (translation != null) {
                        translations.add(translation);
                    }
                }
                return false;
            }
        });

        return translations.toArray(new TargetTranslation[translations.size()]);
    }

    /**
     * Returns the local translations cache directory.
     * This is where import and export operations can expand files.
     * @return
     */
    private File getLocalCacheDir() {
        return new File(mRootDir, "cache");
    }

    /**
     * Initializes a new target translation
     * @param targetLanguage the target language the project will be translated into
     * @param projectId the id of the project that will be translated
     * @return
     */
    public TargetTranslation createTargetTranslation(TargetLanguage targetLanguage, String projectId) {
        try {
            return TargetTranslation.create(mContext, targetLanguage, projectId, mRootDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a target translation if it exists
     * @param targetTranslationId
     * @return
     */
    public TargetTranslation getTargetTranslation(String targetTranslationId) {
        if(targetTranslationId != null) {
            try {
                String projectId = TargetTranslation.getProjectIdFromId(targetTranslationId);
                String targetLanguageId = TargetTranslation.getTargetLanguageIdFromId(targetTranslationId);

                File dir = TargetTranslation.generateTargetTranslationDir(targetTranslationId, mRootDir);
                if(dir.exists()) {
                    return new TargetTranslation(targetLanguageId, projectId, mRootDir);
                } else {
                    return null;
                }
            } catch (StringIndexOutOfBoundsException e) {
                Logger.e(this.getClass().getName(), "Failed to retrieve the target translation '" + targetTranslationId + "'", e);
            }
        }
        return null;
    }

    /**
     * Deletes a target translation from the device
     * @param targetTranslationId
     */
    public void deleteTargetTranslation(String targetTranslationId) {
        if(targetTranslationId != null) {
            try {
                File dir = TargetTranslation.generateTargetTranslationDir(targetTranslationId, mRootDir);
                FileUtils.deleteQuietly(dir);
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Compiles all the spans within the text into human readable strings
     * @param text
     * @return
     */
    public static String compileTranslation(Editable text) {
        StringBuilder compiledString = new StringBuilder();
        int next;
        int lastIndex = 0;
        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, text.length(), SpannedString.class);
            SpannedString[] verses = text.getSpans(i, next, SpannedString.class);
            for (SpannedString s : verses) {
                int sStart = text.getSpanStart(s);
                int sEnd = text.getSpanEnd(s);
                // attach preceeding text
                if (lastIndex >= text.length() | sStart >= text.length()) {
                    // out of bounds
                }
                compiledString.append(text.toString().substring(lastIndex, sStart));
                // explode span
                compiledString.append(s.toString());
                lastIndex = sEnd;
            }
        }
        // grab the last bit of text
        compiledString.append(text.toString().substring(lastIndex, text.length()));
        return compiledString.toString().trim();
    }

    /**
     * Exports a single target translation in .tstudio format
     * @param targetTranslation
     * @param outputFile
     */
    public void exportArchive(TargetTranslation targetTranslation, File outputFile) throws Exception {
        if(!FilenameUtils.getExtension(outputFile.getName()).toLowerCase().equals(ARCHIVE_EXTENSION)) {
            throw new Exception("Not a translationStudio archive");
        }

        // build manifest
        JSONObject manifestJson = new JSONObject();
        JSONObject generatorJson = new JSONObject();
        generatorJson.put("name", GENERATOR_NAME);
        PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        generatorJson.put("build", pInfo.versionCode);
        manifestJson.put("generator", generatorJson);
        manifestJson.put("package_version", TSTUDIO_PACKAGE_VERSION);
        manifestJson.put("timestamp", Util.unixTime());
        JSONArray translationsJson = new JSONArray();
        JSONObject translationJson = new JSONObject();
        translationJson.put("path", targetTranslation.getId());
        translationJson.put("id", targetTranslation.getId());
        translationJson.put("commit_hash", targetTranslation.getCommitHash());
        translationJson.put("direction", targetTranslation.getTargetLanguageDirection());
        translationJson.put("target_language_name", targetTranslation.getTargetLanguageName());
        translationsJson.put(translationJson);
        manifestJson.put("target_translations", translationsJson);

        File tempCache = new File(getLocalCacheDir(), System.currentTimeMillis()+"");
        try {
            tempCache.mkdirs();
            File manifestFile = new File(tempCache, "manifest.json");
            manifestFile.createNewFile();
            FileUtils.write(manifestFile, manifestJson.toString());
            Zip.zip(new File[]{manifestFile, targetTranslation.getPath()}, outputFile);
        } catch (Exception e) {
            FileUtils.deleteQuietly(tempCache);
            FileUtils.deleteQuietly(outputFile);
            throw e;
        }

        // clean
        FileUtils.deleteQuietly(tempCache);
    }

    /**
     * Imports target translations from an archive
     * todo: we should have another method that will inspect the archive and return the details to the user so they can decide if they want to import it
     * @param file
     */
    public void importArchive(File file) throws Exception {
        File tempCache = new File(getLocalCacheDir(), System.currentTimeMillis()+"");
        try {
            tempCache.mkdirs();
            Zip.unzip(file, tempCache);
            File[] targetTranslationDirs = ArchiveImporter.importArchive(tempCache);
            for(File dir:targetTranslationDirs) {
                File newDir = new File(mRootDir, dir.getName());
                // delete existing translation
                FileUtils.deleteQuietly(newDir);
                // import new translation
                FileUtils.moveDirectory(dir, newDir);
            }
            if(targetTranslationDirs.length == 0) {
                throw new Exception("The archive does not contain any valid target translations");
            }
        } catch (Exception e) {
            FileUtils.deleteQuietly(tempCache);
            if(!FilenameUtils.getExtension(file.getName()).toLowerCase().equals(ARCHIVE_EXTENSION)) {
                throw new Exception("Not a translationStudio archive");
            } else {
                throw e;
            }
        }

        // clean
        FileUtils.deleteQuietly(tempCache);
    }

    /**
     * Exports a target translation as a pdf file
     * @param targetTranslation
     * @param outputFile
     */
    public void exportPdf(TargetTranslation targetTranslation, TranslationFormat format, String fontPath, File outputFile) throws Exception {
        PdfPrinter printer = new PdfPrinter(mContext, targetTranslation, format, fontPath);
        printer.includeMedia(false);
        printer.includeIncomplete(true);
        File pdf = printer.print();
        if(pdf.exists()) {
            outputFile.delete();
            FileUtils.moveFile(pdf, outputFile);
        }

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            // use PrintedPdf
//        } else {
//            // legacy pdf export
//        }
    }

    /**
     * Exports a target translation as a single DokuWiki file
     * @param targetTranslation
     * @return
     */
    public void exportDokuWiki(TargetTranslation targetTranslation, File outputFile) throws IOException {
        File tempDir = new File(getLocalCacheDir(), System.currentTimeMillis() + "");
        tempDir.mkdirs();
        ChapterTranslation[] chapters = targetTranslation.getChapterTranslations();
        for(ChapterTranslation chapter:chapters) {
            // TRICKY: the translation format doesn't matter for exporting
            FrameTranslation[] frames = targetTranslation.getFrameTranslations(chapter.getId(), TranslationFormat.DEFAULT);
            if(frames.length == 0) continue;

            // compile translation
            File chapterFile = new File(tempDir, chapter.getId() + ".txt");
            chapterFile.createNewFile();
            PrintStream ps = new PrintStream(chapterFile);

            // language
            ps.print("//");
            ps.print(targetTranslation.getTargetLanguageName());
            ps.println("//");
            ps.println();

            // project
            ps.print("//");
            ps.print(targetTranslation.getProjectId());
            ps.println("//");
            ps.println();

            // chapter title
            ps.print("======");
            ps.print(chapter.title.trim());
            ps.println("======");
            ps.println();

            // frames
            for(FrameTranslation frame:frames) {
                // image
                ps.print("{{");
                // TODO: the api version and image dimensions should be placed in the user preferences
                String apiVersion = "1";
                // TODO: for now all images use the english versions
                String languageCode = "en"; // eventually we should use: getSelectedTargetLanguage().getId()
                ps.print("https://api.unfoldingword.org/" + targetTranslation.getProjectId() + "/jpg/" + apiVersion + "/" + languageCode + "/360px/" + targetTranslation.getProjectId() + "-" + languageCode + "-" + chapter.getId() + "-" + frame.getId() + ".jpg");
                ps.println("}}");
                ps.println();

                // convert tags
                String text = frame.body.trim();

                // TODO: convert usx tags to USFM

                // text
                ps.println(text);
                ps.println();
            }

            // chapter reference
            ps.print("//");
            ps.print(chapter.reference.trim());
            ps.println("//");
            ps.close();
        }
        File[] chapterFiles = tempDir.listFiles();
        if(chapterFiles != null && chapterFiles.length > 0) {
            try {
                Zip.zip(chapterFiles, outputFile);
            } catch (IOException e) {
                FileUtils.deleteQuietly(tempDir);
                throw (e);
            }
        }
        FileUtils.deleteQuietly(tempDir);
    }

    /**
     * Imports a DokuWiki file and converts it into a target translation
     * @param file
     * @return
     */
    public TargetTranslation importDokuWiki(Library library, File file) throws IOException {
        List<TargetTranslation> targetTranslations = new ArrayList<>();
        TargetTranslation targetTranslation = null;
        if(file.exists() && file.isFile()) {
            StringBuilder frameBuffer = new StringBuilder();
            String line, chapterId = "", frameId = "", chapterTitle = "";
            Pattern pattern = Pattern.compile("-(\\d\\d)-(\\d\\d)\\.jpg");
            TargetLanguage targetLanguage = null;
            Project project = null;

            BufferedReader br = new BufferedReader(new FileReader(file));

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if(line.length() >= 4 && line.substring(0, 2).equals("//")) {
                    line = line.substring(2, line.length() - 2).trim();
                    if(targetLanguage == null) {
                        // retrieve the translation language
                        targetLanguage = library.findTargetLanguageByName(line);
                        if(targetLanguage == null) return null;
                    } else if(project == null) {
                        // retrieve project
                        project = library.getProject(line, "en");
                        if(project == null) return null;
                        // create target translation
                        targetTranslation = new TargetTranslation(targetLanguage.getId(), project.getId(), mRootDir);
                    } else if (!chapterId.isEmpty() && !frameId.isEmpty()) {
                        // retrieve chapter reference (end of chapter) and write chapter
                        ChapterTranslation chapterTranslation =targetTranslation.getChapterTranslation(chapterId);
                        targetTranslation.applyChapterTitleTranslation(chapterTranslation, chapterTitle);
                        targetTranslation.applyChapterReferenceTranslation(chapterTranslation, line);

                        // save the last frame of the chapter
                        if (frameBuffer.length() > 0) {
                            FrameTranslation frameTranslation = targetTranslation.getFrameTranslation(chapterId, frameId, TranslationFormat.DEFAULT);
                            targetTranslation.applyFrameTranslation(frameTranslation, frameBuffer.toString().trim());
                        }

                        chapterId = "";
                        frameId = "";
                        frameBuffer.setLength(0);

                        targetTranslations.add(targetTranslation);
                    } else {
                        // start loading a new translation
                        project = null;
                        chapterId = "";
                        frameId = "";
                        chapterTitle = "";
                        frameBuffer.setLength(0);
                        targetTranslation = null;

                        // retrieve the translation language
                        targetLanguage = library.findTargetLanguageByName(line);
                        if(targetLanguage == null) return null;
                    }
                } else if(line.length() >= 12 && line.substring(0, 6).equals("======")) {
                    // start of a new chapter
                    chapterTitle = line.substring(6, line.length() - 6).trim(); // this is saved at the end of the chapter
                } else if(line.length() >= 4 && line.substring(0, 2).equals("{{")) {
                    // save the previous frame
                    if(project != null && !chapterId.isEmpty() && !frameId.isEmpty() && frameBuffer.length() > 0) {
                        FrameTranslation frameTranslation = targetTranslation.getFrameTranslation(chapterId, frameId, TranslationFormat.DEFAULT);
                        targetTranslation.applyFrameTranslation(frameTranslation, frameBuffer.toString().trim());
                    }

                    // image tag. We use this to get the frame number for the following text.
                    Matcher matcher = pattern.matcher(line);
                    while(matcher.find()) {
                        chapterId = matcher.group(1);
                        frameId = matcher.group(2);
                    }
                    // clear the frame buffer
                    frameBuffer.setLength(0);
                } else {
                    // frame translation
                    frameBuffer.append(line);
                    frameBuffer.append('\n');
                }
            }
            return targetTranslation;
        }
        return null;
    }

    /**
     * Imports a DokuWiki zip archive
     * @param archive
     * @return
     */
    public boolean importDokuWikiArchive(Library library, File archive) throws IOException {
        String[] name = archive.getName().split("\\.");
        Boolean success = true;
        if(archive.exists() && archive.isFile() && name[name.length - 1].equals("zip")) {
            File tempDir = new File(getLocalCacheDir() + "/" + System.currentTimeMillis());
            tempDir.mkdirs();
            Zip.unzip(archive, tempDir);

            File[] files = tempDir.listFiles();
            if(files.length > 0) {
                // fix legacy DokuWiki export (contained root directory in archive)
                File realPath = tempDir;
                if(files.length == 1 && files[0].isDirectory()) {
                    realPath = files[0];
                    files = files[0].listFiles();
                    if(files.length == 0) {
                        FileUtilities.deleteRecursive(tempDir);
                        return false;
                    }
                }

                // ensure this is not a legacy project archive
                File gitDir = new File(realPath, ".git");
                if(gitDir.exists() && gitDir.isDirectory()) {
                    FileUtilities.deleteRecursive(tempDir);
                    // We no longer support legacy archives. If you need it look in the history for projects.Sharing.prepareLegacyArchiveImport
                    return false;
                }

                // begin import
                for(File f:files) {
                    TargetTranslation targetTranslation = importDokuWiki(library, f);
                    if(targetTranslation == null) {
                        success = false;
                    }
                }
            }
            FileUtilities.deleteRecursive(tempDir);
        }
        return success;
    }
}
