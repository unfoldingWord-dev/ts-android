package com.door43.translationstudio.projects;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.projects.imports.ChapterImport;
import com.door43.translationstudio.projects.imports.FileImport;
import com.door43.translationstudio.projects.imports.FrameImport;
import com.door43.translationstudio.projects.imports.ImportRequestInterface;
import com.door43.translationstudio.projects.imports.ProjectImport;
import com.door43.translationstudio.projects.imports.TranslationImport;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.Security;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

/**
 * This class handles all the features for importing and exporting projects.
 * TODO: we need to pull all of the existing import and export code into this class.
 */
public class Sharing {

    /**
     * Exports a json array of projects and the translations available.
     * This information is used to present a browsable library to the user from which
     * they may select different translations on which to perform actions
     * @param projects an array of projects to be inclued in the library
     * @return
     */
    public static String generateLibrary(Project[] projects) {
        return generateLibrary(projects, null);
    }

    /**
     * Exports a json array of projects and the translations available.
     * This information is used to present a browsable library to the user from which
     * they may select different translations on which to perform actions
     * @param projects an array of projects to be inclued in the library
     * @param preferredLibraryLanguages the preferred language(S) in which the library will be generated. The first available language will be used by order of index on per project basis.
     * @return
     */
    public static String generateLibrary(Project[] projects, List<Language> preferredLibraryLanguages) {
        JSONArray libraryJson = new JSONArray();

        for(Project p:projects) {
            if(p.isTranslatingGlobal() || p.isTranslating()) {
                JSONObject json = new JSONObject();
                try {
                    json.put("id", p.getId());

                    // for better readability we attempt to give the project details in one of the preferred languages
                    SourceLanguage libraryLanguage = p.getSelectedSourceLanguage();
                    if(preferredLibraryLanguages != null && preferredLibraryLanguages.size() > 0) {
                        for(Language pref:preferredLibraryLanguages) {
                            SourceLanguage l = p.getSourceLanguage(pref.getId());
                            if(l != null) {
                                libraryLanguage = l;
                                break;
                            }
                        }
                    }

                    // project info
                    JSONObject projectInfoJson = new JSONObject();
                    projectInfoJson.put("name", p.getTitle(libraryLanguage));
                    projectInfoJson.put("description", p.getDescription(libraryLanguage));
                    // NOTE: since we are only providing the project details in a single source language we don't need to include the meta id's
                    PseudoProject[] pseudoProjects = p.getPseudoProjects();
                    JSONArray sudoProjectsJson = new JSONArray();
                    for(PseudoProject sp: pseudoProjects) {
                        sudoProjectsJson.put(sp.getTitle(libraryLanguage));
                    }
                    projectInfoJson.put("meta", sudoProjectsJson);
                    json.put("project", projectInfoJson);

                    // library language
                    JSONObject libraryLanguageJson = new JSONObject();
                    libraryLanguageJson.put("slug", libraryLanguage.getId());
                    libraryLanguageJson.put("name", libraryLanguage.getName());
                    if(libraryLanguage.getDirection() == Language.Direction.RightToLeft) {
                        libraryLanguageJson.put("direction", "rtl");
                    } else {
                        libraryLanguageJson.put("direction", "ltr");
                    }
                    json.put("language", libraryLanguageJson);

                    // target languages for which translations are available
                    Language[] targetLanguages = p.getActiveTargetLanguages();
                    JSONArray languagesJson = new JSONArray();
                    for(Language l:targetLanguages) {
                        JSONObject langJson = new JSONObject();
                        langJson.put("slug", l.getId());
                        langJson.put("name", l.getName());
                        if(l.getDirection() == Language.Direction.RightToLeft) {
                            langJson.put("direction", "rtl");
                        } else {
                            langJson.put("direction", "ltr");
                        }
                        languagesJson.put(langJson);
                    }
                    json.put("target_languages", languagesJson);
                    libraryJson.put(json);
                } catch (JSONException e) {
                    Logger.e(Sharing.class.getName(), "Failed to generate a library record for the project "+p.getId(), e);
                }
            }
        }
        return libraryJson.toString();
    }

