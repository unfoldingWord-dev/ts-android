package com.door43.translationstudio.uploadwizard;

import android.os.Bundle;

import com.door43.translationstudio.R;
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
            finish();
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
        app().getSharedTranslationManager().syncSelectedProject();
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
