package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.ProjectTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.publish.ValidationItem;
import com.door43.translationstudio.rendering.MergeConflictHandler;
import com.door43.util.StringUtilities;

import org.json.JSONException;
import org.unfoldingword.door43client.models.*;
import org.unfoldingword.door43client.Door43Client;

import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Performs the validation on a target translation.
 * This process should occur before a target translation is published.
 */
public class ValidationTask extends ManagedTask {
    public static final String TASK_ID = "validation_task";
    private final String mTargetTranslationId;
    private final String mSourceTranslationId;
    private List<ValidationItem> mValidations = new ArrayList<>();

    public ValidationTask(String targetTranslationId, String sourceTranslationId) {
        mTargetTranslationId = targetTranslationId;
        mSourceTranslationId = sourceTranslationId;
    }

    @Override
    public void start() {
        Door43Client library = App.getLibrary();
        Translator translator = App.getTranslator();

        TargetTranslation targetTranslation = translator.getTargetTranslation(mTargetTranslationId);
        TargetLanguage targetLanguage = library.index().getTargetLanguage(targetTranslation.getTargetLanguageId());

        ResourceContainer container;
        try {
            container = library.open(mSourceTranslationId);
        } catch (Exception e) {
            Logger.e("ValidationTask", "Failed to load resource container", e);
            return;
        }
        TranslationFormat format;
        try {
            format = TranslationFormat.parse(container.info.getString("content_mime_type"));
        } catch (JSONException e) {
            Logger.e("ValidationTask", "Failed to read the translation format from the container", e);
            return;
        }
        String projectTitle = "";
        projectTitle = container.readChunk("front", "title");
        SourceLanguage sourceLanguage = library.index().getSourceLanguage(container.language.slug);
        String[] chapters = container.chapters();

        // validate chapters
        int lastValidChapterIndex = -1;
        List<ValidationItem> chapterValidations = new ArrayList<>();

        ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();

        for(int i = 0; i < chapters.length; i ++) {
            String chapterSlug = chapters[i];
            List<String> chunks = new ArrayList(Arrays.asList(container.chunks(chapterSlug)));

            // validate project title
            if(chapterSlug.equals("front")) {
                if (MergeConflictHandler.isMergeConflicted(projectTranslation.getTitle()) || chunks.contains("title") && !projectTranslation.isTitleFinished()) {
                    mValidations.add(ValidationItem.generateInvalidGroup(projectTitle, sourceLanguage));
                    mValidations.add(ValidationItem.generateInvalidFrame(projectTitle, sourceLanguage, projectTranslation.getTitle(), targetLanguage, TranslationFormat.DEFAULT, mTargetTranslationId, "0", "0"));
                }
            }

            // validate frames
            int lastValidFrameIndex = -1;
            boolean chapterIsValid = true;
            List<ValidationItem> frameValidations = new ArrayList<>();

            ChapterTranslation chapterTranslation = targetTranslation.getChapterTranslation(chapterSlug);
            if(MergeConflictHandler.isMergeConflicted(chapterTranslation.title) || chunks.contains("title") && !chapterTranslation.isTitleFinished()) {
                chapterIsValid = false;
                frameValidations.add(ValidationItem.generateInvalidFrame(container.readChunk(chapterSlug, "title"), sourceLanguage, chapterTranslation.title, targetLanguage, TranslationFormat.DEFAULT, mTargetTranslationId, chapterSlug, "00"));
            }

            if(MergeConflictHandler.isMergeConflicted(chapterTranslation.reference) || chunks.contains("reference") && !chapterTranslation.isReferenceFinished()) {
                chapterIsValid = false;
                    frameValidations.add(ValidationItem.generateInvalidFrame(container.readChunk(chapterSlug, "reference"), sourceLanguage, chapterTranslation.reference, targetLanguage, TranslationFormat.DEFAULT, mTargetTranslationId, chapterSlug, "00"));
            }

            for(int j = 0; j < chunks.size(); j ++) {
                String chunkSlug = chunks.get(j);
                FrameTranslation frameTranslation = targetTranslation.getFrameTranslation(chapterSlug, chunkSlug, format);
                String chunkText;
                chunkText = container.readChunk(chapterSlug, chunkSlug);
                // TODO: also validate the checking questions
                if(lastValidFrameIndex == -1 && (frameTranslation.isFinished() || chunkText.isEmpty())) {
                    // start new valid range
                    lastValidFrameIndex = j;
                } else if(MergeConflictHandler.isMergeConflicted(frameTranslation.body) || !(frameTranslation.isFinished() || chunkText.isEmpty()) || (frameTranslation.isFinished() || chunkText.isEmpty()) && j == chunks.size() - 1){
                    // close valid range
                    if(lastValidFrameIndex > -1) {
                        int previousFrameIndex = j - 1;
                        if(frameTranslation.isFinished() || chunkText.isEmpty()) {
                            previousFrameIndex = j;
                        }
                        if(lastValidFrameIndex < previousFrameIndex) {
                            // range
                            String previousFrame = "";
                            previousFrame = container.readChunk(chapterSlug, chunks.get(previousFrameIndex));
                            String lastValidText = "";
                            lastValidText = container.readChunk(chapterSlug, chunks.get(lastValidFrameIndex));
                            String frameTitle = projectTitle + " " + StringUtilities.formatNumber(chapterSlug);
                            frameTitle += ":" + Frame.getStartVerse(lastValidText, format) + "-" + Frame.getEndVerse(previousFrame, format);
                            frameValidations.add(ValidationItem.generateValidFrame(frameTitle, sourceLanguage, true));
                        } else {
                            String lastValidFrame = chunks.get(lastValidFrameIndex);
                            String lastValidText = "";
                            lastValidText = container.readChunk(chapterSlug, chunks.get(lastValidFrameIndex));
                            String frameTitle = projectTitle + " " + StringUtilities.formatNumber(chapterSlug);
                            frameTitle += ":" + Frame.getStartVerse(lastValidText, format);
                            if(!Frame.getStartVerse(lastValidText, format).equals(Frame.getEndVerse(lastValidText, format))) {
                                frameTitle += "-" + Frame.getEndVerse(lastValidText, format);
                            }
                            frameValidations.add(ValidationItem.generateValidFrame(frameTitle, sourceLanguage, false));
                        }
                        lastValidFrameIndex = -1;
                    }

                    // add invalid frame
                    if(!(frameTranslation.isFinished() || chunkText.isEmpty())) {
                        chapterIsValid = false;
                        String frameTitle = projectTitle + " " + StringUtilities.formatNumber(chapterSlug);
                        frameTitle += ":" + Frame.getStartVerse(chunkText, format);
                        if (!Frame.getStartVerse(chunkText, format).equals(Frame.getEndVerse(chunkText, format))) {
                            frameTitle += "-" + Frame.getEndVerse(chunkText, format);
                        }
                        frameValidations.add(ValidationItem.generateInvalidFrame(frameTitle, sourceLanguage, frameTranslation.body,
                                targetLanguage, frameTranslation.getFormat(), mTargetTranslationId, chapterSlug, chunkSlug));
                    }
                }

            }
            if(lastValidChapterIndex == -1 && chapterIsValid) {
                // start new valid range
                lastValidChapterIndex = i;
            } else if(!chapterIsValid || chapterIsValid && i == chapters.length - 1) {
                // close valid range
                if(lastValidChapterIndex > -1) {
                    int previousChapterIndex = i - 1;
                    if(chapterIsValid) {
                        previousChapterIndex = i;
                    }
                    if(lastValidChapterIndex < previousChapterIndex) {
                        // range
                        String previousChapterSlug = chapters[previousChapterIndex];
                        String lastValidChapterSlug = chapters[lastValidChapterIndex];
                        String chapterTitle = projectTitle + " " + StringUtilities.formatNumber(lastValidChapterSlug) + "-" + StringUtilities.formatNumber(previousChapterSlug);
                        chapterValidations.add(ValidationItem.generateValidGroup(chapterTitle, sourceLanguage, true));
                    } else {
                        String lastValidChapter = chapters[lastValidChapterIndex];
                        String chapterTitle  = projectTitle + " " + StringUtilities.formatNumber(lastValidChapter);
                        chapterValidations.add(ValidationItem.generateValidGroup(chapterTitle, sourceLanguage, false));
                    }
                    lastValidChapterIndex = -1;
                }

                // add invalid chapter
                if(!chapterIsValid) {
                    String chapterTitle = "";
                    chapterTitle = container.readChunk(chapterSlug, "title");
                    if (chapterTitle.isEmpty()) {
                        chapterTitle = projectTitle + " " + StringUtilities.formatNumber(chapterSlug);
                    }
                    chapterValidations.add(ValidationItem.generateInvalidGroup(chapterTitle, sourceLanguage));

                    // add frame validations
                    chapterValidations.addAll(frameValidations);
                }
            }
        }

        // close validations
        if(chapterValidations.size() > 1) {
            mValidations.addAll(chapterValidations);
        } else {
            mValidations.add(ValidationItem.generateValidGroup(projectTitle, sourceLanguage, true));
        }
    }

    /**
     * Returns an array of validation items
     *
     * @return
     */
    public ValidationItem[] getValidations() {
        return mValidations.toArray(new ValidationItem[mValidations.size()]);
    }
}
