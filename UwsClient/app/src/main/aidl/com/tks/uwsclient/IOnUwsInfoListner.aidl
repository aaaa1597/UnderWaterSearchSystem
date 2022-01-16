// IOnStatusChangeListner.aidl
package com.tks.uwsclient;
import com.tks.uwsclient.UwsInfo;

interface IOnUwsInfoListner {
	void onUwsInfoResult(in UwsInfo uwsinfo);
}