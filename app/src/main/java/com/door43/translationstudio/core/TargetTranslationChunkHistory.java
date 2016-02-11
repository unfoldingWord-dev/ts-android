package com.door43.translationstudio.core;

import android.app.Activity;
import android.os.AsyncTask;

import com.door43.tools.reporting.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by blm on 2/11/16.
 */
public class TargetTranslationChunkHistory {
    static String TAG = TargetTranslationChunkHistory.class.getSimpleName();

    private RevCommit mCurrentCommit = null; //keeps track of undo position
    private RevCommit[] mCommitHistory = null; //cache commit history
    private TargetTranslation mTargetTranslation = null;
    private File mRepoFile = null;
    private ChunkType mChunkType = ChunkType.ILLEGAL;
    private FrameTranslation mFrameTranslation = null;

    public enum ChunkType {
        CHAPTER_REFERENCE,
        CHAPTER_TITLE,
        PROJECT_TITLE,
        FRAME,
        ILLEGAL
    }

    public TargetTranslationChunkHistory(TargetTranslation targetTranslation, FrameTranslation frameTranslation) {
        mTargetTranslation = targetTranslation;
        mFrameTranslation = frameTranslation;
    }

    public void setChunkTypeAsChapterReference() {
        mChunkType = ChunkType.CHAPTER_REFERENCE;
        mRepoFile = mTargetTranslation.getChapterReferenceFile(mFrameTranslation.getChapterId());
    }

    public void setChunkTypeAsChapterTitle() {
        mChunkType = ChunkType.CHAPTER_TITLE;
        mRepoFile = mTargetTranslation.getChapterTitleFile(mFrameTranslation.getChapterId());
    }

    public void setChunkTypeAsProjectTitle() {
        mChunkType = ChunkType.PROJECT_TITLE;
        mRepoFile = mTargetTranslation.getProjectTitleFile();
    }

    public void setChunkTypeAsFrame() {
        mChunkType = ChunkType.FRAME;
        mRepoFile = mTargetTranslation.getFrameFile(mFrameTranslation.getChapterId(), mFrameTranslation.getId());
    }

    public void clearHistory() {
        mCurrentCommit = null; // clears undo position
        mCommitHistory = null;
    }

    public void fetchCommitList(Git git, File file) throws IOException, GitAPIException {
        if(null == mCommitHistory) {
            mCommitHistory = mTargetTranslation.getCommitList(git, file);
        }
    }

    /**
     * restore the text from previous commit for fragment
     * @param listener
     */
    public void doUndo(final Activity activity, final OnRestoreFinishListener listener) {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Git git = mTargetTranslation.getGit();
                    fetchCommitList(git, mRepoFile);
                    RevCommit commit = mTargetTranslation.getUndoCommit(mCommitHistory, mCurrentCommit);
                    restoreCommitText(activity, git, listener, commit);
                } catch (Exception e) {
                    Logger.w(TAG, "error getting commit list", e);
                }
            }
        });
    }

    /**
     * restore the text from later commit for fragment
     * @param listener
     */
    public void doRedo(final Activity activity, final OnRestoreFinishListener listener) {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Git git = mTargetTranslation.getGit();
                    fetchCommitList(git, mRepoFile);
                    RevCommit commit = mTargetTranslation.getRedoCommit(mCommitHistory, mCurrentCommit);
                    restoreCommitText(activity, git, listener, commit);
                } catch (Exception e) {
                    Logger.w(TAG, "error getting commit list", e);
                }
            }
        });
    }

    /**
     * restore commited file contents to current fragment
     * @param listener
     * * @param git
     * @param commit
     */
    private void restoreCommitText(final Activity activity, final Git git, final OnRestoreFinishListener listener, final RevCommit commit) {

        Date commitTime = null;
        if (commit != null) {
            commitTime = new Date(commit.getCommitTime() * 1000L);
        }

        String committedText = null;

        if (null != commit) {
            committedText = mTargetTranslation.getCommittedFileContents(git, mRepoFile, commit);
            mCurrentCommit = commit;
        } else {
            Logger.i(TAG, "restore commit not found");
        }

        final String finalCommittedText = committedText;
        final Date finalCommitTime = commitTime;

        activity.runOnUiThread(new Runnable() { // need to call back running on UI thread
            public void run() {
                listener.onRestoreFinish(finalCommitTime, finalCommittedText);
            }
        });
    }

    public interface OnRestoreFinishListener {
        /**
         * Called when a view has been clicked.
         * @param restoredText
         */
        void onRestoreFinish(Date restoreTime, String restoredText);
    }


}
