package com.door43.translationstudio.projects.imports;

import com.door43.translationstudio.util.ListMap;

/**
 * Created by joel on 1/19/2015.
 */
public interface ImportRequestInterface {

    /**
     * Returns a listmap of all the child import request
     * @return
     */
    public ListMap<ImportRequestInterface> getChildImportRequests();

    /**
     * Sets the warning message on this request.
     * Requests with warnings may be manually selected for import, but will not be imported automatically
     * @param s The warning message
     */
    public void setWarning(String s);

    /**
     * Sets the error message on this request.
     * Requests with errors will not be imported
     * @param s the error message
     */
    public void setError(String s);

    /**
     * Returns the warning message on this request
     * @return
     */
    public String getWarning();

    /**
     * Returns the error message on this request
     * @return
     */
    public String getError();

    /**
     * Checks if this request is approved
     * @return
     */
    public boolean isApproved();

    /**
     * Marks this request as approved
     * @param approved
     */
    public void setIsApproved(boolean approved);

    /**
     * Returns the id of this import request
     * @return
     */
    public abstract String getId();

    /**
     * Returns the title of the import request as displayed in review dialogs
     * @return
     */
    public abstract String getTitle();
}
