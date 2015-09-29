package com.door43.translationstudio.newui;

import android.app.Fragment;
import android.content.Intent;

import com.door43.tools.reporting.GlobalExceptionHandler;
import com.door43.translationstudio.SplashScreenActivity;
import com.door43.translationstudio.AppContext;

import java.io.File;

/**
 * This should be extended by all activities in the app so that we can perform verification on
 * activities such as recovery from crashes.
 *
 */
public class BaseFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();

        // check if we crashed
        File dir = new File(getActivity().getExternalCacheDir(), AppContext.context().STACKTRACE_DIR);
        String[] files = GlobalExceptionHandler.getStacktraces(dir);
        if (files.length > 0) {
            // restart
            Intent intent = new Intent(getActivity(), SplashScreenActivity.class);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
