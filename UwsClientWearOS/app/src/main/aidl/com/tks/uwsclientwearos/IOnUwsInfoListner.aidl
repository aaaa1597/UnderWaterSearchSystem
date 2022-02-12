// IOnUwsInfoListner.aidl
package com.tks.uwsclientwearos;

interface IOnUwsInfoListner {
	void onLocationResult(in Location location);
	void onHeartbeatResult(int heartbeat);
}
