// IClientService.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.StatusInfo;
import com.tks.uwsclientwearos.IOnUwsInfoChangeListner;

interface IClientService {
	StatusInfo getServiceStatus();
	void setOnUwsInfoChangeListner(IOnUwsInfoChangeListner onUwsInfoChangeListner);
	int startBt(int seekerid);
	void stopBt();
	/* UwsHeartBeatService向け */
	void notifyHeartBeat(int heartbeat);
}
