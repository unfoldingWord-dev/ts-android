package com.door43.translationstudio.events;

import com.door43.translationstudio.projects.Project;

import java.util.List;

/**
 * Created by joel on 1/12/2015.
 */
public class ProjectImportApprovalEvent {
    private final List<Project.ImportRequest> mRequests;

    public ProjectImportApprovalEvent(List<Project.ImportRequest> requests) {
        mRequests = requests;
    }

    public List<Project.ImportRequest> getImportRequests() {
        return mRequests;
    }
}
