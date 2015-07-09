package com.door43.translationstudio.projects;

import android.text.Editable;
import android.text.SpannedString;
import android.util.Log;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.tasks.ProgressCallback;
import com.door43.translationstudio.git.tasks.repo.CommitTask;
import com.door43.translationstudio.git.tasks.repo.PushTask;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.util.FileUtilities;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.TCPClient;
import com.door43.tools.reporting.Logger;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class handles the storage of translated content.
 */
public class TranslationManager {
//    private MainApplication mContext;
    private static final String TAG = "TranslationManager";
    private static final String mParentProjectSlug = "uw"; //  NOTE: not sure if this will ever need to be dynamic
    private static TCPClient mTcpClient;
    private static Frame mFrame;
    private static Editable mText;
    private static boolean mAutosaveEnabled = true;
    private static Timer mAutosaveTimer;
    private final int sAutosaveDelay;

    private static final TranslationManager sInstance;

    static {
        sInstance = new TranslationManager();
    }

    private TranslationManager() {
        sAutosaveDelay = Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTOSAVE, AppContext.context().getResources().getString(R.string.pref_default_autosave)));
    }

    /**
     * Initiates a git sync with the server. This will forcebly push all local changes to the server
     * and discard any discrepencies.
     */
    @Deprecated
    public static void syncSelectedProject(final OnSyncListener listener) {
        if(AppContext.context().isNetworkAvailable()) {
            listener.onProgress(-1, AppContext.context().getResources().getString(R.string.loading));
            if (!AppContext.context().hasRegisteredKeys()) {
                AppContext.context().showProgressDialog(R.string.loading);
                // set up a tcp connection
                if (mTcpClient == null) {
                    mTcpClient = new TCPClient(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER, AppContext.context().getResources().getString(R.string.pref_default_auth_server)), Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER_PORT, AppContext.context().getResources().getString(R.string.pref_default_auth_server_port))), new TCPClient.TcpListener() {
                        @Override
                        public void onConnectionEstablished() {
                            // submit key to the server
                            if (AppContext.context().hasKeys()) {
                                listener.onProgress(-1, AppContext.context().getResources().getString(R.string.submitting_security_keys));
                                JSONObject json = new JSONObject();
                                try {
                                    String key = FileUtilities.getStringFromFile(AppContext.context().getPublicKey().getAbsolutePath()).trim();
                                    json.put("key", key);
                                    json.put("udid", AppContext.udid());
                                    // TODO: provide support for using user names
//                                json.put("username", "");
                                    Log.d(TAG, json.toString());
                                    mTcpClient.sendMessage(json.toString());
                                } catch (JSONException e) {
                                    Logger.e(TranslationManager.class.getName(), "Failed to upload the public key", e);
                                    listener.onError(e.getMessage());
                                } catch (Exception e) {
                                    Logger.e(TranslationManager.class.getName(), "Failed to upload the public key", e);
                                    listener.onError(e.getMessage());
                                }
                            } else {
                                listener.onError(AppContext.context().getResources().getString(R.string.error_missing_ssh_keys));
                            }
                        }

                        @Override
                        public void onMessageReceived(String message) {
                            // check if we get an ok message from sending ssh keys to the server
                            AppContext.context().closeProgressDialog();
                            try {
                                JSONObject json = new JSONObject(message);
                                if (json.has("ok")) {
                                    AppContext.context().setHasRegisteredKeys(true);
                                    pushSelectedProjectRepo();
                                } else {
                                    Logger.e(TranslationManager.class.getName(), "The server rejected the public key", new Throwable(json.getString("error")));
                                    listener.onError(json.getString("error"));
                                }
                            } catch (JSONException e) {
                                Logger.e(TranslationManager.class.getName(), "Failed to parse the response from the server", e);
                                listener.onError(e.getMessage());
                            }
                            mTcpClient.stop();
                        }

                        @Override
                        public void onError(Throwable t) {
                            Logger.e(TranslationManager.class.getName(), "Could not connect to the server", t);
                            listener.onError(t.getMessage());
                        }
                    });
                } else {
                    // TODO: update the sever and port if they have changed... Not sure if this task is applicable now
                }
                // connect to the server so we can submit our key
                mTcpClient.connect();
            } else {
                pushSelectedProjectRepo();
            }
        } else {
            listener.onError(AppContext.context().getResources().getString(R.string.internet_not_available));
        }
    }

    /**
     * Disables the translation autosave
     */
    public static void disableAutosave() {
        mAutosaveEnabled = false;
        if(mAutosaveTimer != null) {
            mAutosaveTimer.cancel();
            mAutosaveTimer = null;
        }
    }

    /**
     * Enables the translation autosave
     */
    public static void enableAutosave() {
        stageTranslation(null, null);
        mAutosaveEnabled = true;
    }

    /**
     * Schedules translation to be automatically saved to a frame
     * @param f the frame that will receive the translation
     * @param text the translation that will be saved
     */
    public static void autosave(Frame f, Editable text) {
        if(mAutosaveTimer != null) {
            mAutosaveTimer.cancel();
        }
        if(mAutosaveEnabled) {
            stageTranslation(f, text);
        }
        if(sInstance.sAutosaveDelay != -1) {
            mAutosaveTimer = new Timer();
            if(mAutosaveEnabled) {
                mAutosaveTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        save();
                    }
                }, sInstance.sAutosaveDelay);
            }
        }
    }

    /**
     * Saves the pending translation
     */
    public static void save() {
        if(mAutosaveEnabled) {
            disableAutosave();
            commitTranslation();
            enableAutosave();
        }
    }

    /**
     * Stages a new translation to be saved. This is usually called whenever the translation in a frame changes
     * @param frame the frame for which the translation changed
     * @param text the raw translation input
     */
    private static void stageTranslation(Frame frame, Editable text) {
        mFrame = frame;
        mText = text;
    }

    /**
     * Compiles all of the spans within a text into their machine readable form
     * @param text
     */
    public static String compileTranslation(Editable text) {
        StringBuilder compiledString = new StringBuilder();
        int next;
        int lastIndex = 0;
        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, text.length(), SpannedString.class);
            SpannedString[] verses = text.getSpans(i, next, SpannedString.class);
            for (SpannedString s : verses) {
                int sStart = text.getSpanStart(s);
                int sEnd = text.getSpanEnd(s);
                // attach preceeding text
                if (lastIndex >= text.length() | sStart >= text.length()) {
                    Logger.e(TranslationManager.class.getName(), "out of bounds");
                }
                compiledString.append(text.toString().substring(lastIndex, sStart));
                // explode span
                compiledString.append(s.toString());
                lastIndex = sEnd;
            }
        }
        // grab the last bit of text
        compiledString.append(text.toString().substring(lastIndex, text.length()));
        return compiledString.toString().trim();
    }

    /**
     * Saves the staged translation
     */
    private static void commitTranslation() {
        if(mFrame != null && mText != null) {
            String compiled = compileTranslation(mText);
            if(mFrame.getChapter() != null && mFrame.getChapter().getProject() != null && mFrame.getChapter().getProject().hasSelectedTargetLanguage()) {
                mFrame.setTranslation(compiled);
                mFrame.save();
            }
            mFrame = null;
            mText = null;
        }
    }

    /**
     * Pushes the currently selected project+language repo to the server
     *
     */
    @Deprecated
    private static void pushSelectedProjectRepo() {
        Project p = AppContext.projectManager().getSelectedProject();

        if(p.translationIsReady()) {
            Logger.i(TranslationManager.class.getName(), "Publishing project " + p.getId() + " to the server");
        }

        final String remotePath = getRemotePath(p, p.getSelectedTargetLanguage());
        final Repo repo = new Repo(p.getRepositoryPath());

        CommitTask add = new CommitTask(repo, ".", new CommitTask.OnAddComplete() {
            @Override
            public void success() {
                PushTask push = new PushTask(repo, remotePath, true, true, new ProgressCallback(R.string.uploading));
                push.executeTask();
                // TODO: we need to check the errors from the push task. If auth fails then we need to re-register the ssh keys.
            }

            @Override
            public void error(Throwable e) {
                AppContext.context().showException(e, R.string.error_git_stage);
            }
        });
        add.executeTask();

        // send the latest profile info to the server as well
        ProfileManager.pushAsync();
    }

    /**
     * Pushes a project repository to the server
     * @param p
     * @param target
     */
    private static void pushProjectRepo(Project p, Language target) {
        // TODO: implement and replace pushSelectedProjectRepo()
    }

    /**
     * Pushes a notes repository to the server
     * @param p
     * @param target
     */
    private static void pushNotesRepo(Project p, Language target) {
        // TODO: implement
    }

    /**
     * Generates the remote path for a local repo
     * @param project
     * @param lang
     * @return
     */
    private static String getRemotePath(Project project, Language lang) {
        String server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
        return server + ":tS/" + AppContext.udid() + "/" + mParentProjectSlug + "-" + project.getId() + "-" + lang.getId();
    }

    /**
     * Imports a translation draft into a project.
     * This will replace any current translation
     * @param p the project to which the draft will be imported
     * @param draft the draft that will be imported
     */
    public static void importTranslationDraft(Project p, SourceLanguage draft) {
        importTranslationDraft(p, draft, null);
    }

    /**
     * Imports a translation draft into a project.
     * This will replace any current translation
     * TODO: right now this will import the selected (default) resource in the draft. We may need to provide support for choosing a resource later.
     * @param p the project to which the draft will be imported
     * @param draft the draft that will be imported
     */
    public static void importTranslationDraft(Project p, SourceLanguage draft, OnProgressListener listener) {
        String source = AppContext.projectManager().getDataStore().pullSource(p.getId(), draft.getId(), draft.getSelectedResource().getId(), false, false);

        if(listener != null) {
            listener.onProgress(-1, "");
        }

        // load source
        JSONArray jsonChapters;
        if(source == null) {
            Logger.e(TranslationManager.class.getName(), "The draft source was not found for "+p.getId());
            return;
        }
        try {
            JSONObject json = new JSONObject(source);
            jsonChapters = json.getJSONArray("chapters");
        } catch (JSONException e) {
            Logger.e(TranslationManager.class.getName(), "malformed draft source", e);
            return;
        }

        // load the data
        for(int i=0; i<jsonChapters.length(); i++) {
            if(Thread.currentThread().isInterrupted()) break;
            try {
                if(listener != null) {
                    listener.onProgress((i+1)/(double)jsonChapters.length(), "");
                }
                JSONObject jsonChapter = jsonChapters.getJSONObject(i);
                if(jsonChapter.has("frames")) {
                    // load chapter
                    String chapterNumber = jsonChapter.get("number").toString();
                    String title = "";
                    String reference = "";
                    if(jsonChapter.has("title") && jsonChapter.has("ref")) {
                        title = jsonChapter.get("title").toString();
                        reference = jsonChapter.get("ref").toString();
                    }
                    Chapter c = p.getChapter(chapterNumber);
                    if(c == null) {
                        Logger.e(TranslationManager.class.getName(), "Unknown chapter ("+chapterNumber+") in translation draft "+draft.getId());
                        continue;
                    }

                    // update title and reference translation
                    c.setTitleTranslation(title);
                    c.setReferenceTranslation(reference);
                    c.save();

                    // load frames
                    JSONArray jsonFrames = jsonChapter.getJSONArray("frames");
                    for(int j=0; j<jsonFrames.length(); j++) {
                        if(Thread.currentThread().isInterrupted()) break;
                        JSONObject jsonFrame = jsonFrames.getJSONObject(j);
                        if(jsonFrame.has("id") && jsonFrame.has("text")) {
                            // parse id
                            Frame draftFrame = new Frame(jsonFrame.getString("id"), "", "", "");
                            Frame f = c.getFrame(draftFrame.getId());

                            // update the frame translation
                            if(f != null) {
                                f.setTranslation(jsonFrame.getString("text"));
                                f.save();
                            } else{
                                Logger.e(TranslationManager.class.getName(), "Unknown frame ("+chapterNumber+":"+jsonFrame.getString("id")+") in translation draft "+draft.getId());
                            }
                        } else {
                            Logger.w(TranslationManager.class.getName(), "missing required parameters in source frame at index "+i+":"+j);
                        }
                    }
                } else {
                    Logger.w(TranslationManager.class.getName(), "missing required parameters in source chapter at index " + i);
                }
            } catch (JSONException e) {
                Logger.e(TranslationManager.class.getName(), "Failed to load project source", e);
                continue;
            }
        }
    }

    /**
     * Imports the source from the given directory
     * @param dir
     */
    public static void importSource(File dir) {
        Logger.i(TranslationManager.class.getName(), "importing source files from "+ dir.getName());
        if(dir.exists() && dir.isDirectory()) {
            File projectsCatalogFile = new File(dir, "projects_catalog.json");
            if(projectsCatalogFile.exists()) {
                try {
                    // load projects
                    String projCat = FileUtils.readFileToString(projectsCatalogFile);
                    JSONArray projCatJson = new JSONArray(projCat);
                    for(int i = 0; i < projCatJson.length(); i ++) {
                        try {
                            JSONObject projJson = projCatJson.getJSONObject(i);
                            String projSlug = projJson.getString("slug");
                            Project existingProject = AppContext.projectManager().getProject(projSlug);
                            int projDateModified = projJson.getInt("date_modified");
                            if (existingProject == null || existingProject.getDateModified() < projDateModified) {
                                // import/replace the project
                                AppContext.projectManager().getDataStore().importProject(projJson.toString());
                                // load languages
                                File projDir = new File(dir, projSlug);
                                if(projDir.exists()) {
                                    File langCatFile = new File(projDir, "languages_catalog.json");
                                    if(langCatFile.exists()) {
                                        String langCat = FileUtils.readFileToString(langCatFile);
                                        JSONArray langCatJson = new JSONArray(langCat);
                                        for(int j = 0; j < langCatJson.length(); j ++) {
                                            try {
                                                JSONObject langJson = langCatJson.getJSONObject(j);
                                                JSONObject langInfoJson = langJson.getJSONObject("language");
                                                String langSlug = langInfoJson.getString("slug");
                                                int langDateModified = langInfoJson.getInt("date_modified");
                                                SourceLanguage existingLanguage = null;
                                                if(existingProject != null) {
                                                    existingLanguage = existingProject.getSourceLanguage(langSlug);
                                                }
                                                if(existingLanguage == null || existingLanguage.getDateModified() < langDateModified) {
                                                    // import/replace the source language
                                                    AppContext.projectManager().getDataStore().importSourceLanguage(projSlug, langJson.toString());
                                                    // load resources
                                                    File langDir = new File(projDir, langSlug);
                                                    if(langDir.exists()) {
                                                        File resCatFile = new File(langDir, "resources_catalog.json");
                                                        if(resCatFile.exists()) {
                                                            String resCat = FileUtils.readFileToString(resCatFile);
                                                            JSONArray resCatJson = new JSONArray(resCat);
                                                            for(int k = 0; k < resCatJson.length(); k ++) {
                                                                try {
                                                                    JSONObject resJson = resCatJson.getJSONObject(k);
                                                                    Resource r = Resource.generate(resJson);
                                                                    if(r != null) {
                                                                        Resource existingResource = null;
                                                                        if (existingLanguage != null) {
                                                                            existingResource = existingLanguage.getResource(r.getId());
                                                                        }
                                                                        if (existingResource == null || existingResource.getDateModified() < r.getDateModified()) {
                                                                            // import/replace the resource catalog
                                                                            AppContext.projectManager().getDataStore().importResource(projSlug, langSlug, resJson.toString());


                                                                            // load the individual resource files
                                                                            File resDir = new File(langDir, r.getId());
                                                                            if (resDir.exists()) {
                                                                                File notesFile = new File(resDir, "notes.json");
                                                                                File sourceFile = new File(resDir, "source.json");
                                                                                File termsFile = new File(resDir, "terms.json");

                                                                                String notes = FileUtils.readFileToString(notesFile);
                                                                                String source = FileUtils.readFileToString(sourceFile);
                                                                                String terms = FileUtils.readFileToString(termsFile);

                                                                                AppContext.projectManager().getDataStore().importNotes(projSlug, langSlug, r.getId(), r.getNotesCatalog(), notes);
                                                                                AppContext.projectManager().getDataStore().importSource(projSlug, langSlug, r.getId(), r.getSourceCatalog(), source);
                                                                                AppContext.projectManager().getDataStore().importTerms(projSlug, langSlug, r.getId(), r.getTermsCatalog(), terms);
                                                                                AppContext.projectManager().getDataStore().importQuestions(projSlug, langSlug, r.getId(), r.getQuestionsCatalog(), terms);
                                                                            }
                                                                        }
                                                                    } else {
                                                                        Logger.w(TranslationManager.class.getName(), "invalid resource definition found while importing");
                                                                    }
                                                                } catch (Exception e) {
                                                                    Logger.e(TranslationManager.class.getName(), "failed to import the resource", e);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) {
                                                Logger.e(TranslationManager.class.getName(), "failed to import the source language", e);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Logger.e(TranslationManager.class.getName(), "failed to import the source project", e);
                        }
                    }
                } catch (Exception e) {
                    Logger.e(TranslationManager.class.getName(), "failed to import the source files", e);
                }
                // reload the projects
                AppContext.projectManager().initProjects();
            }
        }
    }

    public interface OnProgressListener {
        void onProgress(double progress, String message);
    }

    public interface OnSyncListener {
        void onFinish();
        void onError(String message);
        void onProgress(double progress, String message);
    }
}
