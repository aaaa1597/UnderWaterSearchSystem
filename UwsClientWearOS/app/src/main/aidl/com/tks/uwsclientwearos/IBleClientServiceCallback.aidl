// IBleClientServiceCallback.aidl
package com.tks.uwsclientwearos;

interface IBleClientServiceCallback {
	void notifyAdvertising(int ret);
	double getLongitude();	/* 経度 */
	double getLatitude();	/* 緯度 */
	int getHeartbeat();		/* 脈拍 */
}