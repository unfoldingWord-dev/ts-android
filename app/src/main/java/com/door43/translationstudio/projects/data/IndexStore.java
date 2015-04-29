package com.door43.translationstudio.projects.data;

import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.Logger;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 4/22/2015.
 */
public class IndexStore {
    private final File sIndexDir;

    private static IndexStore sInstance = null;
    public static final String READY_FILE = "ready.index";
    public static final String DATA_FILE = "data.json";

    static {
        sInstance = new IndexStore();
    }

    private IndexStore() {
        sIndexDir = new File(AppContext.context().getCacheDir(), "index");
    }

    /**
     * Checks if the project is indexed
     * @param p
     * @return
     */
    public static boolean hasIndex(Project p) {
        File indexReadyFile = new File(sInstance.sIndexDir, p.getId() + "/" + READY_FILE);
        return indexReadyFile.exists();
    }

    /**
     * Returns the indexed frames
     * The project, source language, resource and chapter should all be connected together.
     * @param p
     * @param l
     * @param r
     * @param c
     * @return
     */
    public static Model[] getFrames(Project p, SourceLanguage l, Resource r, final Chapter c) {
        File chapterDir = getSourceChapterDir(p, l, r, c);
        final List<Frame> frames = new ArrayList<>();
        chapterDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(!filename.equals(DATA_FILE)) {
                    try {
                        String data = FileUtils.readFileToString(new File(dir, filename));
                        Frame f = Frame.generate(new JSONObject(data));
                        if(f != null) {
                            f.setChapter(c);
                            frames.add(f);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
        return frames.toArray(new Model[frames.size()]);
    }

    /**
     * Loads the chapters into the project
     * @param p
     * @param l
     * @param r
     * @return
     */
    public static void loadChapters(final Project p, SourceLanguage l, Resource r) {
        final File sourceDir = getSourceDir(p, l, r);
        sourceDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                File dataFile = new File(sourceDir, filename + "/" + DATA_FILE);
                if(dataFile.exists()) {
                    try {
                        String data = FileUtils.readFileToString(dataFile);
                        Chapter c = Chapter.generate(new JSONObject(data));
                        if(c != null) {
                            p.addChapter(c);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
    }

    /**
     * Deletes an indexed project
     * @param p
     */
    public static void deleteIndex(Project p) {
        File projectDir = getProjectDir(p);
        if(projectDir.isDirectory()) {
            FileUtilities.deleteRecursive(projectDir);
        }
    }

    /**
     * Returns the directory for the project index
     * @param p
     * @return
     */
    public static File getProjectDir(Project p) {
        return new File(sInstance.sIndexDir, p.getId());
    }

    /**
     * Returns the directory for the language index
     * @param p
     * @param l
     * @return
     */
    public static File getLanguageDir(Project p, SourceLanguage l) {
        return new File(sInstance.sIndexDir, p.getId() + "/" + l.getId());
    }

    /**
     * Returns the directory for the resource index
     * @param p
     * @param l
     * @param r
     * @return
     */
    public static File getResourceDir(Project p, SourceLanguage l, Resource r) {
        return new File(sInstance.sIndexDir, p.getId() + "/" + l.getId() + "/" + r.getId());
    }



    /**
     * Returns the directory for the chapter index
     * @param p
     * @param l
     * @param r
     * @param c
     * @return
     */
    public static File getSourceChapterDir(Project p, SourceLanguage l, Resource r, Chapter c) {
        return new File(sInstance.sIndexDir, p.getId() + "/" + l.getId() + "/" + r.getId() + "/source/" + c.getId());
    }

    /**
     * Returns the directory for the source
     * @param p
     * @param l
     * @param r
     * @return
     */
    public static File getSourceDir(Project p, SourceLanguage l, Resource r) {
        return new File(sInstance.sIndexDir, p.getId() + "/" + l.getId() + "/" + r.getId() + "/source/");
    }

    /**
     * Returns the directory for the chapter index
     * @param p
     * @param l
     * @param r
     * @param c
     * @return
     */
    public static File getNotesChapterDir(Project p, SourceLanguage l, Resource r, Chapter c) {
        return new File(sInstance.sIndexDir, p.getId() + "/" + l.getId() + "/" + r.getId() + "/notes/" + c.getId());
    }

    /**
     * Returns the directory for the notes
     * @param p
     * @param l
     * @param r
     * @return
     */
    public static File getNotesDir(Project p, SourceLanguage l, Resource r) {
        return new File(sInstance.sIndexDir, p.getId() + "/" + l.getId() + "/" + r.getId() + "/notes/");
    }

    /**
     * Generates a chapter index
     * @param c
     */
    public static void index(Chapter c) {
        Project p = c.getProject();
        if(p != null && c.getSelectedSourceLanguage() != null && c.getSelectedSourceLanguage().getSelectedResource() != null) {
            SourceLanguage l = c.getSelectedSourceLanguage();
            Resource r = l.getSelectedResource();
            File dir = getSourceChapterDir(p, l, r, c);
            dir.mkdirs();
            File datFile = new File(dir, DATA_FILE);
            if (!datFile.exists()) {
                try {
                    FileUtils.write(datFile, c.serialize().toString());
                } catch (Exception e) {
                    Logger.e(IndexStore.class.getName(), "Failed to index chapter info. Project: " + p.getId() + " Language: " + l.getId() + " Resource: " + r.getId() + " Chapter: " + c.getId(), e);
                }
            } else {
                // TODO: make sure we have a good expiration system set up for indexes.
            }
        } else {
            Logger.e(IndexStore.class.getName(), "Chapter is missing required objects");
        }
    }

    /**
     * Generates a frame index
     * @param f
     */
    public static void index(Frame f) {
        Chapter c = f.getChapter();
        if(c != null && c.getProject() != null) {
            Project p = c.getProject();
            SourceLanguage l = c.getProject().getSelectedSourceLanguage();
            if(l != null && l.getSelectedResource() != null) {
                Resource r = l.getSelectedResource();

                File sourceFrameInfo = new File(getSourceChapterDir(p, l, r, c), f.getId() + ".json");
                sourceFrameInfo.getParentFile().mkdirs();
                File notesFrameInfo = new File(getNotesChapterDir(p, l, r, c), f.getId() + ".json");
                notesFrameInfo.getParentFile().mkdirs();

                if(!sourceFrameInfo.exists()) {
                    try {
                        FileUtils.write(sourceFrameInfo, f.serialize().toString());
                    } catch (Exception e) {
                        Logger.e(IndexStore.class.getName(), "Failed to index source frame info. Project: " + p.getId() + " Language: " + l.getId() + " Resource: " + r.getId() + " Chapter: " + c.getId() + " Frame: " + f.getId(), e);
                    }
                }

                if(!notesFrameInfo.exists()) {
                    try {
                        if(f.getTranslationNotes() != null) {
                            FileUtils.write(notesFrameInfo, f.serializeTranslationNote().toString());
                        }
                    } catch (Exception e) {
                        Logger.e(IndexStore.class.getName(), "Failed to index notes frame info. Project: " + p.getId() + " Language: " + l.getId() + " Resource: " + r.getId() + " Chapter: " + c.getId() + " Frame: " + f.getId(), e);
                    }
                }
            }
        }
    }

    /**
     * Checks if the currently selected resource on the project is indexed
     * @param p
     * @return
     */
    public static boolean hasResourceIndex(Project p) {
        File dir = getResourceDir(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
        File readyFile = new File(dir, READY_FILE);
        return readyFile.exists();
    }
}
