package com.door43.translationstudio.core;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by blm on 7/23/16.
 */
public class ExportUsfm {

    public static final String TAG = ExportUsfm.class.getName();

    /**
     * save target translation as USFM file
     * @param activity
     * @param targetTranslation
     */
    static public void saveToUsfmWithPrompt(final Activity activity, final TargetTranslation targetTranslation) {
        new AlertDialog.Builder(activity, R.style.AppTheme_Dialog)
                .setTitle(R.string.title_export_usfm)
                .setMessage(R.string.export_usfm_by_chapter)
                .setPositiveButton(R.string.label_separate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveToUsfmWithSuccessIndication(activity, targetTranslation, true, true);
                    }
                })
                .setNeutralButton(R.string.dismiss, null)
                .setNegativeButton(R.string.label_whole, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveToUsfmWithSuccessIndication(activity, targetTranslation, false, false);
                    }
                })
                .show();
    }

    /**
     * save to usfm file and give success notification
     * @param separateChapters
     */
    static private void saveToUsfmWithSuccessIndication(Activity activity, TargetTranslation targetTranslation, boolean separateChapters, boolean zipFiles) {
        String zipFileName = null;
        if(zipFiles) {
            zipFileName = targetTranslation.getId() + ".zip";
        }

        File exportFile = saveToUSFM( targetTranslation, null, zipFileName, separateChapters);
        boolean success = (exportFile != null);
        String message;
        if(success) {
            String format = activity.getResources().getString(R.string.export_success);
            message = String.format(format, exportFile.toString());
        } else {
            message = activity.getResources().getString(R.string.export_failed);
        }
        new AlertDialog.Builder(activity, R.style.AppTheme_Dialog)
                .setTitle(R.string.title_export_usfm)
                .setMessage(message)
                .setPositiveButton(R.string.dismiss, null)
                .show();
    }

    /**
     * output target translation to USFM file, returns file name to check for success
     * @param targetTranslation
     * @param destinationFolder - if null then public downloads folder on SD card will be used
     * @param zipFileName - this is the filename to be used if files are to be zipped, if null then files are not zipped and default file name is used.
     * @param separateChapters - if true then chapters will be separated
     * @return target zipFileName or null if error
     */
    static public File saveToUSFM(TargetTranslation targetTranslation, File destinationFolder, String zipFileName, boolean separateChapters) {
        if(destinationFolder == null) {
            destinationFolder = App.getPublicDownloadsDirectory();
        }

        File exportFile = null;
        try {
            exportFile = exportAsUSFM(targetTranslation, destinationFolder, zipFileName, separateChapters);
        } catch (Exception e) {
            Logger.e(TAG, "Failed to export the target translation " + targetTranslation.getId(), e);
        }
        if( (exportFile == null) || !exportFile.exists()) {
            return null;
        }

        return exportFile;
    }

    /**
     * Exports a target translation as a USFM file
     * @param targetTranslation
     * @param outputFolder - folder for output file
     * @param zipFileName - this is the filename to be used if files are to be zipped, if null then files are not zipped and default file name is used.
     * @param separateChapters - if true then each chapter will be in a different file
     * @return output file
     */
    static private File exportAsUSFM(TargetTranslation targetTranslation, File outputFolder, String zipFileName, boolean separateChapters) throws IOException {
        File tempDir = new File(App.getTranslator().getLocalCacheDir(), System.currentTimeMillis() + "");
        tempDir.mkdirs();
        ChapterTranslation[] chapters = targetTranslation.getChapterTranslations();
        PrintStream ps = null;
        String outputFileName = null;
        File chapterFile = null;
        for(ChapterTranslation chapter:chapters) {
            // TRICKY: the translation format doesn't matter for exporting
            FrameTranslation[] frames = targetTranslation.getFrameTranslations(chapter.getId(), TranslationFormat.DEFAULT);
            if(frames.length == 0) continue;

            boolean needNewFile = (ps == null) || (separateChapters);
            if(needNewFile) {
                // chapter id
                String bookCode = targetTranslation.getProjectId().toUpperCase();
                String languageId = targetTranslation.getTargetLanguageId();
                String languageName = targetTranslation.getTargetLanguageName();
                ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();
                String bookTitle = "";
                if((projectTranslation != null) && projectTranslation.isTitleFinished()) {
                    bookTitle = projectTranslation.getTitle().trim();
                }
                if(bookTitle.isEmpty()) {
                    bookTitle = bookCode;
                }
                if(separateChapters) {
                    bookTitle += " " + chapter.getId();
                }

                // generate file name
                if(separateChapters) {
                    outputFileName = "chapter_" + chapter.getId() + ".usfm";
                } else {
                    outputFileName = System.currentTimeMillis() + "_" + languageId + "_" + bookCode + "_" + bookTitle + ".usfm";
                }
                chapterFile = new File(tempDir, outputFileName);
                chapterFile.createNewFile();

                if(ps != null) {
                    ps.close();
                }
                ps = new PrintStream(chapterFile);

                String id = "\\id " + bookCode + " " + bookTitle + ", " + (languageId + ", " + languageName);
                ps.println(id);
                String bookID = "\\toc1 " + bookTitle;
                ps.println(bookID);
                String shortBookID = "\\toc3 " + bookCode;
                ps.println(shortBookID);
            }

            if(chapter.isTitleFinished()) {
                String chapterTitle = "\\cl " + chapter.title;
                ps.println(chapterTitle);
            }

            String chapterNumber = "\\c " + chapter.getId();
            ps.println(chapterNumber);

            if(chapter.isReferenceFinished()) {
                String chapterRef = "\\cd " + chapter.reference;
                ps.println(chapterRef);
            }

            // frames
            for(FrameTranslation frame:frames) {

                String text = frame.body;

                // text
                ps.println("\\s5"); // section marker
                ps.println(text);
                ps.println();
            }
        }

        ps.close();

        File destFile = null;
        if(zipFileName != null) { // zip them together
            File[] chapterFiles = tempDir.listFiles();
            if (chapterFiles != null && chapterFiles.length > 0) {
                try {
                    File zipFile = new File(outputFolder, zipFileName);
                    Zip.zip(chapterFiles, zipFile);
                    destFile = zipFile;
                } catch (IOException e) {
                    FileUtilities.deleteQuietly(tempDir);
                    throw (e);
                }
            }
        } else if( (chapterFile != null) && (outputFileName != null) ) {
            File outputFile = new File(outputFolder, outputFileName);
            boolean success = FileUtilities.moveOrCopyQuietly(chapterFile, outputFile);
            if(success) {
                destFile = outputFile;
            }
        }
        FileUtilities.deleteQuietly(tempDir);
        return destFile;
    }
}
