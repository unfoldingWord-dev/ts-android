package com.door43.translationstudio.util;


import android.app.Fragment;

import com.door43.translationstudio.MainApplication;

/**
 * Created by joel on 8/27/2014.
 */
public class TranslatorBaseFragment extends Fragment {

    /**
     * Returns the context of the fragment
     * @return
     */
    protected MainApplication app() {
        return AppContext.context();
    }

    public void onResume() {
        super.onResume();
//        MainContext.getEventBus().register(this);
    }

    public void onPause() {
        // don't receive events when in the background
//        MainContext.getEventBus().unregister(this);
        super.onPause();
    }

    public void onStart() {
        super.onStart();
        AppContext.getEventBus().register(this);
    }

    public void onStop() {
        AppContext.getEventBus().unregister(this);
        super.onStop();
    }

    public void onDestroy() {
//        MainContext.getEventBus().unregister(this);
        super.onDestroy();
    }

}
