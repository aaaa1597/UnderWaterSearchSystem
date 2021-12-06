// IBleService.aidl
package com.tks.uws.blecentral;
import com.tks.uws.blecentral.IBleServiceCallback;
import com.tks.uws.blecentral.DeviceInfo;

interface IBleService {
    void setCallback(IBleServiceCallback callback);	/* 常に後勝ち */
    int initBle();
    int startScan();
    int stopScan();
    List<DeviceInfo> getDeviceInfolist();
    DeviceInfo getDeviceInfo();
    int connectDevice(String deviceAddress);
    void clearDevice();
}
