// IBleServerServiceCallback.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.DeviceInfo;

interface IBleServerServiceCallback {
    /* Scan処理 */
    void notifyDeviceInfolist(in List<DeviceInfo> devices);
    void notifyDeviceInfo(in DeviceInfo device);
    /* 接続処理 */
    void notifyGattConnected(String Address);
    void notifyGattDisConnected(String Address);
    void notifyServicesDiscovered(String Address, int status);
    void notifyApplicable(String Address, boolean status);
    void notifyWaitforRead(String Address, boolean status);
    void notifyResRead(String Address, long ldatetime, double longitude, double latitude, int heartbeat, int status);
}