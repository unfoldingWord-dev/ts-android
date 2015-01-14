package com.door43.translationstudio.projects;

/**
 * Created by joel on 12/23/2014.
 */
public interface Model {
    public String getId();
    public String getTitle();
    public String getDescription();
    public String getImagePath();
    public boolean isTranslating();
    public boolean isTranslatingGlobal();
    public String getType();
    public boolean isSelected();
}
