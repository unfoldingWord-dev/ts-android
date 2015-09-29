package com.door43.translationstudio.projects;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by joel on 9/5/2014.
 */
@Deprecated
public class Language {
    private final String mCode;
    private final String mName;
    private final Direction mDirection;
    private int mSessionVersion; // this is used for target language

    public enum Direction {
        LeftToRight("ltr"),
        RightToLeft("rtl");

        Direction(String label) {
            this.label = label;
        }

        private String label;

        public String getLabel() {
            return label;
        }

        /**
         * Returns a direction by it's label
         * @param label
         * @return
         */
        public static Direction get(String label) {
            for (Direction l : Direction.values()) {
                if (l.getLabel().equals(label.toLowerCase())) {
                    return l;
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return mCode;
    }

    /**
     * Create a new language
     * @param code the language code
     * @param name the name of the language
     * @param direction the text direction
     */
    public Language(String code, String name, Direction direction) {
        mCode = code;
        mName = name;
        mDirection = direction;
    }

    /**
     * Returns the text direction
     * @return
     */
    public Direction getDirection() {
        return mDirection;
    }

    /**
     * Returns the human readable text direction
     * @return
     */
    public String getDirectionName() {
        return mDirection.getLabel();
    }

    /**
     * Returns the language code
     * @return
     */
    public String getId() {
        return mCode;
    }

    /**
     * Returns the human readable language name
     * @return
    */
    public String getName() {
        return mName;
    }

    /**
     * Increases the session version so that cached translations in this language will become invalid and re-loaded from the disk
     */
    public void touch() {
        mSessionVersion ++;
    }

    /**
     * Returns the session version of the target language
     * @return
     */
    public int getSessionVersion() {
        return mSessionVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() == Language.class) {
            return ((Language)obj).getId().equals(getId());
        }
        return false;
    }

    /**
     * Checks if any translation progress has been made on this language.
     * @param project the project for which translation work is searched for
     * @return
     */
    public boolean isTranslating(final Project project) {
        if(project == null) return isTranslating();

        File dir = new File(Project.getRepositoryPath(project.getId(), getId()));
        String[] files = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return !s.equals(".git") && new File(file, s).isDirectory();
            }
        });
        return files != null && files.length > 0;


    }

    /**
     * Checks if any translations have been made for this language for any project
     * @return
     */
    public boolean isTranslating() {
        File dir = new File(Project.getProjectsPath());
        String[] files = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                String[] pieces = s.split("-");
                if(pieces.length == 3) {
                    if(pieces[2].equals(getId())) {
                        // check if there are translations in the project
                        String[] translationFiles = file.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File file, String s) {
                                return !s.equals(".git");
                            }
                        });
                        return translationFiles != null && translationFiles.length > 0;
                    }
                }
                return false;
            }
        });
        return files != null && files.length > 0;
    }
}
