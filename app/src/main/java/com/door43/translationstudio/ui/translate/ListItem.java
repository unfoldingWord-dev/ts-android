package com.door43.translationstudio.ui.translate;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.core.FileHistory;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.MergeConflictsHandler;
import com.door43.translationstudio.core.ProjectTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;

import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single row in the translation list
 */

public abstract class ListItem {
    public final String chapterSlug;
    public final String chunkSlug;

    public String sourceText;
    public CharSequence renderedSourceText = null;
    public String targetText;
    public CharSequence renderedTargetText = null;
    public TranslationFormat sourceTranslationFormat;
    public TranslationFormat targetTranslationFormat;
    public boolean isComplete = false;
    public boolean hasMergeConflicts = false;
    public boolean isEditing = false;

    protected TargetLanguage targetLanguage;
    protected ResourceContainer sourceContainer;
    protected ProjectTranslation pt;
    protected ChapterTranslation ct;
    protected FrameTranslation ft;
    protected FileHistory fileHistory = null;
    private TargetTranslation targetTranslation;

    /**
     * Initializes a new list item
     * @param chapterSlug
     * @param chunkSlug
     */
    protected ListItem(String chapterSlug, String chunkSlug) {
        this.chapterSlug = chapterSlug;
        this.chunkSlug = chunkSlug;
    }

    public boolean isChunk() {
        return !isChapter() && !isProjectTitle();
    }

    public boolean isChapter() {
        return isChapterReference() || isChapterTitle();
    }

    public boolean isProjectTitle() {
        return chapterSlug.equals("front") && chunkSlug.equals("title");
    }

    public boolean isChapterTitle() {
        return !chapterSlug.equals("front") && !chapterSlug.equals("back") && chunkSlug.equals("title");
    }

    public boolean isChapterReference() {
        return !chapterSlug.equals("front") && !chapterSlug.equals("back") && chunkSlug.equals("reference");
    }

    /**
     * Loads the file history or returns it from the cache
     * @return
     */
    public FileHistory getFileHistory() {
        if(this.fileHistory != null) {
            return this.fileHistory;
        }

        FileHistory history = null;
        if(this.isChapterReference()) {
            history = targetTranslation.getChapterReferenceHistory(this.ct);
        } else if(this.isChapterTitle()) {
            history = targetTranslation.getChapterTitleHistory(this.ct);
        } else if(this.isProjectTitle()) {
            history = targetTranslation.getProjectTitleHistory();
        } else if(this.isChunk()) {
            history = targetTranslation.getFrameHistory(this.ft);
        }
        this.fileHistory = history;
        return history;
    }

    /**
     * Removes merge conflicts in text (uses first option)
     * @param text
     * @return
     */
    private String removeConflicts(String text) {
        if(MergeConflictsHandler.isMergeConflicted(text)) {
            CharSequence unConflictedText = MergeConflictsHandler.getMergeConflictItemsHead(text);
            if(unConflictedText == null) {
                unConflictedText = "";
            }
            return unConflictedText.toString();
        }
        return text;
    }

    /**
     * Returns the title of the list item
     * @return
     */
    public String getTargetTitle() {
        if(isProjectTitle()) {
            return removeConflicts(targetLanguage.name);
        } else if(isChapter()) {
            String ptTitle = removeConflicts(pt.getTitle()).trim();
            if(!ptTitle.isEmpty()) {
                return ptTitle + " - " + targetLanguage.name;
            } else {
                return removeConflicts(sourceContainer.project.name).trim() + " - " + targetLanguage.name;
            }
        } else {
            // use project title
            String title = "";
            if(pt != null) {
                title = removeConflicts(pt.getTitle()).trim();
            } else {
                Logger.w("ListItem", "missing project translation for " + targetTranslation.getId());
            }
            if(title.isEmpty()) {
                title = removeConflicts(sourceContainer.project.name).trim();
            }
            title += " " + Integer.parseInt(chapterSlug);

            String verseSpan = Frame.parseVerseTitle(sourceText, sourceTranslationFormat);
            if(verseSpan.isEmpty()) {
                title += ":" + Integer.parseInt(chunkSlug);
            } else {
                title += ":" + verseSpan;
            }
            return title + " - " + targetLanguage.name;
        }
    }

