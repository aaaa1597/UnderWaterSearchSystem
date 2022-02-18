// IHearbertChangeListner.aidl
package com.tks.uwsserverunit00;

interface IHearbertChangeListner {
    void onChange(int seekerid, String name, String addr, long datetime, int hearbeat);
}
