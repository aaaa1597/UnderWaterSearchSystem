// IUwsServer.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.IUwsScanCallback;
import com.tks.uwsserverunit00.IUwsInfoCallback;

interface IUwsServer {
    int initBle();
    int startScan(IUwsScanCallback callback);	/* 常に後勝ち */
    void stopScan();
    int startPeriodicNotify(int seekerid, IUwsInfoCallback callback);
    void stopPeriodicNotify(int seekerid);
}
