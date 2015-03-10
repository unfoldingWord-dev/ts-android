package com.door43.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by joel on 1/9/2015.
 */
public class Security {

    /**
     * Generates an md5 hash of a string
     * @param s
     * @return
     */
    public static String md5(String s){
        MessageDigest encrypter = null;
        try {
            encrypter = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        encrypter.update(s.getBytes(), 0, s.length());
        String md5 = new BigInteger(1, encrypter.digest()).toString(16);
        while ( md5.length() < 32 ) {
            md5 = "0"+md5;
        }
        return md5;
    }
}
