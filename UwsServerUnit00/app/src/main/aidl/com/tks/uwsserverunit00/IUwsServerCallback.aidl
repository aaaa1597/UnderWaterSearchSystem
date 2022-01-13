// IUwsServerServiceCallback.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.DeviceInfo;

interface IUwsServerServiceCallback {
    /* Scan処理 */
    void notifyDeviceInfolist(in List<DeviceInfo> devices);
    void notifyDeviceInfo(in DeviceInfo device);
}