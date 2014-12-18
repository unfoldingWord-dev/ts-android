package com.door43.translationstudio.device2device;

import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.network.Client;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.network.Service;
import com.door43.translationstudio.network.Server;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;

public class DeviceToDeviceActivity extends TranslatorBaseActivity {
    private boolean mStartAsServer = false;
    private Service mService;
    private DevicePeerAdapter mAdapter;
    private ProgressBar mProgressBar;
    private TextView mProgressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_to_device);

        mStartAsServer = getIntent().getBooleanExtra("startAsServer", false);
        final int clientUDPPort = 9939;
        final Handler handler = new Handler(getMainLooper());

        // set up the threads
        if(mStartAsServer) {
            mService = new Server(DeviceToDeviceActivity.this, clientUDPPort, new Server.OnServerEventListener() {

                @Override
                public void onError(final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: it would be nice to place this in a log that could be submitted to github later on.
                            app().showException(e);
                            finish();
                        }
                    });
                }

                @Override
                public void onFoundClient(Peer client) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                }

                @Override
                public void onLostClient(Peer client) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                }

                @Override
                public void onMessageReceived(final Peer client, final String message) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: handle the message
                            app().showToastMessage(message);
                        }
                    });
                }
            });
        } else {
            mService = new Client(DeviceToDeviceActivity.this, clientUDPPort, new Client.OnClientEventListener() {
                @Override
                public void onError(final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: it would be nice to place this in a log that could be submitted to github later on.
                            app().showException(e);
                            finish();
                        }
                    });
                }

                @Override
                public void onFoundServer(final Peer server) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                }

                @Override
                public void onLostServer(final Peer server) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                }

                @Override
                public void onMessageReceived(final Peer server, final String message) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: handle the message
                            app().showToastMessage(message);
                            final Client c = (Client)mService;
                            String[] data = chunk(message, ":");
                            // TODO: we need to create a wrapper class for the parsed response
                            if(data[0].equals("filesocket")) {
//                              // create a socket to connect to the file socket
                                c.createReceiverSocket(server, Integer.parseInt(data[1]), new Service.OnSocketEventListener() {
                                    @Override
                                    public void onOpen(Connection connection) {
                                        connection.setOnCloseListener(new Connection.OnCloseListener() {
                                            @Override
                                            public void onClose() {
                                                // TODO: the socket for transferring the file has closed
                                            }
                                        });
                                        // begin listening for the file
                                        try {
                                            DataInputStream in = new DataInputStream(connection.getSocket().getInputStream());
                                            File file = new File(getExternalCacheDir() + "transferred/" + System.currentTimeMillis() + ".zip");
                                            file.getParentFile().mkdirs();
                                            file.createNewFile();
                                            OutputStream out = new FileOutputStream(file.getAbsolutePath());
                                            byte[] buffer = new byte[8 * 1024];
                                            int count;
                                            // TODO: display a progress bar. We will probably need to send the size of the file with the port #
                                            while ((count = in.read(buffer)) > 0)
                                            {
                                                out.write(buffer, 0, count);
                                            }
                                            // TODO: do something with the file
                                        } catch (IOException e) {
                                            app().showException(e);
                                        }
                                    }
                                });
                            } else {
//                                c.writeTo(server, Service.buildResponse("ok", "What do you want?!"));
                            }
                        }
                    });
                }
            });
        }

        // set up the ui
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mProgressText = (TextView)findViewById(R.id.progressTextView);
        TextView titleText = (TextView)findViewById(R.id.titleText);
        if(mStartAsServer) {
            titleText.setText(R.string.export_to_device);
        } else {
            titleText.setText(R.string.import_from_device);
        }
        ListView peerListView = (ListView)findViewById(R.id.peerListView);
        mAdapter = new DevicePeerAdapter(mService.getPeers(), mStartAsServer, this);
        peerListView.setAdapter(mAdapter);
        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mStartAsServer) {
                    // the server has accepted the client
                    final Server s = (Server)mService;
                    final Peer p = mAdapter.getItem(i);

                    // open a separate port for sending files
                    ServerSocket fileSocket = s.createSenderSocket(new Service.OnSocketEventListener() {
                        @Override
                        public void onOpen(Connection connection) {
                            // send an archive to the connection

                            // create test file
                            File temp = new File(getCacheDir(), "temp.txt");
//                            temp.getParentFile().mkdirs();
//                            try {
//                                OutputStreamWriter outFile = new OutputStreamWriter(new FileOutputStream(temp));
//                                outFile.write("Hello world!");
//                            } catch (IOException e) {
//                                app().showException(e);
//                                return;
//                            }
                            File zipFile = new File(getCacheDir(), "temp.zip");

                            // zip test file
                            try {
                                app().zip(temp.getAbsolutePath(), zipFile.getAbsolutePath());
                            } catch (IOException e) {
                                app().showException(e);
                                return;
                            }

                            // send the file to the connection
                            // TODO: first we should tell client about the available projects
                            // TODO: display a progress bar when the files are being transferred (on each client list item)
                            try {
                                DataOutputStream out = new DataOutputStream(connection.getSocket().getOutputStream());
                                DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
                                byte[] buffer = new byte[8 * 1024];
                                int count;
                                while ((count = in.read(buffer)) > 0)
                                {
                                    out.write(buffer, 0, count);
                                }
                            } catch (IOException e) {
                                app().showException(e);
                            }
                        }
                    });
                    // send the port number to the client
                    s.writeTo(mAdapter.getItem(i), "filesocket:"+fileSocket.getLocalPort());
                } else {
                    // the client is requesting to connect to the server
                    Client c = (Client)mService;
                    c.connectToServer(mAdapter.getItem(i));
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mService.stop();

    }

    @Override
    public void onResume() {
        super.onResume();
        mService.start();
    }

    /**
     * Splits a string by delimiter into two pieces
     * @param string the string to split
     * @param delimiter
     * @return
     */
    private String[] chunk(String string, String delimiter) {
        if(string == null || string.isEmpty()) {
            return new String[]{"", ""};
        }
        String[] pieces = string.split(delimiter, 2);
        if(pieces.length == 1) {
            pieces = new String[] {string, ""};
        }
        return pieces;
    }

    /**
     * Updates the peer list on the screen
     */
    public void updatePeerList() {
        // TRICKY: when using this in threads we need to make sure everything has been initialized and not null
        // update the progress bar dispaly
        if(mProgressBar != null) {
            if(mService.getPeers().size() == 0) {
                mProgressBar.setVisibility(View.VISIBLE);
            } else {
                mProgressBar.setVisibility(View.GONE);
            }
        }
        if(mProgressText != null) {
            if(mService.getPeers().size() == 0) {
                mProgressText.setVisibility(View.VISIBLE);
            } else {
                mProgressText.setVisibility(View.GONE);
            }
        }
        // update the adapter
        if(mAdapter != null) {
            mAdapter.setPeerList(mService.getPeers());
        }
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

        // TODO: we could have additional menu items to adjust the sharing settings.
        if (id == R.id.action_share_to_all) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
