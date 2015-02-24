package com.door43.util.signing;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * This class handles the verification of signatures for uW content.
 */
public class Crypto {
    /**
     * TRICKY: We must use Spongy Castle as the security provider because ECDSA is removed from Bouncy Castle in Android
     * for this we only need the core and prov jars
     * requires https://rtyley.github.io/spongycastle/
     */
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    /**
     * Verifies the signature of some data
     * @param key the public key
     * @param sig the signature
     * @param data the data to verify the signature against
     * @return
     */
    public static Status verifyECDSASignature(PublicKey key, byte[] sig, byte[] data) {
        String sigAlgorithm = "SHA384WITHECDSA";

        if(key != null && data.length > 0 && sig.length > 0) {
            try {
                Signature signature = Signature.getInstance(sigAlgorithm);
                signature.initVerify(key);
                signature.update(data);
                if(signature.verify(sig)) {
                    return Status.VERIFIED;
                } else {
                    return Status.FAILED;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Status.ERROR;
    }

    /**
     * Reads a public ECDSA key from a file
     * The key may contain comments and new lines
     * @param keyFile
     * @return
     */
    public PublicKey loadPublicECDSAKey(File keyFile) {
        try {
            return loadPublicECDSAKey(new FileInputStream(keyFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads a public ECDSA key from an input stream
     * The key may contain comments and new lines
     * @param keyStream
     * @return
     */
    public static PublicKey loadPublicECDSAKey(InputStream keyStream) {
        BufferedReader br = new BufferedReader(new InputStreamReader(keyStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("-----")) {
                    sb.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if(sb.length() > 0) {
            String keyString = sb.toString();
            return loadPublicECDSAKey(keyString);
        } else {
            return null;
        }
    }

    /**
     * Loads a public key from a string.
     * The key should not contain any newlines or comments
     * @param keyString
     * @return
     */
    public static PublicKey loadPublicECDSAKey(String keyString) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.decode(keyString.getBytes("UTF-8"), Base64.DEFAULT);
            return loadPublicECDSAKey(keyBytes);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads the public key from a byte array
     * @param keyBytes the public key
     * @return if an error occures null will be returned
     */
    public static PublicKey loadPublicECDSAKey(byte[] keyBytes) {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("ECDSA", "SC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        }
        PublicKey key;
        try {
            key = keyFactory.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
        return key;
    }

    /**
     * Reads in bytes from an input stream
     * @param is
     * @return
     */
    public static byte[] readInputStreamToBytes(InputStream is) {
        byte[] bytes = new byte[0];
        try {
            bytes = new byte[is.available()];
            is.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }

    /**
     * Returns bytes from a file
     * If there are exceptions while reading the file an empty byte array will be returned.
     * @param file
     * @return
     */
    public byte[] readFileToBytes(File file) {
        try {
            return readInputStreamToBytes(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
