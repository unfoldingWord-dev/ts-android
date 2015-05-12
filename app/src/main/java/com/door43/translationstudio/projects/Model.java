package com.door43.translationstudio.projects;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 12/23/2014.
 */
public interface Model {
    String getId();
    String getTitle();
    CharSequence getDescription();

    /**
     * Returns the selected source language of the model
     * @return
     */
    SourceLanguage getSelectedSourceLanguage();

    /**
     * Returns the key used for sorting
     * @return
     */
    String getSortKey();

    /**
     * The path to the image for this resource
     * @return
     */
    String getImagePath();

    /**
     * The path to the default image to use if the preferred image is not available
     * @return
     */
    String getDefaultImagePath();

    /**
     * Checks if there is a translation in the currently selected language for this model
     * @return
     */
    boolean isTranslating();

    /**
     * Checks if the translation notes are being translated for this currently seelcted language for this model
     * @return
     */
    boolean isTranslatingNotes();

    /**
     * Check if there are any translations in progress for this model except for the currently selected target language
     * @return
     */
    boolean isTranslatingGlobal();

    /**
     * Checks if any trnalsation notes are being translated in this project for any except the currently selected target language
     */
    boolean isTranslatingNotesGlobal();

    /**
     * Returns the type of model this is
     * @return
     */
    String getType();

    /**
     * Checks if this model is currently selected
     * @return
     */
    boolean isSelected();

    /**
     * Serializes the model data.
     * This should not include any sub models, just the immedate data.
     * @return
     * @throws JSONException
     */
    JSONObject serialize() throws JSONException;
}
