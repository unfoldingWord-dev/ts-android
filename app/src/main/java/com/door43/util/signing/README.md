The security module provides tools for verifying signed content.
Below is an example of loading and verifying a Signing Entity from it's Signing Identity file as well as verifying some signed content.

```
try {
    // Signing Identity
    InputStream signingIdentity = AppContext.context().getAssets().open("test_certs/uW-vk.pem");
    // Certificate Authority public key
    InputStream caPubKey = AppContext.context().getAssets().open("test_certs/ca.pub");
    PublicKey pk = Crypto.loadPublicECDSAKey(caPubKey);
    SigningEntity se = SigningEntity.generateFromIdentity(pk, signingIdentity);
    switch(se.getStatus()) {
        case VERIFIED:
            Log.d("CRYPTO", "The Signing Entity is valid!");
            Log.d("CRYPTO", se.organization.toString());

            // verify signed content
            InputStream sigStream = AppContext.context().getAssets().open("test_certs/signed_data/obs-en.sig");
            String sigJson = FileUtilities.convertStreamToString(sigStream);
            JSONArray json = new JSONArray(sigJson);
            JSONObject sigObj = json.getJSONObject(0);
            String sig = sigObj.getString("sig");

            InputStream dataStream = AppContext.context().getAssets().open("test_certs/signed_data/obs-en.json");
            byte[] data = Crypto.readInputStreamToBytes(dataStream);

            Status contentStatus = se.verifyContent(sig, data);
            if(contentStatus == Status.VERIFIED) {
                Log.d("CRYPTO", "The content is verified");
            }
            break;
        case FAILED:
            Log.d("CRYPTO", "The Signing Entity is INVALID!");
            break;
        default:
            Log.d("CRYPTO", "The Signing Entity could not be verified.");
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

See the unit tests for more details.