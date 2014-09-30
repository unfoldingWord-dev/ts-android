package com.door43.translationstudio;

import android.content.Intent;
import android.os.Bundle;

import com.door43.translationstudio.util.TranslatorBaseActivity;

/**
 * Created by joel on 9/29/2014.
 */
public class SplashActivity extends TranslatorBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // TODO: perform labor intensive loading that needs to happen before the app starts here.

        startMainActivity();
    }

    /**
     * Continues to the splash screen where local resources will be loaded
     */
    private void startMainActivity() {
        Intent splashIntent = new Intent(this, MainActivity.class);
        startActivity(splashIntent);
        finish();
    }
}
