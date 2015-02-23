package com.door43.signing;

import android.util.Base64;

import com.door43.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

/**
 * A Signing Identity represents an organization that has been registered to sign content.
 *
 */
public class SigningEntity {
    private final PublicKey mCAPublicKey;
    private final PublicKey mPublicKey;
    public final Organization organization;
    private final byte[] mData;
    private final byte[] mSignature;
    private Crypto.Status mStatus;

    /**
     * Creates a new Signing Entity
     * @param caPublicKey The Certificate Authority's public key
     * @param publicKey The Signing Entity's public key
     * @param organization The entity's organization info
     * @param keyOrgData The concated public key and organization data
     * @param keyOrgSig The signature of the entity's concated public key and organization
     */
    public SigningEntity(PublicKey caPublicKey, PublicKey publicKey, Organization organization, byte[] keyOrgData, byte[] keyOrgSig) {
        mCAPublicKey = caPublicKey;
        mPublicKey = publicKey;
        mSignature = keyOrgSig;
        // this techncially duplicates the key and org data, but we pass it along so we don't convert everything to bytes again
        // and possibly introduce additional points of error
        mData = keyOrgData;
        this.organization = organization;
    }

    /**
     * Checks the validation status of this Signing Entity
     * @return
     */
    public Crypto.Status getStatus() {
        if(mStatus == null) {
            // TODO: we need to ouse the root public key
            mStatus = Crypto.verifyECDSASignature(mCAPublicKey, mSignature, mData);
        }
        return mStatus;
    }

    /**
     * Checks the validation status of the signed content.
     * @param signature The signature of the data as signed by the SE
     * @param data The data that will be validated against the signature (the source translation)
     * @return
     */
    public Crypto.Status verifyContent(InputStream signature,  byte[] data) {
        // TODO: impliment the verification of signed content
        return Crypto.Status.FAILED;
    }

    /**
     * Generates a new signing entity from the signing identity
     * @param caPublicKey The The Certificate Authority's public key
     * @param signingIdentity An input stream to the Signing Identity
     * @return
     */
    public static SigningEntity generateFromIdentity(PublicKey caPublicKey, InputStream signingIdentity) {
        BufferedReader br = new BufferedReader(new InputStreamReader(signingIdentity));
        StringBuilder pkBuilder = new StringBuilder();
        StringBuilder orgBuilder = new StringBuilder();
        StringBuilder sigBuilder = new StringBuilder();
        StringBuilder dataBuilder = new StringBuilder();

        // read Signing Identity
        try {
            String section = null;
            String line;
            while((line = br.readLine()) != null) {
                if(line.startsWith("-----")) {
                    // start/end section
                    section = line;
                } else if(!line.trim().isEmpty()){
                    // build sections
                    if(section.equals("-----BEGIN PUBLIC KEY-----")) {
                        pkBuilder.append(line.trim());
                    } else if(section.equals("-----BEGIN ORG INFO-----")) {
                        orgBuilder.append(line.trim());
                    } else if(section.equals("-----BEGIN SIG-----")) {
                        sigBuilder.append(line.trim());
                    }
                }

                // store everything but the signature for verification
                if(!section.equals("-----BEGIN SIG-----") && !section.equals("-----END SIG-----")) {
                    // TRICKY: we intentionally close with a trailing new line
                    dataBuilder.append(line + "\n");
                }
            }
        } catch (IOException e) {
            Logger.e(SigningEntity.class.getName(), "Failed to read the Signing Identity", e);
            return null;
        }

        // Assemble Signing Entity
        if(dataBuilder.length() > 0 && pkBuilder.length() > 0 && orgBuilder.length() > 0 && sigBuilder.length() > 0) {
            byte[] keyOrgData;
            try {
                keyOrgData = dataBuilder.toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                Logger.e(SigningEntity.class.getName(), "Failed to concat Signing Entity key and organization", e);
                return null;
            }
            PublicKey key = Crypto.loadPublicECDSAKey(pkBuilder.toString());
            byte[] signature = Base64.decode(sigBuilder.toString(), Base64.NO_WRAP);
            String orgJsonString = new String(Base64.decode(orgBuilder.toString(), Base64.NO_WRAP));
            Organization org = Organization.generate(orgJsonString);
            if(org != null) {
                return new SigningEntity(caPublicKey, key, org, keyOrgData, signature);
            }
        }
        return null;
    }
}
