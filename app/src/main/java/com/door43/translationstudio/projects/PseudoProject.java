package com.door43.translationstudio.projects;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.util.ListMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Meta projects are not real projects but virtual placeholders that help to organize
 * real projects into categories. Meta projects are basically linked lists that may chain together
 * multiple meta projects before terminating at a real project.
 */
@Deprecated
public class PseudoProject implements Model {
    private final String mId;
    private ListMap<Model> mChildren = new ListMap<>();
//    private Map<String, Model> mChildrenMap = new HashMap<String, Model>();
//    private List<Model> mChildren = new ArrayList<>();
    private Map<String, Translation> mTranslationMap = new HashMap<String, Translation>();
    private List<Translation> mTranslations = new ArrayList<Translation>();
    private String mSelectedTranslationId;
    private boolean mIsSorted;

    /**
     * Creates a new meta project that contains a sub meta project
     * @param slug
     */
    public PseudoProject(String slug) {
        mId = slug;
    }

    /**
     * Returns the selected translation of the title
     * @return
     */
    public Translation getSelectedTranslation() {
        // use the project source language if selected
        for(Model m:mChildren.values()) {
            if(!m.isSelected()) {
                continue;
            } else {
                SourceLanguage l = m.getSelectedSourceLanguage();
                setSelectedTranslation(l.getId());
                break;
            }
        }

        Translation selectedTranslation = getTranslation(mSelectedTranslationId);
        if(selectedTranslation == null) {
            // auto select the first language
            int defaultLanguageIndex = 0;
            setSelectedTranslation(defaultLanguageIndex);
            return getTranslation(defaultLanguageIndex);
        } else {
            return selectedTranslation;
        }
    }

    /**
     * Sets the translation to use for the title
     * @param index
     * @return
     */
    public boolean setSelectedTranslation(int index) {
        Translation t = getTranslation(index);
        if(t != null) {
            mSelectedTranslationId = t.getLanguage().getId();
        }
        return t != null;
    }

    /**
     * Sets the translation to use for the title
     * @param id
     * @return
     */
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
     * Pseudo projects may contain different translations of it's title so we attempt to dynamically
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
        Project p = null;//AppContext.projectManager().getSelectedProject();
        if(p != null && isSelected()) {
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

    /**
     * Pseudo project may contain multiple projects so we display the title of the selected project or an emtpy string
     * @return
     */
    @Override
    public String getDescription() {
//        if(isSelected() && AppContext.projectManager().getSelectedProject() != null) {
//            return AppContext.projectManager().getSelectedProject().getTerm();
//        } else {
//            return "";
//        }
        return "";
    }

    /**
     * Pseudo projects may contain multiple languages so we attempt to dynamically
     * determine which language to use
     * @return
     */
    @Override
    public SourceLanguage getSelectedSourceLanguage() {
        Translation t;
        // try to use the current device language
        t = getTranslation(Locale.getDefault().getLanguage());
        if(t != null) {
            return (SourceLanguage)t.getLanguage();
        }

        // use the currently selected source language of a child project
        Project p = null;//AppContext.projectManager().getSelectedProject();
        if(p != null && isSelected()) {
            t = getTranslation(p.getSelectedSourceLanguage().getId());
            if (t != null) {
                return (SourceLanguage)t.getLanguage();
            }
        }

        // try to use english
        t = getTranslation("en");
        if(t != null) {
            return (SourceLanguage)t.getLanguage();
        }

        // just use the first language we find.
        t = getSelectedTranslation();
        if(t != null) {
            return (SourceLanguage) t.getLanguage();
        } else {
            return null;
        }
    }

    /**
     * This just returns the first available sort key from it's children. This will be enough for sorting
     * @return
     */
    @Override
    public String getSortKey() {
        for(Model m:mChildren.values()) {
            if(m.getSortKey() != null) {
                return m.getSortKey();
            }
        }
        return null;
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
        for(Model m:mChildren.values()) {
            if(!m.isTranslating()) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTranslatingNotes() {
        for(Model m:mChildren.values()) {
            if(!m.isTranslatingNotes()) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTranslatingNotesGlobal() {
        for(Model m:mChildren.values()) {
            if(!m.isTranslatingNotesGlobal()) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTranslatingGlobal() {
        for(Model m:mChildren.values()) {
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
        for(Model m:mChildren.values()) {
            if(!m.isSelected()) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        return null;
    }

    /**
     * Adds a meta child to this meta project
     * @param meta
     */
    public void addChild(PseudoProject meta) {
        if(mChildren.get("m-"+meta.getId()) != null) {
            mChildren.add("m-" + meta.getId(), meta);
            mIsSorted = false;
        }
    }

    /**
     * Adds a project to the meta project.
     * Previous versions of the project will be replaced to ensure the latest translations are loaded
     * @param child
     */
    public void addChild(Project child) {
        mChildren.replace(child.getId(), child);
        mIsSorted = false;
    }

    /**
     * Returns the child of the meta project
     * @return
     */
    public Model[] getChildren() {
        return mChildren.getAll().toArray(new Model[mChildren.size()]);
    }

    /**
     * Returns a meta child by id
     * @param id
     * @return
     */
    public PseudoProject getMetaChild(String id) {
        if(mChildren.get("m-"+id) != null) {
            return (PseudoProject) mChildren.get("m-"+id);
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

    /**
     * Sorts the children of this Pseudo project
     */
    public void sortChildren() {
        // only sort if needed
        if(!mIsSorted) {
            Collections.sort(mChildren.list(), new Comparator<Model>() {
                @Override
                public int compare(Model model, Model model2) {
                    try {
                        // sort children
                        if (model.getClass().getName().equals(PseudoProject.class.getName())) {
                            ((PseudoProject) model).sortChildren();
                        }
                        if (model2.getClass().getName().equals(PseudoProject.class.getName())) {
                            ((PseudoProject) model2).sortChildren();
                        }
                        // sort models
                        int i = Integer.parseInt(model.getSortKey());
                        int i2 = Integer.parseInt(model2.getSortKey());
                        return i - i2;
                    } catch (Exception e) {
                        Logger.e(this.getClass().getName(), "unable to sort models", e);
                        return 0;
                    }
                }
            });
        }
    }
}
