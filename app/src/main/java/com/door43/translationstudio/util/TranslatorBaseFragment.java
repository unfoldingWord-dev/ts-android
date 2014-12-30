package com.door43.translationstudio.util;


import android.app.Fragment;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.MainActivity;
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
        return MainContext.getContext();
    }

    public void onResume() {
        super.onResume();
        MainContext.getEventBus().register(this);
    }



    public void onDestroy() {
        MainContext.getEventBus().unregister(this);
        super.onDestroy();
    }

}
