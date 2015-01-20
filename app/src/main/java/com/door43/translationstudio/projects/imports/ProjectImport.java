package com.door43.translationstudio.projects.imports;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.MainContext;

import java.io.File;

/**
 * Created by joel on 1/19/2015.
 */
public class ProjectImport extends ImportRequest {
    public final String projectId;
    public final File importDirectory;


    /**
     * Creates a new project import request
     * @param projectId The id of the project that is being imported
     * @param extractedDirectory the directory containing translations that will be imported. This may technically contain translations from other projects as well. This is just the archive extraction dir.
     */
    public ProjectImport(String projectId, File extractedDirectory) {
        this.projectId = projectId;
        this.importDirectory = extractedDirectory;
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
