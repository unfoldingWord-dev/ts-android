package com.door43.translationstudio.newui;

import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.OrientationEventListener;
import android.widget.BaseAdapter;

import com.door43.tools.reporting.GlobalExceptionHandler;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.CrashReporterActivity;
import com.door43.translationstudio.SplashScreenActivity;
import com.door43.translationstudio.util.AppContext;

import java.io.File;

/**
 * This should be extended by all activities in the app so that we can perform verification on
 * activities such as recovery from crashes.
 *
 */
public abstract class BaseActivity extends AppCompatActivity {

    private OrientationEventListener mOrientationEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // log orientation changes to help with debugging rotation bugs
        mOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int arg) {
                if(arg == ORIENTATION_UNKNOWN) {
                    Logger.i(BaseActivity.this.getClass().getName(), "Orientation: UNKNOWN/FLAT");
                } else {
                    Logger.i(BaseActivity.this.getClass().getName(), "Orientation: " + String.valueOf(arg) + "deg");
                }
            }
        };

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // check if we crashed
        File dir = new File(getExternalCacheDir(), AppContext.context().STACKTRACE_DIR);
        String[] files = GlobalExceptionHandler.getStacktraces(dir);
        if (files.length > 0) {
            // restart
            Intent intent = new Intent(this, SplashScreenActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
