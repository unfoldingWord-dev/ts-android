package com.door43.translationstudio.projects;

import com.door43.translationstudio.util.MainContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Meta projects are not real projects but virtual placeholders that help to organize
 * real projects into categories. Meta projects are basically linked lists that may chain together
 * multiple meta projects before terminating at a real project.
 */
public class PseudoProject implements Model {
    private final String mId;
//    private MetaProject mMetaChild;
//    private Project mProjectChild;
    private Map<String, Model> mChildrenMap = new HashMap<String, Model>();
    private Map<String, Translation> mTranslationMap = new HashMap<String, Translation>();
    private List<Translation> mTranslations = new ArrayList<Translation>();
    private String mSelectedTranslationId;

    /**
     * Creates a new meta project that contains a sub meta project
     * @param slug
     */
    public PseudoProject(String slug) {
        mId = slug;
    }

    public Translation getSelectedTranslation() {
        Translation selectedTranslation = getTranslation(mSelectedTranslationId);
        if(selectedTranslation == null) {
            // auto select the first chapter if no other chapter has been selected
            int defaultLanguageIndex = 0;
            setSelectedTranslation(defaultLanguageIndex);
            return getTranslation(defaultLanguageIndex);
        } else {
            return selectedTranslation;
        }
    }

    public boolean setSelectedTranslation(int index) {
        Translation t = getTranslation(index);
        if(t != null) {
            mSelectedTranslationId = t.getLanguage().getId();
//            storeSelectedSourceLanguage(mSelectedSourceLanguageId);
        }
        return t != null;
    }

    public boolean setSelectedTranslation(String id) {
        if(mTranslationMap.containsKey(id)) {
            mSelectedTranslationId = id;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the meta project id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the title of the sudo projects for the specified language
     * @param l
     * @return
     */
    public String getTitle(Language l) {
        Translation t = getTranslation(l.getId());
        if (t != null) {
            return t.getText();
        } else {
            return "";
        }
    }

    /**
     * Returns the title of the sudo project.
     * Sudo projects may contain different translations of it's title so we attempt to dynamically
     * determine which language to use.
     * @return
     */
    @Override
    public String getTitle() {
        Translation t;
        // try to use the current device language
        t = getTranslation(Locale.getDefault().getLanguage());
        if(t != null) {
            return t.getText();
        }

        // use the currently selected source language
        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p != null) {
            t = getTranslation(p.getSelectedSourceLanguage().getId());
            if (t != null) {
                return t.getText();
            }
        }

        // try to use english
        t = getTranslation("en");
        if(t != null) {
            return t.getText();
        }

        // just use the first language we find.
        t = getSelectedTranslation();
        if(t != null) {
            return t.getText();
        } else {
            return mId;
        }
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getImagePath() {
        return "";
    }

    @Override
    public String getDefaultImagePath() {
        return "";
    }

    @Override
    public boolean isTranslating() {
        for(Model m:mChildrenMap.values()) {
            if(!m.isTranslating()) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTranslatingGlobal() {
        for(Model m:mChildrenMap.values()) {
            if(!m.isTranslatingGlobal()) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getType() {
        return "meta-project";
    }

    /**
     * Checks if the meta project is the currently selected one.
     * This will be true of the selected project is somewhere within it's children.
     * @return
     */
    @Override
    public boolean isSelected() {
        for(Model m:mChildrenMap.values()) {
            if(!m.isSelected()) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a meta child to this meta project
     * @param meta
     */
    public void addChild(PseudoProject meta) {
        if(!mChildrenMap.containsKey("m-"+meta.getId())) {
            mChildrenMap.put("m-" + meta.getId(), meta);
        }
    }

    /**
     * Adds a project to the meta project.
     * @param child
     */
    public void addChild(Project child) {
        if(!mChildrenMap.containsKey(child.getId())) {
            mChildrenMap.put(child.getId(), child);
        }
    }

    /**
     * Returns the child of the meta project
     * @return
     */
    public Model[] getChildren() {
        return mChildrenMap.values().toArray(new Model[mChildrenMap.size()]);
    }

    /**
     * Returns a meta child by id
     * @param id
     * @return
     */
    public PseudoProject getMetaChild(String id) {
        if(mChildrenMap.containsKey("m-"+id)) {
            return (PseudoProject) mChildrenMap.get("m-"+id);
        } else {
            return null;
        }
    }

    /**
     * Adds a translation for this meta project
     * @param translation
     */
    public void addTranslation(Translation translation) {
        if(!mTranslationMap.containsKey(translation.getLanguage().getId())) {
            mTranslationMap.put(translation.getLanguage().getId(), translation);
            mTranslations.add(translation);
        }
    }

    /**
     * Returns a translation by id
     * @param id
     * @return
     */
    public Translation getTranslation(String id) {
        if(mTranslationMap.containsKey(id)) {
            return mTranslationMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns a translation by index
     * @param index
     * @return
     */
    public Translation getTranslation(int index) {
        if(index < mTranslations.size() && index >= 0) {
            return mTranslations.get(index);
        } else {
            return null;
        }
    }
}
