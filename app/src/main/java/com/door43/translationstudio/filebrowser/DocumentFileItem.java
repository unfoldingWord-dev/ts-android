package com.door43.translationstudio.filebrowser;

import android.content.Context;
import android.support.v4.provider.DocumentFile;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ArchiveDetails;
import com.door43.translationstudio.core.Translator;
import com.door43.util.FileUtilities;

import org.unfoldingword.door43client.Door43Client;


/**
 * This class represents a single file item
 */
public class DocumentFileItem {
    public static final String ZIP = "zip";
    public final DocumentFile file;
    public final Context context;
    public final boolean isUpButton;
    private ArchiveDetails archiveDetails;


    private DocumentFileItem(Context context, DocumentFile file, boolean isUpButton) {
        this.file = file;
        this.isUpButton = isUpButton;
        this.context = context;
    }

    /**
     * Creates a new instance of a file
     * @param file
     * @return
     */
    public static DocumentFileItem getInstance(Context context, DocumentFile file) {
        return new DocumentFileItem(context, file, false);
    }

    /**
     * Returns a new instance of the up button
     *
     * @return
     */
    public static DocumentFileItem getUpInstance() {
        return new DocumentFileItem(null, null, true);
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
    public void inspect(String preferredLocale, Door43Client library) {
        if(file != null) {
            try {
                this.archiveDetails = ArchiveDetails.newInstance(App.context(), file, preferredLocale, library);
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
                && file.getParentFile().getName().equalsIgnoreCase(App.PUBLIC_DATA_DIR);
    }

    /**
     * Checks if this file is a translation archive
     * @return
     */
    public boolean isTranslationArchive() {
        return !isUpButton
                && file != null
                && !file.isDirectory()
                && FileUtilities.getExtension(file.getName()).equalsIgnoreCase(Translator.ARCHIVE_EXTENSION);
    }

    /**
     * Checks if this file is a translation archive
     * @return
     */
    public boolean isFileMatchesExtension(String extension) {
        return !isUpButton
                && file != null
                && !file.isDirectory()
                && FileUtilities.getExtension(file.getName()).equalsIgnoreCase(extension);
    }

    /**
     * Checks if this file is a zip file
     * @return
     */
    public boolean isThisZipFile() {
        return !isUpButton
                && file != null
                && !file.isDirectory()
                && FileUtilities.getExtension(file.getName()).equalsIgnoreCase(ZIP);
    }
}
