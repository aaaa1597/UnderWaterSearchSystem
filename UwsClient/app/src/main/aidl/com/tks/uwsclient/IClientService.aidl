// IClientService.aidl
package com.tks.uwsclient;
import com.tks.uwsclient.StatusInfo;
import com.tks.uwsclient.IOnStatusChangeListner;
import com.tks.uwsclient.IOnUwsInfoListner;

interface IClientService {
	StatusInfo getServiceStatus();
	int startUws(int seekerid, IOnUwsInfoListner onUwsInfoListner, IOnStatusChangeListner listner);
	void stopUws();
}
