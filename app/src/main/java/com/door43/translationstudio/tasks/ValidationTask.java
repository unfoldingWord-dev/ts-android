package com.door43.translationstudio.tasks;

import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.publish.ValidationItem;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 9/20/2015.
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
        // TODO: perform some real validation here
        Library library = AppContext.getLibrary();
        Translator translator = AppContext.getTranslator();

        TargetTranslation targetTranslation = translator.getTargetTranslation(mTargetTranslationId);
        SourceTranslation sourceTranslation = library.getSourceTranslation(mSourceTranslationId);
        Chapter[] chapters = library.getChapters(sourceTranslation);

        // validate chapters
        int lastValidChapterIndex = -1;
        List<ValidationItem> chapterValidations = new ArrayList<>();
        for(int i = 0; i < chapters.length; i ++) {
            Chapter chapter = chapters[i];
            // TODO: validate title and reference
            Frame[] frames = library.getFrames(sourceTranslation, chapter.getId());

            // validate frames
            int lastValidFrameIndex = -1;
            boolean chapterIsValid = true;
            List<ValidationItem> frameValidations = new ArrayList<>();
            for(int j = 0; j < frames.length; j ++) {
                Frame frame = frames[j];
                FrameTranslation frameTranslation = targetTranslation.getFrameTranslation(frame);
                // TODO: also validate the checking questions
                if(lastValidFrameIndex == -1 && frameTranslation.isFinished()) {
                    // start new valid range
                    lastValidFrameIndex = j;
                } else if(!frameTranslation.isFinished() || frameTranslation.isFinished() && j == frames.length - 1){
                    // close valid range
                    if(lastValidFrameIndex > -1) {
                        int previousFrameIndex = j - 1;
                        if(frameTranslation.isFinished()) {
                            previousFrameIndex = j;
                        }
                        if(lastValidFrameIndex < previousFrameIndex) {
                            // range
                            Frame previousFrame = frames[previousFrameIndex];
                            Frame lastValidFrame = frames[lastValidFrameIndex];
                            String frameTitle = sourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
                            frameTitle += ":" + lastValidFrame.getStartVerse() + "-" + previousFrame.getEndVerse();
                            frameValidations.add(ValidationItem.generateValidFrame(frameTitle, true));
                        } else {
                            Frame lastValidFrame = frames[lastValidFrameIndex];
                            String frameTitle = sourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
                            frameTitle += ":" + lastValidFrame.getStartVerse();
                            if(!lastValidFrame.getStartVerse().equals(lastValidFrame.getEndVerse())) {
                                frameTitle += "-" + lastValidFrame.getEndVerse();
                            }
                            frameValidations.add(ValidationItem.generateValidFrame(frameTitle, false));
                        }
                        lastValidFrameIndex = -1;
                    }

                    // add invalid frame
                    if(!frameTranslation.isFinished()) {
                        chapterIsValid = false;
                        String frameTitle = sourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
                        frameTitle += ":" + frame.getStartVerse();
                        if (!frame.getStartVerse().equals(frame.getEndVerse())) {
                            frameTitle += "-" + frame.getEndVerse();
                        }
                        frameValidations.add(ValidationItem.generateInvalidFrame(frameTitle, frameTranslation.body, mTargetTranslationId, chapter.getId(), frame.getId()));
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
                        Chapter previousChapter = chapters[previousChapterIndex];
                        Chapter lastValidChapter = chapters[lastValidChapterIndex];
                        String chapterTitle = sourceTranslation.getProjectTitle() + " " + Integer.parseInt(lastValidChapter.getId()) + "-" + Integer.parseInt(previousChapter.getId());
                        chapterValidations.add(ValidationItem.generateValidGroup(chapterTitle, true));
                    } else {
                        Chapter lastValidChapter = chapters[lastValidChapterIndex];
                        String chapterTitle  = sourceTranslation.getProjectTitle() + " " + Integer.parseInt(lastValidChapter.getId());
                        chapterValidations.add(ValidationItem.generateValidGroup(chapterTitle, false));
                    }
                    lastValidChapterIndex = -1;
                }

                // add invalid chapter
                if(!chapterIsValid) {
                    String chapterTitle = chapter.title;
                    if (chapterTitle.isEmpty()) {
                        chapterTitle = sourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
                    }
                    chapterValidations.add(ValidationItem.generateInvalidGroup(chapterTitle));

                    // add frame validations
                    chapterValidations.addAll(frameValidations);
                }
            }
        }

        // close validations
        if(chapterValidations.size() > 1) {
            mValidations.addAll(chapterValidations);
        } else {
            mValidations.add(ValidationItem.generateValidGroup(sourceTranslation.getProjectTitle(), true));
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
