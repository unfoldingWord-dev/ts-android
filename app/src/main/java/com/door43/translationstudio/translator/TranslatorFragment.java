package com.door43.translationstudio.translator;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.door43.translationstudio.dialogs.FramesListAdapter;
import com.door43.translationstudio.dialogs.FramesReaderDialog;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 4/17/2015.
 */
public abstract class TranslatorFragment extends TranslatorBaseFragment implements TranslatorFragmentInterface {
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof TranslatorActivityInterface == false) {
            throw new ClassCastException("The activity must implement the TranslatorActivityInterface interface");
        }
    }

    /**
     * Displays the frame reader dialog
     * @param p
     */
    protected void showFrameReaderDialog(Project p, FramesListAdapter.DisplayOption option) {
        if(p != null && p.getSelectedChapter() != null) {
            // move other dialogs to backstack
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create the dialog
            FramesReaderDialog newFragment = new FramesReaderDialog();
            Bundle args = new Bundle();
            args.putString(FramesReaderDialog.ARG_PROJECT_ID, p.getId());
            args.putString(FramesReaderDialog.ARG_CHAPTER_ID, p.getSelectedChapter().getId());

            // configure display option
            args.putInt(FramesReaderDialog.ARG_DISPLAY_OPTION_ORDINAL, option.ordinal());

            // display dialog
            newFragment.setArguments(args);
            newFragment.show(ft, "dialog");
        }
    }
}
