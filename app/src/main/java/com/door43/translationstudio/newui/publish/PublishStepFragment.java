package com.door43.translationstudio.newui.publish;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;

/**
 * Created by joel on 9/20/2015.
 */
public abstract class PublishStepFragment extends Fragment {
    public static final String ARG_SOURCE_TRANSLATION_ID = "arg_source_translation_id";
    private OnEventListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnItemClickListener");
        }
    }

    /**
     * requests the parent activity to navigate to a new activity
     * @param targetTranslationId
     * @param chapterId
     * @param frameId
     */
    protected void openReview(String targetTranslationId, String chapterId, String frameId) {
        Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
        Bundle args = new Bundle();
        args.putString(TargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID, targetTranslationId);
        args.putString(TargetTranslationActivity.EXTRA_CHAPTER_ID, chapterId);
        args.putString(TargetTranslationActivity.EXTRA_FRAME_ID, frameId);
        args.putString(TargetTranslationActivity.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.toString());
        intent.putExtras(args);
        startActivity(intent);
        getActivity().finish();
    }

    public interface OnEventListener {
        // TODO: this is here in case we need it
    }
}
