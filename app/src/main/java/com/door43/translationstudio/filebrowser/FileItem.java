package com.door43.translationstudio.filebrowser;

import com.door43.translationstudio.R;

/**
 * This class represents a single mFile item
 */
public class FileItem {
    private String mFile;
    private int mIcon;
    private boolean mIsUp = false;

    /**
     * Creates a new mFile item
     * @param file
     * @param icon
     */
    public FileItem(String file, Integer icon) {
        mFile = file;
        mIcon = icon;
    }

    /**
     * Creates a new up button
     */
    public FileItem(String title) {
        mFile = title;
        mIcon = R.drawable.directory_up;
        mIsUp = true;
    }

    /**
     * Returns the mIcon
     * @return
     */
    public int getIcon() {
        return mIcon;
    }

    /**
     * Checks if this this is the up button
     * @return
     */
    public boolean isUp() {
        return mIsUp;
    }

    @Override
    public String toString() {
        return mFile;
    }

    public String getFile() {
        return mFile;
    }
}
