// ILocationChangeListner.aidl
package com.tks.uwsserverunit00;

interface ILocationChangeListner {
    void OnChange(int seekerid, String name, String addr, long datetime, in Location loc);
}
