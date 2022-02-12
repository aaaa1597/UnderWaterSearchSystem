// IClientService.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.StatusInfo;
import com.tks.uwsclientwearos.IOnUwsInfoListner;

interface IClientService {
	StatusInfo getServiceStatus();
	void setOnUwsInfoChangeListner(IOnUwsInfoListner onUwsInfoListner);
	int startUws(int seekerid);
	void stopUws();
	/* UwsHeartBeatService向け */
	void notifyHeartBeat(int heartbeat);
}
