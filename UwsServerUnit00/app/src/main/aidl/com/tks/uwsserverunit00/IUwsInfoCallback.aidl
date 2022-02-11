// IUwsServerCallback.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.UwsInfo;

interface IUwsInfoCallback {
    void notifyUwsData(in UwsInfo uwsInfo);
    void notifyStatus(int status);
}
