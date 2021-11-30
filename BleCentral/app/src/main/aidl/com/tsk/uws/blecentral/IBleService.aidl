// IBleService.aidl
package com.tsk.uws.blecentral;
import com.tsk.uws.blecentral.IBleServiceCallback;

interface IBleService {
    boolean addCallback(IBleServiceCallback callback);
    boolean removeCallback(IBleServiceCallback callbac);
    boolean setAddStr(String str);
    boolean setRemoceStr(String str);
}
