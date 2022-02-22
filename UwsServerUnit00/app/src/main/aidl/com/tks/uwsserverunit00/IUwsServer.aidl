// IUwsServer.aidl
package com.tks.uwsserverunit00;
import com.tks.uwsserverunit00.IHearbertChangeListner;
import com.tks.uwsserverunit00.ILocationChangeListner;
import com.tks.uwsserverunit00.IStatusNotifier;

interface IUwsServer {
	void setListners(IHearbertChangeListner hGb, ILocationChangeListner lGb, IStatusNotifier sCb);
	void notifyStartCheckCleared();
}
