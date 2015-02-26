package com.door43.util.signing;

/**
 * Indicates the verification status of Signing Identities and signatures.
 */
public enum Status {
    VERIFIED(0), // everything is ok
    EXPIRED(1), // everything is ok, but the SE expired
    ERROR(2), // something went wrong durring the verification.
    FAILED(3); // the data was tampered with

    private final int mWeight;

    Status(int weight) {
        mWeight = weight;
    }

    /**
     * Returns the weight of this status
     * @return
     */
    public int weight() {
        return mWeight;
    }
}
