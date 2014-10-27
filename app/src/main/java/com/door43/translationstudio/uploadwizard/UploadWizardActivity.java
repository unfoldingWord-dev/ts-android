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
            app().showToastMessage("Invalid fragment index");
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


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.upload_wizard, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}
