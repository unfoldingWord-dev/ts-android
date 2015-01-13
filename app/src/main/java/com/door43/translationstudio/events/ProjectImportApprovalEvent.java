package com.door43.translationstudio.events;

import com.door43.translationstudio.projects.Project;

import java.util.List;

/**
 * Created by joel on 1/12/2015.
 */
public class ProjectImportApprovalEvent {
    private final List<Project.ImportStatus> mStatuses;

    public ProjectImportApprovalEvent(List<Project.ImportStatus> statuses) {
        mStatuses = statuses;
    }

    public List<Project.ImportStatus> getStatuses() {
        return mStatuses;
    }
}
