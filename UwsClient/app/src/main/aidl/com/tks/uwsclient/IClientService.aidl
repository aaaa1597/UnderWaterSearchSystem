// IClientService.aidl
package com.tks.uwsclient;
import com.tks.uwsclient.StatusInfo;
import com.tks.uwsclient.IOnStatusChangeListner;

interface IClientService {
	StatusInfo getServiceStatus();
	int startUws(int seekerid, IOnStatusChangeListner listner);
	void stopUws();
}
