// IBleService.aidl
package com.tsk.uws.blecentral;
import com.tsk.uws.blecentral.IBleServiceCallback;

interface IBleService {
    boolean addCallback(IBleServiceCallback callback);
    boolean removeCallback(IBleServiceCallback callback);
    String startScan();	/* 戻り値 : ステータス文字列 */
    String stopScan();	/* 戻り値 : ステータス文字列 */
    List<ScanResult> getScanResultlist();
    ScanResult getScanResult();
//-------------------------
    boolean setAddStr(String str);
    boolean setRemoceStr(String str);
}
