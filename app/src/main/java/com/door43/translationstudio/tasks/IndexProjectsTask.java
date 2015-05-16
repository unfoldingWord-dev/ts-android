package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;

import java.util.List;

/**
 * This task indexes a project so we don't have to load the entire thing into memory.
 * TODO: most of this should be placed inside the IndexStore
 */
public class IndexProjectsTask extends ManagedTask {

    public static final String TASK_ID = "index_projects";
    private final Project[] mProjects;
    private int mMaxProgress = 100;

    public IndexProjectsTask(Project[] projects) {
        mProjects = projects;
    }

    public IndexProjectsTask(Project project) {
        mProjects = new Project[1];
        mProjects[0] = project;
    }

    @Override
    public void start() {
        int pIndex = -1;
        for(Project p:mProjects) {
            pIndex ++;
            if(interrupted()) return;
            if(IndexStore.hasIndex(p)) continue;
            IndexStore.index(p);

//            File projectDir = IndexStore.getProjectDir(proj);
//            File projectReadyFile = new File(projectDir, IndexStore.READY_FILE);
//            if(projectReadyFile.exists()) {
//                continue;
//            }
//            projectDir.mkdirs();

            // index project
//            File pInfo = new File(projectDir, "data.json");
//            if(!pInfo.exists()) {
//                try {
//                    FileUtils.write(pInfo, proj.serialize().toString());
//                } catch (Exception e) {
//                    Logger.e(this.getClass().getName(), "Failed to index project info. Project: " + proj.getId(), e);
//                    continue;
//                }
//            }

            int lIndex = -1;
            List<SourceLanguage> languages = p.getSourceLanguages();
            languages.addAll(p.getSourceLanguageDrafts());
            for(SourceLanguage l:languages) {
                lIndex ++;
                if(interrupted()) return;
                IndexStore.index(p, l);
//                File languageDir = new File(projectDir, l.getId());
//                languageDir.mkdirs();

                // TRICKY: we create a new project so we don't override anything the user is working on.
//                Project p = proj.softClone();

                // index language
//                File lInfo = new File(languageDir, "data.json");
//                if(!lInfo.exists()) {
//                    try {
//                        FileUtils.write(lInfo, p.serializeSourceLanguage(l).toString());
//                    } catch (Exception e) {
//                        Logger.e(this.getClass().getName(), "Failed to index language info. Project: " + p.getId() + " Language: " + l.getId(), e);
//                        continue;
//                    }
//                }

                int rIndex = -1;
                for(Resource r:l.getResources()) {
                    rIndex ++;
                    if(interrupted()) return;

//                    File resourceDir = new File(languageDir, r.getId());
//                    resourceDir.mkdirs();

                    publishProgress((pIndex + (lIndex + (rIndex + 1) / (double) l.getResources().length) / (double) languages.size()) / (double) mProjects.length, AppContext.context().getResources().getString(R.string.title_projects));

                    IndexStore.index(p, l, r);

                    // index resource
//                    File rInfo = new File(resourceDir, "data.json");
//                    if(!rInfo.exists()) {
//                        try {
//                            FileUtils.write(rInfo, r.serialize().toString());
//                        } catch (Exception e) {
//                            Logger.e(this.getClass().getName(), "Failed to index resource info. Project: " + p.getId() + " Language: " + l.getId() + " Resource: " + r.getId(), e);
//                            continue;
//                        }
//                    }
                }
            }

            // mark the index as complete
            if(!interrupted()) {
                IndexStore.finalizeIndex(p);
            }
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
