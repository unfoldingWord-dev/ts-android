package com.door43.translationstudio.targettranslations;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;

import java.util.Locale;

public class TargetTranslationListActivity extends AppCompatActivity implements TargetTranslationWelcomeFragment.OnCreateNewTargetTranslation, TargetTranslationListFragment.OnItemClickListener {
    private static final int NEW_TARGET_TRANSLATION_REQUEST = 1;
    private Library mLibrary;
    private Translator mTranslator;
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_translations);

        FloatingActionButton addTranslationButton = (FloatingActionButton) findViewById(R.id.addTargetTranslationButton);
        addTranslationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateNewTargetTranslation();
            }
        });

        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                // use current fragment
                mFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
                return;
            }

            if(mTranslator.getTargetTranslations().length > 0) {
                mFragment = new TargetTranslationListFragment();
                mFragment.setArguments(getIntent().getExtras());
            } else {
                mFragment = new TargetTranslationWelcomeFragment();
                mFragment.setArguments(getIntent().getExtras());
            }

            getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
            // TODO: animate
        }

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                // TODO: handle clicks
//                return false;
//            }
//        });
//        toolbar.inflateMenu(R.menu.left_menu);

        ImageButton moreButton = (ImageButton)findViewById(R.id.action_more);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: we need to display a custom popup menu
                Intent intent = new Intent(TargetTranslationListActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }



    @Override
    public void onResume() {
        super.onResume();

        int numTranslations = mTranslator.getTargetTranslations().length;
        if(numTranslations > 0 && mFragment instanceof TargetTranslationWelcomeFragment) {
            // display target translations list
            mFragment = new TargetTranslationListFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
            // TODO: animate
        } else if(numTranslations == 0 && mFragment instanceof TargetTranslationListFragment) {
            // display welcome screen
            mFragment = new TargetTranslationWelcomeFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
            // TODO: animate
        }
    }

    @Override
    public void onBackPressed() {
        // display confirmation before closing the app
        new AlertDialog.Builder(this)
                .setMessage(R.string.exit_confirmation)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TargetTranslationListActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_target_translations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == NEW_TARGET_TRANSLATION_REQUEST) {
            if(resultCode == RESULT_OK) {
                if(mFragment instanceof TargetTranslationWelcomeFragment) {
                    // display target translations list
                    mFragment = new TargetTranslationListFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                }

                Intent intent = new Intent(TargetTranslationListActivity.this, TargetTranslationDetailActivity.class);
                intent.putExtra(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, data.getStringExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID));
                startActivity(intent);
            } else if(resultCode == NewTargetTranslationActivity.RESULT_DUPLICATE) {
                // display duplicate notice to user
                String targetTranslationId = data.getStringExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID);
                TargetTranslation existingTranslation = mTranslator.getTargetTranslation(targetTranslationId);
                if(existingTranslation != null) {
                    Project project = mLibrary.getProject(existingTranslation.getProjectId(), Locale.getDefault().getLanguage());
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), String.format(getResources().getString(R.string.duplicate_target_translation), project.name, existingTranslation.getTargetLanguageName()), Snackbar.LENGTH_LONG);
                    TextView tv = (TextView) snack.getView().findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        }
    }

    @Override
    public void onCreateNewTargetTranslation() {
        Intent intent = new Intent(TargetTranslationListActivity.this, NewTargetTranslationActivity.class);
        startActivityForResult(intent, NEW_TARGET_TRANSLATION_REQUEST);
    }

    @Override
    public void onItemDeleted(String targetTranslationId) {
        if(mTranslator.getTargetTranslations().length > 0) {
            ((TargetTranslationListFragment) mFragment).reloadList();
        } else {
            // display welcome screen
            mFragment = new TargetTranslationWelcomeFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
            // TODO: animate
        }
    }

    @Override
    public void onItemClick(TargetTranslation targetTranslation) {
        Intent intent = new Intent(TargetTranslationListActivity.this, TargetTranslationDetailActivity.class);
        intent.putExtra(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, targetTranslation.getId());
        startActivity(intent);
    }
}
