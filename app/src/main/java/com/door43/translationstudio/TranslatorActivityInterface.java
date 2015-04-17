package com.door43.translationstudio;

/**
 * Created by joel on 4/17/2015.
 */
public interface TranslatorActivityInterface {
    void openLibraryDrawer();
    void openResourcesDrawer();
    void save();
    void refreshLibraryDrawer();
    void showProjectSettingsMenu();
    void refreshResourcesDrawer();
    boolean keyboardIsOpen();
}
