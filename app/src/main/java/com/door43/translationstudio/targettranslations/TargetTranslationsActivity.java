package com.door43.translationstudio.targettranslations;

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

public class TargetTranslationsActivity extends AppCompatActivity {
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

        ListView list = (ListView) findViewById(R.id.translationsList);
        if(list != null) {
            list.setAdapter(mAdapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // TODO: open target translation activity
                }
            });
        }

        FloatingActionButton addTranslationButton = (FloatingActionButton) findViewById(R.id.addTargetTranslationButton);
        addTranslationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TargetTranslationsActivity.this, NewTargetTranslationActivity.class);
                startActivity(intent);
            }
        });

        Button extraAddTranslationButton = (Button) findViewById(R.id.extraAddTargetTranslationButton);
        if(extraAddTranslationButton != null) {
            extraAddTranslationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(TargetTranslationsActivity.this, NewTargetTranslationActivity.class);
                    startActivity(intent);
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
}
