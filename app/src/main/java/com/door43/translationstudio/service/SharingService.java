package com.door43.translationstudio.service;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;

/**
 * Created by joel on 7/23/2015.
 */
public class SharingService extends Service {
    public static final String PARAM_SERVICE_NAME = "param_service_name";
    private final IBinder mBinder = new LocalBinder();
    private Callbacks activity;
    private static boolean sRunning = false;
    private static int mPort = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void registerClient(Activity activity) {
        this.activity = (Callbacks) activity;
        if(sRunning) {
            this.activity.onServiceReady(mPort);
        }
    }

    public int onStartCommand(Intent intent, int flags, int startid) {
        Bundle args = intent.getExtras();
        if(args != null) {
            final String serviceName = args.getString(PARAM_SERVICE_NAME);

            // TODO: start server thread
            if (activity != null) {
                // send message to actvity
            } else {
                // send notification to user. clicking should open app to view request
                // or they can accept/deny from notification without opening the app
            }
            if (this.activity != null) {
                activity.onServiceReady(mPort);
            }
            //

            sRunning = true;
            return START_STICKY;
        } else {
            Logger.w(this.getClass().getName(), "The sharing service requires arguments to operate correctly");
            stopService();
            return START_NOT_STICKY;
        }
    }

    /**
     * Stops the service
     */
    public void stopService() {
        Logger.i(this.getClass().getName(), "Stopping sharing service");
        sRunning = false;
    }

    /**
     * Checks if the service is running
     * @return
     */
    public static boolean isRunning() {
        return sRunning;
    }

    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public SharingService getServiceInstance() {
            return SharingService.this;
        }
    }

    /**
     * Interface for communication with service clients.
     */
    public interface Callbacks {
        void onServiceReady(int port);
        void onConnectionRequest();
        void onConnectionLost();
        void onMessageReceived();
        void onSendMessage();
    }
}
