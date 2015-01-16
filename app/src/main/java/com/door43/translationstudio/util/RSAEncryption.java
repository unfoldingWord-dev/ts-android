package com.door43.translationstudio.util;

import android.util.Base64;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.Cipher;

/**
 *
 * @author Anuj
 * Blog www.goldenpackagebyanuj.blogspot.com
 * RSA - Encrypt Data using Public Key
 * RSA - Descypt Data using Private Key
 */
public class RSAEncryption {

    /**
     * Generates a set of private and public keys
     * @param privateKeyFile the private key file
     * @param publicKeyFile the public key file
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws IOException
     */
    public static void generateKeys(File privateKeyFile, File publicKeyFile) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // pull out parameters which makes up Key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec rsaPubKeySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
        RSAPrivateKeySpec rsaPrivKeySpec = keyFactory.getKeySpec(privateKey, RSAPrivateKeySpec.class);

        // save keys
        saveKeys(publicKeyFile.getAbsolutePath(), rsaPubKeySpec.getModulus(), rsaPubKeySpec.getPublicExponent());
        saveKeys(privateKeyFile.getAbsolutePath(), rsaPrivKeySpec.getModulus(), rsaPrivKeySpec.getPrivateExponent());
    }

    /**
     * Save Files
     * @param fileName
     * @param mod
     * @param exp
     * @throws IOException
     */
    private static void saveKeys(String fileName,BigInteger mod,BigInteger exp) throws IOException{
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream(fileName);
            oos = new ObjectOutputStream(new BufferedOutputStream(fos));

            oos.writeObject(mod);
            oos.writeObject(exp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            if(oos != null){
                oos.close();

                if(fos != null){
                    fos.close();
                }
            }
        }
    }

    /**
     * Encrypt Data
     * @param data
     * @throws IOException
     */
    public static byte[] encryptData(String data, PublicKey pubKey) throws IOException {
        byte[] dataToEncrypt = data.getBytes();
        byte[] encryptedData = null;
        try {
//            PublicKey pubKey = readPublicKeyFromFile(pubFile.getAbsolutePath());
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            encryptedData = cipher.doFinal(dataToEncrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedData;
    }

    /**
     * Encrypt Data
     * @param data
     * @throws IOException
     */
    public static String decryptData(byte[] data, PrivateKey privateKey) throws IOException {
        try {
//            PrivateKey privateKey = readPrivateKeyFromFile(privFile.getAbsolutePath());
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts the public key to a string
     * @param key
     * @return
     */
    public static String getPublicKeyAsString(PublicKey key) throws Exception {
        // pull out parameters which makes up Key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec rsaPubKeySpec = keyFactory.getKeySpec(key, RSAPublicKeySpec.class);

        // save as string
        String modulus = new String(Base64.encode(rsaPubKeySpec.getModulus().toByteArray(), Base64.NO_WRAP));
        String exponent = new String(Base64.encode(rsaPubKeySpec.getPublicExponent().toByteArray(), Base64.NO_WRAP));

        return modulus+"<split>"+exponent;
    }

    /**
     * Creates a public key from a string
     * @param keyString
     * @return
     */
    public static PublicKey getPublicKeyFromString(String keyString) {
        String[] pieces = keyString.split("<split>");
        if(pieces.length == 2) {
            BigInteger modulus = new BigInteger(Base64.decode(pieces[0].getBytes(), Base64.NO_WRAP));
            BigInteger exponent = new BigInteger(Base64.decode(pieces[1].getBytes(), Base64.NO_WRAP));

            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
            try {
                KeyFactory fact = KeyFactory.getInstance("RSA");
                PublicKey publicKey = fact.generatePublic(rsaPublicKeySpec);
                return publicKey;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * read Public Key From File
     * @param file
     * @return PublicKey
     * @throws IOException
     */
    public static PublicKey readPublicKeyFromFile(File file) throws IOException{
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);

            BigInteger modulus = (BigInteger) ois.readObject();
            BigInteger exponent = (BigInteger) ois.readObject();

            //Get Public Key
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PublicKey publicKey = fact.generatePublic(rsaPublicKeySpec);

            return publicKey;

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            if(ois != null){
                ois.close();
                if(fis != null){
                    fis.close();
                }
            }
        }
        return null;
    }

    /**
     * read Public Key From File
     * @param file
     * @return
     * @throws IOException
     */
    public static PrivateKey readPrivateKeyFromFile(File file) throws IOException{
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);

            BigInteger modulus = (BigInteger) ois.readObject();
            BigInteger exponent = (BigInteger) ois.readObject();

            //Get Private Key
            RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, exponent);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = fact.generatePrivate(rsaPrivateKeySpec);

            return privateKey;

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            if(ois != null){
                ois.close();
                if(fis != null){
                    fis.close();
                }
            }
        }
        return null;
    }
}