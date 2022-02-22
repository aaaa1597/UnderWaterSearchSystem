package com.tks.uwsserverunit00.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tks.uwsserverunit00.IUwsServer;
import com.tks.uwsserverunit00.TLog;
import com.tks.uwsserverunit00.ui.DeviceListAdapter.DeviceInfoModel;

import static com.tks.uwsserverunit00.Constants.BT_NORTIFY_CLOSE;
import static com.tks.uwsserverunit00.Constants.BT_NORTIFY_SEEKERID;
import static com.tks.uwsserverunit00.Constants.d2Str;

import android.app.Application;
import android.location.Location;

import java.util.List;

public class FragBleViewModel extends AndroidViewModel {
	private IUwsServer			mUwsServiceIf;
	private DeviceListAdapter	mDeviceListAdapter;
	private Application			mContext;
	private final MutableLiveData<DeviceListAdapter>	mSetDeviceListAdapter = new MutableLiveData<>();
	public MutableLiveData<DeviceListAdapter>			setDeviceListAdapter() { return mSetDeviceListAdapter; }

	public FragBleViewModel(@NonNull Application application) {
		super(application);
		mContext = application;
	}

	/** *************
	 * サービスbind完了
	 * **************/
	public void onServiceConnected(IUwsServer service) {
		mUwsServiceIf = service;
	}

	/** ***********
	 * サービスbind断
	 * ************/
	public void onServiceDisconnected() {
		mUwsServiceIf = null;
	}

	public void setDeviceListAdapter(DeviceListAdapter deviceListAdapter) {
		mDeviceListAdapter = deviceListAdapter;
		mSetDeviceListAdapter.postValue(deviceListAdapter);
	}

	public DeviceListAdapter getDeviceListAdapter() {
		return mDeviceListAdapter;
	}

	public void setBuoy(short seekerid, boolean isChecked) {
		mDeviceListAdapter.setBuoy(seekerid, isChecked);
	}

	/* 脈拍設定 */
	public void setHeartBeat(String name, String addr, long datetime, short hearbeat) {
		mDeviceListAdapter.setHeartBeat(name, addr, datetime, hearbeat);
	}

	/* 経度/緯度設定 */
	public void setLocation(short seekerid, String name, String addr, long datetime, Location loc) {
		mDeviceListAdapter.setLocation(seekerid, name, addr, datetime, loc);
	}

	/* 状態変化通知 */
	public void onChangeStatus(String name, String addr, int resourceid) {
		/* (seekerid通知 or Close通知)の時は、resourceidはseekeridになっている。 */
		if( name.equals(BT_NORTIFY_CLOSE))
			TLog.d(" 状態変化通知 Close通知 seekerid={0}", resourceid);/* Close通知の時は、resourceidはseekeridになっている。 */
		else if( name.equals(BT_NORTIFY_SEEKERID))
			TLog.d(" 状態変化通知 Seekerid通知 seekerid={0}", resourceid);
		else/* (seekerid通知|Close通知)の時は、resourceidはseekeridになっている。 */
			TLog.d(" 状態変化通知 {0}", mContext.getString(resourceid));
		mDeviceListAdapter.OnChangeStatus(name, addr, resourceid);
	}
}
