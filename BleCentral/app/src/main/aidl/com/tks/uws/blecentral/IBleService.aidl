// IBleService.aidl
package com.tks.uws.blecentral;
import com.tks.uws.blecentral.IBleServiceCallback;

interface IBleService {
    void setCallback(IBleServiceCallback callback);	/* 常に後勝ち */
    void setAddStr(String str);
    int initBle();
    int startScan();
    int stopScan();
    List<ScanResult> getScanResultlist();
    ScanResult getScanResult();
}
