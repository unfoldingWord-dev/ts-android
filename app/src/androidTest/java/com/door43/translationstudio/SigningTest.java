package com.door43.translationstudio;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.util.signing.Crypto;
import com.door43.util.signing.SigningEntity;
import com.door43.util.signing.Status;

import java.io.InputStream;
import java.security.PublicKey;

/**
 * Created by joel on 2/25/2015.
 */
public class SigningTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private PublicKey mCA;
    private SigningEntity mSE;
    private byte[] mData;

    public SigningTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        InputStream caPubKey = AppContext.context().getAssets().open("certs/ca.pub");
        mCA = Crypto.loadPublicECDSAKey(caPubKey);

        InputStream verifiedStream = AppContext.context().getAssets().open("tests/signing/si/verified.pem");
        mSE = SigningEntity.generateFromIdentity(mCA, verifiedStream);

        InputStream dataStream = AppContext.context().getAssets().open("tests/signing/data.json");
        mData = Crypto.readInputStreamToBytes(dataStream);
    }

    public void testLoadPublicECDSAKey() throws Exception {
        assertNotNull(mCA);
    }

    public void testLoadSigningIdentity() throws Exception {
        assertNotNull(mSE);
    }

    public void testVerifySigningEntity() throws Exception {
        assertEquals(Status.VERIFIED, mSE.getStatus());

        InputStream failedStream = AppContext.context().getAssets().open("tests/signing/si/failed.pem");
        SigningEntity failed = SigningEntity.generateFromIdentity(mCA, failedStream);
        assertEquals(Status.FAILED, failed.getStatus());

        InputStream expiredStream = AppContext.context().getAssets().open("tests/signing/si/expired.pem");
        SigningEntity expired = SigningEntity.generateFromIdentity(mCA, expiredStream);
        assertEquals(Status.EXPIRED, expired.getStatus());

        InputStream errorStream = AppContext.context().getAssets().open("tests/signing/si/error.pem");
        SigningEntity error = SigningEntity.generateFromIdentity(mCA, errorStream);
        assertEquals(Status.ERROR, error.getStatus());
    }

    public void testVerifySignature() throws Exception {
        Status verified = mSE.verifyContent(Util.loadSig("tests/signing/sig/verified.sig"), mData);
        assertEquals(Status.VERIFIED, verified);

        Status failed = mSE.verifyContent(Util.loadSig("tests/signing/sig/failed.sig"), mData);
        assertEquals(Status.FAILED, failed);

        Status error = mSE.verifyContent(Util.loadSig("tests/signing/sig/error.sig"), mData);
        assertEquals(Status.ERROR, error);

        Status expired = mSE.verifyContent(Util.loadSig("tests/signing/sig/expired.sig"), mData);
        assertEquals(Status.EXPIRED, expired);
    }
}
