package com.door43.translationstudio.util;

import com.door43.translationstudio.MainApplication;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * This is sort of a hack to provide the main application context to all classes
 * without having to manually pass the context to them.
 */
public class MainContext {
//    private static final String IMAGE_REQUEST_HASH = "http://www.gravatar.com/avatar/%s?s=40";
    private static MainApplication mContext;
    private static MainThreadBus mEventBus;

    /**
     * Initializes the basic functions context.
     * @param context The application context. This can only be set once.
     */
    public MainContext(MainApplication context) {
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

    /**
     * Returns the global event bus
     * @return
     */
    public static Bus getEventBus() {
        if(mEventBus == null) {
            mEventBus = new MainThreadBus(ThreadEnforcer.ANY);
        }
        return mEventBus;
    }

    // TODO: place all of these methods in the main application file

    /**

     * @param s
     * @return
     */
//    public static String md5(final String s) {
//        try {
//            // Create MD5 Hash
//            MessageDigest digest = java.security.MessageDigest
//                    .getInstance("MD5");
//            digest.update(s.getBytes());
//            byte messageDigest[] = digest.digest();
//
//            // Create Hex String
//            StringBuffer hexString = new StringBuffer();
//            for (int i = 0; i < messageDigest.length; ++i) {
//                String h = Integer.toHexString(0xFF & messageDigest[i]);
//                while (h.length() < 2)
//                    h = "0" + h;
//                hexString.append(h);
//            }
//            return hexString.toString();
//
//        } catch (NoSuchAlgorithmException e) {
//            MainContextLink.getContext().showException(e);
//        }
//        return "";
//    }

    /**
     *
     * @param email
     * @return
     */
//    public static String buildGravatarURL(String email) {
//        String hash = md5(email);
//        String url = String.format(Locale.getDefault(), IMAGE_REQUEST_HASH,
//                hash);
//        return url;
//    }
}
