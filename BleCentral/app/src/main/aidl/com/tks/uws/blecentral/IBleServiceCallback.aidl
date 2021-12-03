// IBleServiceCallback.aidl
package com.tks.uws.blecentral;

interface IBleServiceCallback {
    void notifyGattConnected(String Address);
    void notifyGattDisConnected(String Address);
    void notifyScanResultlist();
    void notifyScanResult();
    void notifyScanEnd();
    void notifyServicesDiscovered(String Address, int status);
    void notifyApplicable(String Address, boolean status);
    void notifyReady2DeviceCommunication(String Address, boolean status);
    void notifyError(int errcode, String errmsg);
}
