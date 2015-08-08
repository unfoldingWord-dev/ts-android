package com.door43.translationstudio.events;

import com.door43.translationstudio.projects.imports.ProjectImport;

/**
 * Created by joel on 1/12/2015.
 * @deprecated
 */
public class ProjectImportApprovalEvent {
    private final ProjectImport[] mRequests;

    public ProjectImportApprovalEvent(ProjectImport[] requests) {
        mRequests = requests;
    }

    public ProjectImport[] getImportRequests() {
        return mRequests;
    }
}