    /**
     * Prepares a translationStudio project archive for import.
     * This leaves files around so be sure to run the importcleanup when finished.
     * @param archive the archive that will be imported
     * @return true if the import was successful
     */
    public static ProjectImport[] prepareArchiveImport(File archive) {
        Map<String, ProjectImport> projectImports = new HashMap<>();

        if(archive != null && archive.exists()) {
            // validate extension
            String[] name = archive.getName().split("\\.");
            if (name[name.length - 1].equals(Project.PROJECT_EXTENSION)) {
                long timestamp = System.currentTimeMillis();
                File extractedDir = new File(AppContext.context().getCacheDir() + "/" + AppContext.context().getResources().getString(R.string.imported_projects_dir) + "/" + timestamp);

                // extract the archive
                if (!extractedDir.exists()) {
                    try {
                        Zip.unzip(archive, extractedDir);
                    } catch (IOException e) {
                        FileUtilities.deleteRecursive(extractedDir);
                        Logger.e(Sharing.class.getName(), "failed to extract the project archive", e);
                        return projectImports.values().toArray(new ProjectImport[projectImports.size()]);
                    }
                }

                File manifest = new File(extractedDir, "manifest.json");
                if (manifest.exists() && manifest.isFile()) {
                    try {
                        JSONObject manifestJson = new JSONObject(FileUtils.readFileToString(manifest));

                        // load the source files
                        if (manifestJson.has("source")) {
                            File sourceDir = new File(extractedDir, manifestJson.getString("source"));
                            if (sourceDir.exists()) {
                                TranslationManager.importSource(sourceDir);
                            }
                        }

                        // load the list of projects to import
                        if (manifestJson.has("projects")) {
                            JSONArray projectsJson = manifestJson.getJSONArray("projects");
                            for (int i = 0; i < projectsJson.length(); i++) {
                                JSONObject projJson = projectsJson.getJSONObject(i);
                                if (projJson.has("path") && projJson.has("project") && projJson.has("target_language")) {
                                    // create new or load existing project import
                                    ProjectImport pi = new ProjectImport(projJson.getString("project"), extractedDir);
                                    if (projectImports.containsKey(pi.projectId)) {
                                        pi = projectImports.get(pi.projectId);
                                    } else {
                                        projectImports.put(pi.projectId, pi);
                                    }
                                    // prepare the translation import
                                    boolean hadTranslationWarnings = prepareImport(pi, projJson.getString("target_language"), new File(extractedDir, projJson.getString("path")));
                                    if (hadTranslationWarnings) {
                                        pi.setWarning("Some translations already exist");
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Logger.e(Sharing.class.getName(), "failed to parse the manifest", e);
                    } catch (IOException e) {
                        Logger.e(Sharing.class.getName(), "failed to read the manifest file", e);
                    }
                }
            }
        }
        return projectImports.values().toArray(new ProjectImport[projectImports.size()]);
    }

    /**
     * Imports a Translation Studio Project from a directory
     * This is a legacy import method for archives exported by 2.0.2 versions of the app.
     * @param archiveFile the directory that will be imported
     * @return
     */
    public static boolean prepareLegacyArchiveImport(File archiveFile) {
        String[] name = archiveFile.getName().split("\\.");
        if(name[name.length - 1].equals("zip")) {
            // extract archive
            long timestamp = System.currentTimeMillis();
            File extractedDirectory = new File(AppContext.context().getCacheDir() + "/" + AppContext.context().getResources().getString(R.string.imported_projects_dir) + "/" + timestamp);
            File importDirectory;
            Boolean success = false;
            try {
                // extract into a timestamped directory so we don't accidently throw files all over the place
                Zip.unzip(archiveFile, extractedDirectory);
                File[] files = extractedDirectory.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return Sharing.validateProjectArchiveName(s);
                    }
                });
                if(files.length == 1) {
                    importDirectory = files[0];
                } else {
                    // malformed archive
                    FileUtilities.deleteRecursive(extractedDirectory);
                    return false;
                }
            } catch (IOException e) {
                FileUtilities.deleteRecursive(extractedDirectory);
                Logger.e(Sharing.class.getName(), "failed to extract the legacy project archive", e);
                return false;
            }

            // read project info
            TranslationArchiveInfo translationInfo = getTranslationArchiveInfo(importDirectory.getName());
            if(translationInfo != null) {
                ProjectImport pi = new ProjectImport(translationInfo.projectId, extractedDirectory);
                prepareImport(pi, translationInfo.languageId, importDirectory);
                // TODO: for now we are just blindly importing legacy projects (dangerous). We'll need to update this method as well as the DokuWiki import method in order to properly handle these legacy projects
                success = importProject(pi);
                cleanImport(pi);
            }
            FileUtilities.deleteRecursive(extractedDirectory);
            return success;
        } else {
            return false;
        }
    }

    /**
     * Checks if the project archive is named properly
     * @deprecated this is legacy code for old import methods
     * @param name
     * @return
     */
    public static boolean validateProjectArchiveName(String name) {
        String[] fields = name.toLowerCase().split("-");
        return fields.length == 3 && fields[0].equals(Project.GLOBAL_PROJECT_SLUG);
    }

    /**
     * Returns information about the translation archive
     * @deprecated this is legacy code for old import methods
     * @param archiveName
     * @return
     */
    public static TranslationArchiveInfo getTranslationArchiveInfo(String archiveName) {
        String[] parts = archiveName.split("_");
        String name = parts[0];
        // TRICKY: older version of the app mistakenly included the leading directory separator
        while(name.startsWith("/")) {
            name = name.substring(name.indexOf("/"));
        }
        if(validateProjectArchiveName(name)) {
            String[] fields = name.toLowerCase().split("-");
            return new TranslationArchiveInfo(fields[0], fields[1], fields[2]);
        }
        return null;
    }

    /**
     * Stores information about a translation archive
     * @deprecated this is legacy code for the old import methods
     */
    public static class TranslationArchiveInfo {
        public final String globalProjectId;
        public final String projectId;
        public final String languageId;

        public TranslationArchiveInfo(String globalProjectId, String projectId, String languageId) {
            this.globalProjectId = globalProjectId;
            this.projectId = projectId;
            this.languageId = languageId;
        }

        public Project getProject() {
            return AppContext.projectManager().getProject(projectId);
        }

        public Language getLanguage() {
            return AppContext.projectManager().getLanguage(languageId);
        }
    }

    /**
     * Performs some checks on a project to make sure it can be imported.
     * @param projectImport the import request for the project
     * @param languageId the the language id for the translatoin
     * @param projectDir the directory of the project translation that will be imported
     */
    private static boolean  prepareImport(final ProjectImport projectImport, final String languageId, File projectDir) {
        final TranslationImport translationImport = new TranslationImport(languageId, projectDir);
        projectImport.addTranslationImport(translationImport);
        boolean hadTranslationWarnings = false;

        // locate existing project
        final Project p = AppContext.projectManager().getProject(projectImport.projectId);
        if(p == null) {
            projectImport.setMissingSource(true);
            Logger.i(Sharing.class.getName(), "Missing project source for import");
        }

        // look through items to import
        if(Project.isTranslating(projectImport.projectId, languageId)) {
            hadTranslationWarnings = true;
            // the project already exists

            // TODO: we should look at the md5 contents of the files to determine any differences. if files are identical the import should mark them as approved
            boolean hadChapterWarnings = false;

            // read chapters to import
            String[] chapterIds = projectDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return !s.equals(".git") && new File(file, s).isDirectory();
                }
            });
            if(chapterIds != null) {
                for (String chapterId : chapterIds) {
                    ChapterImport chapterImport = new ChapterImport(chapterId, String.format(AppContext.context().getResources().getString(R.string.label_chapter_title_detailed), chapterId));
                    if (Chapter.isTranslating(projectImport.projectId, languageId, chapterId)) {
                        chapterImport.setWarning("Importing will override our existing translation");
                        hadChapterWarnings = true;
                    }
                    translationImport.addChapterImport(chapterImport);
                    boolean hadFrameWarnings = false;

                    // read chapter title and reference
                    File titleFile = new File(projectDir, chapterId + "/title.txt");
                    if (titleFile.exists()) {
                        FileImport fileImport = new FileImport("title", AppContext.context().getResources().getString(R.string.chapter_title_field));
                        chapterImport.addFileImport(fileImport);
                        // check if chapter title translation exists
                        File currentTitleFile = new File(Chapter.getTitlePath(projectImport.projectId, languageId, chapterId));
                        if (currentTitleFile.exists()) {
                            fileImport.setWarning("Importing will override our existing translation");
                        }
                    }
                    File referenceFile = new File(projectDir, chapterId + "/reference.txt");
                    if (referenceFile.exists()) {
                        FileImport fileImport = new FileImport("reference", AppContext.context().getResources().getString(R.string.chapter_reference_field));
                        chapterImport.addFileImport(fileImport);
                        // check if chapter reference translation exists
                        File currentReferenceFile = new File(Chapter.getReferencePath(projectImport.projectId, languageId, chapterId));
                        if (currentReferenceFile.exists()) {
                            fileImport.setWarning("Importing will override our existing translation");
                        }
                    }

                    // read frames to import
                    String[] frameFileNames = new File(projectDir, chapterId).list(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            return !s.equals("title.txt") && !s.equals("reference.txt");
                        }
                    });
                    if(frameFileNames != null) {
                        for (String frameFileName : frameFileNames) {
                            String[] pieces = frameFileName.split("\\.");
                            if (pieces.length != 2) {
                                Logger.w(Sharing.class.getName(), "Unexpected file in frame import " + frameFileName);
                                continue;
                            }
                            String frameId = pieces[0];
                            FrameImport frameImport = new FrameImport(frameId, String.format(AppContext.context().getResources().getString(R.string.label_frame_title_detailed), frameId));
                            chapterImport.addFrameImport(frameImport);

                            // check if frame translation exists
                            File currentFrameFile = new File(Frame.getFramePath(projectImport.projectId, languageId, chapterId, frameId));
                            if (currentFrameFile.exists()) {
                                frameImport.setWarning("Importing will override our existing translation");
                                hadFrameWarnings = true;
                            }
                        }
                    }

