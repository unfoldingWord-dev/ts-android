package com.door43.translationstudio.util;

import android.app.Activity;

import com.door43.translationstudio.MainApplication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * This is sort of a hack to provide the main application context to all classes
 * without having to manually pass the context to them.
 */
public class MainContextLink {
    private static final String IMAGE_REQUEST_HASH = "http://www.gravatar.com/avatar/%s?s=40";
    private static MainApplication mContext;

    /**
     * Initializes the basic functions context.
     * @param context The application context. This can only be set once.
     */
    public MainContextLink(MainApplication context) {
        if(mContext == null) {
            mContext = context;
        }
    }

    /**
     * Returns the main application context
     * @return
     */
    public static MainApplication getContext() {
        return mContext;
    }

    // TODO: place all of these methods in the main application file

    public static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; ++i) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            MainContextLink.getContext().showException(e);
        }
        return "";
    }

    public static String buildGravatarURL(String email) {
        String hash = md5(email);
        String url = String.format(Locale.getDefault(), IMAGE_REQUEST_HASH,
                hash);
        return url;
    }
}
