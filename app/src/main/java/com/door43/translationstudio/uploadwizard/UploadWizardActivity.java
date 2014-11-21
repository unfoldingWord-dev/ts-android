package com.door43.translationstudio.uploadwizard;

import android.os.Bundle;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.util.ArrayList;


public class UploadWizardActivity extends TranslatorBaseActivity implements IntroFragment.OnFragmentInteractionListener {
    private ArrayList<WizardFragment> mFragments = new ArrayList<WizardFragment>();
    private int mCurrentFragmentIndex = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_wizard);
        
        mFragments.add(new IntroFragment());
        mFragments.add(new ReviewFragment());
        
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
            // invalid index
//            Log.d("error", "Invalid fragment index");
            finish();
        }
    }

    /**
     * Begins uploading the translation
     */
    public void startUpload() {
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
}