                    if (hadFrameWarnings) {
                        chapterImport.setWarning("Importing will override our existing translation");
                    }
                }
            }

            if(hadChapterWarnings) {
                translationImport.setWarning("Importing will override our existing translation");
            }
        }
        return hadTranslationWarnings;
    }

    /**
     * Performs the actual import of the project
     * @param request the project import request
     * @return true if the import did not encounter any errors
     */
    public static boolean importProject(ProjectImport request) {
        boolean hadErrors = false;
        if(request.getError() == null) {
            ArrayList<ImportRequestInterface> translationRequests = request.getChildImportRequests().getAll();
            // translations
            for (TranslationImport ti : translationRequests.toArray(new TranslationImport[translationRequests.size()])) {
                if(ti.getError() == null) {
                    File repoDir = new File(Project.getRepositoryPath(request.projectId, ti.languageId));
                    ArrayList<ImportRequestInterface> chapterRequests = ti.getChildImportRequests().getAll();
                    if(repoDir.exists() && chapterRequests.size() > 0) {
                        // chapters
                        for (ChapterImport ci : chapterRequests.toArray(new ChapterImport[chapterRequests.size()])) {
                            if (ci.getError() == null) {
                                ArrayList<ImportRequestInterface> frameRequests = ci.getChildImportRequests().getAll();
                                for (ImportRequestInterface r : frameRequests) {
                                    if(r.getClass().getName().equals(FrameImport.class.getName())) {
                                        // frames
                                        if (r.isApproved()) {
                                            FrameImport fi = (FrameImport)r;
                                            // import frame
                                            File destFile = new File(Frame.getFramePath(request.projectId, ti.languageId, ci.chapterId, fi.frameId));
                                            File srcFile = new File(ti.translationDirectory, ci.chapterId + "/" + fi.frameId + ".txt");
                                            if (destFile.exists()) {
                                                destFile.delete();
                                            }
                                            destFile.getParentFile().mkdirs();
                                            if (!FileUtilities.moveOrCopy(srcFile, destFile)) {
                                                Logger.e(Sharing.class.getName(), "Failed to import frame");
                                                hadErrors = true;
                                            }
                                        }
                                    } else if(r.getClass().getName().equals(FileImport.class.getName())) {
                                        // title and reference
                                        if(r.isApproved()) {
                                            FileImport fi = (FileImport)r;
                                            if(fi.getId().equals("title")) {
                                                // import title
                                                File destFile = new File(Chapter.getTitlePath(request.projectId, ti.languageId, ci.chapterId));
                                                File srcFile = new File(ti.translationDirectory, ci.chapterId + "/title.txt");
                                                if (destFile.exists()) {
                                                    destFile.delete();
                                                }
                                                destFile.getParentFile().mkdirs();
                                                if (!FileUtilities.moveOrCopy(srcFile, destFile)) {
                                                    Logger.e(Sharing.class.getName(), "Failed to import chapter title");
                                                    hadErrors = true;
                                                }
                                            } else if(fi.getId().equals("reference")) {
                                                // import reference
                                                File destFile = new File(Chapter.getReferencePath(request.projectId, ti.languageId, ci.chapterId));
                                                File srcFile = new File(ti.translationDirectory, ci.chapterId + "/reference.txt");
                                                if (destFile.exists()) {
                                                    destFile.delete();
                                                }
                                                destFile.getParentFile().mkdirs();
                                                if (!FileUtilities.moveOrCopy(srcFile, destFile)) {
                                                    Logger.e(Sharing.class.getName(), "Failed to import chapter reference");
                                                    hadErrors = true;
                                                }
                                            } else {
                                                Logger.w(Sharing.class.getName(), "Unknown file import request. Expecting title or reference but found "+fi.getId());
                                            }
                                        }
                                    } else {
                                        Logger.w(Sharing.class.getName(), "Unknown import request. Expecting FrameImport or FileImport but found "+r.getClass().getName());
                                    }
                                }
                            }
                        }
                    } else {
                        // import the new project
                        if(ti.isApproved()) {
                            FileUtilities.deleteRecursive(repoDir);
                            try {
                                FileUtils.moveDirectory(ti.translationDirectory, repoDir);
                            } catch (IOException e) {
                                Logger.e(Sharing.class.getName(), "failed to import the project directory", e);
                                hadErrors = true;
                                continue;
                            }
                        }
                    }
                    // causes the ui to reload the fresh content from the disk
                    Language l = AppContext.projectManager().getLanguage(ti.languageId);
                    l.touch();
                }
            }

            // commit changes if this was an existing project
            Project p = AppContext.projectManager().getProject(request.projectId);
            if(p != null) {
                p.commit(null);
            }
        }
        return !hadErrors;
    }

    /**
     * This performs house cleaning operations after a project has been imported.
     * You should still run this even if you just prepared the import but didn't actually import
     * because some files get extracted during the process.
     * @param request the import request that will be cleaned
     */
    public static void cleanImport(ProjectImport request) {
        if(request.importDirectory.exists()) {
            FileUtilities.deleteRecursive(request.importDirectory);
        }
    }

    /**
     * This performs house cleaning operations after a project has been imported.
     * You should still run this even if you just prepared the import but didn't actually import
     * because some files get extracted during the process.
     * @param requests the import requests that will be cleaned
     */
    public static void cleanImport(ProjectImport[] requests) {
        for(ProjectImport pi:requests) {
            cleanImport(pi);
        }
    }

    /**
     * Exports the project with the currently selected target language as a translationStudio project
     * This is process heavy and should not be ran on the main thread.
     * @param p the project to export
     * @return the path to the export archive
     * @throws IOException
     */
    public static String export(Project p) throws IOException {
        return export(p, new SourceLanguage[0], new Language[]{p.getSelectedTargetLanguage()});
    }

    /**
     * Exports all of the source for multiple projects
     * @param projects an array of source projects that will be exported
     * @return the path to the export archive
     */
    public static String exportSource(Project[] projects, OnProgressCallback callback) throws IOException {
        File exportDir = new File(AppContext.context().getCacheDir() + "/" + AppContext.context().getResources().getString(R.string.exported_projects_dir));
        ArrayList<File> zipList = new ArrayList<>();
        MainApplication context = AppContext.context();
        File stagingDir = new File(exportDir, System.currentTimeMillis() + "");
        File sourceDir = new File(stagingDir, "sourceTranslations");
        File dataDir = new File(stagingDir, "data");
        File manifestFile = new File(stagingDir, "manifest.json");
        JSONObject manifestJson = new JSONObject();
        JSONArray projectsCatalogJson = new JSONArray();
        DataStore ds = AppContext.projectManager().getDataStore();
        sourceDir.mkdirs();
        dataDir.mkdirs();

        // prepare manifest
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            manifestJson.put("generator", "translationStudio");
            manifestJson.put("version", pInfo.versionCode);
            manifestJson.put("timestamp", System.currentTimeMillis());
        } catch (JSONException e) {
            Logger.e(Sharing.class.getName(), "failed to add to json object", e);
            return "";
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(Sharing.class.getName(), "failed to get the package name", e);
            return "";
        }

        // export project source
        for(int i = 0; i < projects.length; i ++) {
            if(Thread.currentThread().isInterrupted()) break;
            callback.onProgress(i/(double)projects.length, String.format(AppContext.context().getResources().getString(R.string.loading_project), projects[i].getId()));

            try {
                // build project catalog
                JSONArray projectMetaJson = new JSONArray();
                JSONObject projectCatalogItemJson = new JSONObject();
                projectCatalogItemJson.put("date_modified", projects[i].getDateModified() + "");
                projectCatalogItemJson.put("slug", projects[i].getId());
                projectCatalogItemJson.put("sort", projects[i].getSortKey());
                for (PseudoProject sp : projects[i].getPseudoProjects()) {
                    projectMetaJson.put(sp.getId());
                }
                projectCatalogItemJson.put("meta", projectMetaJson);
                if(projects[i].getSourceLanguageCatalog() != null) {
                    projectCatalogItemJson.put("lang_catalog", projects[i].getSourceLanguageCatalog());
                } else {
                    projectCatalogItemJson.put("lang_catalog", ds.sourceLanguageCatalogUri(projects[i].getId()));
                }

                // prepare source
                prepareProjectSourceExport(projects[i], sourceDir, dataDir);

                // write projects catalog
                projectsCatalogJson.put(projectCatalogItemJson);
            } catch (Exception e) {
                continue;
            }
        }

        String projKey = ds.getKey(ds.projectCatalogUri());
        FileUtils.writeStringToFile(new File(dataDir, projKey), projectsCatalogJson.toString());
        FileUtils.writeStringToFile(new File(sourceDir, "projects_catalog.link"), projKey);

        // close manifest
        FileUtils.write(manifestFile, manifestJson.toString());
        zipList.add(manifestFile);
        zipList.add(sourceDir);
        zipList.add(dataDir);

        // zip
        File outputZipFile = new File(exportDir, Project.GLOBAL_PROJECT_SLUG + "_source.zip");
        Zip.zip(zipList.toArray(new File[zipList.size()]), outputZipFile);

        // clean up staging area
        FileUtilities.deleteRecursive(stagingDir);

        return outputZipFile.getAbsolutePath();
    }

    /**
     * Prepares the source of a single project for export
     * The prepared source is placed in the correct locations within the provided source and data directories
     * @param p the project who's source will be prepared for export
     * @return the path to the staging directory for this project's source
     */
    private static void prepareProjectSourceExport(Project p, File sourceDir, File dataDir) throws IOException {
        List<SourceLanguage> languages = p.getSourceLanguages();
        // TRICKY: if we don't include the source language drafts we need to exclude the date_modified from the catalog url in the projects catalog.
        // otherwise when users try to browse updates they won't see the drafts until the next update in the api.
        languages.addAll(p.getSourceLanguageDrafts());
        SourceLanguage[] sourceLanguages = languages.toArray(new SourceLanguage[languages.size()]);

        // compile source languages to include
        if(sourceLanguages.length > 0) {
            try {
                DataStore ds = AppContext.projectManager().getDataStore();
                dataDir.mkdirs();



                File projectSourceDir = new File(sourceDir, p.getId());
                projectSourceDir.mkdirs();
                JSONArray srcLangCatJson = new JSONArray();

                // build language catalogs
                for (SourceLanguage s : sourceLanguages) {
                    SourceLanguage l = p.getSourceLanguage(s.getId());
                    if(l == null) continue;

                    JSONObject srcLangCatItemJson = new JSONObject();
                    JSONArray srcLangCatProjMetaJson = new JSONArray();
                    JSONObject srcLangCatProj = new JSONObject();
                    srcLangCatProj.put("desc", p.getDescription(l));
                    for(PseudoProject sp:p.getPseudoProjects()) {
                        srcLangCatProjMetaJson.put(sp.getTitle(l));
                    }
                    srcLangCatProj.put("meta",srcLangCatProjMetaJson);
                    srcLangCatProj.put("name", p.getTitle(l));

                    JSONObject srcLangCatLang = new JSONObject();
                    srcLangCatLang.put("date_modified", l.getDateModified() + "");
                    srcLangCatLang.put("direction", l.getDirectionName());
                    srcLangCatLang.put("name", l.getName());
                    srcLangCatLang.put("slug", l.getId());

                    srcLangCatItemJson.put("language", srcLangCatLang);
                    srcLangCatItemJson.put("project", srcLangCatProj);
                    Uri resCatalogUri;
                    if(l.getResourceCatalog() != null) {
                        resCatalogUri = l.getResourceCatalog();
                    } else {
                        resCatalogUri = ds.resourceCatalogUri(p.getId(), l.getId());
                    }
                    srcLangCatItemJson.put("res_catalog", resCatalogUri);

                    srcLangCatJson.put(srcLangCatItemJson);

                    // resources catalog
                    File languageSourceDir = new File(projectSourceDir, l.getId());
                    languageSourceDir.mkdirs();
                    String resources = ds.pullResourceCatalog(p.getId(), l.getId(), false, false);
                    String resKey = ds.getKey(resCatalogUri);
                    FileUtils.writeStringToFile(new File(dataDir, resKey), resources);
                    FileUtils.writeStringToFile(new File(languageSourceDir, "resources_catalog.link"), resKey);

                    for(Resource r:l.getResources()) {
                        // Do not include draft content
                        if(r.getCheckingLevel() < AppContext.minCheckingLevel()) continue;

                        File resourceDir = new File(languageSourceDir, r.getId());
                        resourceDir.mkdirs();

                        // terms
                        String terms = ds.pullTerms(p.getId(), l.getId(), r.getId(), false, false);
                        String termKey;
                        if(r.getTermsCatalog() != null) {
                            termKey = ds.getKey(r.getTermsCatalog());
                        } else {
                            termKey = ds.getKey(ds.termsUri(p.getId(), l.getId(), r.getId()));
                        }
                        FileUtils.writeStringToFile(new File(dataDir, termKey), terms);
                        FileUtils.writeStringToFile(new File(resourceDir, "terms.link"), termKey);

                        // source
                        String source = ds.pullSource(p.getId(), l.getId(), r.getId(), false, false);
                        String srcKey;
                        if(r.getSourceCatalog() != null) {
                            srcKey = ds.getKey(r.getSourceCatalog());
                        } else {
                            srcKey = ds.getKey(ds.sourceUri(p.getId(), l.getId(), r.getId()));
                        }
                        FileUtils.writeStringToFile(new File(dataDir, srcKey), source);
                        FileUtils.writeStringToFile(new File(resourceDir, "source.link"), srcKey);

                        // questions
                        String questions = ds.pullCheckingQuestions(p.getId(), l.getId(), r.getId(), false, false);
                        String questionsKey;
                        if(r.getQuestionsCatalog() != null) {
                            questionsKey = ds.getKey(r.getQuestionsCatalog());
                        } else {
                            questionsKey = ds.getKey(ds.questionsUri(p.getId(), l.getId(), r.getId()));
                        }
                        FileUtils.writeStringToFile(new File(dataDir, questionsKey), questions);
                        FileUtils.writeStringToFile(new File(resourceDir, "checking_questions.link"), questionsKey);

                        // notes
                        String notes = ds.pullNotes(p.getId(), l.getId(), r.getId(), false, false);
                        String notesKey;
                        if(r.getNotesCatalog() != null) {
                            notesKey = ds.getKey(r.getNotesCatalog());
                        } else {
                            notesKey = ds.getKey(ds.notesUri(p.getId(), l.getId(), r.getId()));
                        }
                        FileUtils.writeStringToFile(new File(dataDir, notesKey), notes);
                        FileUtils.writeStringToFile(new File(resourceDir, "notes.link"), notesKey);

                        // images
                        // TODO: copy images over as well. we don't want to do this until the zipper supports specifying the location in the archive so we don't have to copy all the images.
                    }
                }
                // write languages catalog
                String langKey;
                if(p.getSourceLanguageCatalog() != null) {
                    langKey = ds.getKey(p.getSourceLanguageCatalog());
                } else {
                    langKey = ds.getKey(ds.sourceLanguageCatalogUri(p.getId()));
                }
                FileUtils.writeStringToFile(new File(dataDir, langKey), srcLangCatJson.toString());
                FileUtils.writeStringToFile(new File(projectSourceDir, "languages_catalog.link"), langKey);
            } catch (JSONException e) {
                Logger.e(Sharing.class.getName(), "Failed to generate source catalogs", e);
            }
        }
    }

    /**
     * Exports the project in multiple languages as a translationStudio project.
     * This is process heavy and should not be ran on the main thread.
     * @param p the project to export
     * @param sourceLanguages an array of source languages that will be exported
     * @param targetLanguages an array of target languages that will be exported
     * @return the path to the export archive
     */
    public static String export(Project p, SourceLanguage[] sourceLanguages, Language[] targetLanguages) throws IOException {
        MainApplication context = AppContext.context();
        File exportDir = new File(AppContext.context().getCacheDir() + "/" + AppContext.context().getResources().getString(R.string.exported_projects_dir));
        File stagingDir = new File(exportDir, System.currentTimeMillis() + "");
        ArrayList<File> zipList = new ArrayList<>();
        File manifestFile = new File(stagingDir, "manifest.json");
        File sourceCatalogDir = new File(stagingDir, "source_catalog");
        JSONObject manifestJson = new JSONObject();
        JSONArray projectsJson = new JSONArray();
        stagingDir.mkdirs();
        Boolean stagingSucceeded = true;
        String signature = "";
        String archivePath = "";

        // prepare manifest
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            manifestJson.put("generator", "translationStudio");
            manifestJson.put("version", pInfo.versionCode);
            manifestJson.put("timestamp", System.currentTimeMillis());
        } catch (JSONException e) {
            Logger.e(Sharing.class.getName(), "failed to add to json object", e);
            return archivePath;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(Sharing.class.getName(), "failed to get the package name", e);
            return archivePath;
        }

        // compile source languages to include
        // TODO: we could probably place this in a different method.
        // TODO: use prepareProjectSourceExport in place of this below.
        if(sourceLanguages.length > 0) {
            try {
                DataStore ds = AppContext.projectManager().getDataStore();

                // build project catalog
                File projectCatalogFile = new File(sourceCatalogDir, "projects_catalog.json");
                JSONArray projectsCatalogJson = new JSONArray();
                JSONArray projectMetaJson = new JSONArray();
                JSONObject projectCatalogItemJson = new JSONObject();
                projectCatalogItemJson.put("date_modified", p.getDateModified());
                projectCatalogItemJson.put("slug", p.getId());
                for(PseudoProject sp:p.getPseudoProjects()) {
                    projectMetaJson.put(sp.getId());
                }
                projectCatalogItemJson.put("meta", projectMetaJson);
                projectCatalogItemJson.put("lang_catalog", ds.sourceLanguageCatalogUri(p.getId()));
                projectsCatalogJson.put(projectCatalogItemJson);
                FileUtils.writeStringToFile(projectCatalogFile, projectsCatalogJson.toString());

                File projectSourceDir = new File(sourceCatalogDir, p.getId());
                projectSourceDir.mkdirs();
                File srcLangCatFile = new File(projectSourceDir, "languages_catalog.json");
                JSONArray srcLangCatJson = new JSONArray();

                // build language catalogs
                for (SourceLanguage s : sourceLanguages) {
                    SourceLanguage l = p.getSourceLanguage(s.getId());
                    if(l == null) continue;

                    JSONObject srcLangCatItemJson = new JSONObject();
                    JSONArray srcLangCatProjMetaJson = new JSONArray();
                    JSONObject srcLangCatProj = new JSONObject();
                    srcLangCatProj.put("desc", p.getDescription(l));
                    for(PseudoProject sp:p.getPseudoProjects()) {
                        srcLangCatProjMetaJson.put(sp.getTitle(l));
                    }
                    srcLangCatProj.put("meta",srcLangCatProjMetaJson);
                    srcLangCatProj.put("name", p.getTitle(l));

                    JSONObject srcLangCatLang = new JSONObject();
                    srcLangCatLang.put("date_modified", l.getDateModified());
                    srcLangCatLang.put("direction", l.getDirectionName());
                    srcLangCatLang.put("name", l.getName());
                    srcLangCatLang.put("slug", l.getId());

                    srcLangCatItemJson.put("language", srcLangCatLang);
                    srcLangCatItemJson.put("project", srcLangCatProj);
                    srcLangCatItemJson.put("res_catalog", ds.resourceCatalogUri(p.getId(), l.getId()));

                    srcLangCatJson.put(srcLangCatItemJson);

                    // resources cat
                    File languageSourceDir = new File(projectSourceDir, l.getId());
                    languageSourceDir.mkdirs();
                    File resCatFile = new File(languageSourceDir, "resources_catalog.json");
                    String resources = ds.pullResourceCatalog(p.getId(), l.getId(), false, false);
                    FileUtils.writeStringToFile(resCatFile, resources);

                    for(Resource r:l.getResources()) {
                        File resourceDir = new File(languageSourceDir, r.getId());
                        resourceDir.mkdirs();

                        // terms
                        File termsFile = new File(resourceDir, "terms.json");
                        String terms = ds.pullTerms(p.getId(), l.getId(), r.getId(), false, false);
                        FileUtils.writeStringToFile(termsFile, terms);

                        // source
                        File sourceFile = new File(resourceDir, "source.json");
                        String source = ds.pullSource(p.getId(), l.getId(), r.getId(), false, false);
                        FileUtils.writeStringToFile(sourceFile, source);

                        // notes
                        File notesFile = new File(resourceDir, "notes.json");
                        String notes = ds.pullNotes(p.getId(), l.getId(), r.getId(), false, false);
                        FileUtils.writeStringToFile(notesFile, notes);

                        // images
                        // TODO: copy images over as well. we don't want to do this until the zipper supports specifying the location in the archive so we don't have to copy all the images.
                    }
                }
                FileUtils.writeStringToFile(srcLangCatFile, srcLangCatJson.toString());

                // project icon
                // TODO: rather than copy the project icon into the assets dir we could just copy it directly
                File projectIcon = context.getAssetAsFile(p.getImagePath());
                File copiedIcon = new File(projectSourceDir, "icon.jpg");
                if(projectIcon != null && projectIcon.exists()) {
                    FileUtils.copyFile(projectIcon, copiedIcon);
                }

                zipList.add(sourceCatalogDir);
            } catch (JSONException e) {
                Logger.e(Sharing.class.getName(), "Failed to generate source catalogs", e);
            }
        }

        // stage all the translations
        for(Language l:targetLanguages) {
            String projectComplexName = Project.GLOBAL_PROJECT_SLUG + "-" + p.getId() + "-" + l.getId();
            String repoPath = p.getRepositoryPath(p.getId(), l.getId());
            Repo repo = new Repo(repoPath);

            // prepare the repo
            try {
                // commit changes if the repo is dirty
                if(!repo.getGit().status().call().isClean()) {
                    // add
                    AddCommand add = repo.getGit().add();
                    add.addFilepattern(".").call();

                    // commit changes
                    CommitCommand commit = repo.getGit().commit();
                    commit.setAll(true);
                    commit.setMessage("auto save before export");
                    commit.call();
                }
            } catch (Exception e) {
                Logger.e(Sharing.class.getName(), "failed to stage the repo before exporting tS archive", e);
                stagingSucceeded = false;
                continue;
            }

            // TRICKY: this has to be read after we commit changes to the repo
            // TODO: we need to provide better error handling when the commit returns null (e.g. try to commit again)
            String gitCommit = p.getLocalTranslationVersion(l);
            if(gitCommit == null) {
                gitCommit = "(null)";
            }
            signature += gitCommit;

            // update manifest
            JSONObject translationJson = new JSONObject();
            try {
                translationJson.put("global_identifier", Project.GLOBAL_PROJECT_SLUG);
                translationJson.put("project", p.getId());
                translationJson.put("title", p.getTitle());
                translationJson.put("target_language", l.getId());
                translationJson.put("source_language", p.getSelectedSourceLanguage().getId());
                translationJson.put("git_commit", gitCommit);
                translationJson.put("path", projectComplexName);
            } catch (JSONException e) {
                Logger.e(Sharing.class.getName(), "failed to add to json object", e);
                return archivePath;
            }
            projectsJson.put(translationJson);

            zipList.add(new File(repoPath));
        }
        signature = Security.md5(signature);
        String tag = signature.substring(0, 10);

        // close manifest
        try {
            manifestJson.put("projects", projectsJson);
            if(sourceCatalogDir.exists()) {
                manifestJson.put("source", sourceCatalogDir.getName());
            }
            manifestJson.put("signature", signature);
        } catch (JSONException e) {
            Logger.e(Sharing.class.getName(), "failed to add to json object", e);
            return archivePath;
        }
        FileUtils.write(manifestFile, manifestJson.toString());
        zipList.add(manifestFile);

        // zip
        if(stagingSucceeded) {
            File outputZipFile;
            if(targetLanguages.length == 1) {
                // include the language id if this archive only contains a single language
                outputZipFile = new File(exportDir, Project.GLOBAL_PROJECT_SLUG + "-" + p.getId() + "-" + targetLanguages[0].getId() + "_" + tag + "." + Project.PROJECT_EXTENSION);
            } else {
                outputZipFile = new File(exportDir, Project.GLOBAL_PROJECT_SLUG + "-" + p.getId() + "_" + tag + "." + Project.PROJECT_EXTENSION);
            }

            // create the archive if it does not already exist
            if(!outputZipFile.exists()) {
                Zip.zip(zipList.toArray(new File[zipList.size()]), outputZipFile);
            }

            archivePath = outputZipFile.getAbsolutePath();
        }

        // clean up old exports. Android should do this automatically, but we'll make sure
        File[] cachedExports = exportDir.listFiles();
        if(cachedExports != null) {
            for(File f:cachedExports) {
                // trash cached files that are more than 12 hours old.
                if(System.currentTimeMillis() - f.lastModified() > 1000 * 60 * 60 * 12) {
                    if(f.isFile()) {
                        f.delete();
                    } else {
                        FileUtilities.deleteRecursive(f);
                    }
                }
            }
        }

        // clean up staging area
        FileUtilities.deleteRecursive(stagingDir);

        return archivePath;
    }

    /**
     * Exports the project with the currently selected target language in DokuWiki format
     * This is a process heavy method and should not be ran on the main thread
     * TODO: we need to update this so we don't include the root directory. We already support the new method (no root dir) as well as provide legacy suport for importing this format.
     * @param p the project to export
     * @return the path to the export archive
     */
    public static String exportDW(Project p) throws IOException {
        String projectComplexName = Project.GLOBAL_PROJECT_SLUG + "-" + p.getId() + "-" + p.getSelectedTargetLanguage().getId();
        File exportDir = new File(AppContext.context().getCacheDir() + "/" + AppContext.context().getResources().getString(R.string.exported_projects_dir));
        Boolean commitSucceeded = true;

        Pattern pattern = Pattern.compile(NoteSpan.PATTERN);
        Pattern defPattern = Pattern.compile("def=\"(((?!\").)*)\"");
        exportDir.mkdirs();

        // commit changes to repo
        Repo repo = new Repo(p.getRepositoryPath());
        try {
            // only commit if the repo is dirty
            if(!repo.getGit().status().call().isClean()) {
                // add
                AddCommand add = repo.getGit().add();
                add.addFilepattern(".").call();

                // commit
                CommitCommand commit = repo.getGit().commit();
                commit.setAll(true);
                commit.setMessage("auto save");
                commit.call();
            }
        } catch (Exception e) {
            commitSucceeded = false;
        }

        // TRICKY: this has to be read after we commit changes to the repo
        // TODO: we need to provide better error handling when the git commit is null (e.g. try to commit again)
        String tag = p.getLocalTranslationVersion();
        if(tag != null && tag.length() >= 10) {
            tag = tag.substring(0, 10);
        } else {
            tag = "(null)";
        }
        File outputZipFile = new File(exportDir, projectComplexName + "_" + tag + ".zip");
        File outputDir = new File(exportDir, projectComplexName + "_" + tag);

        // clean up old exports
        String[] cachedExports = exportDir.list();
        for(int i=0; i < cachedExports.length; i ++) {
            String[] pieces = cachedExports[i].split("_");
            if(pieces[0].equals(projectComplexName) && !pieces[1].equals(tag)) {
                File oldDir = new File(exportDir, cachedExports[i]);
                FileUtilities.deleteRecursive(oldDir);
            }
        }

        // return the already exported project
        // TRICKY: we can only rely on this when all changes are commited to the repo
        if(outputZipFile.isFile() && commitSucceeded) {
            return outputZipFile.getAbsolutePath();
        }

        // export the project
        outputDir.mkdirs();
        for(int i = 0; i < p.numChapters(); i ++) {
            Chapter c = p.getChapter(i);
            if(c != null) {
                // check if any frames have been translated
                File chapterDir = new File(p.getRepositoryPath(), c.getId());
                if(!chapterDir.exists()) continue;
                String[] translatedFrames = chapterDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return !s.equals("title") && !s.equals("reference");
                    }
                });
                if(translatedFrames.length == 0 && c.getTitleTranslation().getText().trim().isEmpty() && c.getReferenceTranslation().getText().trim().isEmpty()) continue;

                // compile translation
                File chapterFile = new File(outputDir, c.getId() + ".txt");
                chapterFile.createNewFile();
                PrintStream ps = new PrintStream(chapterFile);

                // language
                ps.print("//");
                ps.print(p.getSelectedTargetLanguage().getName());
                ps.println("//");
                ps.println();

                // project
                ps.print("//");
                ps.print(p.getId());
                ps.println("//");
                ps.println();

                // chapter title
