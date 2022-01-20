// IOnStatusChangeListner.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.UwsInfo;

interface IOnUwsInfoListner {
	void onUwsInfoResult(in UwsInfo uwsinfo);
}