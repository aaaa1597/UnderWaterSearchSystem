// IBleServiceCallback.aidl
package com.tks.uws.blecentral;

interface IBleServiceCallback {
    void notifyGattConnected(String Address);
    void notifyGattDisConnected(String Address);
    void notifyDeviceInfolist();
    void notifyDeviceInfo();
    void notifyScanEnd();
    void notifyServicesDiscovered(String Address, int status);
    void notifyApplicable(String Address, boolean status);
    void notifyReady2DeviceCommunication(String Address, boolean status);
    void notifyResRead(String Address, int rcvval, int status);
    void notifyFromPeripheral(String Address, int retval);
    void notifyError(int errcode, String errmsg);
}
