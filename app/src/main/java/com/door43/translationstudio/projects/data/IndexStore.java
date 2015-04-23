package com.door43.translationstudio.projects.data;

import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;

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
    private static final String READY_INDEX = "ready.index";
    private static final String DATA_FILE = "data.json";

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
        File indexReadyFile = new File(sInstance.sIndexDir, p.getId() + "/" + READY_INDEX);
        return indexReadyFile.exists();
    }

    /**
     * Returns the indexed frames
     * @param p
     * @param l
     * @param r
     * @param c
     * @return
     */
    public static Model[] getFrames(Project p, SourceLanguage l, Resource r, Chapter c) {
        File chapterDir = new File(sInstance.sIndexDir, p.getId() + "/" + l.getId() + "/" + r.getId() + "/source/" + c.getId() + "/");
        final List<Frame> frames = new ArrayList<>();
        chapterDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(!filename.equals(DATA_FILE)) {
                    try {
                        String data = FileUtils.readFileToString(new File(dir, filename));
                        Frame f = Frame.generate(new JSONObject(data));
                        if(f != null) {
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
}
