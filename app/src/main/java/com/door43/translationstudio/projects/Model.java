package com.door43.translationstudio.projects;

/**
 * Created by joel on 12/23/2014.
 */
public interface Model {
    public String getId();
    public String getTitle();
    public CharSequence getDescription();

    /**
     * The path to the image for this resource
     * @return
     */
    public String getImagePath();

    /**
     * The path to the default image to use if the preferred image is not available
     * @return
     */
    public String getDefaultImagePath();
    public boolean isTranslating();
    public boolean isTranslatingGlobal();
    public String getType();
    public boolean isSelected();
}
