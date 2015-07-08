package com.door43.translationstudio.projects.data;

import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.reporting.Logger;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
     * This will reverse the effect of finalizing the index.
     * The index is not destroyed, but running an indexing task again
     * will check each part of the index for completeness
     * @param p
     */
    public static void dirtyIndex(Project p) {
        File dir = getProjectDir(p);
        File readyFile = new File(dir, READY_FILE);
        readyFile.delete();
    }

    /**
     * This will reverse the effect of finalizing the index.
     * The index is not destroyed, but running an indexing task again
     * will check each part of the index for completeness
     * @param p
     * @param l
     * @param r
     */
    public static void dirtyResourceIndex(Project p, SourceLanguage l, Resource r) {
        File dir = getResourceDir(p, l, r);
        File readyFile = new File(dir, READY_FILE);
        readyFile.delete();
    }

    /**
     * This will delete the entire index
     * @param p
     */
    public static void destroy(Project p) {
        File dir = getProjectDir(p);
        FileUtilities.deleteRecursive(dir);
    }

    /**
     * Checks if the project is indexed
     * @param p
     * @return
     */
    public static boolean hasIndex(Project p) {
        File dir = getProjectDir(p);
        File readyFile = new File(dir, READY_FILE);
        return readyFile.exists();
    }


    /**
     * Checks if the currently selected resource on the project is indexed
     * @param p
     * @return
     */
    public static boolean hasResourceIndex(Project p, SourceLanguage l, Resource r) {
        File dir = getResourceDir(p, l, r);
        File readyFile = new File(dir, READY_FILE);
        return readyFile.exists();
    }

    /**
     * Marks the index as being ready. e.g. hasIndex() will return true;
     * @param p
     */
    public static void finalizeIndex(Project p) {
        File dir = getProjectDir(p);
        File readyFile = new File(dir, READY_FILE);
        try {
            FileUtils.write(readyFile, p.getDateModified() + "");
        } catch (IOException e) {
            Logger.e(IndexStore.class.getName(), "Failed to create the ready.index file", e);
        }
    }

    /**
     * Marks the index as being ready. e.g. hasResourceIndex() will return true;
     * @param p
     */
    public static void finalizeResourceIndex(Project p, SourceLanguage l, Resource r) {
        File dir = getResourceDir(p, l, r);
        File readyFile = new File(dir, READY_FILE);
        try {
            FileUtils.write(readyFile, r.getDateModified() + "");
        } catch (IOException e) {
            Logger.e(IndexStore.class.getName(), "Failed to create the ready.index file", e);
        }
    }

    /**
     * Returns the indexed frames
     * The project, source language, resource and chapter should all be connected together.
     * This will not add the frames into the chapter
     * @param p
     * @param l
     * @param r
     * @param c
     * @return
     */
    public static Model[] getFrames(final Project p, final SourceLanguage l, final Resource r, final Chapter c) {
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
                            TranslationNote note = getTranslationNote(p, l, r, c, f);
                            f.setTranslationNotes(note);
                            f.setChapter(c);
                            frames.add(f);
                        }
                    } catch (Exception e) {
                        Logger.e(IndexStore.class.getName(), "Failed to load the frames for " + p.getId() + ":" + l.getId() + ":" + r.getId() + ":" + c.getId(), e);
                    }
                }
                return false;
            }
        });
        return frames.toArray(new Model[frames.size()]);
    }

    /**
     * Loads a single frame from the index
     * @param projectId
     * @param sourceId
     * @param resourceId
     * @param chapterId
     * @param frameId
     * @return
     */
    public static Frame getFrame(String projectId, String sourceId, String resourceId, String chapterId, String frameId) {
        File chapterDir = getSourceChapterDir(projectId, sourceId, resourceId, chapterId);
        final File frameFile = new File(chapterDir, frameId+".json");
        if(frameFile.exists()) {
            try {
                return Frame.generate(new JSONObject(FileUtils.readFileToString(frameFile)));
            } catch (Exception e) {
                Logger.e(IndexStore.class.getName(), "Failed to load the frame " + frameFile.getPath(), e);
            }
        }
        return null;
    }

    /**
     * Loads the frames into a chapter
     * TODO: we need to load the notes and terms as well
     * @param p
     * @param l
     * @param r
     * @param c
     */
    public static void loadFrames(final Project p, final SourceLanguage l, final Resource r, final Chapter c) {
        File chapterDir = getSourceChapterDir(p, l, r, c);
        chapterDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(!filename.equals(DATA_FILE)) {
                    try {
                        String data = FileUtils.readFileToString(new File(dir, filename));
                        Frame f = Frame.generate(new JSONObject(data));
                        if(f != null)  {
                            TranslationNote note = getTranslationNote(p, l, r, c, f);
                            f.setTranslationNotes(note);
                            c.addFrame(f);
                        }
                    } catch (Exception e) {
                        Logger.e(IndexStore.class.getName(), "Failed to load the frames for "+p.getId()+":"+l.getId()+":"+r.getId()+":"+c.getId(), e);
                    }
                }
                return false;
            }
        });
    }

    /**
     * Loads a translation note from the index.
     * @param p
     * @param l
     * @param r
     * @param c
     * @param f
     * @return
     */
    public static TranslationNote getTranslationNote(Project p, SourceLanguage l, Resource r, Chapter c, Frame f) {
        File noteFile = new File(getNotesChapterDir(p, l, r, c), f.getId() + ".json");
        if(noteFile.exists()) {
            try {
                String data = FileUtils.readFileToString(noteFile);
                TranslationNote note = TranslationNote.Generate(new JSONObject(data));
                if(note != null) {
                    note.setFrame(f);
                    return note;
                }
            } catch (Exception e) {
                Logger.e(IndexStore.class.getName(), "Failed to load the translation notes for " + p.getId() + ":" + l.getId() + ":" + r.getId() + ":" + c.getId() + ":" + f.getId(), e);
            }
        }
        return null;
    }

    /**
     * Loads the chapters into the project
     * @param p
     * @param l
     * @param r
     * @return
     */
    public static void loadChapters(final Project p, final SourceLanguage l, final Resource r) {
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
                        Logger.e(IndexStore.class.getName(), "Failed to load the chapters for " + p.getId() + ":" + l.getId() + ":" + r.getId(), e);
                    }
                }
                return false;
            }
        });
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
     * Returns the directory for the chapter index
     * @param projectId
     * @param sourceId
     * @param resourceId
     * @param chapterId
     * @return
     */
    public static File getSourceChapterDir(String projectId, String sourceId, String resourceId, String chapterId) {
        return new File(sInstance.sIndexDir, projectId + "/" + sourceId + "/" + resourceId + "/source/" + chapterId);
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
     * Generates a project index
     * @param p
     */
    public static void index(Project p) {
        File dir = getProjectDir(p);
        dir.mkdirs();
        File dataFile = new File(dir, DATA_FILE);
        if(!dataFile.exists()) {
            try {
                FileUtils.write(dataFile, p.serialize().toString());
            } catch (Exception e) {
                Logger.e(IndexStore.class.getName(), "Failed to index the project info. Project: " + p.getId(), e);
            }
        } else {
            // TODO: make sure we have a good expiration system set up for indexes.
        }
    }

    /**
     * Deletes a source language index
     * @param p
     * @param l
     */
    public static void destroy(Project p, SourceLanguage l) {
        dirtyIndex(p);
        File dir  = getLanguageDir(p, l);
        FileUtilities.deleteRecursive(dir);
    }

    /**
     * Generates a source language index
     * @param l
     */
    public static void index(Project p, SourceLanguage l) {
        File dir = getLanguageDir(p, l);
        dir.mkdirs();
        File dataFile = new File(dir, DATA_FILE);
        if(!dataFile.exists()) {
            try {
                FileUtils.write(dataFile, p.serializeSourceLanguage(l).toString());
            } catch (Exception e) {
                Logger.e(IndexStore.class.getName(), "Failed to index the source language. Project: "+p.getId()+" Language: "+l.getId(), e);
            }
        } else {
            // TODO: make sure we have a good expiration system set up for indexes.
        }
    }

    /**
     * Generates a resource index
     * @param p
     * @param l
     * @param r
     */
    public static void index(Project p, SourceLanguage l, Resource r) {
        File dir = getResourceDir(p, l, r);
        dir.mkdirs();
        File dataFile = new File(dir, DATA_FILE);
        if(!dataFile.exists()) {
            try {
                FileUtils.write(dataFile, r.serialize().toString());
            } catch (Exception e) {
                Logger.e(IndexStore.class.getName(), "Failed to index resource info. Project: " + p.getId() + " Language: " + l.getId() + " Resource: " + r.getId(), e);
            }
        } else {
            // TODO: make sure we have a good expiration system set up for indexes.
        }
    }

    /**
     * Generates a chapter index
     * @param c
     */
    public static void index(Project p, SourceLanguage l, Resource r, Chapter c) {
        File dir = getSourceChapterDir(p, l, r, c);
        dir.mkdirs();
        File dataFile = new File(dir, DATA_FILE);
        if (!dataFile.exists()) {
            try {
                FileUtils.write(dataFile, c.serialize().toString());
            } catch (Exception e) {
                Logger.e(IndexStore.class.getName(), "Failed to index chapter info. Project: " + p.getId() + " Language: " + l.getId() + " Resource: " + r.getId() + " Chapter: " + c.getId(), e);
            }
        } else {
            // TODO: make sure we have a good expiration system set up for indexes.
        }
    }

    /**
     * Generates a frame index
     * @param f
     */
    public static void index(Project p, SourceLanguage l, Resource r, Chapter c, Frame f) {
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
