package com.door43.translationstudio.filebrowser;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ArchiveDetails;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.util.Zip;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

/**
 * This class represents a single file item
 */
public class FileItem {
    public final File file;
    public final boolean isUpButton;
    private ArchiveDetails archiveDetails;


    private FileItem(File file, boolean isUpButton) {
        this.file = file;
        this.isUpButton = isUpButton;
    }

    /**
     * Creates a new instance of a file
     * @param file
     * @return
     */
    public static FileItem getInstance(File file) {
        return new FileItem(file, false);
    }

    /**
     * Returns a new instance of the up button
     *
     * @return
     */
    public static FileItem getUpInstance() {
        return new FileItem(null, true);
    }

    /**
     * Returns the title of the file
     * @return
     */
    public String getTitle() {
        if(isUpButton) {
            return "";
        } else {
            return file.getName();
        }
    }

    /**
     * Checks if this file is a translationStudio archive
     */
    public void inspect(String preferredLocale, Library library) {
        if(file != null) {
            try {
                this.archiveDetails = ArchiveDetails.newInstance(file, preferredLocale, library);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.archiveDetails = ArchiveDetails.newDummyInstance();
    }

    /**
     * Returns the archive details.
     * You must call inspect first in order to load the
     * archive details
     * @return
     */
    public ArchiveDetails getArchiveDetails() {
        return this.archiveDetails;
    }

    /**
     * Returns the icon for this file
     * @return
     */
    public int getIconResource() {
        if(isUpButton) {
            return R.drawable.ic_arrow_back_black_24dp;
        } else if(isBackupsDir()) {
            return R.drawable.ic_history_black_24dp;
        } else if(isTranslationArchive()) {
            return R.drawable.ic_library_books_black_24dp;
        } else if(file.isDirectory()) {
            return R.drawable.ic_folder_open_black_24dp;
        } else {
            return R.drawable.ic_insert_drive_file_black_24dp;
        }
    }

    /**
     * Checks if this is the automatic backups directory.
     * translationStudio/backups - contains the automatic backups
     * @return
     */
    public boolean isBackupsDir() {
        return !isUpButton
            && file != null
            && file.isDirectory()
                && file.getName().equalsIgnoreCase("backups")
                && file.getParentFile().getName().equalsIgnoreCase("translationStudio");
    }

    /**
     * Checks if this file is a translation archive
     * @return
     */
    public boolean isTranslationArchive() {
        return !isUpButton
                && file != null
                && !file.isDirectory()
                && FilenameUtils.getExtension(file.getName()).equalsIgnoreCase(Translator.ARCHIVE_EXTENSION);
    }
}
