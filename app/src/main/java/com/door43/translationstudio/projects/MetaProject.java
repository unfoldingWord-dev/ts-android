package com.door43.translationstudio.projects;

import java.util.HashMap;
import java.util.Map;

/**
 * Meta projects are not real projects but virtual placeholders that help to organize
 * real projects into categories. Meta projects are basically linked lists that may chain together
 * multiple meta projects before terminating at a real project.
 */
public class MetaProject {
    private final String mSlug;
//    private MetaProject mMetaChild;
//    private Project mProjectChild;
    private Map<String, Object> mChildrenMap = new HashMap<String, Object>();
    private Map<String, Translation> mLanguageMap = new HashMap<String, Translation>();

    /**
     * Creates a new meta project that contains a sub meta project
     * @param slug
     */
    public MetaProject(String slug) {
        mSlug = slug;
    }


    /**
     * Returns the meta project id
     * @return
     */
    public String getId() {
        return mSlug;
    }

    /**
     * Adds a meta child to this meta project
     * @param meta
     */
    public void addChild(MetaProject meta) {
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
    public Object[] getChildren() {
        return mChildrenMap.values().toArray();
    }

    /**
     * Returns a meta child by id
     * @param id
     * @return
     */
    public MetaProject getMetaChild(String id) {
        if(mChildrenMap.containsKey("m-"+id)) {
            return (MetaProject) mChildrenMap.get("m-"+id);
        } else {
            return null;
        }
    }

    /**
     * Adds a translation for this meta project
     * @param translation
     */
    public void addTranslation(Translation translation) {
        if(!mLanguageMap.containsKey(translation.getLanguage().getId())) {
            mLanguageMap.put(translation.getLanguage().getId(), translation);
        }
    }
}
