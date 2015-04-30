package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.util.threads.ManagedTask;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This task indexes a project so we don't have to load the entire thing into memory.
 */
public class IndexProjectsTask extends ManagedTask {

    public static final String TASK_ID = "index_projects";
    private final Project[] mProjects;
    private int mMaxProgress = 100;

    public IndexProjectsTask(Project[] projects) {
        mProjects = projects;
    }

    @Override
    public void start() {
        int pIndex = -1;
        for(Project proj:mProjects) {
            pIndex ++;
            if(interrupted()) return;
            File projectDir = IndexStore.getProjectDir(proj);
            File projectReadyFile = new File(projectDir, IndexStore.READY_FILE);
            if(projectReadyFile.exists()) {
                continue;
            }
            projectDir.mkdirs();

            // index project
            File pInfo = new File(projectDir, "data.json");
            if(!pInfo.exists()) {
                try {
                    FileUtils.write(pInfo, proj.serialize().toString());
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to index project info. Project: " + proj.getId(), e);
                    continue;
                }
            }

            int lIndex = -1;
            List<SourceLanguage> languages = proj.getSourceLanguages();
            languages.addAll(proj.getSourceLanguageDrafts());
            for(SourceLanguage l:languages) {
                lIndex ++;
                if(interrupted()) return;
                File languageDir = new File(projectDir, l.getId());
                languageDir.mkdirs();

                // TRICKY: we create a new project so we don't override anything the user is working on.
                Project p = proj.softClone();

                // index language
                File lInfo = new File(languageDir, "data.json");
                if(!lInfo.exists()) {
                    try {
                        FileUtils.write(lInfo, p.serializeSourceLanguage(l).toString());
                    } catch (Exception e) {
                        Logger.e(this.getClass().getName(), "Failed to index language info. Project: " + p.getId() + " Language: " + l.getId(), e);
                        continue;
                    }
                }

                int rIndex = -1;
                for(Resource r:l.getResources()) {
                    rIndex ++;
                    if(interrupted()) return;
                    File resourceDir = new File(languageDir, r.getId());
                    resourceDir.mkdirs();

                    publishProgress((pIndex + (lIndex + (rIndex + 1) / (double) l.getResources().length) / (double) languages.size()) / (double) mProjects.length, AppContext.context().getResources().getString(R.string.title_projects));

                    // index resource
                    File rInfo = new File(resourceDir, "data.json");
                    if(!rInfo.exists()) {
                        try {
                            FileUtils.write(rInfo, r.serialize().toString());
                        } catch (Exception e) {
                            Logger.e(this.getClass().getName(), "Failed to index resource info. Project: " + p.getId() + " Language: " + l.getId() + " Resource: " + r.getId(), e);
                            continue;
                        }
                    }
                }
            }

            // mark the index as complete
            if(!interrupted()) {
                try {
                    FileUtils.write(projectReadyFile, proj.getDateModified() + "");
                } catch (IOException e) {
                    Logger.e(this.getClass().getName(), "Failed to create the ready.index file", e);
                }
            }
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
