package com.door43.translationstudio.core;

import org.unfoldingword.door43client.models.SourceLanguage;
import org.unfoldingword.resourcecontainer.ContainerTools;
import org.unfoldingword.resourcecontainer.Resource;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.resourcecontainer.ResourceContainer;

/**
 * Source translations are a special abstraction that represent the concept of a resource container.
 * A source translation is composed of a single source language, project, and resource.
 * As such a source translation uniquely represents a single resource container,
 * though the existence of a source translation does not demand the existence of a resource container.
 *
 */
public class SourceTranslation {
    public final SourceLanguage language;
    public final Project project;
    public final Resource resource;

    /**
     * The slug of the resource container represented by this source translation
     */
    public final String resourceContainerSlug;

    @Deprecated
    private final String mVersion;
    @Deprecated
    private final TranslationFormat format;

    public SourceTranslation(SourceLanguage language, Project project, Resource resource) {
        this.language = language;
        this.project = project;
        this.resource = resource;

        resourceContainerSlug = ContainerTools.makeSlug(language.slug, project.slug, resource.slug);

        mVersion = null;
        format = null;
    }

    /**
     * Creates a new source translation from a resource container
     * @param container
     */
    public SourceTranslation(ResourceContainer container) {
        this.language = new SourceLanguage(container.language.slug, container.language.name, container.language.direction);
        this.project = container.project;
        this.resource = container.resource;

        resourceContainerSlug = ContainerTools.makeSlug(language.slug, project.slug, resource.slug);


        mVersion = null;
        format = null;
    }



    /**
     * Returns the translation format of this source translation.
     * @return
     */
    public TranslationFormat getFormat() {
        return format;
    }


    /**
     * Returns the project id
     * @param sourceTranslationId
     * @return
     */
    public static String getProjectIdFromId(String sourceTranslationId) {
        String[] complexId = sourceTranslationId.split("-", 3);
        if(complexId.length == 3) {
            return complexId[0];
        } else {
            throw new StringIndexOutOfBoundsException("malformed source translation id" + sourceTranslationId);
        }
    }

    /**
     * Returns the source language id
     * @param sourceTranslationId
     * @return
     */
    public static String getSourceLanguageIdFromId(String sourceTranslationId) {
        String[] complexId = sourceTranslationId.split("-");
        if(complexId.length >= 3) {
            // TRICKY: source language id's can have dashes in them.
            String sourceLanguageId = complexId[1];
            for(int i = 2; i < complexId.length - 1; i ++) {
                sourceLanguageId += "-" + complexId[i];
            }
            return sourceLanguageId;
        } else {
            throw new StringIndexOutOfBoundsException("malformed source translation id" + sourceTranslationId);
        }
    }

    /**
     * Returns the resource id
     * @param sourceTranslationId
     * @return
     */
    public static String getResourceIdFromId(String sourceTranslationId) {
        String[] complexId = sourceTranslationId.split("-");
        if(complexId.length >= 3) {
            return complexId[complexId.length - 1];
        } else {
            throw new StringIndexOutOfBoundsException("malformed source translation id" + sourceTranslationId);
        }
    }
}
