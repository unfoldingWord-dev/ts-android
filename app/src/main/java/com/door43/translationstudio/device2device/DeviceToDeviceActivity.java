package com.door43.translationstudio.device2device;

import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.network.Client;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.network.Service;
import com.door43.translationstudio.network.Server;
import com.door43.translationstudio.util.TranslatorBaseActivity;

public class DeviceToDeviceActivity extends TranslatorBaseActivity {
    private boolean mStartAsServer = false;
    private Service mService;
    private DevicePeerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_to_device);

        mStartAsServer = getIntent().getBooleanExtra("startAsServer", false);
        final int clientUDPPort = 9939;
        final Handler handler = new Handler(getMainLooper());

        // set up the threads
        if(mStartAsServer) {
            mService = new Server(DeviceToDeviceActivity.this, clientUDPPort);
        } else {
            mService = new Client(DeviceToDeviceActivity.this, clientUDPPort, new Client.OnClientEventListener() {
                @Override
                public void onError(final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            app().showException(e);
                        }
                    });
                }

                @Override
                public void onFoundServer(final Peer server) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(mAdapter != null) {
                                mAdapter.setPeerList(mService.getPeers());
                            }
                            app().showToastMessage("Found server " + server.getIpAddress());
                        }
                    });
                }

                @Override
                public void onLostServer(final Peer server) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(mAdapter != null) {
                                mAdapter.setPeerList(mService.getPeers());
                            }
                            app().showToastMessage("Lost server " + server.getIpAddress());
                        }
                    });
                }
            });
        }

        // set up the ui
        ListView peerListView = (ListView)findViewById(R.id.peerListView);
        mAdapter = new DevicePeerAdapter(mService.getPeers(), this); // TRICKY: when using this in threads we need to make sure it's not null due to order of initialization.
        peerListView.setAdapter(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        app().showToastMessage("stopping service");
        mService.stop();

    }

    public void onResume() {
        super.onResume();
        app().showToastMessage("starting service");
        mService.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_to_device, menu);
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
