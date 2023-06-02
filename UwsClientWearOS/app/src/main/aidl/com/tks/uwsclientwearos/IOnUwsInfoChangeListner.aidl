// IOnUwsInfoChangeListner.aidl
package com.tks.uwsclientwearos;

interface IOnUwsInfoChangeListner {
	void onLocationResultChange(in Location location);
	void onHeartbeatResultChange(int heartbeat);
}