//                if(!c.getTitleTranslation().getText().trim().isEmpty()) {
                ps.print("======");
                ps.print(c.getTitleTranslation().getText().trim());
                ps.println("======");
                ps.println();
//                }

                // frames
                for(int j = 0; j < c.numFrames(); j ++) {
                    Frame f = c.getFrame(j);
                    if(f != null && !f.getTranslation().getText().isEmpty()) {
                        // image
                        ps.print("{{");
                        // TODO: the api version and image dimensions should be placed in the user preferences
                        String apiVersion = "1";
                        // TODO: for now all images use the english versions
                        String languageCode = "en"; // eventually we should use: getSelectedTargetLanguage().getId()
                        ps.print(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, AppContext.context().getResources().getString(R.string.pref_default_media_server))+"/"+p.getId()+"/jpg/"+apiVersion+"/"+languageCode+"/360px/"+p.getId()+"-"+languageCode+"-"+c.getId()+"-"+f.getId()+".jpg");
                        ps.println("}}");
                        ps.println();

                        // convert tags
                        String text = f.getTranslation().getText().trim();
                        Matcher matcher = pattern.matcher(text);
                        String convertedText = "";
                        int lastEnd = 0;
                        while(matcher.find()) {
                            if(matcher.start() > lastEnd) {
                                // add the last piece
                                convertedText += text.substring(lastEnd, matcher.start());
                            }
                            lastEnd = matcher.end();

                            // extract note
                            NoteSpan note = NoteSpan.parseNote(matcher.group());
                            if(note.getStyle().equals("f")) {
                                // include footnotes
                                convertedText += note.generateDokuWikiTag();
                            } else if(note.getStyle().equals(NoteSpan.STYLE_USERNOTE)) {
                                // skip user notes
                                convertedText += note.getPassage();
                            }
                        }
                        if(lastEnd < text.length()) {
                            convertedText += text.substring(lastEnd, text.length());
                        }

                        // text
                        ps.println(convertedText);
                        ps.println();
                    }
                }

                // chapter reference