    /**
     * Clears the loaded translation data
     */
    public void reset() {
        this.sourceText = null;
        this.targetText = null;
        this.renderedSourceText = null;
        this.renderedTargetText = null;
        this.hasMergeConflicts = false;
        sourceContainer = null;
        targetLanguage = null;
    }

    /**
     * Loads the translation text from the disk.
     * This will not do anything if the sourceText is already loaded
     *
     * @param sourceContainer
     * @param targetTranslation TODO: this will become a resource container eventually
     */
    public void load(ResourceContainer sourceContainer, TargetTranslation targetTranslation) {
        if(this.sourceText == null) {
            this.sourceContainer = sourceContainer;
            this.targetLanguage = targetTranslation.getTargetLanguage();
            this.renderedTargetText = null;
            this.renderedSourceText = null;
            if (this.sourceText == null) {
                this.sourceText = sourceContainer.readChunk(chapterSlug, chunkSlug);
            }
            this.sourceTranslationFormat = TranslationFormat.parse(sourceContainer.contentMimeType);
            this.targetTranslationFormat = targetTranslation.getFormat();
            loadTarget(targetTranslation);
        }
    }

    /**
     * used for reloading target translation to get any changes from file
     * @param targetTranslation
     */
    public void loadTarget(TargetTranslation targetTranslation) {
        // TODO: 10/1/16 this will be simplified once we migrate target translations to resource containers
        this.targetTranslation = targetTranslation;
        this.pt = targetTranslation.getProjectTranslation();
        if (chapterSlug.equals("front")) {
            // project stuff
            if (chunkSlug.equals("title")) {
                this.targetText = pt.getTitle();
                this.isComplete = pt.isTitleFinished();
            }
        } else if (chapterSlug.equals("back")) {
            // back matter

        } else {
            // chapter stuff
            this.ct = targetTranslation.getChapterTranslation(chapterSlug);
            if (chunkSlug.equals("title")) {
                this.targetText = ct.title;
                this.isComplete = ct.isTitleFinished();
            } else if (chunkSlug.equals("reference")) {
                this.targetText = ct.reference;
                this.isComplete = ct.isReferenceFinished();
            } else {
                this.ft = targetTranslation.getFrameTranslation(chapterSlug, chunkSlug, this.targetTranslationFormat);
                this.targetText = ft.body;
                this.isComplete = ft.isFinished();
            }
        }
        this.hasMergeConflicts = MergeConflictsHandler.isMergeConflicted(this.targetText);
    }

    public ResourceContainer getSource() {
        return sourceContainer;
    }

    /**
     * Returns the config options for a chunk
     * @return
     */
    public Map<String, List<String>> getChunkConfig() {
        if(sourceContainer != null) {
            Map config = null;
            if(sourceContainer.config == null || !sourceContainer.config.containsKey("content") || !(sourceContainer.config.get("content") instanceof Map)) {
                // TODO: look in the UST, then UDB, then english UST, then english UDB for the config.
                return new HashMap<>();
                // default to english if no config is found
//                ResourceContainer rc = ContainerCache.cacheClosest(App.getLibrary(), "en", sourceContainer.project.slug, sourceContainer.resource.slug);
//                if(rc != null) config = rc.config;
            } else {
                config = sourceContainer.config;
            }

            // look up config for chunk
            if (config != null && config.containsKey("content") && config.get("content") instanceof Map) {
                Map contentConfig = (Map<String, Object>) config.get("content");
                if (contentConfig.containsKey(chapterSlug)) {
                    Map chapterConfig = (Map<String, Object>) contentConfig.get(chapterSlug);
                    if (chapterConfig.containsKey(chunkSlug)) {
                        return (Map<String, List<String>>) chapterConfig.get(chunkSlug);
                    }
                }
            }
        }
        return new HashMap<>();
    }

    public TargetTranslation getTarget() {
        return targetTranslation;
    }
}
