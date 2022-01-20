// IOnStatusChangeListner.aidl
package com.tks.uwsclientwearos;

interface IOnStatusChangeListner {
	void OnStatusChange(int oldStatus, int newStatus);
}