package com.door43.translationstudio.targettranslations;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetLanguage;

public class NewTargetTranslationActivity extends AppCompatActivity implements TargetLanguageListFragment.OnItemClickListener, ProjectListFragment.OnItemClickListener {

    private TargetLanguage mSelectedTargetLanguage = null;
    private String mSelectedProjectId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_target_translation);

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                return;
            }

            TargetLanguageListFragment fragment = new TargetLanguageListFragment();
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
            // TODO: animate
        }
    }

    @Override
    public void onItemClick(TargetLanguage targetLanguage) {
        mSelectedTargetLanguage = targetLanguage;

        ProjectListFragment fragment = new ProjectListFragment();
        fragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        // TODO: animate
    }

    @Override
    public void onItemClick(String projectId) {
        mSelectedProjectId = projectId;

        // TODO: finish
    }
}
