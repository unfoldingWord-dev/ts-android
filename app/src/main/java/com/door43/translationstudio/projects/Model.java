package com.door43.translationstudio.projects;

/**
 * Created by joel on 12/23/2014.
 */
public interface Model {
    public String getId();
    public String getTitle();
    public CharSequence getDescription();

    /**
     * Returns the selected source language of the model
     * @return
     */
    public SourceLanguage getSelectedSourceLanguage();

    /**
     * Returns the key used for sorting
     * @return
     */
    public String getSortKey();

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

    /**
     * Checks if there is a translation in the currently selected language for this model
     * @return
     */
    public boolean isTranslating();

    /**
     * Check if there are any translations in progress for this model except for the currently selected language
     * @return
     */
    public boolean isTranslatingGlobal();

    /**
     * Returns the type of model this is
     * @return
     */
    public String getType();

    /**
     * Checks if this model is currently selected
     * @return
     */
    public boolean isSelected();
}
