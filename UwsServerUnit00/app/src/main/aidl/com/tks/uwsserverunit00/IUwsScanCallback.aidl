// IUwsServerCallback.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.DeviceInfo;

interface IUwsScanCallback {
    void notifyDeviceInfo(in DeviceInfo device);
}
