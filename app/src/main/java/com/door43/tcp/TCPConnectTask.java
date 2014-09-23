package com.door43.tcp;

import android.os.AsyncTask;

import com.door43.translationstudio.util.AsyncTaskResultEvent;
import com.door43.translationstudio.util.EventBus;

/**
 * Created by joel on 9/22/2014.
 * @deprecated
 */
public class TCPConnectTask extends AsyncTask<TCPClient, Void, String> {
    @Override
    protected String doInBackground(TCPClient... tcpClients) {
        TCPClient client = tcpClients[0];
        client.connectAsync();
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        EventBus.getInstance().post(new AsyncTaskResultEvent(result));
    }
}
