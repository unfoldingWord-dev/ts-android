package com.door43.translationstudio.device2device;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.ThreadableUI;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class DeviceToDeviceActivity extends ActionBarActivity {
    private boolean mStartAsServer = false;
    private Server mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_to_device);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            mStartAsServer = extras.getBoolean("startAsServer", false);
        }
        final int serverPort = 8838;
        final int clientPort = 9939;

        // TODO: client and server should extend the same base class.
//        if(mStartAsServer) {
        mService = new Server(DeviceToDeviceActivity.this, serverPort);
        mService.start(clientPort);
//        } else {
//            Client service = new Client(DeviceToDeviceActivity.this);
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mService.stop();

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
