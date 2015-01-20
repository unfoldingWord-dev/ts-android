package com.door43.translationstudio.projects.imports;

import com.door43.translationstudio.util.ListMap;

/**
 * This provides the base class for an import request.
 */
public abstract class ImportRequest implements ImportRequestInterface {
    private String mError;
    private String mWarning;
    private boolean mApproved = true;
    private ListMap<ImportRequestInterface> mChildRequests = new ListMap<ImportRequestInterface>();

    /**
     * Adds a child import request
     * @param request
     */
    protected void addChildImportRequest(ImportRequestInterface request) {
        mChildRequests.add(request.getId(), request);
    }

    /**
     * Returns a listmap of all the child import request
     * @return
     */
    public ListMap<ImportRequestInterface> getChildImportRequests() {
      return mChildRequests;
    }

    /**
     * Sets the warning message on this request.
     * Requests with warnings may be manually selected for import, but will not be imported automatically
     * @param s The warning message
     */
    public void setWarning(String s) {
        mWarning = s;
        mApproved = false;
    }

    /**
     * Sets the error message on this request.
     * Requests with errors will not be imported
     * @param s the error message
     */
    public void setError(String s) {
        mError = s;
        mApproved = false;
    }

    /**
     * Returns the warning message on this request
     * @return
     */
    public String getWarning() {
        return mWarning;
    }

    /**
     * Returns the error message on this request
     * @return
     */
    public String getError() {
        return mError;
    }

    /**
     * Checks if this request is approved
     * @return
     */
    public boolean isApproved() {
        return mApproved;
    }

    /**
     * Marks this request as approved
     * @param approved
     */
    public void setIsApproved(boolean approved) {
        // Import requests with errors can never be approved
        if(mError == null) {
            mApproved = approved;
            for(ImportRequestInterface i:mChildRequests.getAll()) {
                i.setIsApproved(true);
            }
        }
    }
}
