package com.door43.translationstudio.events;

import com.door43.translationstudio.projects.imports.ImportRequestInterface;
import com.door43.translationstudio.projects.imports.ProjectImport;

import java.util.List;

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
