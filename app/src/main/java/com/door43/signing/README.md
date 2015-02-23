The security module provides tools for verifying signed content.
Below is an example of loading and verifying a Signing Entity from it's Signing Identity file.

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

Below is an example of verifying some signed content with the Signing Entity above

TODO: create example.