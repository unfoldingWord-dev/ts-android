package com.door43.translationstudio.ui.translate;

import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.FileHistory;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.ProjectTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.tasks.MergeConflictsParseTask;

import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.resourcecontainer.ResourceContainer;

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
     * @param targetTranslation
     * @return
     */
    public FileHistory getFileHistory(TargetTranslation targetTranslation) {
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
     * Returns the title of the list item
     * @return
     */
    public String getTargetTitle() {
        if(isProjectTitle()) {
            return targetLanguage.name;
        } else if(isChapter()) {
            if(!pt.getTitle().trim().isEmpty()) {
                return pt.getTitle().trim() + " - " + targetLanguage.name;
            } else {
                return sourceContainer.project.name.trim() + " - " + targetLanguage.name;
            }
        } else {
            // use chapter title
            String title = ct.title.trim();
            if(title.isEmpty()) {
                title = sourceContainer.readChunk(chapterSlug, "title").trim();
            }
            // use project title
            if(title.isEmpty()) {
                title = pt.getTitle().trim();
                if(title.isEmpty()) {
                    title = sourceContainer.project.name.trim();
                }
                title += " " + Integer.parseInt(chapterSlug);
            }
            String verseSpan = Frame.parseVerseTitle(sourceText, targetTranslationFormat);
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
            this.pt = targetTranslation.getProjectTranslation();
            this.sourceContainer = sourceContainer;
            this.targetLanguage = targetTranslation.getTargetLanguage();
            this.renderedTargetText = null;
            this.renderedSourceText = null;
            if (this.sourceText == null) {
                this.sourceText = sourceContainer.readChunk(chapterSlug, chunkSlug);
            }
            this.sourceTranslationFormat = TranslationFormat.parse(sourceContainer.contentMimeType);
            this.targetTranslationFormat = targetTranslation.getFormat();
            // TODO: 10/1/16 this will be simplified once we migrate target translations to resource containers
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
            this.hasMergeConflicts = MergeConflictsParseTask.isMergeConflicted(this.targetText);
        }
    }
}
