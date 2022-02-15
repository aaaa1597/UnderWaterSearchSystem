// IClientService.aidl
package com.tks.uwsclient;
import com.tks.uwsclient.StatusInfo;
import com.tks.uwsclient.IOnUwsInfoChangeListner;
import com.tks.uwsclient.IStatusNotifier;
import com.tks.uwsclient.IStartCheckClearedCallback;

interface IClientService {
	StatusInfo getServiceStatus();
	void setListners(IOnUwsInfoChangeListner onUwsInfoChangeListner, IStatusNotifier onServiceStatusChangeListner);
	void notifyStartCheckCleared();
	int startBt(int seekerid, in BluetoothDevice btServer);
	void stopBt();
	/* UwsHeartBeatService向け */
	void setNotifyStartCheckCleared(IStartCheckClearedCallback cb);
	void notifyHeartBeat(int heartbeat);
}
