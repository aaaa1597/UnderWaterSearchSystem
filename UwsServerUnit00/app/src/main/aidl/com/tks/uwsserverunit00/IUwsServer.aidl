// IUwsServer.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.IUwsScanCallback;

interface IUwsServer {
    int initBle();
    int startScan(IUwsScanCallback callback);	/* 常に後勝ち */
    void stopScan();
}
