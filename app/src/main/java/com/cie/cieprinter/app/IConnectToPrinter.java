package com.cie.cieprinter.app;

/**
 * Created by Natarajan on 2/23/2015.
 */
public interface IConnectToPrinter {
    public void connectToPairedPrinter(String address);
    public void connectToUnPairedPrinter(String address);
}
