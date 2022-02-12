// IClientService.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.StatusInfo;
import com.tks.uwsclientwearos.IOnUwsInfoChangeListner;

interface IClientService {
	StatusInfo getServiceStatus();
	void setOnUwsInfoChangeListner(IOnUwsInfoChangeListner onUwsInfoChangeListner);
	int startUws(int seekerid);
	void stopUws();
	/* UwsHeartBeatService向け */
	void notifyHeartBeat(int heartbeat);
}
