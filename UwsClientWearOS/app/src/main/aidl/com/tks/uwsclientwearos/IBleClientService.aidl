// IBleClientService.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.IBleClientServiceCallback;

interface IBleClientService {
	void setCallback(IBleClientServiceCallback callback);	/* 常に後勝ち */
	int initBle();
	void startAdvertising(int seekerid, float difflong, float difflat, int heartbeat);
	void stopAdvertising();
}