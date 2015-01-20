package com.door43.translationstudio.projects.imports;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;

import java.io.File;

/**
 * Created by joel on 1/19/2015.
 */
public class ProjectImport extends ImportRequest {
    public final String projectId;

    public ProjectImport(String projectId) {
        this.projectId = projectId;
    }

    /**
     * Adds a translation import request to this project
     * @param request
     */
    public void addTranslationImport(TranslationImport request) {
        super.addChildImportRequest(request);
    }

    @Override
    public String getId() {
        return projectId;
    }

    @Override
    public String getTitle() {
        Project p = MainContext.getContext().getSharedProjectManager().getProject(projectId);
        if(p != null) {
            return p.getTitle();
        } else {
            return p.getId();
        }
    }
}
