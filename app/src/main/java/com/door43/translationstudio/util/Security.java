package com.door43.translationstudio.util;

import android.util.Base64;
import android.util.Log;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

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
            Logger.e(Security.class.getName(), "failed to generate md5 sum", e);
        }
        encrypter.update(s.getBytes(), 0, s.length());
        String md5 = new BigInteger(1, encrypter.digest()).toString(16);
        while ( md5.length() < 32 ) {
            md5 = "0"+md5;
        }
        return md5;
    }

    /**
     * Encrypts a messages with a public key
     * @param pubKey
     * @param message
     * @return
     */
    public static String rsaEncrypt(String pubKey, String message) {
        try {
            // converts the String to a PublicKey instance
            JSch jsch = new JSch();
            KeyPair pair = KeyPair.load(jsch, MainContext.getContext().getPrivateKey().getAbsolutePath(), MainContext.getContext().getPublicKey().getAbsolutePath());
            byte[] keyBytes = pair.getPublicKeyBlob();


//            byte[] keyBytes = pubKey.getBytes("UTF-8");// Base64.decode(pubKey.getBytes("utf-8"), Base64.URL_SAFE);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey key = keyFactory.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes()); //Base64.decode(message.getBytes("utf-8"), Base64.NO_WRAP));
            return new String(encryptedBytes); //Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
        } catch(Exception e) {
            Logger.e(Security.class.getName(), "Failed to encrypt message", e);
        }
        return "";
    }
}
