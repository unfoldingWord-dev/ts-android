package com.door43.translationstudio;

import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.FileUtilities;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by joel on 2/25/2015.
 */
public class Util {
    /**
     * Utiltiy to load a signature
     * @param sig
     * @return
     * @throws Exception
     */
    public static String loadSig(String sig) throws Exception {
        InputStream sigStream = AppContext.context().getAssets().open(sig);
        String sigJson = FileUtilities.convertStreamToString(sigStream);
        JSONArray json = new JSONArray(sigJson);
        JSONObject sigObj = json.getJSONObject(0);
        return sigObj.getString("sig");
    }
}
