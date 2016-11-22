package com.door43.translationstudio.core;

import android.os.Handler;
import android.os.Looper;

import com.door43.translationstudio.App;
import com.door43.translationstudio.ui.home.ImportDialog;

import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blm on 11/22/16.
 */

public class MergeConflictsHandler {
    public static final String MergeConflictHead = "(?:<<<<<<< HEAD.*\\n)";
    public static Pattern MergeConflictPatternHead = Pattern.compile(MergeConflictHead);


    /**
     * Detects merge conflict tags
     *
     * @param text
     * @return
     */
    static public boolean isMergeConflicted(CharSequence text) {
        if ((text != null) && (text.length() > 0)) {
            Matcher matcher = MergeConflictPatternHead.matcher(text);
            boolean matchFound = matcher.find();
            return matchFound;
        }
        return false;
    }

    /**
     * search for first merge conflict - We need this to double check that there is a conflict in any chunks
     *
     * @param targetTranslationId
     * @return
     */
    static public boolean isMergeConflicted(String targetTranslationId) {
        if(targetTranslationId == null) {
            return false;
        }

        Translator translator = App.getTranslator();

        TargetTranslation targetTranslation = translator.getTargetTranslation(targetTranslationId);
        ProjectTranslation pt = targetTranslation.getProjectTranslation();
        if(isMergeConflicted(pt.getTitle())) {
            return true;
        }

        ChapterTranslation[] chapters = targetTranslation.getChapterTranslations();
        for(ChapterTranslation ct:chapters) {
            if(isMergeConflicted(ct.title)) {
                return true;
            }

            if(isMergeConflicted(ct.reference)) {
                return true;
            }

            FrameTranslation[] frames = targetTranslation.getFrameTranslations(ct.getId(), TranslationFormat.DEFAULT);
            for (int i = 0; i < frames.length; i++) {
                FrameTranslation frame = frames[i];
                if(isMergeConflicted(frame.body)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * check the whole project to see if there is actually a chunk conflict
     * @param targetTranslationId
     */
    public static void backgroundTestForConflictedChunks(final String targetTranslationId, final OnMergeConflictListener listener) {
        ManagedTask task = new ManagedTask() {
            @Override
            public void start() {
                try {
                    if(interrupted()) return;
                    boolean conflicted = MergeConflictsHandler.isMergeConflicted(targetTranslationId);
                    setResult(conflicted);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        task.addOnFinishedListener(new ManagedTask.OnFinishedListener() {
            @Override
            public void onTaskFinished(ManagedTask task) {
                TaskManager.clearTask(task);
                boolean conflicted = false;
                if(task.getResult() != null) conflicted = (boolean)task.getResult();
                if(!task.isCanceled()) {
                    Handler hand = new Handler(Looper.getMainLooper());
                    final boolean finalConflicted = conflicted;
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            if(finalConflicted) {
                                listener.onMergeConflict(targetTranslationId);
                            } else {
                                listener.onMergeConflict(targetTranslationId);
                            }
                        }
                    });
                }
            }
        });
    }

    public interface OnMergeConflictListener {
        void onNoMergeConflict(String targetTranslationId);
        void onMergeConflict(String targetTranslationId);
    }
}
