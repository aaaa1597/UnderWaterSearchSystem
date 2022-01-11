// IUwsServer.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.IUwsServerCallback;

interface IUwsServer {
    void setCallback(IUwsServerCallback callback);	/* 常に後勝ち */
    int initBle();
    /* Scan処理 */
    int startScan();
    int stopScan();
}
