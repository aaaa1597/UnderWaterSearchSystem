// IStatusNotifier.aidl
package com.tks.uwsclient;
import com.tks.uwsclient.StatusInfo;

interface IStatusNotifier {
	void onStatusChange(int statusid);
}