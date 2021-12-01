// IBleServiceCallback.aidl
package com.tks.uws.blecentral;

interface IBleServiceCallback {
    void notifyScanResultlist();
    void notifyScanResult();
    void notifyError(int errcode, String errmsg);
}
