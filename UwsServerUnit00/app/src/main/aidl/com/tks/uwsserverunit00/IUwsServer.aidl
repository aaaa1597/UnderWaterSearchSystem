// IUwsServer.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.IUwsScanCallback;
import com.tks.uwsserverunit00.IUwsInfoCallback;
import com.tks.uwsserverunit00.IUwsSystemCallback;

interface IUwsServer {
    int initBle(IUwsSystemCallback cb);			/* 常に後勝ち */
    int startScan(IUwsScanCallback callback);	/* 常に後勝ち */
    void stopScan();
    int startPeriodicNotify(int seekerid, IUwsInfoCallback callback);
    void stopPeriodicNotify(int seekerid);
}
