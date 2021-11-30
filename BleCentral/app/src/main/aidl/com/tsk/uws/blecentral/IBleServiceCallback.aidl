// IBleServiceCallback.aidl
package com.tsk.uws.blecentral;

interface IBleServiceCallback {
    void onItemAdded(String name);
    void onItemRemoved(String name);
}
