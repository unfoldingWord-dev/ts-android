package com.door43.translationstudio.projects.imports;

import com.door43.util.ListMap;

/**
 * Created by joel on 1/19/2015.
 */
@Deprecated
public interface ImportRequestInterface {

    /**
     * Returns a listmap of all the child import request
     * @return
     */
    public ListMap<ImportRequestInterface> getChildImportRequests();

    /**
     * Sets the parent request of this request.
     * This allows us to do some automatic processing for things such as approvals.
     * @param parent
     */
    public void setParentRequest(ImportRequestInterface parent);

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
     * Marks this request as approved
     * @param approved
     * @param approveRecursively if set to false the children will not be updated recursively
     */
    public void setIsApproved(boolean approved, boolean approveRecursively);

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
