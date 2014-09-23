package com.door43.tcp.tasks.tcp;

import android.util.Log;

import com.door43.tcp.TCPConnection;
import com.door43.tcp.tasks.TCPAsyncTask;
import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContextLink;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by joel on 9/19/2014.
 */
public class ConnectTask extends TCPTask {
    AsyncTaskCallback mCallback;

    public ConnectTask(TCPConnection connection, AsyncTaskCallback callback) {
        super(connection);
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean result = connectTCP();
        if(mCallback != null) {
            result = mCallback.doInBackground(params) & result;
        }
        return result;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        super.onProgressUpdate(progress);
        if (mCallback != null) {
            mCallback.onProgressUpdate(progress);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mCallback != null) {
            mCallback.onPreExecute();
        }
    }

    @Override
    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.onPostExecute(isSuccess);
        }
    }

    public boolean connectTCP() {
        mTCPConnection.setListener(new TCPConnection.OnMessageReceived() {
            @Override
            public void messageReceived(String message) {
                Log.d("TCP", message);
            }
        });
        mTCPConnection.run();
        return true;
//
//        Socket socket = null;
//        try {
//            socket = new Socket(mTCPConnection.getServer(), mTCPConnection.getPort());
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//
//        OutputStream out = null;
//        try {
//            out = socket.getOutputStream();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//        PrintWriter output = new PrintWriter(out);
//
//        if (mCallback != null) {
//            mCallback.onProgressUpdate("Sending data to "+mTCPConnection.getServer());
//        }
//        output.println("Hello from Android");
//        try {
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (mCallback != null) {
//            mCallback.onProgressUpdate("Data sent to "+mTCPConnection.getServer());
//        }
//
//        try {
//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return true;
    }
}
