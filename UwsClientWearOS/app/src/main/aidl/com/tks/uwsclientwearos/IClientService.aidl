// IClientService.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.StatusInfo;
import com.tks.uwsclientwearos.IOnStatusChangeListner;
import com.tks.uwsclientwearos.IOnUwsInfoListner;

interface IClientService {
	StatusInfo getServiceStatus();
	int startUws(int seekerid, IOnUwsInfoListner onUwsInfoListner, IOnStatusChangeListner listner);
	void stopUws();
	/* UwsHeartBeatService向け */
	void notifyHeartBeat(int heartbeat);
}
