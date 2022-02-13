// IHearbertChangeListner.aidl
package com.tks.uwsserverunit00;

interface IHearbertChangeListner {
    void OnChange(String name, String addr, long datetime, int hearbeat);
}
