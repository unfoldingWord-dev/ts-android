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
    public String getType();
    public boolean isSelected();
    // TODO: place a method here where you can register to receive events from a child. This would have to be an abstract class then.
}
