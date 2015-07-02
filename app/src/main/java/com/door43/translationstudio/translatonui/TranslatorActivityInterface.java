package com.door43.translationstudio.translatonui;

import com.door43.translationstudio.projects.Term;

/**
 * Created by joel on 4/17/2015.
 */
public interface TranslatorActivityInterface {
    void openLibraryDrawer();
    void openResourcesDrawer();
    void refreshLibraryDrawer();
    void showProjectSettingsMenu();
    void refreshResourcesDrawer();
    boolean keyboardIsOpen();
    void closeKeyboard();
    void openKeyboard();
    void closeDrawers();
    void disableResourcesDrawer();
    void enableResourcesDrawer();
    void setContextualMenu(int menuRes);
    void openKeyTerm(Term term);

    void reloadTranslatorFragment();
}
