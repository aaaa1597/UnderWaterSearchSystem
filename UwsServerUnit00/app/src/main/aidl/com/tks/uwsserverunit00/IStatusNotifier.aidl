// IStatusNotifier.aidl
package com.tks.uwsserverunit00;

interface IStatusNotifier {
    void OnChangeStatus(String name, String address, int resourceid);
}
