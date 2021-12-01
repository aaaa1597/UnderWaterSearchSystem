// IBleServiceCallback.aidl
package com.tks.uws.blecentral;

interface IBleServiceCallback {
    void notifyScanResultlist();
    void notifyScanResult();
    void notifyMsg(int msgid, String msg);
    void notifyError(int errcode, String errmsg);
}
