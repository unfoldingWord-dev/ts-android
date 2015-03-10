package com.door43.translationstudio.projects.imports;

import com.door43.util.ListMap;

/**
 * This provides the base class for an import request.
 */
public abstract class ImportRequest implements ImportRequestInterface {
    private String mError;
    private String mWarning;
    private boolean mApproved = true;
    private ImportRequestInterface mParent;
    private ListMap<ImportRequestInterface> mChildRequests = new ListMap<ImportRequestInterface>();

    /**
     * Sets the parent of the import request
     * @param parent the parent request
     */
    public void setParentRequest(ImportRequestInterface parent) {
        mParent = parent;
    }

    /**
     * Adds a child import request
     * @param request
     */
    protected void addChildImportRequest(ImportRequestInterface request) {
        request.setParentRequest(this);
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
     * Marks this request as approved.
     * This will recursively set the children as approved
     * @param approved
     */
    public void setIsApproved(boolean approved) {
        // Import requests with errors can never be approved
        setIsApproved(approved, true);
    }

    /**
     * Marks this request as approved.
     * Parents will always be updated when all the children are (dis)approved
     * @param approved
     * @param approveRecursively if set to false the children will not be updated recursively
     */
    public void setIsApproved(boolean approved, boolean approveRecursively) {
        // imports with errors cannot be approved
        if(mError == null) {
            if(mApproved != approved) {
                mApproved = approved;
                // set children
                if (approveRecursively) {
                    for (ImportRequestInterface i : mChildRequests.getAll()) {
                        i.setIsApproved(approved, approveRecursively);
                    }
                }
                // update parent
                if (mParent != null && mParent.isApproved() != mApproved) {
                    boolean allChildrenApproved = true;
                    for (ImportRequestInterface r : mParent.getChildImportRequests().getAll()) {
                        if (!r.isApproved()) {
                            allChildrenApproved = false;
                        }
                    }
                    // only update the parent if it matches the action performed on this request
                    if(allChildrenApproved == approved) {
                        mParent.setIsApproved(allChildrenApproved, false);
                    }
                }
            }
        }
    }
}
