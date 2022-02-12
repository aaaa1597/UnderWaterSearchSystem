// IOnServiceStatusChangeListner.aidl
package com.tks.uwsclientwearos;
import com.tks.uwsclientwearos.StatusInfo;

interface IOnServiceStatusChangeListner {
	void onServiceStatusChange(in StatusInfo statusInfo);
}