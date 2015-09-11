package com.door43.translationstudio.targettranslations;

import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

import com.door43.translationstudio.R;
import com.door43.widget.VerticalSeekBar;

public class TargetTranslationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    private TargetTranslationDetailMode mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_translation_detail);

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (TargetTranslationDetailMode)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                // TODO: remember and restore last mode
                mFragment = new ReadModeFragment();
                ((Fragment)mFragment).setArguments(getIntent().getExtras());

                getFragmentManager().beginTransaction().add(R.id.fragment_container, (Fragment) mFragment).commit();
                // TODO: animate
            }
        }

        // TODO: set up menu items
        VerticalSeekBar seekBar = (VerticalSeekBar)findViewById(R.id.action_seek);
        seekBar.setMax(100);
        seekBar.setProgress(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mFragment.onScrollProgressUpdate((100 - progress) / 100.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_target_translation_detail, menu);
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
