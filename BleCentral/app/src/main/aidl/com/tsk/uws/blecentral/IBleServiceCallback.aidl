// IBleServiceCallback.aidl
package com.tsk.uws.blecentral;

interface IBleServiceCallback {
    void onItemAdded(String name);
    void onItemRemoved(String name);
    void notifyScanResultlist();
    void notifyScanResult();
    void notifyError(int errcode, String errmsg);
}
