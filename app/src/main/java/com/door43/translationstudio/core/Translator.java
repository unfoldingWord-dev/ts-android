package com.door43.translationstudio.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.Editable;
import android.text.SpannedString;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.rendering.USXtoUSFMConverter;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.unfoldingword.resourcecontainer.Resource;

/**
 * Created by joel on 8/29/2015.
 */
public class Translator {
    private static final int TSTUDIO_PACKAGE_VERSION = 2;
    private static final String GENERATOR_NAME = "ts-android";
    public static final String ARCHIVE_EXTENSION = "tstudio";
    public static final String TAG = Translator.class.getName();

    private final File mRootDir;
    private final Context mContext;
    private Profile profile;

    public Translator(Context context, Profile profile, File rootDir) {
        mContext = context;
        mRootDir = rootDir;
        this.profile = profile;
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
        Logger.i(TAG, "getTargetTranslations: Reading all target translations");
        final List<TargetTranslation> translations = new ArrayList<>();
        mRootDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(!filename.equalsIgnoreCase("cache") && new File(dir, filename).isDirectory()) {
                    Logger.i(TAG, "getTargetTranslations: Reading " + translations.size() + " : " + filename);
                    TargetTranslation translation = getTargetTranslation(filename);
                    if (translation != null) {
                        translations.add(translation);
                    }
                }
                return false;
            }
        });

        Logger.i(TAG, "getTargetTranslations: Finished Reading all target translations");
        return translations.toArray(new TargetTranslation[translations.size()]);
    }

    /**
     * Returns an array of all active translation IDs - this does not hold in memory each manifest.  Requires less memory to just get a count of items.
     * @return
     */
    public String[] getTargetTranslationIDs() {
        Logger.i(TAG, "getTargetTranslationIDs: Reading all target translations");
        final List<String> translations = new ArrayList<>();
        mRootDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(!filename.equalsIgnoreCase("cache") && new File(dir, filename).isDirectory()) {
                    Logger.i(TAG, "getTargetTranslationIDs: Reading " + translations.size() + " : " + filename);
                    TargetTranslation translation = getTargetTranslation(filename);
                    if (translation != null) {
                        translations.add(translation.getId());
                    }
                }
                return false;
            }
        });

        Logger.i(TAG, "getTargetTranslationIDs: Finished Reading all target translations");
        return translations.toArray(new String[translations.size()]);
    }

    /**
     * Returns an array of all translation File names - this does not verify the list.
     * @return
     */
    public String[] getTargetTranslationFileNames() {
        Logger.i(TAG, "getTargetTranslationFileNames: Reading all target translations");
        final List<String> translations = new ArrayList<>();
        mRootDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(!filename.equalsIgnoreCase("cache") && new File(dir, filename).isDirectory()) {
                    translations.add(filename);
                }
                return false;
            }
        });

        Logger.i(TAG, "getTargetTranslationFileNames: Finished Reading all target translations");
        return translations.toArray(new String[translations.size()]);
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
     * Creates a new Target Translation. If one already exists it will return it without changing anything.
     * @param nativeSpeaker the human translator
     * @param targetLanguage the language that is being translated into
     * @param projectSlug the project that is being translated
     * @param resourceType the type of translation that is occurring
     * @param resourceSlug the resource that is being created
     * @param translationFormat the format of the translated text
     * @return A new or existing Target Translation
     */
    public TargetTranslation createTargetTranslation(NativeSpeaker nativeSpeaker, TargetLanguage targetLanguage, String projectSlug, ResourceType resourceType, String resourceSlug, TranslationFormat translationFormat) {
        // TRICKY: force deprecated formats to use new formats
        if(translationFormat == TranslationFormat.USX) {
            translationFormat = TranslationFormat.USFM;
        } else if(translationFormat == TranslationFormat.DEFAULT) {
            translationFormat = TranslationFormat.MARKDOWN;
        }

        String targetTranslationId = TargetTranslation.generateTargetTranslationId(targetLanguage.slug, projectSlug, resourceType, resourceSlug);
        TargetTranslation targetTranslation = getTargetTranslation(targetTranslationId);
        if(targetTranslation == null) {
            File targetTranslationDir = new File(this.mRootDir, targetTranslationId);
            try {
                PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                return TargetTranslation.create(this.mContext, nativeSpeaker, translationFormat, targetLanguage, projectSlug, resourceType, resourceSlug, pInfo, targetTranslationDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return targetTranslation;
    }

    private void setTargetTranslationAuthor(TargetTranslation targetTranslation) {
        if(profile != null && targetTranslation != null) {
            String name = profile.getFullName();
            String email = "";
            if(profile.gogsUser != null) {
                name = profile.gogsUser.fullName;
                email = profile.gogsUser.email;
            }
            targetTranslation.setAuthor(name, email);
        }
    }

    /**
     * Returns a target translation if it exists
     * @param targetTranslationId
     * @return
     */
    public TargetTranslation getTargetTranslation(String targetTranslationId) {
        if(targetTranslationId != null) {
            Logger.i(TAG, "getTargetTranslation: Reading :" + targetTranslationId);
            File targetTranslationDir = new File(mRootDir, targetTranslationId);
            TargetTranslation targetTranslation = TargetTranslation.open(targetTranslationDir);
            setTargetTranslationAuthor(targetTranslation);
            return targetTranslation;
        }
        return null;
    }

    /**
     * Deletes a target translation from the device
     * @param targetTranslationId
     */
    public void deleteTargetTranslation(String targetTranslationId) {
        if(targetTranslationId != null) {
            File targetTranslationDir = new File(mRootDir, targetTranslationId);
            FileUtilities.safeDelete(targetTranslationDir);
        }
    }

    /**
     * Compiles all the editable text back into source that could be either USX or USFM.  It replaces
     *   the displayed text in spans with their mark-ups.
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
     * creates a JSON object that contains the manifest.
     * @param targetTranslation
     * @return
     * @throws Exception
     */
    private JSONObject buildArchiveManifest(TargetTranslation targetTranslation) throws Exception {
        targetTranslation.commit();

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
        return manifestJson;
    }

    /**
     * Exports a single target translation in .tstudio format to File
     * @param targetTranslation
     * @param outputFile
     */
    public void exportArchive(TargetTranslation targetTranslation, File outputFile) throws Exception {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            exportArchive(targetTranslation, out, outputFile.toString());
        } catch (Exception e) {
            throw e;
        } finally {
            FileUtilities.closeQuietly(out);
        }
    }

    /**
     * Exports a single target translation in .tstudio format to OutputStream
     * @param targetTranslation
     * @param out
     */
    public void exportArchive(TargetTranslation targetTranslation, OutputStream out, String fileName) throws Exception {
        if(!FileUtilities.getExtension(fileName).toLowerCase().equals(ARCHIVE_EXTENSION)) {
            throw new Exception("Output file must have '" + ARCHIVE_EXTENSION + "' extension");
        }
        if(targetTranslation == null) {
            throw new Exception("Not a valid target translation");
        }

        targetTranslation.commitSync();

        JSONObject manifestJson = buildArchiveManifest(targetTranslation);
        File tempCache = new File(getLocalCacheDir(), System.currentTimeMillis()+"");
        try {
            tempCache.mkdirs();
            File manifestFile = new File(tempCache, "manifest.json");
            manifestFile.createNewFile();
            FileUtilities.writeStringToFile(manifestFile, manifestJson.toString());
            Zip.zipToStream(new File[]{manifestFile, targetTranslation.getPath()}, out);
        } catch (Exception e) {
            throw e;
        } finally {
            FileUtilities.closeQuietly(out);
            FileUtilities.deleteQuietly(tempCache);
        }
    }

    /**
     * Imports a draft translation into a target translation.
     * A new target translation will be created if one does not already exist.
     * This is a lengthy operation and should be ran within a task
     * @param draftTranslation the draft translation to be imported
     * @param library
     * @return
     */
    public TargetTranslation importDraftTranslation(NativeSpeaker nativeSpeaker, ResourceContainer draftTranslation, Door43Client library) {
        TargetLanguage targetLanguage = library.index().getTargetLanguage(draftTranslation.language.slug);
        // TRICKY: for now android only supports "regular" or "obs" "text" translations
        // TODO: we should technically check if the project contains more than one resource when determining if it needs a regular slug or not.
        String resourceSlug = draftTranslation.project.slug.equals("obs") ? "obs" : Resource.REGULAR_SLUG;

        TranslationFormat format = TranslationFormat.parse(draftTranslation.contentMimeType);
        TargetTranslation t = createTargetTranslation(nativeSpeaker, targetLanguage, draftTranslation.project.slug, ResourceType.TEXT, resourceSlug, format);

        // convert legacy usx format to usfm
        boolean convertToUSFM = format == TranslationFormat.USX;

        try {
            if (t != null) {
                // commit local changes to history
                t.commitSync();

                // begin import
                t.applyProjectTitleTranslation(draftTranslation.readChunk("front", "title"));
                for(String cSlug:draftTranslation.chapters()) {
                    ChapterTranslation ct = t.getChapterTranslation(cSlug);
                    t.applyChapterTitleTranslation(ct, draftTranslation.readChunk(cSlug, "title"));
                    t.applyChapterReferenceTranslation(ct, draftTranslation.readChunk(cSlug, "reference"));
                    for(String fSlug:draftTranslation.chunks(cSlug)) {
                        String body = draftTranslation.readChunk(cSlug, fSlug);
                        String text = convertToUSFM ? USXtoUSFMConverter.doConversion(body).toString() : body;
                        t.applyFrameTranslation(t.getFrameTranslation(cSlug, fSlug, format), text);
                    }
                }
                // TODO: 3/23/2016 also import the front and back matter along with project title
                t.setParentDraft(draftTranslation);
                t.commitSync();
            }
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "Failed to import target translation", e);
            // TODO: 1/20/2016 revert changes
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to save target translation before importing target translation", e);
        }
        return t;
    }

    /**
     * Imports a tstudio archive, uses default of merge, not overwrite
     * @param file
     * @return ImportResults object
     */
    public ImportResults importArchive(File file) throws Exception {
        return importArchive( file, false);
    }

   /**
    * Imports a tstudio archive
    * @param file
    * @param overwrite - if true then local changes are clobbered
    * @return ImportResults object
    */
    public ImportResults importArchive(File file, boolean overwrite) throws Exception {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return importArchive(in, overwrite);
        } catch (Exception e) {
            throw e;
        } finally {
            FileUtilities.closeQuietly(in);
        }
    }

    /**
     * Imports a tstudio archive from an input stream, uses default of merge, not overwrite
     * @param in
     * @return ImportResults object
     */
    public ImportResults importArchive(InputStream in) throws Exception {
        return importArchive( in, false);
    }

    /**
     * Imports a tstudio archive from an input stream
     * @param in
     * @param overwrite - if true then local changes are clobbered
     * @return ImportResults object
     */
    public ImportResults importArchive(InputStream in, boolean overwrite) throws Exception {
        File archiveDir = new File(getLocalCacheDir(), System.currentTimeMillis()+"");
        String importedSlug = null;
        boolean mergeConflict = false;
        try {
            archiveDir.mkdirs();
            Zip.unzipFromStream(in, archiveDir);

            File[] targetTranslationDirs = ArchiveImporter.importArchive(archiveDir);
            for(File newDir:targetTranslationDirs) {
                TargetTranslation newTargetTranslation = TargetTranslation.open(newDir);
                if(newTargetTranslation != null) {
                    // TRICKY: the correct id is pulled from the manifest to avoid propogating bad folder names
                    String targetTranslationId = newTargetTranslation.getId();
                    File localDir = new File(mRootDir, targetTranslationId);
                    TargetTranslation localTargetTranslation = TargetTranslation.open(localDir);
                    if((localTargetTranslation != null) && !overwrite) {
                        // commit local changes to history
                        if(localTargetTranslation != null) {
                            localTargetTranslation.commitSync();
                        }

                        // merge translations
                        try {
                            boolean mergeSuccess = localTargetTranslation.merge(newDir);
                            if(!mergeSuccess) {
                                mergeConflict = true;
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    }  else {
                        // import new translation
                        FileUtilities.safeDelete(localDir); // in case local was an invalid target translation
                        FileUtilities.moveOrCopyQuietly(newDir, localDir);
                    }
                    // update the generator info. TRICKY: we re-open to get the updated manifest.
                    TargetTranslation.updateGenerator(mContext, TargetTranslation.open(localDir));

                    importedSlug = targetTranslationId;
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            FileUtilities.closeQuietly(in);
            FileUtilities.deleteQuietly(archiveDir);
        }

        return new ImportResults(importedSlug, mergeConflict);
    }

    /**
     * returns the import results which includes:
     *   the target translation slug that was successfully imported
     *   a flag indicating a merge conflict
     */
    public class ImportResults {
        public final String importedSlug;
        public final boolean mergeConflict;

        ImportResults(String importedSlug, boolean mergeConflict) {
            this.importedSlug = importedSlug;
            this.mergeConflict = mergeConflict;
        }

        public boolean isSuccess() {
            boolean success = (importedSlug != null) && (!importedSlug.isEmpty());
            return success;
        }
    }

    /**
     * Exports a target translation as a pdf file
     * @param targetTranslation
     * @param outputFile
     */
    public void exportPdf(Door43Client library, TargetTranslation targetTranslation, TranslationFormat format, String fontPath, File imagesDir, boolean includeImages, boolean includeIncompleteFrames, File outputFile) throws Exception {
        PdfPrinter printer = new PdfPrinter(mContext, library, targetTranslation, format, fontPath, imagesDir);
        printer.includeMedia(includeImages);
        printer.includeIncomplete(includeIncompleteFrames);
        File pdf = printer.print();
        if(pdf.exists()) {
            outputFile.delete();
            FileUtilities.moveOrCopyQuietly(pdf, outputFile);
        }

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            // use PrintedPdf
//        } else {
//            // legacy pdf export
//        }
    }

     /**
     * This will move a target translation into the root dir.
     * Any existing target translation will be replaced
     * @param tempTargetTranslation
     * @throws IOException
     */
    public void restoreTargetTranslation(TargetTranslation tempTargetTranslation) throws IOException {
        if(tempTargetTranslation != null) {
            File destDir = new File(mRootDir, tempTargetTranslation.getId());
            FileUtilities.safeDelete(destDir);
            FileUtilities.moveOrCopyQuietly(tempTargetTranslation.getPath(), destDir);
        }
    }

    /**
     * Ensures the name of the target translation directory matches the target translation id and corrects it if not
     * If the destination already exists the file path will not be changed
     * @param tt
     */
    public boolean normalizePath(TargetTranslation tt) {
        if(!tt.getPath().getName().equals(tt.getId())) {
            File dest = new File(tt.getPath().getParentFile(), tt.getId());
            if(!dest.exists()) {
                return FileUtilities.moveOrCopyQuietly(tt.getPath(), dest);
            }
        }
        return false;
    }
}
