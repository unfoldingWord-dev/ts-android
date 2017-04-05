package com.door43.translationstudio.core;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ContainerTools;
import org.unfoldingword.resourcecontainer.Link;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.resourcecontainer.errors.InvalidRCException;
import org.unfoldingword.tools.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a cache of resource containers.
 * This should usually only be used to load source containers since they will not change very often.
 */
public class ContainerCache {
    /**
     * A map of cached containers
     */
    private Map<String, ResourceContainer> resourceContainers = new ConcurrentHashMap<>();

    /**
     * A base for the synchronized list below
      */
    private List<String> inspected_list = new ArrayList<>();

    /**
     * A list of container slugs that have already been searched for
     */
    private List<String> inspectedContainers = Collections.synchronizedList(inspected_list);

    /**
     * A base for the synchronized list below
     */
    private List<String> inspecting_list = new ArrayList<>();

    /**
     * A list of container slugs that are currently being inspected
     */
    private List<String> loadingContainers = Collections.synchronizedList(inspecting_list);

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
     * Caches resource container if it exists
     * If the container has already been cached it will not touch the disk.
     *
     * @param client
     * @param resourceContainerSlug
     * @return
     */
    public static ResourceContainer cache(Door43Client client, String resourceContainerSlug) {
        // wait for other threads
        waitForLoadingContainers(resourceContainerSlug);

        // check cache
        if (sInstance.resourceContainers.containsKey(resourceContainerSlug)) {
            return sInstance.resourceContainers.get(resourceContainerSlug);
        }

        // load from disk once
        if(!sInstance.inspectedContainers.contains(resourceContainerSlug)) {
            // flag as loading
            sInstance.loadingContainers.add(resourceContainerSlug);
            try {
                ResourceContainer rc = client.open(resourceContainerSlug);
                sInstance.resourceContainers.put(rc.slug, rc);
                return rc;
            } catch (InvalidRCException e) {
                e.printStackTrace();
                // delete invalid container
                client.delete(resourceContainerSlug);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // fag as inspected
                sInstance.inspectedContainers.add(resourceContainerSlug);
                // remove loading flag
                sInstance.loadingContainers.remove(resourceContainerSlug);
            }
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

        // search for translation
        List<Translation> translations = client.index.findTranslations(languageSlug, projectSlug, resourceSlug, null, null, 0, -1);
        if (translations.size() == 0) {
            // search for similar translations
            translations = client.index.findTranslations(null, projectSlug, resourceSlug, null, null, 0, -1);
        }

        // return first successful cache
        for (Translation translation : translations) {
            ResourceContainer rc = cache(client, translation.resourceContainerSlug);
            if(rc != null) return rc;
        }
        return null;
    }

    /**
     * Puts the thread to sleep while the container is loading
     * @param containerSlug the translation to load
     */
    private static void waitForLoadingContainers(String containerSlug) {
        while (sInstance.loadingContainers.contains(containerSlug)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Looks up a resource container from the cache without hitting the disk
     * @return
     */
    public static ResourceContainer get(String containerSlug) {
        waitForLoadingContainers(containerSlug);
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
                } else {
                    Logger.w("ContainerCache", "RC not found for link " + rawLink);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return links;
    }

    /**
     * Removes a resource container from the cache
     * @param resourceContainerSlug the slug of the resource container that will be removed
     */
    public static void remove(String resourceContainerSlug) {
        // TODO: 2/8/17 this could be slow since these are synchronized lists.
        sInstance.resourceContainers.remove(resourceContainerSlug);
        sInstance.inspectedContainers.remove(resourceContainerSlug);
    }
}
