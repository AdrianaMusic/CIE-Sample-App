package com.cie.cieprinter.loopedlabs;

public interface FragmentMessageListener {
    public void onAppSignal(int iAppSignal);

    public void onAppSignal(int iAppSignal, String data);

    public void onAppSignal(int iAppSignal, boolean data);

    public void onAppSignal(int iAppSignal, byte[] data);

}
