// IClientService.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.StatusInfo;
import com.tks.uwsclientwearos.IOnUwsInfoChangeListner;
import com.tks.uwsclientwearos.IOnServiceStatusChangeListner;
import com.tks.uwsclientwearos.IStartCheckClearedCallback;

interface IClientService {
	StatusInfo getServiceStatus();
	void setListners(IOnUwsInfoChangeListner onUwsInfoChangeListner, IOnServiceStatusChangeListner onServiceStatusChangeListner);
	void notifyStartCheckCleared();
	int startBt(int seekerid, in BluetoothDevice btServer);
	void stopBt();
	/* UwsHeartBeatService向け */
	void setNotifyStartCheckCleared(IStartCheckClearedCallback cb);
	void notifyHeartBeat(int heartbeat);
}
