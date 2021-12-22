// IBleServerService.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.IBleServerServiceCallback;

interface IBleServerService {
    void setCallback(IBleServerServiceCallback callback);	/* 常に後勝ち */
    int initBle();
    /* Scan処理 */
    int startScan();
    int stopScan();
    /* 接続処理 */
    int connectDevice(String deviceAddress);
    void disconnectDevice(String deviceAddress);
}
