package com.door43.translationstudio;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.util.ArrayList;

public class GetMoreProjectsActivity extends TranslatorBaseActivity {

    private ArrayList<ToolItem> mGetProjectTools = new ArrayList<>();
    private ToolAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_more_projects);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView list = (ListView)findViewById(R.id.getProjectsListView);
        mAdapter = new ToolAdapter(mGetProjectTools, this);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ToolItem tool = mAdapter.getItem(i);
                // execute the get/update project action
                if (tool.isEnabled()) {
                    tool.getAction().run();
                } else {
                    app().showToastMessage(tool.getDisabledNotice());
                }
            }
        });

        init();
    }

    private void init() {
        Boolean hasNetwork = AppContext.context().isNetworkAvailable();
        mGetProjectTools.add(new ToolItem("Download from the server", "Requires an internet connection. Projects will be downloaded from the server", 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                // TODO: implement
            }
        }, hasNetwork, R.string.internet_not_available));
        mGetProjectTools.add(new ToolItem("Transfer from a nearby device ", "Requires a local area network connection. Projects will be transferred over the network from a nearby device", 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                // TODO: implement
            }
        }, hasNetwork, R.string.internet_not_available));
        mGetProjectTools.add(new ToolItem("Import from the external storage", "Projects will be imported from the external storage on this device", 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                // TODO: implement
            }
        }));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_get_more_projects, menu);
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
