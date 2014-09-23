package com.door43.tcp.tasks.tcp;

import com.door43.tcp.TCPConnection;
import com.door43.tcp.tasks.TCPAsyncTask;
import com.door43.translationstudio.util.MainContextLink;

/**
 * Created by joel on 9/19/2014.
 */
public abstract class TCPTask extends TCPAsyncTask<Void, String, Boolean>{
    protected TCPConnection mTCPConnection;
    protected boolean mIsTaskAdded;

    public TCPTask(TCPConnection connection) {
        mTCPConnection = connection;
        mIsTaskAdded = connection.addTask(this);
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
    }

    public void executeTask() {
        if (mIsTaskAdded) {
            execute();
            return;
        }
    }
}
