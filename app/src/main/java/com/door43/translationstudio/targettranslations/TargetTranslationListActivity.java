package com.door43.translationstudio.targettranslations;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.AppContext;

public class TargetTranslationListActivity extends AppCompatActivity {
    private static final int NEW_TARGET_TRANSLATION_REQUEST = 1;
    private TargetTranslationAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_translations);
    }

    public void onResume() {
        super.onResume();

        mAdapter = new TargetTranslationAdapter(AppContext.getTranslator().getTargetTranslations());

        if(mAdapter.getCount() == 0) {
            View welcomeView = getLayoutInflater().inflate(R.layout.fragment_target_translations_welcome, null);
            FrameLayout containerView = (FrameLayout)findViewById(R.id.fragment_container);
            containerView.addView(welcomeView);
        } else {
            View listView = getLayoutInflater().inflate(R.layout.fragment_target_translation_list, null);
            FrameLayout containerView = (FrameLayout)findViewById(R.id.fragment_container);
            containerView.addView(listView);
        }

        // target translations list
        ListView list = (ListView) findViewById(R.id.translationsList);
        if(list != null) {
            mAdapter.setOnInfoClickListener(new TargetTranslationAdapter.OnInfoClickListener() {
                @Override
                public void onClick(String targetTranslationId) {
                    // move other dialogs to backstack
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    ft.addToBackStack(null);

                    // target translation info
                    TargetTranslationInfoDialog dialog = new TargetTranslationInfoDialog();
                    Bundle args = new Bundle();
                    args.putString(TargetTranslationInfoDialog.ARG_TARGET_TRANSLATION_ID, targetTranslationId);
                    dialog.show(ft, "dialog");
                }
            });
            list.setAdapter(mAdapter);
            // open target translation detail
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(TargetTranslationListActivity.this, TargetTranslationDetailActivity.class);
                    intent.putExtra(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, mAdapter.getItem(position).getId());
                    startActivity(intent);
                }
            });
        }

        // new project FAB
        FloatingActionButton addTranslationButton = (FloatingActionButton) findViewById(R.id.addTargetTranslationButton);
        addTranslationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TargetTranslationListActivity.this, NewTargetTranslationActivity.class);
                startActivityForResult(intent, NEW_TARGET_TRANSLATION_REQUEST);
            }
        });

        // new project Button
        Button extraAddTranslationButton = (Button) findViewById(R.id.extraAddTargetTranslationButton);
        if(extraAddTranslationButton != null) {
            extraAddTranslationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(TargetTranslationListActivity.this, NewTargetTranslationActivity.class);
                    startActivityForResult(intent, NEW_TARGET_TRANSLATION_REQUEST);
                }
            });
        }
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
                Intent intent = new Intent(TargetTranslationListActivity.this, TargetTranslationDetailActivity.class);
                intent.putExtra(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, data.getStringExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID));
                startActivity(intent);
            } else if(resultCode == NewTargetTranslationActivity.RESULT_DUPLICATE) {
                // TODO: display snack noting the target translation already exists
            }
        }
    }
}
