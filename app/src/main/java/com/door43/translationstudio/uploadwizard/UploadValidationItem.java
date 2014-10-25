package com.door43.translationstudio.uploadwizard;

/**
 * Created by joel on 10/24/2014.
 */
public class UploadValidationItem {
    private String mTitle;
    private Status mStatus;
    private String mDescription;

    public UploadValidationItem(String title, Status status) {
        mTitle = title;
        mStatus = status;
        mDescription = "";
    }

    public UploadValidationItem(String titleResource, String description, Status status) {
        mTitle = titleResource;
        mStatus = status;
        mDescription = description;
    }

    public String getTitle() {
        return mTitle;
    }

    public Status getStatus() {
        return mStatus;
    }

    public String getDescription() {
        return mDescription;
    }

    public static enum Status {
        SUCCESS,
        ERROR,
        WARNING
    }
}
