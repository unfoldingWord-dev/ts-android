package com.door43.translationstudio.projects.imports;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;

import java.io.File;

/**
 * Created by joel on 1/19/2015.
 */
@Deprecated
public class ProjectImport extends ImportRequest {
    public final String projectId;
    public final File importDirectory;
    private boolean mMissingSource = false;


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
        // TODO: this will throw a null pointer exception if we don't have the source
        Project p = AppContext.projectManager().getProject(projectId);
        if(p != null) {
            return p.getTitle();
        } else {
            return p.getId();
        }
    }

    /**
     * Sets whether or not the project source is missing
     * @param missingSource
     */
    public void setMissingSource(boolean missingSource) {
        mMissingSource = missingSource;
    }

    /**
     * Checks if the source is missing
     * @return
     */
    public boolean isSourceMissing() {
        return mMissingSource;
    }
}
