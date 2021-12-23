// IBleServerServiceCallback.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.DeviceInfo;

interface IBleServerServiceCallback {
    /* Scan処理 */
    void notifyDeviceInfolist(in List<DeviceInfo> devices);
    void notifyDeviceInfo(in DeviceInfo device);
    /* 接続処理 */
    void notifyGattConnected(String shortUuid, String Address);
    void notifyGattDisConnected(String shortUuid, String Address);
    void notifyServicesDiscovered(String shortUuid, String Address, int status);
    void notifyApplicable(String shortUuid, String Address, boolean status);
    void notifyWaitforRead(String shortUuid, String Address, boolean status);
    void notifyResRead(String shortUuid, String Address, long ldatetime, double longitude, double latitude, int heartbeat, int status);
}