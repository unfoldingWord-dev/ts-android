package com.door43.util.signing;

/**
 * Created by joel on 2/23/2015.
 */
public enum Status {
    VERIFIED, // everything is ok
    EXPIRED, // everything is ok, but the SI expired
    FAILED, // the data was tampered with
    ERROR // something went wrong durring the verification.
}
