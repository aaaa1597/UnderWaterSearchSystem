// IOnStatusChangeListner.aidl
package com.tks.uwsclient;

interface IOnStatusChangeListner {
	void OnStatusChange(int oldStatus, int newStatus);
}