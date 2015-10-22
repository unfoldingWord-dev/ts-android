package com.door43.translationstudio.core;

/**
 * A project category can represent a project or a category of projects.
 */
public class ProjectCategory {
    public final String projectId;
    public final String categoryId;
    public final long parentCategoryId;
    public final String title;
    public final String sourcelanguageId;

    /**
     *
     * @param title
     * @param categoryId
     * @param projectId
     * @param sourceLanguageId the id of the source languaged to which the project title belongs
     * @param parentCategoryId
     */
    public ProjectCategory(String title, String categoryId, String projectId, String sourceLanguageId , long parentCategoryId) {
        this.title = title;
        this.categoryId = categoryId;
        this.projectId = projectId;
        this.sourcelanguageId = sourceLanguageId;
        this.parentCategoryId = parentCategoryId;
    }

    /**
     * Returns the project category id
     *
     * This is different from the category id and the project id.
     * This id will uniquely identify this ProjectCategory from all categories and projects.
     *
     * @return
     */
    public String getId() {
        if(categoryId == null) {
            return "cat-" + projectId;
        } else {
            return categoryId;
        }
    }

    /**
     * Checks if this category represents a single project
     *
     * If this returns false then one or more projects are categorized under this category
     *
     * @return
     */
    public Boolean isProject() {
        return categoryId == null;
    }
}
