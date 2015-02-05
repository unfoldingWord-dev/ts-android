package com.door43.translationstudio.uploadwizard;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ContactFormDialog;
import com.door43.translationstudio.dialogs.NoteMarkerDialog;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.util.ArrayList;


public class UploadWizardActivity extends TranslatorBaseActivity implements IntroFragment.OnFragmentInteractionListener {
    private ArrayList<WizardFragment> mFragments = new ArrayList<WizardFragment>();
    private int mCurrentFragmentIndex = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_wizard);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        mFragments.add(new IntroFragment());
        mFragments.add(new ReviewFragment());

        // restore state
        if(savedInstanceState != null) {
            mCurrentFragmentIndex = savedInstanceState.getInt("frameIndex") - 1;
        }

        loadNextFragment();
    }

    public void loadNextFragment() {
        mCurrentFragmentIndex ++;
        if(mCurrentFragmentIndex >= 0 && mCurrentFragmentIndex < mFragments.size()) {
            getFragmentManager().beginTransaction().replace(R.id.upload_wizard_content, mFragments.get(mCurrentFragmentIndex)).addToBackStack(null).commit();
        } else if(mCurrentFragmentIndex >= mFragments.size()) {
            // we are done
            startUpload();
        } else {
            Logger.w(this.getClass().getName(), "invalid fragment index");
            finish();
        }
    }

    /**
     * Begins uploading the translation
     */
    public void startUpload() {
        // get user info if publishing translation
        if(app().getSharedProjectManager().getSelectedProject().translationIsReady()) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the dialog
            ContactFormDialog newFragment = new ContactFormDialog();
//            Bundle args = new Bundle();
//            args.putString("name", name);
//            args.putString("email", email);
//            newFragment.setArguments(args);
            newFragment.setOkListener(new ContactFormDialog.OnOkListener() {
                @Override
                public void onOk(Profile profile) {
                    app().getSharedProjectManager().getSelectedProject().setProfile(profile);
                    upload();
                }
            });
            newFragment.show(ft, "dialog");
        } else {
            upload();
        }
    }

    /**
     * Initiates the actual upload
     */
    private void upload() {
        // prepare upload
        app().showProgressDialog(R.string.preparing_upload);
        app().getSharedProjectManager().getSelectedProject().commit(new Project.OnCommitComplete() {
            @Override
            public void success() {
                app().getSharedTranslationManager().syncSelectedProject();
                finish();
            }

            @Override
            public void error() {
                // We don't care. Worst case is the server won't know that the translation is ready.
                app().getSharedTranslationManager().syncSelectedProject();
                finish();
            }
        });
    }

    @Override
    public void onContinue() {
        loadNextFragment();
    }

    @Override
    public void onCancel() {
        finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("frameIndex", mCurrentFragmentIndex);
        super.onSaveInstanceState(outState);
    }
}
