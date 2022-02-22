// IOnUwsInfoChangeListner.aidl
package com.tks.uwsclient;

interface IOnUwsInfoChangeListner {
	void onLocationResultChange(in Location location);
	void onHeartbeatResultChange(int heartbeat);
}
