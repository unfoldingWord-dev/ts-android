package com.door43.translationstudio.newui.translate;

import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.ProjectTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;

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
    public TranslationFormat translationFormat;
    public boolean isComplete = false;

    protected TargetLanguage targetLanguage;
    protected ResourceContainer sourceContainer;
    protected ProjectTranslation pt;
    protected ChapterTranslation ct;
    protected FrameTranslation ft;

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
            String verseSpan = Frame.parseVerseTitle(sourceText, translationFormat);
            if(verseSpan.isEmpty()) {
                title += ":" + Integer.parseInt(chunkSlug);
            } else {
                title += ":" + verseSpan;
            }
            return title + " - " + targetLanguage.name;
        }
    }

    /**
     * Loads the translation text from the disk
     * @param sourceContainer
     * @param targetTranslation TODO: this will become a resource container eventually
     */
    public void loadTranslations(ResourceContainer sourceContainer, TargetTranslation targetTranslation) {
        this.pt = targetTranslation.getProjectTranslation();
        this.sourceContainer = sourceContainer;
        this.targetLanguage = targetTranslation.getTargetLanguage();
        this.renderedTargetText = null;
        this.renderedSourceText = null;
        if(this.sourceText == null) {
            this.sourceText = sourceContainer.readChunk(chapterSlug, chunkSlug);
        }
        this.translationFormat = TranslationFormat.parse(sourceContainer.contentMimeType);
        // TODO: 10/1/16 this will be simplified once we migrate target translations to resource containers
        if(chapterSlug.equals("front")) {
            // project stuff
            if (chunkSlug.equals("title")) {
                this.targetText = pt.getTitle();
                this.isComplete = pt.isTitleFinished();
            }
        } else if(chapterSlug.equals("back")) {
            // back matter

        } else {
            // chapter stuff
            this.ct = targetTranslation.getChapterTranslation(chapterSlug);
            if(chunkSlug.equals("title")) {
                this.targetText = ct.title;
                this.isComplete = ct.isTitleFinished();
            } else if(chunkSlug.equals("reference")) {
                this.targetText = ct.reference;
                this.isComplete = ct.isReferenceFinished();
            } else {
                this.ft = targetTranslation.getFrameTranslation(chapterSlug, chunkSlug, this.translationFormat);
                this.targetText = ft.body;
                this.isComplete = ft.isFinished();
            }
        }
    }
}