//                if(!c.getReferenceTranslation().getText().trim().isEmpty()) {
                ps.print("//");
                ps.print(c.getReferenceTranslation().getText().trim());
                ps.println("//");
//                }

                ps.close();
            }
        }

        // zip
        try {
            com.door43.util.Zip.zip(outputDir.getAbsolutePath(), outputZipFile.getAbsolutePath());
        } catch (ZipException e) {
            Logger.w(Sharing.class.getName(), "Failed to zip the files", e);
        }
        // cleanup
        FileUtilities.deleteRecursive(outputDir);
        return outputZipFile.getAbsolutePath();
    }

    /**
     * Imports a DokuWiki file into a project.
     * This works files with a single translation/project as well as those with multiple projects/translations.
     * If multiple translations exist in a file they should each include their own project and language tags as in the case of single translations
     * @param file the doku wiki file
     * @return
     */
    public static boolean importDokuWiki(File file) {
        if(file.exists() && file.isFile()) {
            StringBuilder frameBuffer = new StringBuilder();
            String line, chapterId = "", frameId = "", chapterTitle = "";
            Pattern pattern = Pattern.compile("-(\\d\\d)-(\\d\\d)\\.jpg");
            Language targetLanguage = null;
            Project project = null;

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));

                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if(line.length() >= 4 && line.substring(0, 2).equals("//")) {
                        line = line.substring(2, line.length() - 2).trim();
                        if(targetLanguage == null) {
                            // retrieve the translation language
                            targetLanguage = AppContext.projectManager().getLanguageByName(line);
                            if(targetLanguage == null) return false;
                        } else if(project == null) {
                            // retrieve project
                            project = AppContext.projectManager().getProject(line);
                            if(project == null) return false;

                            // load the project source
                            AppContext.projectManager().fetchProjectSource(project, false);

                            // place this translation into the correct language
                            project.setSelectedTargetLanguage(targetLanguage.getId());
                        } else if (!chapterId.isEmpty() && !frameId.isEmpty()) {
                            // retrieve chapter reference (end of chapter)
                            Chapter c = project.getChapter(chapterId);
                            if (c != null) {
                                c.setReferenceTranslation(line);
                                if (!chapterTitle.isEmpty()) {
                                    c.setTitleTranslation(chapterTitle);
                                }
                                c.save();

                                // save the last frame of the chapter
                                if (frameBuffer.length() > 0) {
                                    Frame f = c.getFrame(frameId);
                                    f.setTranslation(frameBuffer.toString().trim());
                                    f.save();
                                }
                            } else {
                                Logger.w(Sharing.class.getName(), "importDokuWiki: unknown chapter " + chapterId);
                            }
                            chapterId = "";
                            frameId = "";
                            frameBuffer.setLength(0);
                        } else {
                            // start loading a new translation
                            project = null;
                            chapterId = "";
                            frameId = "";
                            chapterTitle = "";
                            frameBuffer.setLength(0);

                            // retrieve the translation language
                            targetLanguage = AppContext.projectManager().getLanguageByName(line);
                            if(targetLanguage == null) return false;
                        }
                    } else if(line.length() >= 12 && line.substring(0, 6).equals("======")) {
                        // start of a new chapter
                        chapterTitle = line.substring(6, line.length() - 6).trim(); // this is saved at the end of the chapter
                    } else if(line.length() >= 4 && line.substring(0, 2).equals("{{")) {
                        // save the previous frame
                        if(project != null && !chapterId.isEmpty() && !frameId.isEmpty() && frameBuffer.length() > 0) {
                            Frame f = project.getChapter(chapterId).getFrame(frameId);
                            f.setTranslation(frameBuffer.toString().trim());
                            f.save();
                        }

                        // image tag. We use this to get the frame number for the following text.
                        Matcher matcher = pattern.matcher(line);
                        while(matcher.find()) {
                            chapterId = matcher.group(1);
                            frameId = matcher.group(2);
                        }
                        // clear the frame buffer
                        frameBuffer.setLength(0);
                    } else {
                        // frame translation
                        frameBuffer.append(line);
                        frameBuffer.append('\n');
                    }
                }
            } catch (IOException e) {
                Logger.e(Sharing.class.getName(), "failed to import the DokuWiki file", e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Imports a DokuWIki archive into a project
     * @return
     */
    public static boolean importDokuWikiArchive(File archive) {
        String[] name = archive.getName().split("\\.");
        Boolean success = true;
        if(archive.exists() && archive.isFile() && name[name.length - 1].equals("zip")) {
            long timestamp = System.currentTimeMillis();
            File extractedDirectory = new File(AppContext.context().getCacheDir() + "/" + AppContext.context().getResources().getString(R.string.imported_projects_dir) + "/" + timestamp);
            try {
                Zip.unzip(archive, extractedDirectory);
            } catch (IOException e) {
                Logger.e(Sharing.class.getName(), "failed to extract the DokuWiki translation", e);
                FileUtilities.deleteRecursive(extractedDirectory);
                return false;
            }

            File[] files = extractedDirectory.listFiles();
            if(files.length > 0) {
                // fix legacy DokuWiki export (contained root directory in archive)
                File realPath = extractedDirectory;
                if(files.length == 1 && files[0].isDirectory()) {
                    realPath = files[0];
                    files = files[0].listFiles();
                    if(files.length == 0) {
                        FileUtilities.deleteRecursive(extractedDirectory);
                        return false;
                    }
                }

                // ensure this is not a legacy project archive
                File gitDir = new File(realPath, ".git");
                if(gitDir.exists() && gitDir.isDirectory()) {
                    FileUtilities.deleteRecursive(extractedDirectory);
                    return Sharing.prepareLegacyArchiveImport(archive);
                }

                // begin import
                for(File f:files) {
                    if(!importDokuWiki(f)) {
                        success = false;
                    }
                }
            }
            FileUtilities.deleteRecursive(extractedDirectory);
        }
        return success;
    }

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
        void onSuccess();
    }
}
