package com.door43.translationstudio.newui.publish;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;

import java.security.InvalidParameterException;

public class PublishActivity extends AppCompatActivity implements PublishStepFragment.OnEventListener {

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    private PublishStepFragment mFragment;
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTranslator = AppContext.getTranslator();

        // validate parameters
        Bundle args = getIntent().getExtras();
        final String targetTranslationId = args.getString(EXTRA_TARGET_TRANSLATION_ID, null);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if(mTargetTranslation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (PublishStepFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                mFragment = new ValidationFragment();
               String sourceTranslationId = mTranslator.getSelectedSourceTranslationId(targetTranslationId);
                args.putSerializable(PublishStepFragment.ARG_SOURCE_TRANSLATION_ID, sourceTranslationId);
                mFragment.setArguments(args);
                getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
                // TODO: animate
            }
        }

        // TODO: set up progress menu
    }
}
