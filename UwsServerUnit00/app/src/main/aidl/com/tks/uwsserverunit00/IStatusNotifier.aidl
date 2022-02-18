// IStatusNotifier.aidl
package com.tks.uwsserverunit00;

interface IStatusNotifier {
    void onChangeStatus(String name, String address, int resourceid);
}
