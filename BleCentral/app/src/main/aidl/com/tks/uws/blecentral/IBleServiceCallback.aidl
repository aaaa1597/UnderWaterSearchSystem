// IBleServiceCallback.aidl
package com.tks.uws.blecentral;

interface IBleServiceCallback {
    void onItemAdded(String name);
    void notifyScanResultlist();
    void notifyScanResult();
    void notifyError(int errcode, String errmsg);
}
