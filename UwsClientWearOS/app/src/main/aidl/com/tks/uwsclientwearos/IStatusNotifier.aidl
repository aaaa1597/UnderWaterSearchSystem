// IStatusNotifier.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.StatusInfo;

interface IStatusNotifier {
	void onStatusChange(int statusid);
}