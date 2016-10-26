package com.door43.translationstudio.core;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ContainerTools;
import org.unfoldingword.resourcecontainer.Link;
import org.unfoldingword.resourcecontainer.ResourceContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides a cache of resource containers.
 * This should usually only be used to load source containers since they will not change very often.
 */
public class ContainerCache {
    /**
     * A map of cached containers
     */
    private Map<String, ResourceContainer> resourceContainers = new HashMap<>();
    /**
     * A map of translation slugs and their container (or closest match)
     */
    private Map<String, String> translationToContainerSlugs = new HashMap<>();
    /**
     * A list of container slugs that have already been searched for
     */
    private List<String> inspectedContainers = new ArrayList<>();
    /**
     * A list of translations hat have already been searched for
     */
    private List<String> inspectedTranslations = new ArrayList<>();

    private static ContainerCache sInstance = null;

    static {
        sInstance = new ContainerCache();
    }

    /**
     * Empties the cache
     */
    public static void empty() {
        sInstance.resourceContainers.clear();
        sInstance.inspectedContainers.clear();
    }

    /**
     * Caches an exact match to a resource container if one exists.
     * @param client
     * @param resourceContainerSlug
     * @return
     */
    public static ResourceContainer cache(Door43Client client, String resourceContainerSlug) {
        // check the cache first
        if(sInstance.resourceContainers.containsKey(resourceContainerSlug)) {
            return sInstance.resourceContainers.get(resourceContainerSlug);
        }
        // cache it
        try {
            ResourceContainer rc = client.open(resourceContainerSlug);
            sInstance.resourceContainers.put(rc.slug, rc);
            return rc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Looks up a resource container from the cache or loads a new one from the disk.
     * If an exact match cannot be found then the closed matching resource container for the project
     * will be cached and returned.
     *
     * @param client
     * @param languageSlug the desired language or null. If null the default system language will be used.
     * @param projectSlug
     * @param resourceSlug
     * @return If the container can not be found null is returned.
     */
    public static ResourceContainer cacheClosest(Door43Client client, String languageSlug, String projectSlug, String resourceSlug) {
        if(languageSlug == null || languageSlug.isEmpty()) languageSlug = Locale.getDefault().getLanguage();
        String translationSlug = ContainerTools.makeSlug(languageSlug, projectSlug, resourceSlug);
        // attempt to cache the container
        if(!sInstance.inspectedTranslations.contains(translationSlug)) {
            sInstance.inspectedTranslations.add(translationSlug);
            List<Translation> translations = client.index().findTranslations(languageSlug, projectSlug, resourceSlug, null, null, 0, -1);
            if (translations.size() == 0) {
                // try to find any language
                translations = client.index().findTranslations(null, projectSlug, resourceSlug, null, null, 0, -1);
            }
            // load the first available container
            for (Translation translation : translations) {
                if(!sInstance.inspectedContainers.contains(translation.resourceContainerSlug)) {
                    // only inspect container once
                    sInstance.inspectedContainers.add(translation.resourceContainerSlug);
                    try {
                        ResourceContainer rc = client.open(translation.resourceContainerSlug);
                        sInstance.resourceContainers.put(rc.slug, rc);
                        sInstance.translationToContainerSlugs.put(translationSlug, rc.slug);
                        // TRICKY: we only need the first match
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if(sInstance.translationToContainerSlugs.containsKey(translationSlug)) {
            String containerSlug = sInstance.translationToContainerSlugs.get(translationSlug);
            return sInstance.resourceContainers.get(containerSlug);
        }
        return null;
    }

    /**
     * Looks up a resource container from the cache without hitting the disk
     * @return
     */
    public static ResourceContainer get(String containerSlug) {
        if(sInstance.resourceContainers.containsKey(containerSlug)) {
            return sInstance.resourceContainers.get(containerSlug);
        }
        return null;
    }

    /**
     * Parses an array of links and caches the needed resource containers.
     * Links that have a matching container will be returned.
     *
     * @param client
     * @param linkData
     * @return
     */
    public static List<Link> cacheClosestFromLinks(Door43Client client, List<String> linkData) {
        List<Link> links = new ArrayList<>();
        for(String rawLink:linkData) {
            try {
                Link link = Link.parseLink(rawLink);
                ResourceContainer container = ContainerCache.cacheClosest(client, link.language, link.project, link.resource);
                if(container != null) {
                    links.add(link);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return links;
    }
}