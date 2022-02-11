// IOnUwsInfoListner.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.UwsInfo;

interface IOnUwsInfoListner {
	void onLocationResult(in Location location);
	void onHeartbeatResult(int heartbeat);
}