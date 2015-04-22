package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.util.threads.ManagedTask;

import org.apache.commons.io.FileUtils;
import java.io.File;

/**
 * This task indexes a project so we don't have to load the entire thing into memory.
 */
public class IndexProjectsTask extends ManagedTask {

    private final Project[] mProjects;

    public IndexProjectsTask(Project[] projects) {
        mProjects = projects;
    }

    @Override
    public void start() {
        File indexDir = new File(AppContext.context().getCacheDir(), "index");
        for(Project p:mProjects) {
            File projectDir = new File(indexDir, p.getId());
            projectDir.mkdirs();

            // index project
            File pInfo = new File(projectDir, "data.json");
            try {
                FileUtils.write(pInfo, p.serialize().toString());
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to index project info. Project: " + p.getId(), e);
                continue;
            }

            for(SourceLanguage l:p.getSourceLanguages()) {
                File languageDir = new File(projectDir, l.getId());
                languageDir.mkdirs();

                // index language
                File lInfo = new File(languageDir, "data.json");
                try {
                    FileUtils.write(lInfo, p.serializeSourceLanguage(l).toString());
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to index language info. Project: " + p.getId() + " Language: " + l.getId(), e);
                    continue;
                }

                for(Resource r:l.getResources()) {
                    File resourceDir = new File(languageDir, r.getId());
                    resourceDir.mkdirs();

                    // index resource
                    File rInfo = new File(resourceDir, "data.json");
                    try {
                        FileUtils.write(rInfo, r.serialize().toString());
                    } catch (Exception e) {
                        Logger.e(this.getClass().getName(), "Failed to index resource info. Project: " + p.getId() + " Language: " + l.getId() + " Resource: " + r.getId(), e);
                    }

                    // TODO: index the terms, notes, and source.
                }

            }
        }
    }
}
