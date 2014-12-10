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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_to_device);

        new ThreadableUI() {

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                // broadcast 10 times once every 5 seconds
//                int runs = 10;
//                while(runs >0) {
//                    Log.d("test", "pinging network");
//                    try {
//                        NetworkUtils.broadcast(DeviceToDeviceActivity.this);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    runs --;
//                }


//                WifiManager wifi = (WifiManager) DeviceToDeviceActivity.this.getSystemService(Context.WIFI_SERVICE);
//                WifiManager.MulticastLock lock = wifi.createMulticastLock("dk.aboaya.pingpong");
//                lock.acquire();
//                DatagramSocket serverSocket;
//                try {
//                    serverSocket = new DatagramSocket(19876);
//                } catch (SocketException e) {
//                    e.printStackTrace();
//                    lock.release();
//                    return;
//                }
//                try {
//                    serverSocket.setSoTimeout(15000); //15 sec wait for the client to connect
//                } catch (SocketException e) {
//                    e.printStackTrace();
//                    lock.release();
//                    return;
//                }
//                byte[] data = new byte[255];
//                DatagramPacket packet = new DatagramPacket(data, data.length);
//                try {
//                    serverSocket.receive(packet);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    lock.release();
//                    return;
//                }
//                lock.release();
//                String s = new String(packet.getData());
//                System.out.println(s);
            }

            @Override
            public void onPostExecute() {

            }
        }.start(this);
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
