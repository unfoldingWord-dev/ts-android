package com.door43.translationstudio.security;

import android.util.Base64;

import com.door43.translationstudio.util.Logger;

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
import java.util.ArrayList;
import java.util.List;

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

    public static enum Status {
        VERIFIED, // everything is ok
        FAILED, // the data was tampered with
        ERROR // something went wrong durring the verification.
    }

    /**
     * Verifies a signature
     * @deprecated use verifyECDSASignature(PubliKey, byte[], byte[]) instead
     * @param publicKeyFile the public key
     * @param signatureFile the signature
     * @param dataFile the data on which the signature was made
     * @return the verification status
     */
    public Status verifyECDSASignature(File publicKeyFile, File signatureFile, File dataFile) {
        String sigAlgorithm = "SHA384WITHECDSA";
        PublicKey pub = loadPublicECKey(publicKeyFile);
        byte[] data = readFileToBytes(dataFile);
        byte[] sig = readFileToBytes(signatureFile);

        if(pub != null && data.length > 0 && sig.length > 0) {
            try {
                Signature signature = Signature.getInstance(sigAlgorithm);
                signature.initVerify(pub);
                signature.update(data);
                if(signature.verify(sig)) {
                    return Status.VERIFIED;
                } else {
                    return Status.FAILED;
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "failed to verify the signature", e);
            }
        }
        return Status.ERROR;
    }

    /**
     * Verifies the signature of some data
     * @param key the public key
     * @param sig the signature
     * @param data the data to verify the signature against
     * @return
     */
    public static Status verifyECDSASignature(PublicKey key, byte[] sig, byte[] data) {
        // TODO: we may want to change this to SHA384WITHECDSA if we decide the iOS code can support it.
        String sigAlgorithm = "SHA1WITHECDSA";

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
                Logger.e(Crypto.class.getName(), "failed to verify the signature", e);
            }
        }
        return Status.ERROR;
    }

    /**
     * Reads a public ECDSA key from a file
     * @deprecated use loadPublicECKey(InputStream) instead
     * @param publicKey
     * @return
     */
    public PublicKey loadPublicECKey(File publicKey) {
        // read key from file
        InputStream is;
        try {
            is = new FileInputStream(publicKey);
        } catch (FileNotFoundException e) {
            Logger.e(this.getClass().getName(), "the public key could not be found", e);
            return null;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        List<String> lines = new ArrayList<>();
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to read the public key file", e);
            return null;
        }

        // removes the first and last lines of the file (comments)
        if (lines.size() > 1 && lines.get(0).startsWith("-----") && lines.get(lines.size()-1).startsWith("-----")) {
            lines.remove(0);
            lines.remove(lines.size()-1);
        }

        // concats the remaining lines to a single String
        StringBuilder sb = new StringBuilder();
        for (String aLine: lines)
            sb.append(aLine);
        String keyString = sb.toString();

        // converts the String to a PublicKey instance
        byte[] keyBytes;
        try {
            keyBytes = Base64.decode(keyString.getBytes("UTF-8"), Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            Logger.e(this.getClass().getName(), "wrong encoding for key decoding", e);
            return null;
        }
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("ECDSA", "SC");
        } catch (NoSuchAlgorithmException e) {
            Logger.e(this.getClass().getName(), "The public key algorithm is invalid", e);
            return null;
        } catch (NoSuchProviderException e) {
            Logger.e(this.getClass().getName(), "The public key security provider is invalid", e);
            return null;
        }
        PublicKey key;
        try {
            key = keyFactory.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            Logger.e(this.getClass().getName(), "The public key keyspec is invalid", e);
            return null;
        }
        return key;
    }

    /**
     * Loads a public key from an input stream
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
            Logger.e(Crypto.class.getName(), "Failed to read the public key stream", e);
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
     * Loads the public key from a string.
     * The key should not include any comments or new lines
     * @param keyString the public key string
     * @return if an error occures null will be returned
     */
    public static PublicKey loadPublicECDSAKey(String keyString) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.decode(keyString.getBytes("UTF-8"), Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            Logger.e(Crypto.class.getName(), "wrong encoding for key decoding", e);
            return null;
        }
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("ECDSA", "SC");
        } catch (NoSuchAlgorithmException e) {
            Logger.e(Crypto.class.getName(), "The public key algorithm is invalid", e);
            return null;
        } catch (NoSuchProviderException e) {
            Logger.e(Crypto.class.getName(), "The public key security provider is invalid", e);
            return null;
        }
        PublicKey key;
        try {
            key = keyFactory.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            Logger.e(Crypto.class.getName(), "The public key keyspec is invalid", e);
            return null;
        }
        return key;
    }

    /**
     * Reads in bytes from an input stream
     * @deprecated
     * @param is
     * @return
     */
    public byte[] readInputStreamToBytes(InputStream is) {
        byte[] bytes = new byte[0];
        try {
            bytes = new byte[is.available()];
            is.read(bytes);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to read bytes from the stream", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to close the input stream", e);
            }
        }
        return bytes;
    }

    /**
     * Returns bytes from a file
     * If there are exceptions while reading the file an empty byte array will be returned.
     * @deprecated use readInputStreamToBytes instead
     * @param file
     * @return
     */
    public byte[] readFileToBytes(File file) {
        byte[] bytes = new byte[0];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Logger.e(this.getClass().getName(), "the file could not be found", e);
        }
        try {
            bytes = new byte[fis.available()];
            fis.read(bytes);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to read bytes from the file", e);
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to close the file input stream", e);
            }
        }
        return bytes;
    }
}
